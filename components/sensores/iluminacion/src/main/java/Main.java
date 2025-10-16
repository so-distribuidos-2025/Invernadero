import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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
    public static void main(String[] args) {
        InetAddress ipServidor;
        PrintWriter pw;

        try {
            // Dirección del servidor
            ipServidor = InetAddress.getByName("localhost");

            // Conexión al servidor en el puerto 20000
            Socket cliente = new Socket(ipServidor, 20000);
            System.out.println("Conectado al servidor: " + cliente);

            // Flujo de salida con autoflush activado
            pw = new PrintWriter(cliente.getOutputStream(), true);

            // Identificación inicial del sensor
            pw.println("iluminacion");

            // Creación e inicio del hilo que simula el sensor
            HiloSensor sensor = new HiloSensor(cliente, pw);
            sensor.encender();
            sensor.start();

        } catch (UnknownHostException e) {
            throw new RuntimeException("No se pudo resolver la dirección del servidor.", e);
        } catch (IOException e) {
            throw new RuntimeException("Error al establecer conexión con el servidor.", e);
        }
    }
}
