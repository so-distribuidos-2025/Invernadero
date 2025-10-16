package hilos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hilo encargado de gestionar la comunicación con un temporizador.
 *
 * <p>Este hilo recibe información desde un cliente de temporizador y
 * actualiza el estadoTemporizador global en el {@link ConcurrentHashMap}. Cada
 * temporizador se identifica por un {@code id} y controla un contador
 * de segundos que puede reiniciarse o detenerse según la entrada.</p>
 */
public class HiloReceptorTiempo extends Thread {

    private Socket clienteTiempo;
    private final BufferedReader br;
    private int estadoTemporizador;

    /**
     * Devuelve el valor actual del temporizador en segundos.
     *
     * @return segundos restantes o acumulados
     */
    public int getTotalSegundos() {
        return estadoTemporizador;
    }

    /**
     * Establece manualmente el valor del temporizador.
     *
     * @param estadoTemporizador nuevo valor en segundos
     */
    public void setTotalSegundos(int estadoTemporizador) {
        this.estadoTemporizador = estadoTemporizador;
    }

    public Socket getClienteTiempo() {
        return clienteTiempo;
    }

    public int getEstadoTemporizador() {
        return estadoTemporizador;
    }

    public void setEstadoTemporizador(int estadoTemporizador) {
        this.estadoTemporizador = estadoTemporizador;
    }

    /**
     * Constructor de la clase.
     *
     * @param clienteTemporizador socket del cliente que envía los datos del temporizador
     */
    public HiloReceptorTiempo(Socket clienteTemporizador) {
        this.clienteTiempo = clienteTemporizador;
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteTemporizador.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Bucle principal del hilo.
     *
     * <p>Verifica periódicamente el estadoTemporizador del temporizador asociado al {@code id}./p>
     */
    @Override
    public void run() {
        while (true) {
            try {
                int lectura = Integer.parseInt(br.readLine());
                this.estadoTemporizador = lectura;
                //System.out.println(estadoTemporizador);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
