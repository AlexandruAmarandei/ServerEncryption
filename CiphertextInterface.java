/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author alexa
 */
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author alexa
 */
public interface CiphertextInterface extends Remote {

    /**
     *
     * @param uid
     * @param key
     * @return
     * @throws RemoteException
     */
    public String get(String uid, int key)
            throws RemoteException;
}
