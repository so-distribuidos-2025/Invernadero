import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
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
        InetAddress ipServidor;   // Dirección IP del servidor
        PrintWriter pw;                  // Flujo de salida para enviar datos

        try {
            // Obtiene la dirección IP del servidor (localhost en este caso)
            ipServidor = InetAddress.getByName("localhost");

            // Se conecta al servidor en el puerto 20000
            Socket cliente = new Socket(ipServidor, 20000);
            System.out.println(cliente);

            // Prepara el PrintWriter con autoflush activado para enviar datos
            pw = new PrintWriter(cliente.getOutputStream(), true);

            // Envía un mensaje inicial indicando el tipo de sensor
            pw.println("temperatura");

            // Crea e inicia el hilo que simula el sensor de temperatura
            HiloSensado sensor = new HiloSensado(cliente, pw);
            sensor.start();

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}