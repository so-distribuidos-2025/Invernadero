package hilos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hilo encargado de recibir y procesar continuamente los datos de un sensor de iluminación.
 *
 * <p>Este hilo se dedica a leer los valores de radiación solar enviados por un
 * sensor a través de una conexión de socket. En un bucle infinito, lee una
 * línea de texto, la convierte a {@code double} y actualiza el estado global
 * del sistema en el {@link ConcurrentHashMap} compartido bajo la clave {@code "radiacion"}.</p>
 *
 */
public class HiloReceptorIluminacion extends Thread {

    private Socket clienteIluminacion;
    private final BufferedReader br;
    private double iluminacion;
    private ConcurrentHashMap<String, Object> estado;

    /**
     * Construye un nuevo hilo receptor para un sensor de iluminación.
     *
     * @param clienteIluminacion el {@link Socket} de la conexión con el sensor.
     * @param estado el mapa {@link ConcurrentHashMap} que contiene el estado global del sistema.
     * @throws RuntimeException si ocurre un error al inicializar el lector de entrada del socket.
     */
    public HiloReceptorIluminacion(Socket clienteIluminacion, ConcurrentHashMap<String, Object> estado) {
        this.clienteIluminacion = clienteIluminacion;
        this.estado = estado;
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteIluminacion.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Bucle principal del hilo.
     *
     * <p>Lee continuamente datos del socket, los convierte a {@code double},
     * actualiza el valor local y actualiza el mapa de estado compartido
     * bajo la clave {@code "radiacion"}. Realiza una pausa de 1 segundo
     * entre lecturas.</p>
     */
    public void run() {
        while (true) {
            try {
                String entrada = br.readLine();
                iluminacion = Double.parseDouble(entrada);
                estado.put("radiacion", iluminacion);
                sleep(1000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}