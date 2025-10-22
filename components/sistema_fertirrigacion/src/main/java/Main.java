import rmi.IClienteEM;
import rmi.IServicioExclusionMutua;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Main extends UnicastRemoteObject implements IClienteEM {

    private static final String CLIENTE_ID = "SistemaFertirrigacion";
    private static final String RECURSO_BOMBA = "BombaAgua";

    // Constantes para la lógica de reintentos de conexión ---
    /** Número máximo de intentos para conectar con el servidor de exclusión mutua. */
    private static final int MAX_INTENTOS_CONEXION = 10;
    /** Tiempo de espera en milisegundos entre cada intento de conexión. */
    private static final int ESPERA_ENTRE_INTENTOS_MS = 5000; // 5 segundos

    // volatile es crucial para asegurar la visibilidad entre el hilo principal y el hilo RMI del callback.
    private volatile boolean tieneAccesoBomba = false;

    /**
     * Constructor. Llama a super() para exportar este objeto como un objeto remoto,
     * permitiendo que el servidor de exclusión mutua pueda invocar `RecibirToken()`.
     * @throws RemoteException si hay un error durante la exportación RMI.
     */
    public Main() throws RemoteException {
        super();
    }

    /**
     * Este es el método de callback que el servidor de exclusión mutua invocará
     * cuando nos conceda el acceso al recurso.
     */
    @Override
    public void RecibirToken() throws RemoteException {
        System.out.println("\n[" + CLIENTE_ID + "] -> Token RECIBIDO. Acceso a '" + RECURSO_BOMBA + "' CONCEDIDO.");
        this.tieneAccesoBomba = true;
    }

    @Override
    public String getNombreCliente(){
        return "Sistema de Fertirrigacion";
    }

    /**
     * Bucle principal que solicita el recurso, espera a recibirlo, lo utiliza y lo libera.
     */
    public void ejecutarCiclo() {
        try {
            IServicioExclusionMutua exclusion = conectarAlServidorExclusion();

            // Si después de todos los intentos no se pudo conectar, el programa termina.
            if (exclusion == null) {
                System.err.println("No se pudo establecer la conexión inicial con el servidor. Abortando el programa.");
                return;
            }

            // El ciclo principal comienza solo si la conexión fue exitosa.
            while (true) {
                System.out.println("\n[" + CLIENTE_ID + "] -> Intentando iniciar ciclo de fertirrigación. Solicitando acceso a '" + RECURSO_BOMBA + "'...");

                // Solicitar el recurso. La llamada es asíncrona.
                // Pasamos 'this' como el cliente al que el servidor debe llamar de vuelta.
                exclusion.ObtenerRecurso(RECURSO_BOMBA, this);

                // Esperar hasta que el callback 'RecibirToken' ponga la bandera en true.
                // Usamos Thread.sleep para evitar un busy-wait que consuma 100% de CPU.
                System.out.println("[" + CLIENTE_ID + "] -> Esperando el token...");
                while (!this.tieneAccesoBomba) {
                    Thread.sleep(500); // Esperar medio segundo antes de volver a comprobar.
                }

                // Una vez que tenemos acceso, realizamos el trabajo.
                // Usamos un bloque try-finally para GARANTIZAR que el recurso se libera.
                try {
                    System.out.println("[" + CLIENTE_ID + "] -> Proceso de fertirrigación en curso... (Duración: 10 segundos)");
                    Thread.sleep(10000);
                    System.out.println("[" + CLIENTE_ID + "] -> Proceso de fertirrigación FINALIZADO.");
                } finally {
                    // 4. Liberar el recurso para que otros puedan usarlo.
                    System.out.println("[" + CLIENTE_ID + "] -> Recurso '" + RECURSO_BOMBA + "' LIBERADO.");
                    exclusion.DevolverRecurso(RECURSO_BOMBA);
                    this.tieneAccesoBomba = false; // Resetear la bandera para el siguiente ciclo.
                }

                // Esperar un tiempo antes de intentar el siguiente ciclo.
                System.out.println("[" + CLIENTE_ID + "] -> Esperando 20 segundos para el próximo ciclo.");
                Thread.sleep(20000);
            }

        } catch (Exception e) {
            System.err.println("Ha ocurrido un error crítico en el Sistema de Fertirrigación:");
            e.printStackTrace();
        }
    }

    /**
     * --- NUEVO MÉTODO ---
     * Intenta conectar con el servidor de exclusión mutua RMI.
     * Realiza varios intentos si la conexión falla, esperando un tiempo entre cada uno.
     * @return El objeto remoto del servicio si la conexión es exitosa, o null si falla después de todos los intentos.
     */
    private IServicioExclusionMutua conectarAlServidorExclusion() {
        String exclusionHost = System.getenv("EXCLUSION_HOST");
        if (exclusionHost == null) exclusionHost = "localhost";

        String exclusionPortEnv = System.getenv("EXCLUSION_PORT");
        int exclusionPort = (exclusionPortEnv != null) ? Integer.parseInt(exclusionPortEnv) : 10000;

        String url = "rmi://" + exclusionHost + ":" + exclusionPort + "/servidorCentralEM";

        for (int intento = 1; intento <= MAX_INTENTOS_CONEXION; intento++) {
            try {
                System.out.println("Intentando conectar con el servidor de Exclusión Mutua en '" + url + "' (Intento " + intento + "/" + MAX_INTENTOS_CONEXION + ")...");
                IServicioExclusionMutua servicio = (IServicioExclusionMutua) Naming.lookup(url);
                System.out.println("¡Conexión establecida con éxito!");
                return servicio; // Si la conexión es exitosa, retornamos el objeto y salimos del metodo.
            } catch (NotBoundException | RemoteException e) {
                System.err.println("Fallo en la conexión: " + e.getMessage());
                if (intento < MAX_INTENTOS_CONEXION) {
                    try {
                        System.out.println("Reintentando en " + (ESPERA_ENTRE_INTENTOS_MS / 1000) + " segundos...");
                        Thread.sleep(ESPERA_ENTRE_INTENTOS_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // Buena práctica para manejar interrupciones.
                        System.err.println("La espera fue interrumpida. Abortando conexión.");
                        return null;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error inesperado durante el intento de conexión:");
                e.printStackTrace();
            }
        }

        System.err.println("No se pudo conectar al servidor de Exclusión Mutua después de " + MAX_INTENTOS_CONEXION + " intentos.");
        return null; // Si el bucle termina sin éxito, retornamos null.
    }

    public static void main(String[] args) {
        try {
            System.out.println("Iniciando Sistema de Fertirrigación...");
            Main cliente = new Main();
            cliente.ejecutarCiclo();
        } catch (RemoteException e) {
            System.err.println("No se pudo crear el cliente RMI para Fertirrigación:");
            e.printStackTrace();
        }
    }
}