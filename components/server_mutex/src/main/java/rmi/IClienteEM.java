package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author lesca
 */
public interface IClienteEM extends Remote{
    
    public void RecibirToken() throws RemoteException;
    
}
