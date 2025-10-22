import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase principal SensorTemperatura
 * ---------------------------------
 * Este programa representa el cliente que simula un sensor de temperatura.
 * <p>
 * Funcionalidades:
 * - Establece una conexión con un servidor en un puerto específico.
 * - Envía un mensaje inicial identificándose como "temperatura".
 * - Inicia un hilo (HiloSensado) que genera y transmite valores de
 * temperatura de forma periódica.
 */
public class Main {

    /**
     * Método principal. Crea la conexión con el servidor, prepara el flujo
     * de salida, envía la identificación del sensor, y arranca el hilo de sensado.
     *
     * @param args Argumentos de línea de comando (no utilizados).
     */
    public static void main(String[] args) {
        InetAddress ipServidor = null;   // Dirección IP del servidor
        PrintWriter pw;                  // Flujo de salida para enviar datos

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

            // Prepara el PrintWriter con autoflush activado para enviar datos
            pw = new PrintWriter(cliente.getOutputStream(), true);

            // Envía un mensaje inicial indicando el tipo de sensor
            pw.println("temperatura");

            // Crea e inicia el hilo que simula el sensor de temperatura
            HiloSensado sensor = new HiloSensado(cliente, pw);

            sensor.start();

            //Clase unicast para mandar los datos
            HiloServerRMI hiloServerRMI = new HiloServerRMI(sensor);

            try {
                String name = "SensorTemperaturaRMI";
                String sensorHostname = System.getenv("HOSTNAME");
                if (sensorHostname == null) {
                    sensorHostname = "localhost";
                }
                String envPort = System.getenv("PORT");
                if (envPort == null) {
                    envPort = "22000";
                }
                int sensorPort = Integer.parseInt(envPort);

                // Crea el registro RMI para la consola
                LocateRegistry.createRegistry(sensorPort);

                // Publica el servidor remoto en el registro
                Naming.rebind("rmi://" + sensorHostname + ":" + sensorPort + "/" + name, hiloServerRMI);
                System.out.println("rmi://" + sensorHostname + ":" + sensorPort + "/" + name + " bound in registry");

            } catch (RemoteException ex) {
                Logger.getLogger(HiloServerRMI.class.getName()).log(Level.SEVERE,
                        "Error al iniciar el servidor RMI", ex);
            } catch (MalformedURLException ex) {
                Logger.getLogger(HiloServerRMI.class.getName()).log(Level.SEVERE,
                        "URL mal formada para el servidor RMI", ex);
            }


        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
