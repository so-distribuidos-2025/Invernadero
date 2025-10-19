package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ISensorRMI extends Remote {
    void setValor(double valor) throws RemoteException;
    void setAuto(boolean auto) throws RemoteException;
}