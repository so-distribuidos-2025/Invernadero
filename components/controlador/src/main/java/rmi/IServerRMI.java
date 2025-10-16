package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServerRMI extends Remote {
    void abrirValvula() throws RemoteException;
    void cerrarValvula() throws RemoteException;
    boolean estaAbierta() throws RemoteException;
}
