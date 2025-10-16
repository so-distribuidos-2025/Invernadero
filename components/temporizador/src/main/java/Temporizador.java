import java.util.Timer;
import java.util.TimerTask;

/**
 * Temporizador simplificado. Su única responsabilidad es contar hacia atrás.
 * Puede ser detenido desde el exterior.
 */
public class Temporizador {

    private final Timer timer;
    private volatile boolean isRunning = false; // volatile para visibilidad entre hilos

    public Temporizador(int totalSegundos) {
        this.timer = new Timer(true); // true para que sea un hilo daemon
    }

    /**
     * Inicia el conteo del temporizador.
     * Esta tarea se ejecuta en el hilo del Timer.
     */
    public void iniciar(int totalSegundos) {
        if (isRunning) {
            return;
        }
        isRunning = true;

        TimerTask tarea = new TimerTask() {
            private int segundosRestantes = totalSegundos;

            @Override
            public void run() {
                if (segundosRestantes > 0) {
                    int minutos = segundosRestantes / 60;
                    int segundos = segundosRestantes % 60;
                    System.out.printf("Tiempo restante: %02d:%02d\n", minutos, segundos);
                    segundosRestantes--;
                } else {
                    System.out.println("¡Tiempo finalizado!");
                    parar(); // Detiene el timer y actualiza el estado
                }
            }
        };

        timer.scheduleAtFixedRate(tarea, 0, 1000); // Inicia inmediatamente, repite cada segundo
    }

    /**
     * Detiene el temporizador de forma forzada.
     */
    public synchronized void parar() {
        if (isRunning) {
            timer.cancel(); // Detiene todas las tareas programadas
            timer.purge();  // Elimina las tareas canceladas
            isRunning = false;
            System.out.println("Temporizador detenido.");
        }
    }

    /**
     * Verifica si el temporizador está actualmente en funcionamiento.
     * @return true si está contando, false en caso contrario.
     */
    public boolean estaCorriendo() {
        return isRunning;
    }
}