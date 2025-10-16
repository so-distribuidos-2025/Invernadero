package hilos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hilo encargado de recibir y procesar continuamente los datos de un sensor de lluvia.
 *
 * <p>Este hilo se dedica a leer los datos enviados por un sensor de lluvia, que
 * consisten en valores de {@code 1.0} (lloviendo) o {@code 0.0} (no lloviendo).
 * En un bucle infinito, lee una línea de texto, la convierte a un valor
 * booleano y actualiza el estado global del sistema en el
 * {@link ConcurrentHashMap} compartido bajo la clave {@code "lluvia"}.</p>
 */
public class HiloReceptorLluvia extends Thread {

    private Socket clientelluvia;
    private final BufferedReader br;
    private boolean lluvia;
    private ConcurrentHashMap<String, Object> estado;

    /**
     * Construye un nuevo hilo receptor para un sensor de lluvia.
     *
     * @param clientelluvia el {@link Socket} de la conexión con el sensor.
     * @param estado el mapa {@link ConcurrentHashMap} que contiene el estado global del sistema.
     * @throws RuntimeException si ocurre un error al inicializar el lector de entrada del socket.
     */
    public HiloReceptorLluvia(Socket clientelluvia, ConcurrentHashMap<String, Object> estado) {
        this.clientelluvia = clientelluvia;
        this.estado = estado;
        try {
            this.br = new BufferedReader(new InputStreamReader(clientelluvia.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Bucle principal del hilo.
     *
     * <p>Lee continuamente datos del socket, los interpreta como {@code 1.0} para
     * {@code true} y cualquier otro valor para {@code false}, y actualiza el
     * mapa de estado compartido bajo la clave {@code "lluvia"}. Realiza una
     * pausa de 1 segundo entre lecturas.</p>
     */
    public void run() {
        while (true) {
            try {
                String entrada = br.readLine();
                lluvia = Double.parseDouble(entrada) == 1.0;
                this.estado.put("lluvia", lluvia);
                Thread.sleep(1000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}