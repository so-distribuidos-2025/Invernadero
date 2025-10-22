import rmi.IClienteEM;
import rmi.IServicioExclusionMutua;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Implementación del servidor de exclusión mutua con múltiples recursos.
 * Cada recurso tiene su propio token y cola de espera.
 */
public class ServerExclusionMutuaRMI extends UnicastRemoteObject implements IServicioExclusionMutua {

    // Representa el estado de un recurso: token libre/ocupado + cola de clientes en espera
    private static class Recurso {
        boolean token;
        Queue<IClienteEM> cola;

        Recurso() {
            this.token = true;
            this.cola = new ArrayDeque<>();
        }
    }

    private final Map<String, Recurso> recursos;

    public ServerExclusionMutuaRMI() throws RemoteException {
        super();
        this.recursos = new HashMap<>();
    }

    /**
     * Solicita acceso a un recurso. Si el token está libre, se entrega al cliente;
     * si no, se lo pone en la cola.
     */
    @Override
    public synchronized void ObtenerRecurso(String nombreRecurso, IClienteEM cliente) throws RemoteException {
        Recurso recurso = recursos.computeIfAbsent(nombreRecurso, k -> new Recurso());

        if (recurso.token) {
            recurso.token = false;
            System.out.println("Token entregado a cliente \"" + cliente.getNombreCliente() + "\" para recurso: " + nombreRecurso);
            cliente.RecibirToken();
        } else {
            System.out.println("Recurso ocupado, cliente \"" + cliente.getNombreCliente() + "\" en espera: " + nombreRecurso);
            recurso.cola.add(cliente);
        }
    }

    /**
     * Devuelve el token de un recurso. Si hay clientes esperando, se le pasa al siguiente;
     * si no, se marca el token como libre.
     */
    @Override
    public synchronized void DevolverRecurso(String nombreRecurso) throws RemoteException {
        Recurso recurso = recursos.get(nombreRecurso);
        if (recurso == null) return; // recurso no registrado

        if (!recurso.cola.isEmpty()) {
            IClienteEM siguiente = recurso.cola.poll();
            System.out.println("Token pasado al siguiente cliente en cola para recurso: " + nombreRecurso);
            siguiente.RecibirToken();
        } else {
            recurso.token = true;
            System.out.println("Token liberado para recurso: " + nombreRecurso);
        }
    }

    /**
     * Consulta el estado actual de un recurso: LIBRE o OCUPADO.
     */
    public synchronized String consultarEstado(String nombreRecurso) throws RemoteException {
        Recurso recurso = recursos.get(nombreRecurso);
        if (recurso == null || recurso.token) {
            return "LIBRE";
        } else {
            return "OCUPADO";
        }
    }
}
