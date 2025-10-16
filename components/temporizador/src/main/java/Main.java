import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {

    private PrintWriter pw;
    private String id;
    private Socket socketCliente;
    private static final int TCP_PORT = 20000;
    private static final String SERVER_NAME = "localhost";

    public Main() {
    }

    String obtenerID(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java Temporizador <id>");
            return " ";
        }
        String id = args[0];
        return id;
    }

    InetAddress obtenerIPServidor() {
        InetAddress ipServidor = null;
        try {
            String serverName = System.getenv("CONTROLADOR_HOST");
            if (serverName == null) serverName = "localhost";
            ipServidor = InetAddress.getByName(serverName);
        } catch (UnknownHostException e) {
            System.err.println("No se pudo establecer la conexion en el puerto " + TCP_PORT);
            throw new RuntimeException("Error al crear la conexión", e);
        }
        return ipServidor;

    }

    Socket crearConexion() {
        Socket nuevoSocketCliente = null;
        InetAddress ipServidor = obtenerIPServidor();
        try {
            String portEnv = System.getenv("CONTROLADOR_PORT");
            int tcpPort = (portEnv != null) ? Integer.parseInt(portEnv) : 20000;
            nuevoSocketCliente = new Socket(ipServidor, tcpPort);
            System.out.println("Conectado al servidor: " + nuevoSocketCliente);
        } catch (IOException e) {
            System.err.println("Error al conectar al servidor en " + SERVER_NAME + ":" + TCP_PORT);
            throw new RuntimeException(e);
        }
        return nuevoSocketCliente;

    }

    OutputStream obtenerOutputStream(Socket s) {
        OutputStream outputStream;
        try {
            outputStream = s.getOutputStream();
        } catch (IOException e) {
            System.err.println("No se pudo obtener el OutputStream del socket");
            throw new RuntimeException("Error al crear canal de salida", e);
        }
        return outputStream;
    }


    void execute(String[] args) {

        id = obtenerID(args);
        socketCliente = crearConexion();
        OutputStream outputStream = obtenerOutputStream(socketCliente);
        pw = new PrintWriter(outputStream, true);
        pw.println("temporizador");
        pw.println(id);
        HiloTemporizado hiloTemporizado = new HiloTemporizado(socketCliente, pw);
        hiloTemporizado.start();

    }

    public static void main(String[] args) {
        Main main = new Main();
        main.execute(args);
    }
}
