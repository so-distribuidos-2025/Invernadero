import java.util.TimerTask;

/**
 *
 * @author lesca
 */
public class ControlRespuesta extends TimerTask {
    
    DetectorFallo detector;
    
    public ControlRespuesta(DetectorFallo d){
        detector = d;
    }

    @Override
    public void run() {
        if (!detector.getNombre().equals("ServidorMaestro")) {
            detector.chequearRespuesta();
        }
    }
    
}
