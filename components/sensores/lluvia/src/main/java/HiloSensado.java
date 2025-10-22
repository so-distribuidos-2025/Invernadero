import java.io.PrintWriter;
import java.net.Socket;

/**
 * La clase {@code HiloSensado} simula el funcionamiento de un sensor de lluvia.
 *
 * <p>Extiende de {@link Thread} y se encarga de generar de forma periódica
 * datos aleatorios que representan si está lloviendo o no. Los valores generados
 * se envían a través de un {@link PrintWriter} conectado a un servidor.</p>
 *
 * <p>El sensor utiliza una probabilidad aleatoria para determinar si llueve:</p>
 * <ul>
 *   <li>Si la probabilidad es mayor que 50, se considera que está lloviendo ({@code 1}).</li>
 *   <li>Si la probabilidad es menor o igual a 50, se considera que no llueve ({@code 0}).</li>
 * </ul>
 *
 * <p>El ciclo de sensado se repite cada 1 segundo mientras el hilo esté encendido.</p>
 *
 * @author 
 * @version 1.0
 */
public class HiloSensado extends Thread {
    /** Indica si el sensor está encendido o apagado. */
    private boolean on;
    /** Representa si llueve (1) o no (0). */
    private double lluvia;
    /** Valor aleatorio entre 0 y 100 usado para determinar la lluvia. */
    private float probabilidad;
    /** Conexión con el servidor. */
    private Socket cnxServidor;
    /** Flujo de salida para enviar datos al servidor. */
    PrintWriter pw;

    boolean isAuto = false;

    /**
     * Crea un nuevo hilo de sensado para el sensor de lluvia.
     *
     * @param s  el {@link Socket} conectado al servidor.
     * @param pw el {@link PrintWriter} asociado al socket para enviar datos.
     */
    public HiloSensado(Socket s, PrintWriter pw) {
        this.on = true;
        this.lluvia = 0;
        this.probabilidad = 0;
        this.cnxServidor = s;
        this.pw = pw;
    }

    /**
     * Devuelve el valor actual de lluvia generado por el sensor.
     *
     * @return {@code 1.0} si está lloviendo, {@code 0.0} en caso contrario.
     */
    public double getLluvia() {
        return lluvia;
    }

    /**
     * Genera un nuevo valor de lluvia de forma aleatoria.
     *
     * <p>Si la probabilidad generada aleatoriamente es mayor que 50,
     * asigna el valor {@code 1} a {@code lluvia}. En caso contrario,
     * asigna el valor {@code 0}.</p>
     */
    private void generarLluvia() {
        probabilidad = (float) (Math.random() * 100);

        if (probabilidad > 50) {
            lluvia = 1;
        } else {
            lluvia = 0;
        }
    }

    /**
     * Enciende el sensor, permitiendo que el hilo siga ejecutándose.
     */
    public void encender() {
        on = true;
    }

    /**
     * Apaga el sensor, deteniendo el bucle de ejecución.
     */
    public void apagar() {
        on = false;
    }


    public void setValor(double valor) {
        this.lluvia = valor;
    }

    public void setAuto(boolean auto) {
        this.isAuto = auto;
    }

    /**
     * Método principal del hilo.
     *
     * <p>Mientras el sensor esté encendido, genera un nuevo valor de lluvia
     * cada segundo, lo envía al servidor y lo muestra por consola.</p>
     *
     * @throws RuntimeException si el hilo es interrumpido durante la espera.
     */
    @Override
    public void run() {
        while (on) {
            generarLluvia();
            pw.println(this.lluvia);
            System.out.println("L=" + this.lluvia);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

