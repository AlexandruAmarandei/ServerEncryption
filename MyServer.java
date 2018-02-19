
import java.io.IOException;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.*;

/**
 * This is the main server class. It has 2 ways to communicate with the client.
 * <p>
 * First one is the RMI remote call. In this case, the serverCommunicator acts
 * simply as a storage class.
 * <p>
 * Second is the classic socket connection. In this case, the serverCommunicator
 * acts as a thread until it resolves all the calls.
 *
 */
public class MyServer implements Runnable, ServerInterface {

    /**
     * This hashmap stores all the users by using an ID for each one and a
     * server thread.
     */
    private HashMap<Integer, ServerCommunicator> users = new HashMap<>();
    //Number of IDs and the current port on which the server resides
    private int currentIDCount, port;
    private String sotonServerName = "svm-tjn1f15-comp2207.ecs.soton.ac.uk";

    //Those are used for the socket part
    private ServerSocket thisServer;
    private Thread serverThread;
    //This is to store all possible prime numbers smaller than 1000.
    //Never should 2 users have the same prime numbers.
    private boolean ciur[] = new boolean[1000];
    private Random random;

    /**
     * Those are used to get the rmi registry and the stud class
     */
    public CiphertextInterface serverInterface;
    private Registry reg;

    /**
     * Creates a new server and connects it to the University of Southampton
     * server through an RMI interface.
     *
     * @param port port for the server
     */
    public MyServer(int port) {
        this.port = port;
        thisServer = null;
        serverThread = null;
        random = new Random();
        //Create ciur
        makeCiur();
        currentIDCount = 0;
        try {
            //The the soton registy
            Registry registry = LocateRegistry.getRegistry(sotonServerName, 12345);
            //And then try to get the stub class
            serverInterface = (CiphertextInterface) registry.lookup("CiphertextProvider");

        } catch (RemoteException | NotBoundException ex) {
            System.err.println("Error in reaching soton server");
            System.err.println(ex);
        }
    }

    /**
     * Creates a ciur of prime numbers
     */
    public final void makeCiur() {
        int limit = (int) Math.sqrt(ciur.length);
        for (int i = 0; i < ciur.length; i++) {
            ciur[i] = true;
        }
        ciur[0] = ciur[1] = false;
        for (int i = 2; i <= limit; i++) {
            for (int j = 1; j * i < ciur.length; j++) {
                ciur[j * i] = false;
            }
        }
    }

    /**
     * Get a random P for the key calculation
     *
     * @return random prime P
     */
    public int getRandomP() {
        int p;
        do {
            p = random.nextInt(ciur.length - 1);
        } while (ciur[p] == false);
        ciur[p] = true;
        return p;
    }

    /**
     * Get a random G for the key calculation smaller than p.
     *
     * @param p G should be smaller than p
     * @return return G
     */
    public int getRandomG(int p) {
        int g;
        do {
            g = random.nextInt(p - 1);
        } while (ciur[g] == false);
        ciur[g] = true;
        return g;
    }

    /**
     * Start the socket communication
     */
    public final void start() {
        serverThread = new Thread(this);
        serverThread.start();

    }

    /**
     * Run the server and wait for users to connect using socket communication
     */
    @Override
    public void run() {
        while (serverThread != null) {
            try {
                //When a new user tries to connect we add it to the users hashmap
                addUser(thisServer.accept());
            } catch (IOException e) {
                System.err.println("Problem with connection");
                System.err.println(e);
                serverThread = null;
            }
        }
    }

