package hilos;

import rmi.IClienteEM;
import rmi.IServerRMI;
import rmi.IServicioExclusionMutua;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Date;

/**
 * <p>
 * Clase {@code HiloControlador} que representa el hilo principal de control del
 * sistema de invernadero. Esta clase se ejecuta de forma concurrente para
 * supervisar las condiciones ambientales y el estado de las parcelas, además de
 * coordinar el acceso exclusivo a la bomba de agua utilizando un servicio RMI
 * de exclusión mutua.
 * </p>
 *
 * <p>
 * Funcionalidades principales:
 * <ul>
 *     <li>Conectarse a un servicio de exclusión mutua mediante RMI para controlar un recurso compartido (bomba de agua).</li>
 *     <li>Conectarse a la válvula maestra del sistema de riego mediante RMI.</li>
 *     <li>Supervisar periódicamente el estado de las parcelas y solicitar o liberar el acceso a la bomba según la demanda.</li>
 *     <li>Mostrar por consola el estado detallado de las parcelas y variables ambientales.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Implementa la interfaz remota {@link rmi.IClienteEM}, lo que permite que el
 * servidor de exclusión mutua invoque de manera asíncrona el método
 * {@link #RecibirToken()} cuando este cliente obtiene acceso exclusivo al
 * recurso.
 * </p>
 */
public class HiloControlador extends UnicastRemoteObject implements IClienteEM, Runnable {

    private final Semaphore s;

    private Connection conn;

    /**
     * Estructura compartida que almacena variables ambientales globales (temperatura, radiación y lluvia).
     */
    private final ConcurrentHashMap<String, Object> estado;

    /**
     * Lista de hilos que representan las parcelas del invernadero.
     * Cada {@link HiloParcela} controla su propia humedad, INR y válvula.
     */
    private final List<HiloParcela> listaParcelas = new ArrayList<>();

    /**
     * Temperatura actual del ambiente (°C).
     */
    private double temperatura;

    /**
     * Radiación solar actual (W/m²).
     */
    private double radiacion;

    /**
     * Indica si está lloviendo en el entorno monitoreado.
     */
    private boolean lluvia;
    
    private int bdCounter = 10;

    /**
     * Referencia al servicio remoto de exclusión mutua.
     * Utilizado para solicitar y liberar el acceso a la bomba de agua.
     */
    private IServicioExclusionMutua exclusionService;

    /**
     * Referencia al servidor remoto que controla la válvula maestra de riego.
     */
    private IServerRMI valvulaMaestraParcelas;

    /**
     * Indica si actualmente el controlador posee el token que da acceso a la bomba de agua.
     */
    private volatile boolean tieneAccesoBomba = false;

    /**
     * Nombre del recurso compartido que se administra mediante exclusión mutua.
     */
    private static final String RECURSO_BOMBA = "BombaAgua";

    /**
     * Construye el hilo controlador, inicializando variables globales, creando hilos de parcelas,
     * y estableciendo conexiones RMI con los servicios de exclusión mutua y control de válvula.
     *
     * @param estado estructura compartida que mantiene variables globales.
     * @param s      Semáforo para sincronizar la recepción del token.
     * @throws RemoteException si ocurre un error al exportar el objeto remoto.
     */
    public HiloControlador(ConcurrentHashMap<String, Object> estado, Semaphore s, Connection conn) throws RemoteException {
        super();
        this.estado = estado;
        this.s = s;
        this.conn = conn;

        // Inicializar las parcelas
        for (int i = 0; i < 5; i++) {
            HiloParcela parcela = new HiloParcela(i, estado);
            listaParcelas.add(parcela);
            parcela.start();
        }

        this.valvulaMaestraParcelas = conectarValvulaMaestra();
        if (this.valvulaMaestraParcelas == null) {
            throw new RuntimeException("No se pudo establecer la conexión inicial con la Válvula Maestra.");
        }

        this.exclusionService = conectarServicioExclusion();
        if (this.exclusionService == null) {
            throw new RuntimeException("No se pudo establecer la conexión inicial con el servicio de Exclusión Mutua.");
        }
    }

