package hilos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona la conexión inicial de un dispositivo con el servidor y lo deriva
 * al hilo receptor correspondiente.
 *
 * <p>Esta clase actúa como un despachador. Cuando el servidor
 * principal ({@link net.ServerTCP}) acepta una nueva conexión, crea una instancia de
 * {@code HiloConexionTCP}. Este hilo es responsable de leer la primera línea
 * enviada por el cliente para identificar su tipo (ej: {@code "humedad"},
 * {@code "temperatura"}, {@code "lluvia"}, etc.).</p>
 *
 * <p>Según el tipo de dispositivo, se crea y se inicia un hilo receptor
 * especializado para manejar la comunicación continua con ese dispositivo. Para los sensores
 * que pertenecen a una parcela específica (humedad y temporizador), este hilo
 * también los registra en el {@link HiloControlador} principal para que
 * sean asignados a su {@link HiloParcela} correspondiente.</p>
 *
 * @author Brunardo19
 */
public class HiloConexionTCP extends Thread {
    /**
     * Socket de la conexión entrante del dispositivo.
     */
    private Socket s;

    String tipoDispositivo = "";

    /**
     * Mapa de estado global, compartido entre todos los hilos del sistema.
     */
    ConcurrentHashMap<String, Object> estado;
    /**
     * Referencia al hilo de control principal para registrar nuevos sensores.
     */
    HiloControlador hiloControlador;

    /**
     * Construye un nuevo hilo para gestionar la conexión inicial de un dispositivo.
     *
     * @param s               el {@link Socket} de la conexión del cliente.
     * @param estado          el mapa {@link ConcurrentHashMap} que contiene el estado global del sistema.
     * @param hiloControlador la instancia del hilo de control principal, necesaria
     *                        para registrar sensores específicos de parcela.
     */
    public HiloConexionTCP(Socket s, ConcurrentHashMap<String, Object> estado, HiloControlador hiloControlador) {
        this.s = s;
        this.estado = estado;
        this.hiloControlador = hiloControlador;
    }

    /**
     * Bucle principal de ejecución del hilo.
     *
     * <p>Realiza los siguientes pasos:</p>
     * <ol>
     *   <li>Lee el tipo de dispositivo desde el flujo de entrada del socket.</li>
     *   <li>Para dispositivos de parcela, lee su identificador (ID).</li>
     *   <li>Utiliza una estructura {@code switch} para determinar el tipo de dispositivo:</li>
     *   <ul>
     *      <li>Crea e inicia el hilo receptor adecuado para el dispositivo.</li>
     *      <li>Si el dispositivo es un sensor de humedad o un temporizador, lo registra
     *          en el {@link HiloControlador} para asociarlo a la parcela correcta.</li>
     *   </ul>
     *   <li>Si el tipo de dispositivo no se reconoce, imprime un mensaje en consola.</li>
     * </ol>
     *
     * @throws RuntimeException si ocurre un error de entrada/salida durante la comunicación inicial.
     */
    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            tipoDispositivo = br.readLine();
            int id;
            switch (tipoDispositivo) {
                case "humedad":
                    id = Integer.parseInt(br.readLine()); // Leer id
                    System.out.printf("---Conectado sensor humedad %d---\n", id);
                    HiloReceptorHumedad receptorHumedad = new HiloReceptorHumedad(s);
                    receptorHumedad.start();
                    hiloControlador.setSensorHumedad(receptorHumedad, id);
                    break;
                case "temperatura":
                    System.out.println("---Conectado sensor temperatura---");
                    HiloReceptorTemperatura receptorT = new HiloReceptorTemperatura(s, estado);
                    receptorT.start();
                    break;
                case "lluvia":
                    System.out.println("---Conectado sensor lluvia---");
                    HiloReceptorLluvia receptorL = new HiloReceptorLluvia(s, estado);
                    receptorL.start();
                    break;
                case "temporizador":
                    id = Integer.parseInt(br.readLine()); // Leer id
                    System.out.printf("---Conectado temporizador %d---\n", id);
                    HiloReceptorTiempo receptorTiempo = new HiloReceptorTiempo(s);
                    receptorTiempo.start();
                    hiloControlador.setSensorTiempo(receptorTiempo, id);
                    break;
                case "iluminacion":
                    System.out.println("---Conectado sensor iluminacion---");
                    HiloReceptorIluminacion receptorIluminacion = new HiloReceptorIluminacion(s, estado);
                    receptorIluminacion.start(); // Nota: se ejecuta en el mismo hilo
                    break;
                default:
                    System.out.println("Disposivo no reconocido");
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}