package hilos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hilo encargado de recibir y procesar continuamente los datos de un sensor de temperatura.
 *
 * <p>Este hilo se dedica a leer los valores de temperatura enviados por un
 * sensor a través de una conexión de socket. En un bucle infinito, lee una
 * línea de texto, la convierte a {@code double} y actualiza el estado global
 * del sistema en el {@link ConcurrentHashMap} compartido bajo la clave
 * {@code "temperatura"}.</p>
 */
public class HiloReceptorTemperatura extends Thread {

    private Socket clienteTemperatura;
    private final BufferedReader br;
    private double temperatura;
    private ConcurrentHashMap<String, Object> estado;

    /**
     * Construye un nuevo hilo receptor para un sensor de temperatura.
     *
     * @param clienteTemperatura el {@link Socket} de la conexión con el sensor.
     * @param estado el mapa {@link ConcurrentHashMap} que contiene el estado global del sistema.
     * @throws RuntimeException si ocurre un error al inicializar el lector de entrada del socket.
     */
    public HiloReceptorTemperatura(Socket clienteTemperatura, ConcurrentHashMap<String, Object> estado) {
        this.clienteTemperatura = clienteTemperatura;
        this.estado = estado;
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteTemperatura.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Bucle principal del hilo.
     *
     * <p>Lee continuamente datos del socket, los convierte a {@code double},
     * actualiza el valor local y actualiza el mapa de estado compartido
     * bajo la clave {@code "temperatura"}. Realiza una pausa de 1 segundo
     * entre lecturas.</p>
     */
    public void run() {
        while (true) {
            try {
                String entrada = br.readLine();
                temperatura = Double.parseDouble(entrada);
                estado.put("temperatura", temperatura);
                sleep(1000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}