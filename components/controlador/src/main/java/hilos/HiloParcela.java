package hilos;

import rmi.IServerRMI;
import util.INR;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona la lógica de control para una parcela individual del invernadero.
 *
 * <p>Cada instancia de {@code HiloParcela} se ejecuta como un hilo independiente
 * y es responsable de una única parcela. Su función principal es integrar los
 * datos de los sensores asociados a ella (humedad y temporizador) con los
 * datos ambientales globales (temperatura, radiación y lluvia) para tomar
 * decisiones sobre el riego.</p>
 *
 * <li>Actualiza su estado local con los valores globales del sistema.</li>
 * <li>Lee la humedad actual de su sensor de humedad asociado.</li>
 * <li>Comprueba el estado de su temporizador.</li>
 * <li>Calcula el Índice de Necesidad de Riego (INR) basado en la humedad,
 * radiación y temperatura, siempre que no esté lloviendo y el temporizador
 * esté activo.</li>
 * <li>En función del valor del INR, decide si debe abrir la electroválvula
 * y durante cuánto tiempo, comunicando esta duración al dispositivo temporizador.</li>
 * <li>Controla la electroválvula a través de una conexión RMI.</li>
 *
 * @author Brunardo19
 */
public class HiloParcela extends Thread {
    /**
     * Hilo receptor de datos del sensor de humedad asociado a esta parcela.
     */
    private HiloReceptorHumedad hiloHumedad;
    /**
     * Hilo receptor de datos del temporizador asociado a esta parcela.
     */
    private HiloReceptorTiempo hiloTiempo;
    /**
     * Interfaz RMI para controlar la electroválvula de la parcela.
     */
    private IServerRMI electrovalvula;

    private int id;

    /**
     * Estado local.
     */
    private boolean lluvia;
    private double radiacion;
    private double temperatura;

    /**
     * Canal de escritura para enviar comandos al temporizador.
     */
    private PrintWriter timeWriter;
    /**
     * Estado actual del temporizador (1 para activo, 0 para inactivo).
     */
    volatile int estadoTemporizador;

    /**
     * Último valor de humedad registrado para esta parcela.
     */
    volatile double humedad;
    /**
     * Índice de Necesidad de Riego calculado para esta parcela.
     */
    volatile double inr;

    /**
     * Referencia al mapa de estado global del sistema.
     */
    ConcurrentHashMap estado;

    private volatile boolean estaRegando = false;

    /**
     * Bandera para indicar al HiloControlador si esta parcela necesita agua.
     * Es volatile porque es accedida por dos hilos diferentes.
     */
    private volatile boolean necesitaAgua = false;

