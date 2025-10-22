import rmi.ISensorRMI;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;


/**
 * Clase principal que inicia el cliente sensor de iluminación.
 * <p>
 * Funciona de la siguiente manera:
 * <ul>
 *   <li>Se conecta a un servidor en {@code localhost}, puerto {@code 20000}.</li>
 *   <li>Envía un mensaje inicial identificándose como {@code "iluminacion"}.</li>
 *   <li>Crea un hilo {@link HiloSensor} que genera lecturas de iluminación.</li>
 *   <li>El hilo envía periódicamente (cada 1 segundo) las lecturas al servidor.</li>
 * </ul>
 *
 * <b>Uso:</b><br>
 * Ejecutar la clase y asegurarse de que el servidor esté en escucha en el puerto 20000.
 *
 * @author Anita
 */
public class Main {

    /**
     * Método principal del programa.
     *
     * @param args no se utilizan argumentos en esta implementación
     */
    public static void main(String[] args) throws IOException, RemoteException, MalformedURLException {
        InetAddress ipServidor;
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

            pw = new PrintWriter(cliente.getOutputStream(), true);
            pw.println("iluminacion");

            HiloSensor sensor = new HiloSensor(cliente, pw);
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
            int sensorPort = Integer.parseInt(envPort);
            String name = "SensorRadiacionRMI"; // La consola busca "radiacion"
            HiloServerRMI hiloServerRMI = new HiloServerRMI(sensor);

            try {
                LocateRegistry.createRegistry(sensorPort);
                System.out.println("RMI registry created on port " + sensorPort);
            } catch (RemoteException e) {
                System.out.println("RMI registry already running on port " + sensorPort);
            }

            Naming.rebind("rmi://" + sensorHostname + ":" + sensorPort + "/" + name, hiloServerRMI);
            System.out.println("rmi://" + sensorHostname + ":" + sensorPort + "/" + name + " bound in registry");

        } catch (UnknownHostException e) {
            throw new RuntimeException("No se pudo resolver la dirección del servidor.", e);
        }
    }
}
