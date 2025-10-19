import rmi.ISensorRMI;

import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;

/**
 * Clase HiloSensado
 * -----------------
 * Este hilo simula un sensor de temperatura que genera valores
 * aleatorios dentro de un rango válido y los envía al servidor
 * a través de un socket.
 *
 * Funcionalidades principales:
 *  - Genera valores de temperatura con fluctuaciones aleatorias.
 *  - Mantiene la temperatura dentro de los límites -40°C y 125°C.
 *  - Envía periódicamente los valores al servidor usando PrintWriter.
 *  - Permite encender y apagar el sensor.
 */
public class HiloSensado extends Thread implements ISensorRMI {
    
    // Indica si el sensor está encendido o apagado
    private boolean on;

    // Valor actual de la temperatura medida
    private double temperatura;

    // Conexión con el servidor al cual se envían los datos
    private Socket cnxServidor;

    // Flujo de salida para enviar los valores de temperatura
    private PrintWriter pw;

    // Bandera para saber si el sensor esta en modo manual o automatico
    private boolean isAuto = true;


    /**
     * Constructor de la clase HiloSensado.
     * 
     * @param s  Socket de conexión al servidor.
     * @param pw PrintWriter asociado al socket para enviar datos.
     */
    public HiloSensado(Socket s, PrintWriter pw) {
        this.on = true;             // El sensor inicia encendido
        this.temperatura = 40;      // Valor inicial de la temperatura
        this.cnxServidor = s;       // Referencia al socket del servidor
        this.pw = pw;               // PrintWriter para enviar datos
    }

    /**
     * Obtiene la última temperatura generada por el sensor.
     * 
     * @return temperatura actual.
     */
    public double getTemperatura() {
        return temperatura;
    }

    /**
     * Genera un nuevo valor de temperatura aplicando un cambio aleatorio
     * en el rango [-10, +10]. Luego, asegura que la temperatura se mantenga
     * entre -40°C y 125°C.
     */
    private void generarTemperatura() {
        double cambio = Math.random() * 20 - 10; // Variación aleatoria
        temperatura += cambio;

        // Se aplican los límites permitidos
        if (temperatura > 125) {
            temperatura = 125;
        }
        if (temperatura < -40) {
            temperatura = -40;
        }
    }

    /**
     * Enciende el sensor, permitiendo que siga generando y enviando datos.
     */
    public void encender() {
        on = true;
    }

    /**
     * Apaga el sensor, deteniendo el envío de datos.
     */
    public void apagar() {
        on = false;
    }

    /**
     * Método principal del hilo. Mientras el sensor esté encendido:
     *  - Genera un nuevo valor de temperatura.
     *  - Lo envía al servidor mediante el PrintWriter.
     *  - Lo muestra en consola para depuración.
     *  - Espera 1 segundo antes de repetir el ciclo.
     */
    @Override
    public void run() {
        while (on) {
            if (isAuto) {
            generarTemperatura();           // Genera un nuevo valor si esta en automatico
            }
            pw.println(this.temperatura);      // Envía al servidor
            System.out.println(temperatura);   // Muestra en consola

            try {
                Thread.sleep(1000); // Pausa de 1 segundo
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setValor(double valor) throws RemoteException {
        this.temperatura = valor;
    }

    @Override
    public void setAuto(boolean auto) throws RemoteException {
        this.isAuto = auto;
    }
}
