/*
 * Clase principal SensorHumedad1
 * ------------------------------
 * Representa el programa cliente que simula un sensor de humedad.
 * 
 * Funcionalidades principales:
 *  - Se conecta al servidor central en la dirección IP y puerto especificados.
 *  - Envía su tipo de dispositivo ("humedad") y un identificador único.
 *  - Inicia un hilo (HiloSensado) que genera y transmite valores de humedad al servidor.
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {

    /**
     * Método principal del programa.
     * 
     * @param args Argumentos de línea de comandos.
     *             El primer argumento (args[0]) corresponde al identificador del sensor.
     */
    public static void main(String[] args) {
        // Identificador de las parcelas, pasado como argumento
        String id = args[0];

        InetAddress ipServidor = null;
        PrintWriter pw;

        try {
            // Dirección IP del servidor (localhost en este caso)
            ipServidor = InetAddress.getByName("localhost");

            // Crear conexión con el servidor en el puerto 20000
            Socket cliente = new Socket(ipServidor, 20000);
            System.out.println("Conectado al servidor: " + cliente);

            // Flujo de salida con autoflush activado para enviar datos
            pw = new PrintWriter(cliente.getOutputStream(), true);

            // Enviar tipo de dispositivo e identificador al servidor
            pw.println("humedad");
            pw.println(id);

            // Crear e iniciar el hilo que simula el sensado de humedad
            HiloSensado sensor = new HiloSensado(cliente, pw);
            sensor.start();

        } catch (UnknownHostException e) {
            throw new RuntimeException("No se pudo resolver el host del servidor", e);
        } catch (IOException e) {
            throw new RuntimeException("Error de E/S en la conexión con el servidor", e);
        }
    }
}

