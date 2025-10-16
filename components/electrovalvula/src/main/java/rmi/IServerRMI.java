/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author lesca
 */
public interface IServerRMI extends Remote {
    void abrirValvula() throws RemoteException;
    void cerrarValvula() throws RemoteException;
    boolean estaAbierta() throws RemoteException;
}
