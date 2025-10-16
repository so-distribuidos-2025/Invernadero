import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase principal de la aplicación que inicia un servidor RMI
 * y gestiona la simulación de una {@link Electrovalvula}.
 * <p>
 * El flujo de ejecución es el siguiente:
 * <ol>
 *   <li>Lee un identificador ({@code id}) desde los argumentos de la línea de comandos.</li>
 *   <li>Crea una instancia de {@link Electrovalvula} y muestra su estado inicial.</li>
 *   <li>Crea un objeto {@link ServerRMI} asociado al identificador.</li>
 *   <li>Inicia un hilo {@link HiloServerRMI} para publicar el servidor en el registro RMI.</li>
 *   <li>Vuelve a imprimir el estado de la electrovalvula.</li>
 * </ol>
 *
 * <b>Uso:</b><br>
 * {@code java Main <id>} <br>
 * Donde {@code id} es un entero que determina el puerto de publicación
 * del servidor RMI ({@code 21000 + id}).
 *
 * <b>Ejemplo:</b><br>
 * {@code java Main 1} → inicia el servidor en el puerto {@code 21001}.
 *
 * @author 
 */
public class Main {

    /**
     * Método principal que lanza la aplicación.
     *
     * @param args Argumentos de línea de comandos. El primer argumento debe ser un número entero (id).
     */
    public static void main(String[] args) {
        // Se obtiene el ID del primer argumento recibido por consola
        int id = Integer.parseInt(args[0]);

        // Objeto servidor RMI (se inicializa más adelante)
        ServerRMI server = null;

        // Se crea una electrovalvula simulada
        Electrovalvula ev = new Electrovalvula();

        try {
            // Se instancia el servidor RMI pasando el identificador
            server = new ServerRMI(id);

            // Se inicia un hilo que levantará el servidor RMI en el puerto 21000 + id
            HiloServerRMI hServer = new HiloServerRMI(server, id);
            hServer.start();

        } catch (RemoteException ex) {
            // Manejo de error en caso de que no pueda crearse el servidor RMI
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