    /**
     * Construye un nuevo hilo para gestionar una parcela.
     * <p>
     * En el constructor, se establece la conexión RMI con la electroválvula
     * correspondiente al {@code id} de la parcela. El puerto RMI se calcula como
     * {@code 21000 + id}. También se inicializan los valores de estado
     * locales a partir del mapa de estado global.
     *
     * @param id     el identificador único de la parcela (de 0 a 4).
     * @param estado el mapa compartido con el estado global del sistema.
     */
    public HiloParcela(int id, ConcurrentHashMap estado) {
        this.id = id;
        this.humedad = 0;
        this.estado = estado;
        this.radiacion = (Double) estado.get("radiacion");
        this.lluvia = (Boolean) estado.get("lluvia");
        this.temperatura = (Double) estado.get("temperatura");
        this.inr = 0;

        int maxRetries = 10;
        long delay = 1000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                String valvulaHost = System.getenv("VALVULA_HOST");
                if (valvulaHost == null) {
                    valvulaHost = "localhost";
                } else {
                    valvulaHost = String.format(valvulaHost, id);
                }
                String basePortEnv = System.getenv("VALVULA_BASE_PORT");
                int basePort = (basePortEnv != null) ? Integer.parseInt(basePortEnv) : 21000;
                int puerto = basePort + id;
                String direccionRMI = String.format("rmi://" + valvulaHost + ":%d/ServerRMI", puerto);

                System.out.println("Intento " + (i + 1) + ": Conectandose a electrovalvula " + id + " en " + direccionRMI);
                this.electrovalvula = (IServerRMI) Naming.lookup(direccionRMI);
                System.out.println("Conectado a electrovalvula " + id);
                break; // Success, exit loop
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                System.err.println("Error al conectarse a electrovalvula " + id + ": " + e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        System.err.println("Reintentando en " + delay / 1000 + " segundos...");
                        Thread.sleep(delay);
                        delay *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.err.println("Could not connect to electrovalvula " + id + " after " + maxRetries + " attempts. HiloParcela will not function correctly.");
                }
            }
        }
    }


    public void setHiloHumedad(HiloReceptorHumedad hiloHumedad) {
        this.hiloHumedad = hiloHumedad;
    }

    /**
     * Asocia el hilo receptor del temporizador a esta parcela y prepara
     * el canal de escritura para enviarle comandos.
     *
     * @param hiloTiempo la instancia del hilo que gestiona el temporizador.
     * @throws IOException si ocurre un error al obtener el flujo de salida del socket.
     */
    public void setHiloTiempo(HiloReceptorTiempo hiloTiempo) throws IOException {
        this.hiloTiempo = hiloTiempo;
        this.timeWriter = new PrintWriter(hiloTiempo.getClienteTiempo().getOutputStream(), true);
    }

    public double getHumedad() {
        return humedad;
    }

    public double getInr() {
        return inr;
    }

    /**
     * Devuelve la instancia del cliente RMI para la electroválvula.
     *
     * @return el objeto {@link IServerRMI} que controla la electroválvula.
     */
    public IServerRMI getElectrovalvula() {
        return electrovalvula;
    }

    /**
     * Devuelve el estado actual del temporizador.
     *
     * @return 1 si el temporizador está activo, 0 si está inactivo.
     */
    public int getEstadoTemporizador() {
        return estadoTemporizador;
    }

    /**
     * Permite al HiloControlador saber si esta parcela ha solicitado agua.
     *
     * @return true si la parcela necesita agua, false en caso contrario.
     */
    public boolean necesitaAgua() {
        return this.necesitaAgua;
    }

    /**
     * Bucle principal de ejecución del hilo.
     *
     * <p>Lógica central de control de la parcela.
     * Se ejecuta continuamente, realizando las siguientes acciones en cada ciclo:</p>
     * <ol>
     *   <li>Sincroniza las variables locales de radiación, lluvia y temperatura
     *       con el estado global del sistema.</li>
     *   <li>Obtiene el estado del temporizador y el valor de humedad de los hilos
     *       receptores correspondientes.</li>
     *   <li>Si no está lloviendo y el temporizador está activado, calcula el INR.</li>
     *   <li>Basado en el INR, determina la duración del riego, envía el comando al
     *       temporizador y abre la electroválvula.</li>
     *   <li>Si el temporizador no está activo o si está lloviendo, se asegura de
     *       que la electroválvula esté cerrada.</li>
     * </ol>
     */
    @Override
    public void run() {
        while (true) {
            try {
                // Asegurarse de que los sensores están conectados antes de hacer nada
                if (this.hiloHumedad == null || this.hiloTiempo == null) {
                    Thread.sleep(2000); // Esperar a que se conecten
                    System.out.println("Hilo " + id + " esperando sensor de humedad o temporizador");
                    continue;
                }

                // Obtener estado actual
                this.radiacion = (Double) this.estado.get("radiacion");
                this.lluvia = (Boolean) this.estado.get("lluvia");
                this.temperatura = (Double) this.estado.get("temperatura");
                this.estadoTemporizador = this.hiloTiempo.getEstadoTemporizador(); // 1 = listo, 0 = ocupado
                this.humedad = this.hiloHumedad.getHumedad();

                // Calcular necesidad de riego
                boolean necesitaRegarAhora;
                if (lluvia) {
                    this.inr = 0;
                    necesitaRegarAhora = false;
                } else {
                    this.inr = INR.calcularInr(humedad, radiacion, temperatura);
                    necesitaRegarAhora = inr > 0.7;
                }

                if (estaRegando) {
                    // ESTADO: REGANDO
                    // Condición para detener el riego: llueve o el temporizador terminó.
                    boolean temporizadorTermino = (estadoTemporizador == 1);

                    if (lluvia || temporizadorTermino) {
                        System.out.println("Parcela " + this.id + " - DETENIENDO RIEGO. Causa: "
                                + (lluvia ? "Lluvia" : "Temporizador finalizado"));

                        estaRegando = false;
                        this.necesitaAgua = false;
                        electrovalvula.cerrarValvula();

                        // Si el temporizador no ha terminado por su cuenta, le decimos que pare.
                        if (!temporizadorTermino) {
                            timeWriter.println(0);
                        }
                    }

                } else {
                    // ESTADO: NO REGANDO
                    // Condición para iniciar el riego: se necesita y el temporizador está listo.
                    boolean temporizadorListo = (estadoTemporizador == 1);

                    if (necesitaRegarAhora && temporizadorListo) {
                        System.out.println("Parcela " + this.id + " - INICIANDO RIEGO (INR: " + String.format("%.2f", inr) + ")");

                        estaRegando = true;
                        this.necesitaAgua = true; // Pedimos agua al controlador
                        electrovalvula.abrirValvula();

                        // Calcular y enviar duración al temporizador
                        int duracion = 300; // Por defecto
                        if (inr > 0.9) duracion = 600;
                        else if (inr > 0.8) duracion = 420;
                        timeWriter.println(duracion);
                    }
                }

                Thread.sleep(500);
            } catch (InterruptedException | RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }
}