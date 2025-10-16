/*
 * Clase HiloServerRMI
 * -------------------
 * Este hilo permite iniciar un servidor RMI (Remote Method Invocation)
 * en un puerto dinámico calculado a partir de un identificador.
 *
 * Cada instancia de este hilo crea un registro RMI en el puerto correspondiente
 * y publica un objeto remoto que implementa la interfaz {@link interfaces.IServerRMI}.
 */

import rmi.IServerRMI;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hilo encargado de levantar un servidor RMI en un puerto específico.
 * <p>
 * Su funcionamiento es el siguiente:
 * <ul>
 *   <li>Calcula el puerto de escucha como {@code 21000 + id}.</li>
 *   <li>Crea un registro RMI en ese puerto.</li>
 *   <li>Publica el objeto remoto {@link IServerRMI} bajo la URL
 *       {@code rmi://localhost:puerto/ServerRMI}.</li>
 * </ul>
 *
 * Esto permite ejecutar múltiples instancias de servidores RMI en puertos diferentes,
 * diferenciados por el parámetro {@code id}.
 *
 * <b>Ejemplo:</b><br>
 * Si {@code id = 1}, el servidor se publicará en {@code rmi://localhost:21001/ServerRMI}.
 *
 * @author lesca
 */
public class HiloServerRMI extends Thread {

    /** Objeto remoto que implementa la interfaz de servidor RMI */
    private final IServerRMI server;

    /** Identificador del servidor, usado para calcular el puerto de publicación */
    private final int id;

    /**
     * Constructor del hilo del servidor RMI.
     *
     * @param s  objeto remoto que implementa {@link IServerRMI}
     * @param id identificador del servidor (se usa para determinar el puerto)
     */
    public HiloServerRMI(IServerRMI s, int id) {
        this.server = s;
        this.id = id;
    }

    /**
     * Ejecuta el hilo, creando un registro RMI y publicando el servidor remoto.
     * <p>
     * El puerto de escucha se define como {@code 21000 + id}.
     */
    @Override
    public void run() {
        try {

            String portEnv = System.getenv("VALVULA_BASE_PORT");
            if (portEnv == null) {
                portEnv = "21000";
            }

            int port = Integer.parseInt(portEnv);

            port += this.id;

            // Crea el registro RMI en el puerto calculado
            LocateRegistry.createRegistry(port);

            String hostname = System.getenv("HOSTNAME");
            if (hostname == null) {
                hostname = "localhost";
            }
            Naming.rebind(String.format("rmi://%s:%d/ServerRMI", hostname, port), server);

        } catch (RemoteException ex) {
            Logger.getLogger(HiloServerRMI.class.getName()).log(Level.SEVERE, 
                    "Error al iniciar el servidor RMI", ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(HiloServerRMI.class.getName()).log(Level.SEVERE, 
                    "URL mal formada para el servidor RMI", ex);
        }
    }
}
