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
public interface IDetectorFalla extends Remote {
    void DameMensaje(IDetectorFalla cliente, String mensaje) throws RemoteException;
}
