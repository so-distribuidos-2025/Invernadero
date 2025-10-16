/*
 * Clase HiloSensado
 * -----------------
 * Representa un hilo que simula el funcionamiento de un sensor de humedad.
 *
 * Funcionalidades principales:
 *  - Genera valores de humedad de forma aleatoria (entre 0% y 100%).
 *  - Envía el valor generado al servidor a través de un PrintWriter.
 *  - Muestra en consola el valor de humedad leído.
 *  - Permite encender y apagar el sensor con métodos dedicados.
 */

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class HiloSensado extends Thread {

    /**
     * Indica si el sensor está encendido o apagado
     */
    private boolean on;

    /**
     * Último valor de humedad generado
     */
    private float humedad;

    /**
     * Socket de conexión con el servidor
     */
    private Socket cnxServidor;

    /**
     * Flujo de salida para enviar datos al servidor
     */
    private PrintWriter pw;

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
        Random random = new Random();
        double media = 60;       // Humedad promedio
        double desviacion = 15;  // Variabilidad
        double valor;
        do {
            valor = media + random.nextGaussian() * desviacion;
        } while (valor < 0 || valor > 100); // Restringir entre 0 y 100
        return (float) valor;
    }

    /**
     * Enciende el sensor
     */
    public void encender() {
        on = true;
    }

    /**
     * Apaga el sensor
     */
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

    /**
     * Método principal del hilo.
     * Mientras el sensor esté encendido:
     * - Genera un nuevo valor de humedad aleatorio.
     * - Envía el valor al servidor.
     * - Muestra el valor en consola.
     * - Espera 1 segundo antes de la próxima lectura.
     */
    @Override
    public void run() {
        while (on) {
            this.humedad = generarHumedad();

            // Enviar valor al servidor
            pw.println(this.humedad);

            // Mostrar valor por consola
            System.out.println(getTiempo() + " | Humedad: " + leerHumedad());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getTiempo() {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        return myDateObj.format(myFormatObj);
    }
}
