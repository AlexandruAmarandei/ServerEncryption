
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI interface.
 * @author Alexandru Amarandei Stanescu aas1u16 University of Southampton
 */
public interface ServerInterface extends Remote {

    /**
     * Users requests primes from server while providing it's username.
     * @param name Name of the user
     * @return Primes plus ID as a string
     * @throws RemoteException
     */
    public String getPrimes(String name) throws RemoteException;

    /**
     * The user send to the server the Y obtained from computation.
     * @param response Y
     * @param lock Value to lock the key with.
     * @param ID User ID
     * @throws RemoteException
     */
    public void setY(String response, int lock, int ID) throws RemoteException;

    /**
     * Users request a cipher from the server.
     * @param lock Lock value to unlock the key 
     * @param ID ID of user
     * @return A new cipher.
     * @throws RemoteException
     */
    public String getCipher(int lock, int ID) throws RemoteException;

}
