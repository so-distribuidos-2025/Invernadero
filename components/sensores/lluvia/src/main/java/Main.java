import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * La clase {@code SensorLluvia} representa un cliente que simula un sensor de lluvia.
 *
 * <p>Este programa establece una conexión con un servidor en el puerto {@code 20000}
 * en la dirección {@code localhost}. Una vez conectado, se identifica enviando el
 * mensaje "lluvia" al servidor y luego inicia un hilo encargado de simular el proceso
 * de sensado y transmisión de datos.</p>
 *
 * <p>El hilo de sensado está representado por la clase {@link HiloSensado},
 * que maneja la lógica de envío de datos del sensor al servidor.</p>
 *
 * <p>Ejemplo de uso:</p>
 * <pre>
 *     java com.mycompany.sensorlluvia.SensorLluvia
 * </pre>
 *
 * @author
 * @version 1.0
 */
public class Main {

    /**
     * Punto de entrada principal del programa.
     *
     * <p>Realiza los siguientes pasos:</p>
     * <ul>
     *   <li>Obtiene la dirección IP del servidor ({@code localhost}).</li>
     *   <li>Establece la conexión con el servidor en el puerto {@code 20000}.</li>
     *   <li>Crea un {@link PrintWriter} con autoflush para enviar datos al servidor.</li>
     *   <li>Envía la cadena {@code "lluvia"} como identificación del sensor.</li>
     *   <li>Inicia un hilo de tipo {@link HiloSensado} para simular el envío de datos periódicos.</li>
     * </ul>
     *
     * @param args argumentos de la línea de comandos (no se utilizan en este programa).
     */
    public static void main(String[] args) {
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
            // PrintWriter con autoflush activado
            pw = new PrintWriter(cliente.getOutputStream(), true);
            // Identifica el sensor al servidor
            pw.println("lluvia");
            // Arranca el hilo de sensado
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
            int sensorPort = Integer.parseInt(envPort);
            String name = "SensorLluviaRMI"; // La consola busca "lluvia"
            HiloServerRMI hiloServerRMI = new HiloServerRMI(sensor);

            try {
                LocateRegistry.createRegistry(sensorPort);
                System.out.println("RMI registry created on port " + sensorPort);
            } catch (RemoteException e) {
                System.out.println("RMI registry already running on port " + sensorPort);
            }

            Naming.rebind("rmi://" + sensorHostname + ":" + sensorPort + "/" + name, hiloServerRMI);
            System.out.println(name + " bound in registry");

        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
