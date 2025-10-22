import java.io.PrintWriter;
import java.net.Socket;

public class HiloSensado extends Thread {

    /** Indica si el sensor está encendido o apagado */
    private boolean on;

    /** Último valor de humedad generado */
    private float humedad;

    /** Socket de conexión con el servidor */
    private Socket cnxServidor;

    /** Flujo de salida para enviar datos al servidor */
    private PrintWriter pw;

    private boolean isAuto = true;

    /**
     * Constructor principal del sensor de humedad.
     *
     * @param s  Socket de conexión con el servidor
     * @param pw PrintWriter para enviar los datos al servidor
     */
    public HiloSensado(Socket s, PrintWriter pw) {
        this.on = true;
        this.humedad = 0;
        this.cnxServidor = s;
        this.pw = pw;
        this.isAuto = true;
    }

    /**
     * Constructor alternativo sin conexión a servidor.
     * Inicializa el sensor encendido con valor de humedad en 0.
     */
    public HiloSensado() {
        this.on = true;
        this.humedad = 0;
    }

    /**
     * Genera un valor de humedad aleatorio entre 0 y 100.
     *
     * @return humedad generada (en porcentaje)
     */
    public float generarHumedad() {
        return (float) (Math.random() * 100);
    }

    /** Enciende el sensor */
    public void encender() {
        on = true;
    }

    /** Apaga el sensor */
    public void apagar() {
        on = false;
    }

    /**
     * Devuelve el último valor de humedad generado.
     *
     * @return valor actual de la humedad
     */
    public float leerHumedad() {
        return humedad;
    }

    public void setValor(double valor) {
        this.humedad = (float) valor;
    }

    public void setAuto(boolean auto) {
        this.isAuto = auto;
    }
    /**
     * Método principal del hilo.
     * Mientras el sensor esté encendido:
     *  - Genera un nuevo valor de humedad aleatorio.
     *  - Envía el valor al servidor.
     *  - Muestra el valor en consola.
     *  - Espera 1 segundo antes de la próxima lectura.
     */
    @Override
    public void run() {
        while (on) {
            if (isAuto) {
                this.humedad = generarHumedad();
            }

            // Enviar valor al servidor
            pw.println(this.humedad);

            // Mostrar valor por consola
            System.out.println("Humedad: " + leerHumedad());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
