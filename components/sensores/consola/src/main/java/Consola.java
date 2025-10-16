import rmi.ISensorRMI;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Consola de depuración interactiva para controlar los sensores del sistema de invernadero.
 * <p>
 * Esta clase proporciona una interfaz de línea de comandos (CLI) para enviar comandos
 * a los diferentes sensores a través de RMI. Todos los sensores se buscan en un
 * único puerto de registro RMI, diferenciados por su nombre de servicio.
 * </p>
 */
public class Consola {

    private final Scanner sc;
    private final ISensorRMI sensorTemperatura;
    private final Map<Integer, ISensorRMI> sensoresHumedad = new HashMap<>();
    private final ISensorRMI sensorRadiacion;
    private final ISensorRMI sensorLluvia;

    // Puerto RMI centralizado para todos los servicios.
    private static final int RMI_PORT = 22000;

    /**
     * Constructor de la clase Consola.
     * Inicializa las conexiones RMI con los sensores y entra en el bucle principal
     * para procesar los comandos del usuario.
     */
    public Consola() {
        sc = new Scanner(System.in);
        System.out.println("--- SOD 2025 Consola depuracion ---");
        System.out.println("Conectando a los sensores RMI en el puerto " + RMI_PORT + "...");

        // Todos los sensores se conectan al mismo puerto pero con nombres de servicio diferentes.
        sensorTemperatura = conexionRMI("SensorTemperaturaRMI");

        // Conectar a los 5 sensores de humedad (IDs 0 a 4)
        for (int i = 0; i < 5; i++) {
            String serviceName = "SensorHumedadRMI" + i;
            ISensorRMI sensor = conexionRMI(serviceName);
            if (sensor != null) {
                sensoresHumedad.put(i, sensor);
            }
        }

        sensorRadiacion   = conexionRMI("SensorRadiacionRMI");
        sensorLluvia      = conexionRMI("SensorLluviaRMI");

        System.out.println("\n--- Lista para recibir comandos. Escriba 'help' para ayuda. ---");

        // Bucle principal para leer y procesar comandos
        while (true) {
            System.out.print("> ");
            String command = sc.nextLine();
            if (command.equalsIgnoreCase("exit")) {
                break;
            }
            parseCommand(command);
        }

        System.out.println("Cerrando consola.");
        sc.close();
    }

