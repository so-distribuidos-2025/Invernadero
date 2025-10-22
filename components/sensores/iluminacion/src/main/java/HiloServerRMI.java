import rmi.ISensorRMI;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class HiloServerRMI extends UnicastRemoteObject implements ISensorRMI {
    private final HiloSensor sensor;

    public HiloServerRMI(HiloSensor sensor) throws RemoteException {
        super();
        this.sensor = sensor;
    }

    @Override
    public void setValor(double valor) throws RemoteException {
        sensor.setValor(valor);
    }

    @Override
    public void setAuto(boolean auto) throws RemoteException {
        sensor.setAuto(auto);
    }
}