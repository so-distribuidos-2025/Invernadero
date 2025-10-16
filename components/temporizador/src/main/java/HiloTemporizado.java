import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Hilo principal de comunicación para el cliente Temporizador.
 * Gestiona el estado y la comunicación con el servidor en un único bucle.
 */
public class HiloTemporizado extends Thread {

    private final Socket cnxServidor;
    private final PrintWriter pw;
    private final BufferedReader br;

    // Referencia al temporizador actualmente en ejecucion
    private Temporizador temporizadorActual = null;

    public HiloTemporizado(Socket s, PrintWriter pw) {
        this.cnxServidor = s;
        this.pw = pw;
        try {
            this.br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el lector del socket", e);
        }
    }

    @Override
    public void run() {
        try {
            // Bucle de comunicación principal
            while (!Thread.currentThread().isInterrupted()) {

                // Informar al servidor del estado actual
                if (temporizadorActual != null && temporizadorActual.estaCorriendo()) {
                    pw.println(0); // Estado: OCUPADO
                } else {
                    pw.println(1); // Estado: LISTO
                }

                // Esperar un comando del servidor (bloqueante)
                String comandoStr = br.readLine();
                if (comandoStr == null) {
                    System.out.println("El servidor cerró la conexión.");
                    break;
                }

                // Procesar el comando
                try {
                    int segundos = Integer.parseInt(comandoStr);
                    procesarComando(segundos);
                } catch (NumberFormatException e) {
                    System.err.println("Comando no válido recibido del servidor: " + comandoStr);
                }
            }
        } catch (IOException e) {
            System.err.println("Error de comunicación con el servidor: " + e.getMessage());
        } finally {
            cerrarConexion();
        }
    }

    /**
     * Lógica para actuar según el comando recibido.
     * @param segundos El valor numérico del comando.
     */
    private void procesarComando(int segundos) {
        if (segundos > 0) {
            System.out.println("Recibido comando para iniciar temporizador de " + segundos + " segundos.");

            // Si hay un temporizador antiguo, lo detenemos primero.
            if (temporizadorActual != null && temporizadorActual.estaCorriendo()) {
                temporizadorActual.parar();
            }

            // Creamos e iniciamos el nuevo temporizador.
            temporizadorActual = new Temporizador(segundos);
            temporizadorActual.iniciar(segundos);

        } else {
            System.out.println("Recibido comando para detener el temporizador.");
            if (temporizadorActual != null && temporizadorActual.estaCorriendo()) {
                temporizadorActual.parar();
            }
        }
    }

    private void cerrarConexion() {
        if (temporizadorActual != null && temporizadorActual.estaCorriendo()) {
            temporizadorActual.parar();
        }
        try {
            if (cnxServidor != null && !cnxServidor.isClosed()) {
                cnxServidor.close();
                System.out.println("Conexión cerrada.");
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }
}