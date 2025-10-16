package hilos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Hilo encargado de recibir y procesar continuamente los datos de un sensor de humedad.
 *
 * <p>Este hilo se dedica a leer los datos enviados por un dispositivo sensor de
 * humedad a través de una conexión de socket. En un bucle infinito, lee una
 * línea de texto, la convierte a un valor de tipo {@code double} y la almacena
 * en una variable local. El valor más reciente puede ser consultado por otros
 * hilos
 *
 * @author Brunardo19
 */
public class HiloReceptorHumedad extends Thread {
    private Socket clienteHumedad;
    private final BufferedReader br;
    private double humedad;


    public double getHumedad() {
        return humedad;
    }

    /**
     * Construye un nuevo hilo receptor para un sensor de humedad.
     *
     * @param clienteHumedad el {@link Socket} de la conexión con el sensor.
     */
    public HiloReceptorHumedad(Socket clienteHumedad) {
        this.clienteHumedad = clienteHumedad;
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteHumedad.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Bucle principal del hilo.
     *
     * <p>Lee continuamente datos del socket, los convierte a {@code double}
     * y actualiza la variable de instancia {@code humedad}. Realiza una pausa
     * de 500 milisegundos entre lecturas.</p>
     */
    public void run() {
        while (true) {
            try {
                String entrada = br.readLine();
                humedad = Double.parseDouble(entrada);
                sleep(500);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}