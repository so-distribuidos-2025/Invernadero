/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import rmi.IServerRMI;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementación del servidor RMI que representa una {@code Electrovalvula}.
 * <p>
 * Esta clase extiende {@link UnicastRemoteObject} y expone métodos definidos en
 * la interfaz remota {@link IServerRMI}, lo que permite controlar la apertura
 * y cierre de una válvula de manera remota.
 * </p>
 *
 * <b>Características principales:</b>
 * <ul>
 *   <li>Permite abrir y cerrar la electrovalvula.</li>
 *   <li>Expone el estado actual de la válvula (abierta o cerrada).</li>
 *   <li>Se identifica con un {@code id} único, asociado al servidor RMI correspondiente.</li>
 * </ul>
 *
 * Ejemplo de uso remoto:
 * <pre>{@code
 * IServerRMI server = (IServerRMI) Naming.lookup("rmi://localhost:21001/ServerRMI");
 * server.abrirValvula();
 * boolean abierta = server.estaAbierta();
 * }</pre>
 *
 * @author 
 */
public class ServerRMI extends UnicastRemoteObject implements IServerRMI {

    /** Estado de la electrovalvula: {@code true} si está abierta, {@code false} si está cerrada. */
    private boolean estaAbierta;

    /** Identificador único del servidor (se usa también para mostrar mensajes). */
    private final int id;

    /**
     * Constructor del servidor RMI.
     *
     * @param id Identificador único del servidor asociado a esta electrovalvula.
     * @throws RemoteException si ocurre un error al exportar el objeto RMI.
     */
    public ServerRMI(int id) throws RemoteException {
        super();
        this.id = id;
    }

    /**
     * Indica si la electrovalvula se encuentra abierta.
     *
     * @return {@code true} si la válvula está abierta, {@code false} si está cerrada.
     * @throws RemoteException si ocurre un error en la invocación remota.
     */
    @Override
    public boolean estaAbierta() throws RemoteException {
        return estaAbierta;
    }

    /**
     * Abre la electrovalvula y muestra un mensaje en consola.
     *
     * @throws RemoteException si ocurre un error en la invocación remota.
     */
    @Override
    public void abrirValvula() throws RemoteException {
        if (!this.estaAbierta) {
            this.estaAbierta = true;
            System.out.printf("%s | Se abrió la electrovalvula %d\n", this.getTiempo(), id);
        }
    }

    /**
     * Cierra la electrovalvula y muestra un mensaje en consola.
     *
     * @throws RemoteException si ocurre un error en la invocación remota.
     */
    public void cerrarValvula() throws RemoteException {
        if (this.estaAbierta) {
            this.estaAbierta = false;
            System.out.printf("%s | Se cerró la electrovalvula %d\n", this.getTiempo(), id);
        }
    }

    private String getTiempo(){
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        return myDateObj.format(myFormatObj);
    }
}
