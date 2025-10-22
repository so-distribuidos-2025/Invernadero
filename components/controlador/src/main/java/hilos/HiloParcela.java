package hilos;

import rmi.IServerRMI;
import util.INR;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class HiloParcela extends Thread {
    private HiloReceptorHumedad hiloHumedad;
    private HiloReceptorTiempo hiloTiempo;

    private IServerRMI electrovalvula;

    private int id;
    private boolean lluvia;
    private double radiacion;
    private double temperatura;
    private PrintWriter timeWriter;
    volatile int estadoTemporizador;
    volatile double humedad;
    volatile double inr;
    ConcurrentHashMap estado;
    private volatile boolean estaRegando = false;
    private volatile boolean necesitaAgua = false;

    public HiloParcela(int id, ConcurrentHashMap estado) {
        this.id = id;
        this.humedad = 0;
        this.estado = estado;
        this.radiacion = (Double) estado.get("radiacion");
        this.lluvia = (Boolean) estado.get("lluvia");
        this.temperatura = (Double) estado.get("temperatura");
        this.inr = 0;

        this.electrovalvula = conectarElectrovalvula();
        if (this.electrovalvula == null) {
            System.err.println("Conexión inicial a la electroválvula " + id + " fallida. Se reintentará en el ciclo 'run'.");
        }
    }

    private IServerRMI conectarElectrovalvula() {
        int maxRetries = 10;
        long delay = 1000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                String valvulaHost = System.getenv("VALVULA_HOST");
                if (valvulaHost == null) {
                    valvulaHost = "localhost";
                } else {
                    valvulaHost = String.format(valvulaHost, id);
                }
                String basePortEnv = System.getenv("VALVULA_BASE_PORT");
                int basePort = (basePortEnv != null) ? Integer.parseInt(basePortEnv) : 21000;
                int puerto = basePort + id;
                String direccionRMI = String.format("rmi://" + valvulaHost + ":%d/ServerRMI", puerto);

                System.out.println("Intento " + (i + 1) + ": Conectando a la electroválvula " + id + " en " + direccionRMI);
                IServerRMI server = (IServerRMI) Naming.lookup(direccionRMI);
                System.out.println("Conectado a la electroválvula " + id);
                return server; // Éxito
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                System.err.println("Error al conectar a la electroválvula " + id + ": " + e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        System.err.println("Reintentando en " + delay / 1000 + " segundos...");
                        Thread.sleep(delay);
                        delay *= 2; // Backoff exponencial
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        System.err.println("No se pudo conectar a la electroválvula " + id + " después de " + maxRetries + " intentos.");
        return null; // Fallo
    }

    public void setHiloHumedad(HiloReceptorHumedad hiloHumedad) {
        this.hiloHumedad = hiloHumedad;
    }

    public void setHiloTiempo(HiloReceptorTiempo hiloTiempo) throws IOException {
        this.hiloTiempo = hiloTiempo;
        this.timeWriter = new PrintWriter(hiloTiempo.getClienteTiempo().getOutputStream(), true);
    }

    public double getHumedad() {
        return humedad;
    }

    public double getInr() {
        return inr;
    }

    public IServerRMI getElectrovalvula() {
        return electrovalvula;
    }

    public int getEstadoTemporizador() {
        return estadoTemporizador;
    }

    public boolean necesitaAgua() {
        return this.necesitaAgua;
    }


    @Override
    public void run() {
        while (true) {
            try {
                if (this.electrovalvula == null) {
                    System.err.println("Electrovalvula " + id + " desconectada. Intentando reconectar...");
                    this.electrovalvula = conectarElectrovalvula();
                    if (this.electrovalvula == null) {
                        Thread.sleep(5000); // Esperar antes de intentar de nuevo
                        continue;
                    }
                }

                if (this.hiloHumedad == null || this.hiloTiempo == null) {
                    Thread.sleep(2000);
                    System.out.println("Parcela " + id + " esperando sensor de humedad o temporizador.");
                    continue;
                }

                this.radiacion = (Double) this.estado.get("radiacion");
                this.lluvia = (Boolean) this.estado.get("lluvia");
                this.temperatura = (Double) this.estado.get("temperatura");
                this.estadoTemporizador = this.hiloTiempo.getEstadoTemporizador();
                this.humedad = this.hiloHumedad.getHumedad();

                boolean necesitaRegarAhora;
                if (lluvia) {
                    this.inr = 0;
                    necesitaRegarAhora = false;
                } else {
                    this.inr = INR.calcularInr(humedad, radiacion, temperatura);
                    necesitaRegarAhora = inr > 0.7;
                }

                if (estaRegando) {
                    boolean temporizadorTermino = (estadoTemporizador == 1);
                    if (lluvia || temporizadorTermino) {
                        //System.out.println("Parcela " + this.id + " - DETENIENDO RIEGO.");
                        estaRegando = false;
                        this.necesitaAgua = false;
                        electrovalvula.cerrarValvula();
                        if (!temporizadorTermino && timeWriter != null) {
                            timeWriter.println(0);
                        }
                    }
                } else {
                    boolean temporizadorListo = (estadoTemporizador == 1);
                    if (necesitaRegarAhora && temporizadorListo) {
                        //System.out.println("Parcela " + this.id + " - INICIANDO RIEGO (INR: " + String.format("%.2f", inr) + ")");
                        estaRegando = true;
                        this.necesitaAgua = true;
                        electrovalvula.abrirValvula();
                        int duracion = 300;
                        if (inr > 0.9) duracion = 600;
                        else if (inr > 0.8) duracion = 420;
                        if (timeWriter != null) timeWriter.println(duracion);
                    }
                }

                Thread.sleep(500);
            } catch (RemoteException e) {
                System.err.println("Error RMI en HiloParcela " + id + ": " + e.getMessage() + ". La conexión se restablecerá.");
                this.electrovalvula = null;
                // Si se estaba regando, debemos asumir que se detuvo.
                this.estaRegando = false;
                this.necesitaAgua = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}