    /**
     * Adds a user from a socket connection while assigning it it's variables p
     * g and a and it's ID.
     *
     * @param userToAdd User socket to be added
     */
    public void addUser(Socket userToAdd) {
        //Assign random values to the suer
        int p, g, a;
        p = getRandomP();
        g = getRandomG(p);
        a = random.nextInt(4096) + 4096;
        int newID = currentIDCount++;
        //Add the user to the map
        users.put(newID, new ServerCommunicator(this, userToAdd, p, g, a, newID));
        try {
            //Set it's input and output and then start the thread
            users.get(newID).setIO();
            users.get(newID).start();

        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * This function adds an user for RMI communication.
     *
     * @param ID The ID of the user
     */
    public synchronized void addUserRMI(int ID) {
        int p, g, a;
        p = getRandomP();
        g = getRandomG(p);
        a = random.nextInt(4096) + 4096;
        ServerCommunicator st = new ServerCommunicator(this, ID, p, g, a);
        users.put(ID, st);
    }

    /**
     * Starts the server. Default way to communicate: rmi Change startRMI value
     * to start socket communication.
     *
     * @param args
     */
    public static void main(String args[]) {
        MyServer chatServer = new MyServer(1500);
        boolean startRMI = true;
        if (startRMI) {
            chatServer.startRMI();
        } else {
            chatServer.startSocket();
        }
    }

    /**
     * Starts the socket server.
     */
    public void startSocket() {
        try {

            thisServer = new ServerSocket(port);
            //System.out.println("Connection establieshed on port:" + port);
            start();
        } catch (IOException ex) {
            System.err.println("Error while opening port");
            System.err.println(ex);
        }
    }

    /**
     * Starts the rmi server by creating a registry and exporthing this stud
     * class on port 1500. Then binds them.
     */
    public void startRMI() {
        
        //First we set the new policyFile
        String directory = System.getProperty("user.dir");
        System.setProperty("java.security.policy", "file:policy.policy");
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        ServerInterface stub;
        try {
            stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 1098);
            try{
                reg = LocateRegistry.getRegistry();
            }catch (Exception e){
                try{ reg = LocateRegistry.createRegistry(1099);
                }catch (Exception ex){
                    reg = LocateRegistry.getRegistry(1099);
                }
            }
            
            reg.rebind(" MyServer ", stub);
        } catch (RemoteException e) {
            System.err.println("Failed to register; is the rmiregistry running?");
            System.err.println(e.getMessage());

        }
    }

    /**
     * Gets the cipher from the server.
     *
     * @param name Username
     * @param key key
     * @return New cipher from soton server.
     */
    public String getCipher(String name, int key) {
        String result = "";
        try {
            result = serverInterface.get(name, key);
        } catch (RemoteException e) {
            System.err.println("Problem while getting the cipher from server.");
            System.err.println(e);
        }
        return result;
    }

    /**
     * Gets the next available ID to give to a client.
     *
     * @return nextID
     */
    public int getNextAvailableID() {
        int nextID = currentIDCount++;
        return nextID;
    }

    /**
     * Unlocks the key stored locally with information from the user.
     *
     * @param key Locally contained key
     * @param lock Lock to resolve key
     * @return True value of key
     */
    public int unlockKey(String key, int lock) {
        char[] stringKey = key.toCharArray();
        String resultKey = "";
        for (int i = 0; i < stringKey.length; i++) {
            char temp = (char) (stringKey[stringKey.length - i - 1] - (char) lock);
            resultKey = resultKey.concat(Character.toString(temp));
        }

        return Integer.parseInt(resultKey);
    }

    /**
     * A new client requests a new set of primes values to create a key. The
     * server adds the new client to the client list.
     *
     * @param name Client name
     * @return A string containing the new variables from which the key can be
     * obtained
     * @throws RemoteException
     */
    @Override
    public synchronized String getPrimes(String name) throws RemoteException {
        int newID = getNextAvailableID();
        addUserRMI(newID);
        return users.get(newID).getPrimes(name) + " " + Integer.toString(newID);
    }

    /**
     * This function sets the Y in the client and while creating the key it
     * locks it.
     *
     * @param response Y value from client
     * @param lock Lock to lock the key
     * @param ID ID of user
     * @throws RemoteException
     */
    @Override
    public synchronized void setY(String response, int lock, int ID) throws RemoteException {
        if (users.containsKey(ID)) {
            users.get(ID).setY(response, lock);
        }
    }

    /**
     * Gets the cipher for the user by requesting a cipher with the user's key.
     * To get the true value of the key, the user provides the lock to unlock
     * the key.
     *
     * @param lock For unlocking the key
     * @param ID ID of user to send info to
     * @return A new cipher
     * @throws RemoteException
     */
    @Override
    public synchronized String getCipher(int lock, int ID) throws RemoteException {
        if (users.containsKey(ID)) {
            int key = unlockKey(users.get(ID).getKey(), lock);
            String name = users.get(ID).name;
            return serverInterface.get(name, key);
        } else {
            return "WRONGIDZ";
        }
    }

    /**
     * Removes the user and makes it's values free.
     *
     * @param ID User to be delited
     */
    protected synchronized void removeUser(int ID) {
        ServerCommunicator toRemove = users.get(ID);
        ciur[toRemove.getP()] = true;
        ciur[toRemove.getG()] = true;
        users.remove(ID);
    }

}
