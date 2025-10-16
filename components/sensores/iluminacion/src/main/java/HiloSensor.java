import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Hilo que simula un sensor de iluminación.
 * Cada segundo genera un valor aleatorio de iluminación y lo envía al servidor.
 * <p>
 * El hilo puede iniciarse y detenerse usando los métodos {@link #encender()} y {@link #apagar()}.
 *
 * @author Anita
 */
public class HiloSensor extends Thread {

    /**
     * Estado del sensor (true = encendido, false = apagado).
     */
    private boolean on;

    /**
     * Valor actual de la iluminación medida (0 - 100).
     */
    private double iluminacion;

    /**
     * Canal de salida para enviar datos al servidor.
     */
    private PrintWriter pw;

    /**
     * Conexión con el servidor.
     */
    private Socket s;

    /**
     * Constructor principal.
     *
     * @param s  el socket de conexión con el servidor
     * @param pw flujo de salida para enviar lecturas de iluminación
     */
    public HiloSensor(Socket s, PrintWriter pw) {
        this.on = true;              // El sensor arranca encendido
        this.iluminacion = 0.0;
        this.pw = pw;
        this.s = s;
    }

    /**
     * Constructor por defecto (sin conexión establecida).
     * Inicia el sensor encendido con iluminación inicial 0.
     */
    public HiloSensor() {
        this.on = true;
        this.iluminacion = 0;
    }

    /**
     * Genera un valor aleatorio de iluminación entre 0 y 100.
     *
     * @return valor generado de iluminación
     */
    public double generarIluminacion() {
        Random random = new Random();
        double media = 50;
        double desviacion = 20;
        double valor;
        do {
            valor = media + random.nextGaussian() * desviacion;
        } while (valor < 0 || valor > 100);
        return valor;
    }

    /**
     * Enciende el sensor (habilita el bucle de sensado).
     */
    public void encender() {
        on = true;
    }

    /**
     * Apaga el sensor (finaliza el bucle de sensado).
     */
    public void apagar() {
        on = false;
    }

    /**
     * Devuelve la última lectura registrada de iluminación.
     *
     * @return valor actual de iluminación
     */
    public double leerIluminacion() {
        return iluminacion;
    }

    /**
     * Lógica principal del hilo.
     * Mientras el sensor esté encendido:
     * - Genera un nuevo valor de iluminación.
     * - Envía el valor al servidor.
     * - Espera 1 segundo antes de la siguiente lectura.
     */
    @Override
    public void run() {
        while (on) {
            try {
                this.iluminacion = generarIluminacion();
                System.out.println(getTiempo() + " | Iluminacion: " + iluminacion);
                pw.println(iluminacion);
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.getLogger(HiloSensor.class.getName())
                        .log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }

    private String getTiempo() {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        return myDateObj.format(myFormatObj);
    }
}