    /**
     * Intenta conectar con el servidor RMI de la Válvula Maestra.
     * Implementa reintentos con backoff exponencial.
     *
     * @return Referencia al servidor RMI o null si falla la conexión.
     */
    private IServerRMI conectarValvulaMaestra() {
        int maxRetries = 10;
        long delay = 1000;
        IServerRMI tempValvulaMaestraParcelas = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                String valvulaHost = System.getenv("VALVULA_MAESTRA_HOST");
                if (valvulaHost == null) valvulaHost = "localhost";
                String valvulaEnv = System.getenv("VALVULA_MAESTRA_PORT");
                int valvulaPort = (valvulaEnv != null) ? Integer.parseInt(valvulaEnv) : 21005;

                tempValvulaMaestraParcelas = (IServerRMI) Naming.lookup("rmi://" + valvulaHost + ":" + valvulaPort + "/ServerRMI");
                System.out.println("Controlador conectado exitosamente a la Válvula Maestra de Parcelas.");
                return tempValvulaMaestraParcelas; // Éxito
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                System.err.println("Error al conectar con la Válvula Maestra: " + e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        System.out.printf("Reintentando en %d ms... (Intento %d/%d)%n", delay, i + 2, maxRetries);
                        Thread.sleep(delay);
                        delay *= 2; // Backoff exponencial
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        System.err.println("Fallo al conectar con la Válvula Maestra después de " + maxRetries + " intentos.");
        return null; // Fallo
    }

    /**
     * Intenta conectar con el servidor RMI de Exclusión Mutua.
     * Implementa reintentos con backoff exponencial.
     *
     * @return Referencia al servicio de Exclusión Mutua o null si falla la conexión.
     */
    private IServicioExclusionMutua conectarServicioExclusion() {
        int maxRetries = 10;
        long delay = 1000;
        IServicioExclusionMutua tempExclusionService = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                String exclusionHost = System.getenv("EXCLUSION_HOST");
                if (exclusionHost == null) exclusionHost = "localhost";
                String portEnv = System.getenv("EXCLUSION_PORT");
                int port = (portEnv != null) ? Integer.parseInt(portEnv) : 10000;
                String url = "rmi://" + exclusionHost + ":" + port + "/servidorCentralEM";

                System.out.println("Intento " + (i + 1) + ": Conectando al servidor de exclusión mutua en " + url);
                tempExclusionService = (IServicioExclusionMutua) Naming.lookup(url);
                System.out.println("Controlador conectado exitosamente al servicio de Exclusión Mutua.");
                return tempExclusionService; // Éxito
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                System.err.println("Error al conectar con el servicio de Exclusión Mutua: " + e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        System.out.printf("Reintentando en %d ms... (Intento %d/%d)%n", delay, i + 2, maxRetries);
                        Thread.sleep(delay);
                        delay *= 2; // Espera exponencial
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        System.err.println("Fallo al conectar con el servicio de Exclusión Mutua después de " + maxRetries + " intentos.");
        return null; // Fallo
    }


    /**
     * Asocia un hilo receptor de datos de humedad con una parcela específica.
     *
     * @param hr la instancia de {@link HiloReceptorHumedad} que se va a asignar.
     * @param id el identificador de la parcela (0-4) a la que se asignará el sensor.
     */
    public void setSensorHumedad(HiloReceptorHumedad hr, int id) {
        listaParcelas.get(id).setHiloHumedad(hr);
    }

    /**
     * Asocia un hilo receptor de datos de un temporizador con una parcela específica.
     *
     * @param hr la instancia de {@link HiloReceptorTiempo} que se va a asignar.
     * @param id el identificador de la parcela (0-4) a la que se asignará el temporizador.
     * @throws IOException si ocurre un error al configurar el canal de comunicación con el temporizador.
     */
    public void setSensorTiempo(HiloReceptorTiempo hr, int id) throws IOException {
        listaParcelas.get(id).setHiloTiempo(hr);
    }




    /**
     * Método principal de ejecución del hilo controlador.
     * <p>
     * Se ejecuta en bucle continuo, verificando periódicamente si alguna parcela
     * necesita agua. Si hay demanda y no se posee el token, se solicita al
     * servidor de exclusión mutua. Si no hay demanda pero se posee el token, se
     * libera el recurso. Además, actualiza las variables globales y muestra el
     * estado de las parcelas y del sistema por consola.
     * </p>
     */
    @Override
    public void run() {
        while (true) {
            try {
                bdCounter -= 1;

                if (bdCounter == 0){
                    escribirBd();
                    bdCounter = 10;
                }
                if (exclusionService == null) {
                    System.err.println("La conexión con el servicio de exclusión se ha perdido. Intentando reconectar...");
                    exclusionService = conectarServicioExclusion();
                    if (exclusionService == null) {
                        Thread.sleep(5000); // Esperar antes del próximo intento
                        continue;
                    }
                }
                if (valvulaMaestraParcelas == null) {
                    System.err.println("La conexión con la Válvula Maestra se ha perdido. Intentando reconectar...");
                    valvulaMaestraParcelas = conectarValvulaMaestra();
                    if (valvulaMaestraParcelas == null) {
                        Thread.sleep(5000);
                        continue;
                    }
                }

                boolean demandaActual = algunaParcelaNecesitaAgua();

                if (demandaActual && !tieneAccesoBomba) {
                    System.out.println("Pidiendo token de acceso a bomba de agua...");
                    exclusionService.ObtenerRecurso(RECURSO_BOMBA, this);
                    this.s.acquire();
                } else if (!demandaActual && tieneAccesoBomba) {
                    valvulaMaestraParcelas.cerrarValvula();
                    exclusionService.DevolverRecurso(RECURSO_BOMBA);
                    tieneAccesoBomba = false;
                    System.out.println("Token devuelto");
                }

                this.temperatura = (double) this.estado.get("temperatura");
                this.radiacion = (double) this.estado.get("radiacion");
                this.lluvia = (boolean) this.estado.get("lluvia");

                mostrarEstadoParcelas();
                mostrarEstadoGeneral(demandaActual);

            } catch (RemoteException e) {
                System.err.println("Error RMI en HiloControlador: " + e.getMessage() + ". La conexión se intentará restablecer.");
                this.exclusionService = null;
                this.valvulaMaestraParcelas = null;
                this.tieneAccesoBomba = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("HiloControlador interrumpido");
                break; // Salir del bucle si se interrumpe
            }
            


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("HiloControlador interrumpido durante la espera");
                break;
            }
        }
    }

    private void escribirBd() {
        String sql = "INSERT INTO log (info) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Create a descriptive log message with a timestamp
            String logMessage = new Date() + " Estado:\n" + this.estado.toString();

            // Set the value for the first placeholder (?)
            pstmt.setString(1, logMessage);

            // Execute the insert statement
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Successfully wrote test entry to the log table.");
            } else {
                System.err.println("Warning: Test log entry was not written to the database.");
            }

        } catch (SQLException e) {
            System.err.println("Error writing to log table: " + e.getMessage());
        }
    }

    /**
     * Método invocado por el servidor de exclusión mutua cuando este cliente obtiene el token.
     *
     * @throws RemoteException si ocurre un error durante la comunicación RMI.
     */
    @Override
    public void RecibirToken() throws RemoteException {
        System.out.println("Token recibido para el recurso " + RECURSO_BOMBA);
        this.tieneAccesoBomba = true;
        try {
            if (valvulaMaestraParcelas != null) {
                valvulaMaestraParcelas.abrirValvula();
            } else {
                System.err.println("Token recibido pero la Válvula Maestra no está conectada");
            }
        } catch (RemoteException e) {
            System.err.println("Fallo al abrir la Válvula Maestra después de recibir el token: " + e.getMessage());
            this.valvulaMaestraParcelas = null; // Invalidar conexión
        }
        this.s.release();
    }

    /**
     * Devuelve el nombre del cliente remoto.
     *
     * @return El nombre del cliente.
     * @throws RemoteException si ocurre un error durante la comunicación RMI.
     */
    @Override
    public String getNombreCliente() throws RemoteException {
        return "Controlador de Riego";
    }

    /**
     * Determina si alguna de las parcelas necesita agua.
     *
     * @return {@code true} si al menos una parcela requiere riego; {@code false} en caso contrario.
     */
    private boolean algunaParcelaNecesitaAgua() {
        for (HiloParcela parcela : listaParcelas) {
            if (parcela.necesitaAgua()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Muestra en consola el estado actual de todas las parcelas en formato tabular.
     */
    private void mostrarEstadoParcelas() {
        System.out.println("\n========== ESTADO DE PARCELAS ==========");
        System.out.printf("%-8s | %-12s | %-10s | %-15s | %-12s%n",
                "Parcela", "Humedad (%)", "INR", "Electrovalvula", "Temporizador");
        System.out.println("------------------------------------------------------------------");

        for (int i = 0; i < listaParcelas.size(); i++) {
            HiloParcela parcela = listaParcelas.get(i);
            double humedad = parcela.getHumedad();
            double inr = parcela.getInr();
            boolean estaAbierta = false; // Por defecto cerrada si no hay conexión
            try {
                // Esta llamada puede fallar si la electroválvula está desconectada
                if (parcela.getElectrovalvula() != null) {
                    estaAbierta = parcela.getElectrovalvula().estaAbierta();
                }
            } catch (RemoteException e) {
                System.err.println("No se pudo obtener el estado de la electroválvula " + i + ". Asumiendo cerrada.");
            }
            boolean temporizadorActivo = parcela.getEstadoTemporizador() == 0;

            System.out.printf("%-8d | %-12.2f | %-10.3f | %-15s | %-12s%n",
                    i,
                    humedad,
                    inr,
                    (estaAbierta ? "ABIERTA" : "CERRADA"),
                    (temporizadorActivo ? "ACTIVO" : "APAGADO"));
        }
    }

    /**
     * Muestra el estado general del sistema, incluyendo variables ambientales y demanda de agua.
     *
     * @param demandaActual Indica si actualmente hay demanda de agua.
     */
    private void mostrarEstadoGeneral(boolean demandaActual) {
        System.out.println("\n========== ESTADO GENERAL ==========");
        System.out.printf("  Temperatura : %.2f °C%n", this.temperatura);
        System.out.printf("  Radiación   : %.2f W/m²%n", this.radiacion);
        System.out.printf("  Se necesita agua? : %s%n", (demandaActual ? "Sí" : "No"));
        System.out.printf("  Lloviendo   : %s%n", (this.lluvia ? "Sí" : "No"));
        System.out.println("=====================================\n");
    }
}