    /**
     * Intenta establecer una conexión RMI con un sensor usando el puerto RMI compartido.
     *
     * @param serviceName El nombre con el que el servicio RMI fue publicado en el registro.
     * @return Una instancia del stub {@link ISensorRMI} si la conexión es exitosa, o {@code null} si falla.
     */
    public ISensorRMI conexionRMI(String serviceName) {
        try {
            String sensorRmiHost = System.getenv("SENSOR_RMI_HOST");
            if (sensorRmiHost == null) sensorRmiHost = "localhost";

            String sensorRmiPortEnv = System.getenv("SENSOR_RMI_PORT");
            int rmiPort = (sensorRmiPortEnv != null) ? Integer.parseInt(sensorRmiPortEnv) : 22000;

            String direccionRMI = String.format("rmi://%s:%d/%s", sensorRmiHost, rmiPort, serviceName);
            ISensorRMI sensor = (ISensorRMI) Naming.lookup(direccionRMI);
            System.out.println("  [OK] Conectado a " + serviceName);
            return sensor;
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            System.err.println("  [ERROR] Fallo al conectar a " + serviceName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Analiza el comando introducido por el usuario y lo delega al manejador correspondiente.
     *
     * @param command La línea de texto introducida por el usuario.
     */
    void parseCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        String[] args = command.trim().split("\\s+");
        String sensorType = args[0].toLowerCase();

        switch (sensorType) {
            case "temperatura":
                handleSensorCommand(sensorTemperatura, "Temperatura", args);
                break;
            case "humedad":
                handleHumedadCommand(args);
                break;
            case "radiacion":
                handleSensorCommand(sensorRadiacion, "Radiacion", args);
                break;
            case "lluvia":
                handleLluviaCommand(sensorLluvia, args);
                break;
            case "help":
                printHelp();
                break;
            case "exit":
                // Manejado en el bucle principal.
                break;
            default:
                System.out.println("Comando no reconocido: '" + sensorType + "'. Escriba 'help' para ver los comandos disponibles.");
                break;
        }
    }

    /**
     * Gestiona los comandos para los sensores de humedad, que requieren un ID.
     *
     * @param args Los argumentos del comando, ej: ["humedad", "1", "set", "60.5"]
     */
    private void handleHumedadCommand(String[] args) {
        if (args.length < 3) {
            System.out.println("Comando 'humedad' incompleto. Uso: humedad <id> <set|mode> [valor]");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("El ID del sensor de humedad debe ser un número. '" + args[1] + "' no es válido.");
            return;
        }

        ISensorRMI sensor = sensoresHumedad.get(id);
        if (sensor == null) {
            System.err.println("El sensor de humedad con ID " + id + " no está conectado o no existe.");
            return;
        }

        String sensorName = "Humedad[" + id + "]";
        String operation = args[2].toLowerCase();

        try {
            switch (operation) {
                case "set":
                    if (args.length < 4) {
                        System.out.println("Falta el valor para 'set'. Ejemplo: humedad " + id + " set 50.0");
                        return;
                    }
                    double value = Double.parseDouble(args[3]);
                    sensor.setValor(value);
                    System.out.println(sensorName + " -> valor establecido a " + value);
                    break;
                case "mode":
                    if (args.length < 4) {
                        System.out.println("Falta el modo para 'mode'. Use 'auto' o 'manual'.");
                        return;
                    }
                    String mode = args[3].toLowerCase();
                    if (mode.equals("auto")) {
                        sensor.setAuto(true);
                        System.out.println(sensorName + " -> modo establecido a AUTOMÁTICO");
                    } else if (mode.equals("manual")) {
                        sensor.setAuto(false);
                        System.out.println(sensorName + " -> modo establecido a MANUAL");
                    } else {
                        System.out.println("Modo no reconocido: '" + mode + "'. Use 'auto' o 'manual'.");
                    }
                    break;
                default:
                    System.out.println("Operación no reconocida para " + sensorName + ": '" + operation + "'. Use 'set' o 'mode'.");
                    break;
            }
        } catch (RemoteException e) {
            System.err.println("Error de RMI al comunicarse con " + sensorName + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("El valor proporcionado '" + args[3] + "' no es un número válido.");
        }
    }

    /**
     * Gestiona los comandos para sensores genéricos (temperatura, radiación).
     *
     * @param sensor La instancia del sensor RMI a controlar.
     * @param sensorName El nombre del sensor para los mensajes de log.
     * @param args Los argumentos del comando.
     */
    private void handleSensorCommand(ISensorRMI sensor, String sensorName, String[] args) {
        if (sensor == null) {
            System.err.println("El sensor de " + sensorName + " no está conectado. No se puede ejecutar el comando.");
            return;
        }
        if (args.length < 2) {
            System.out.println("Faltan argumentos para el comando de " + sensorName + ". Escriba 'help'.");
            return;
        }

        String operation = args[1].toLowerCase();
        try {
            switch (operation) {
                case "set":
                    if (args.length < 3) {
                        System.out.println("Falta el valor para la operación 'set'. Ejemplo: set 25.0");
                        return;
                    }
                    double value = Double.parseDouble(args[2]);
                    sensor.setValor(value);
                    System.out.println(sensorName + " -> valor establecido a " + value);
                    break;
                case "mode":
                    if (args.length < 3) {
                        System.out.println("Falta el modo para la operación 'mode'. Use 'auto' o 'manual'.");
                        return;
                    }
                    String mode = args[2].toLowerCase();
                    if (mode.equals("auto")) {
                        sensor.setAuto(true);
                        System.out.println(sensorName + " -> modo establecido a AUTOMÁTICO");
                    } else if (mode.equals("manual")) {
                        sensor.setAuto(false);
                        System.out.println(sensorName + " -> modo establecido a MANUAL");
                    } else {
                        System.out.println("Modo no reconocido: '" + mode + "'. Use 'auto' o 'manual'.");
                    }
                    break;
                default:
                    System.out.println("Operación no reconocida para " + sensorName + ": '" + operation + "'.");
                    break;
            }
        } catch (RemoteException e) {
            System.err.println("Error de RMI al comunicarse con el sensor de " + sensorName + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("El valor proporcionado '" + args[2] + "' no es un número válido.");
        }
    }

    /**
     * Gestiona los comandos específicos para el sensor de lluvia, que solo acepta valores 0 o 1.
     *
     * @param sensor La instancia del sensor de lluvia RMI.
     * @param args Los argumentos del comando.
     */
    private void handleLluviaCommand(ISensorRMI sensor, String[] args) {
        if (sensor == null) {
            System.err.println("El sensor de Lluvia no está conectado. No se puede ejecutar el comando.");
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
            System.out.println("Uso inválido para el sensor de lluvia. Ejemplo: lluvia set <0|1>");
            return;
        }

        try {
            double value = Double.parseDouble(args[2]);
            if (value == 0 || value == 1) {
                sensor.setValor(value);
                System.out.println("Lluvia -> estado establecido a " + (int) value + " (" + (value == 1 ? "lloviendo" : "no lloviendo") + ")");
            } else {
                System.out.println("Valor inválido para lluvia. Use 0 (no lloviendo) o 1 (lloviendo).");
            }
        } catch (RemoteException e) {
            System.err.println("Error de RMI al comunicarse con el sensor de Lluvia: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("El valor proporcionado '" + args[2] + "' no es un número válido.");
        }
    }

    /**
     * Imprime en la consola un mensaje de ayuda con la lista de comandos disponibles y su sintaxis.
     */
    private void printHelp() {
        System.out.println("\n--- Ayuda de Comandos ---");
        System.out.println("Uso general: <sensor> <operacion> [valor]");
        System.out.println("  Para sensor de humedad: humedad <id> <operacion> [valor]");
        System.out.println("Sensores disponibles: temperatura, humedad (IDs 0-4), radiacion, lluvia");
        System.out.println("\nOperaciones:");
        System.out.println("  set <valor>        - Establece un valor manual para el sensor.");
        System.out.println("                       Para 'lluvia', <valor> debe ser 0 (seco) o 1 (lloviendo).");
        System.out.println("  mode <auto|manual> - Cambia el modo del sensor (no aplica a 'lluvia').");
        System.out.println("\nComandos adicionales:");
        System.out.println("  help               - Muestra esta ayuda.");
        System.out.println("  exit               - Cierra la consola.");
        System.out.println("\nEjemplos:");
        System.out.println("  > temperatura set 25.5");
        System.out.println("  > humedad 1 mode auto");
        System.out.println("  > humedad 2 set 65.0");
        System.out.println("  > lluvia set 1");
        System.out.println("------------------------\n");
    }
}