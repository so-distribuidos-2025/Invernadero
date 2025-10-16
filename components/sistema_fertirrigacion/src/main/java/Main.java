import rmi.IClienteEM;
import rmi.IServicioExclusionMutua;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Main extends UnicastRemoteObject implements IClienteEM {

    private static final String CLIENTE_ID = "SistemaFertirrigacion";
    private static final String RECURSO_BOMBA = "BombaAgua";

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

    /**
     * Bucle principal que solicita el recurso, espera a recibirlo, lo utiliza y lo libera.
     */
    public void ejecutarCiclo() {
        try {
            // Conectar al servicio de exclusión mutua
            String exclusionHost = System.getenv("EXCLUSION_HOST");
            if (exclusionHost == null) exclusionHost = "localhost";

            // ATENCIÓN: El puerto por defecto correcto es 10000
            String exclusionPortEnv = System.getenv("EXCLUSION_PORT");
            int exclusionPort = (exclusionPortEnv != null) ? Integer.parseInt(exclusionPortEnv) : 10000;

            // ATENCIÓN: El nombre del servicio debe coincidir con el del Controlador: "ExclusionMutua"
            IServicioExclusionMutua exclusion = (IServicioExclusionMutua) Naming.lookup("rmi://" + exclusionHost + ":" + exclusionPort + "/ExclusionMutua");
            System.out.println("Conexión establecida con el servidor de Exclusión Mutua.");

            while (true) {
                System.out.println("\n[" + CLIENTE_ID + "] -> Intentando iniciar ciclo de fertirrigación. Solicitando acceso a '" + RECURSO_BOMBA + "'...");

                // 1. Solicitar el recurso. La llamada es asíncrona.
                //    Pasamos 'this' como el cliente al que el servidor debe llamar de vuelta.
                exclusion.ObtenerRecurso(RECURSO_BOMBA, this);

                // 2. Esperar hasta que el callback 'RecibirToken' ponga la bandera en true.
                //    Usamos Thread.sleep para evitar un busy-wait que consuma 100% de CPU.
                System.out.println("[" + CLIENTE_ID + "] -> Esperando el token...");
                while (!this.tieneAccesoBomba) {
                    Thread.sleep(500); // Esperar medio segundo antes de volver a comprobar.
                }

                // 3. Una vez que tenemos acceso, realizamos el trabajo.
                //    Usamos un bloque try-finally para GARANTIZAR que el recurso se libera.
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

                // 5. Esperar un tiempo antes de intentar el siguiente ciclo.
                System.out.println("[" + CLIENTE_ID + "] -> Esperando 20 segundos para el próximo ciclo.");
                Thread.sleep(20000);
            }

        } catch (Exception e) {
            System.err.println("Ha ocurrido un error crítico en el Sistema de Fertirrigación:");
            e.printStackTrace();
        }
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