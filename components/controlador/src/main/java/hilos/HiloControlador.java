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

    /**
     * Referencia al servicio remoto de exclusión mutua.
     * Utilizado para solicitar y liberar el acceso a la bomba de agua.
     */
    private final IServicioExclusionMutua exclusionService;

    /**
     * Referencia al servidor remoto que controla la válvula maestra de riego.
     */
    private final IServerRMI valvulaMaestraParcelas;

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
     * @throws RemoteException si ocurre un error al exportar el objeto remoto.
     */
    public HiloControlador(ConcurrentHashMap<String, Object> estado) throws RemoteException {
        super();
        this.estado = estado;
        this.temperatura = 0.0;
        this.radiacion = 0.0;
        this.lluvia = false;

        // Inicialización de estado global
        this.estado.put("temperatura", temperatura);
        this.estado.put("radiacion", radiacion);
        this.estado.put("lluvia", lluvia);

        // Inicializar 5 parcelas y lanzar sus hilos
        for (int i = 0; i < 5; i++) {
            HiloParcela parcela = new HiloParcela(i, estado);
            listaParcelas.add(parcela);
            parcela.start();
        }

        try {
            String exclusionHost = System.getenv("EXCLUSION_HOST");
            if (exclusionHost == null) {
                exclusionHost = "localhost";
            }

            String portEnv = System.getenv("EXCLUSION_PORT");
            int port = (portEnv != null) ? Integer.parseInt(portEnv) : 10000;
            System.out.println("rmi://"+exclusionHost+":"+port+"/servidorCentralEM");

            // Conexión con el servicio de exclusión mutua (token por recurso)
            this.exclusionService = (IServicioExclusionMutua) Naming.lookup("rmi://"+exclusionHost+":"+port+"/servidorCentralEM");
            System.out.println("Controlador conectado al servicio de Exclusión Mutua.");

            String valvulaHost = System.getenv("VALVULA_MAESTRA_HOST");
            if (valvulaHost == null) {
                valvulaHost = "localhost";
            }
            String valvulaEnv = System.getenv("VALVULA_MAESTRA_PORT");
            int valvulaPort = (valvulaEnv != null) ? Integer.parseInt(valvulaEnv) : 21005;

            // Conexión con la válvula maestra de parcelas
            this.valvulaMaestraParcelas = (IServerRMI) Naming.lookup("rmi://"+valvulaHost+":"+valvulaPort+"/ServerRMI");
            System.out.println("Controlador conectado a la Válvula Maestra de Parcelas.");

        } catch (NotBoundException e) {
            System.err.println("Error crítico al conectar con servicios RMI: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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
                boolean demandaActual = algunaParcelaNecesitaAgua();

                if (demandaActual && !tieneAccesoBomba) {
                    // Solicitar acceso exclusivo al recurso
                    exclusionService.ObtenerRecurso(RECURSO_BOMBA, this);
                } else if (!demandaActual && tieneAccesoBomba) {
                    // Liberar recurso cuando ya no hay demanda
                    valvulaMaestraParcelas.cerrarValvula();
                    exclusionService.DevolverRecurso(RECURSO_BOMBA);
                    tieneAccesoBomba = false;
                }

                // Actualizar estado global
                this.temperatura = (double) this.estado.get("temperatura");
                this.radiacion = (double) this.estado.get("radiacion");
                this.lluvia = (boolean) this.estado.get("lluvia");

                mostrarEstadoParcelas();
                mostrarEstadoGeneral(demandaActual);

            } catch (RemoteException e) {
                System.err.println("Error RMI en HiloControlador: " + e.getMessage());
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println("HiloControlador interrumpido");
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Método remoto invocado por el servidor de exclusión mutua cuando se otorga el token.
     * <p>
     * Al recibir el token, el controlador abre la válvula maestra para permitir el
     * riego de las parcelas.
     * </p>
     *
     * @throws RemoteException si ocurre un error en la comunicación remota.
     */
    @Override
    public void RecibirToken() throws RemoteException {
        System.out.println("Token recibido para recurso " + RECURSO_BOMBA);
        this.tieneAccesoBomba = true;
        valvulaMaestraParcelas.abrirValvula();
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
    private void mostrarEstadoParcelas() throws RemoteException {
        System.out.println("\n========== ESTADO DE PARCELAS ==========");
        System.out.printf("%-8s | %-12s | %-10s | %-15s | %-12s%n",
                "Parcela", "Humedad (%)", "INR", "Electrovalvula", "Temporizador");
        System.out.println("------------------------------------------------------------------");

        for (int i = 0; i < listaParcelas.size(); i++) {
            HiloParcela parcela = listaParcelas.get(i);
            double humedad = parcela.getHumedad();
            double inr = parcela.getInr();
            boolean estaAbierta = parcela.getElectrovalvula().estaAbierta();
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
     * Muestra el estado general de las variables ambientales y la demanda de agua.
     *
     * @param demandaActual valor booleano que indica si existe demanda en el ciclo actual.
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
