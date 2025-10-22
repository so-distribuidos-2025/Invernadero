import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Main {

    /**
     * Método principal del programa.
     *
     * @param args Argumentos de línea de comandos.
     *             El primer argumento (args[0]) corresponde al identificador del sensor.
     */
    public static void main(String[] args) {
        // Identificador de las parcelas, pasado como argumento
        String id = args[0];

        InetAddress ipServidor = null;
        PrintWriter pw;

        try {
            String controladorHost = System.getenv("CONTROLADOR_HOST");
            if (controladorHost == null) {
                controladorHost = "localhost";
            }
            String controladorPort = System.getenv("CONTROLADOR_PORT");
            if (controladorPort == null) {
                controladorPort = "20000";
            }
            ipServidor = InetAddress.getByName(controladorHost);
            Socket cliente = new Socket(ipServidor, Integer.parseInt(controladorPort));
            System.out.println("Conectado al servidor: " + cliente);

            // Flujo de salida con autoflush activado para enviar datos
            pw = new PrintWriter(cliente.getOutputStream(), true);

            // Enviar tipo de dispositivo e identificador al servidor
            pw.println("humedad");
            pw.println(id);

            // Crear e iniciar el hilo que simula el sensado de humedad
            HiloSensado sensor = new HiloSensado(cliente, pw);
            sensor.start();

            // --- Lógica RMI ---
            String sensorHostname = System.getenv("HOSTNAME");
            if (sensorHostname == null) {
                sensorHostname = "localhost";
            }
            String envPort = System.getenv("PORT");
            if (envPort == null) {
                envPort = "22000";
            }
            int port = Integer.parseInt(envPort);

            String name = "SensorHumedadRMI" + id;
            HiloServerRMI hiloServerRMI = new HiloServerRMI(sensor);

            try {
                LocateRegistry.createRegistry(port);
                System.out.println("RMI registry created on port " + port);
            } catch (RemoteException e) {
                System.out.println("RMI registry already running on port " + port);
            }

            Naming.rebind("rmi://" + sensorHostname + ":" + port + "/" + name, hiloServerRMI);
            System.out.println("rmi://" + sensorHostname + ":" + port + "/" + name + " bound in registry");

        } catch (UnknownHostException e) {
            throw new RuntimeException("No se pudo resolver el host del servidor", e);
        } catch (IOException e) {
            throw new RuntimeException("Error de E/S en la conexión con el servidor", e);
        }
    }
}

