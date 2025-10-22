import rmi.IDetectorFalla;
import rmi.IServicioExclusionMutua;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author lesca
 */
public class Main {

    public static void main(String[] args) {

        String hostname = System.getenv("HOSTNAME");
        if (hostname == null) {
            hostname = "localhost";
        }

        String nombreServidor = "ServidorMaestro";
        String detectorPortEnv = System.getenv("DETECTOR_FALLOS_PORT");
        int nroPuerto = (detectorPortEnv != null) ? Integer.parseInt(detectorPortEnv) : 9000;

        String exclusionPortEnv = System.getenv("EXCLUSION_PORT");
        int nroPuertoExcMutua = (exclusionPortEnv != null) ? Integer.parseInt(exclusionPortEnv) : 10000;
        
        Timer planificador = new Timer();


        try {

            //Obviar el detector de fallos

            /*
            LocateRegistry.createRegistry(nroPuerto);
            DetectorFallo detector = new DetectorFallo();
            detector.setNombre(nombreServidor);

            Naming.rebind("rmi://" + hostname + ":" + nroPuerto + "/" + nombreServidor, (IDetectorFalla) detector);


            AveriguarEstado tareaAveriguarEstado = new AveriguarEstado(detector);
            ControlRespuesta tareaControlRespuesta = new ControlRespuesta(detector);
            planificador.schedule(tareaAveriguarEstado, 0, 5000);
            planificador.schedule(tareaControlRespuesta, 0, 8000);
             */


            LocateRegistry.createRegistry(nroPuertoExcMutua);

            System.out.println("Puerto RMI creado en: " + nroPuertoExcMutua);

            ServerExclusionMutuaRMI serverEM = new ServerExclusionMutuaRMI();

            Naming.rebind("rmi://" + hostname + ":" + nroPuertoExcMutua + "/servidorCentralEM", (IServicioExclusionMutua) serverEM);

            System.out.println("Inicado en "+ "rmi://" + hostname + ":" + nroPuertoExcMutua + "/servidorCentralEM");

        } catch (RemoteException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

}


