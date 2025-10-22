/*
 * Clase HiloSensor
 * ----------------
 * Representa el hilo que simula un sensor de iluminación.
 *
 * Funcionalidades principales:
 *  - Genera valores aleatorios de iluminación en un rango de 0 a 100.
 *  - Envía periódicamente los valores generados al servidor mediante un PrintWriter.
 *  - Permite encender y apagar el sensado de forma controlada.
 */
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Hilo que simula un sensor de iluminación.
 * Cada segundo genera un valor aleatorio de iluminación y lo envía al servidor.
 *
 * El hilo puede iniciarse y detenerse usando los métodos {@link #encender()} y {@link #apagar()}.
 *
 * @author Anita
 */
public class HiloSensor extends Thread {

    /** Estado del sensor (true = encendido, false = apagado). */
    private boolean on;

    /** Valor actual de la iluminación medida (0 - 100). */
    private double iluminacion;

    /** Canal de salida para enviar datos al servidor. */
    private PrintWriter pw;

    /** Conexión con el servidor. */
    private Socket s;

    private boolean isAuto = true; // Flag para modo automático/manual

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
        return Math.random() * 100;
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

    public void setValor(double valor) {
        this.iluminacion = valor;
    }

    public void setAuto(boolean auto) {
        this.isAuto = auto;
    }

    /**
     * Lógica principal del hilo.
     * Mientras el sensor esté encendido:
     *  - Genera un nuevo valor de iluminación.
     *  - Envía el valor al servidor.
     *  - Espera 1 segundo antes de la siguiente lectura.
     */
    @Override
    public void run() {
        while (on) {
            try {
                if (isAuto) {
                    this.iluminacion = generarIluminacion();
                }
                System.out.println(getTiempo() + " | Iluminacion: " + String.format("%.2f", leerIluminacion()));
                pw.println(iluminacion);
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.getLogger(HiloSensor.class.getName())
                        .log(System.Logger.Level.ERROR, "Hilo de iluminación interrumpido", ex);
            }
        }
    }

    private String getTiempo(){
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        return myDateObj.format(myFormatObj);
    }
}
