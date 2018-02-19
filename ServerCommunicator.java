
import java.io.*;
import java.net.*;

/**
 * This class has two purposes. One, if the server is communicating trough RMI then it just stores the values of the client such as the locked key.
 * Second, if the server is communicating by sockets, then starts a thread which communicates with the server.
 * @author Alexandru Amarandei Stanescu aas1u16 University of Southampton
 */
public class ServerCommunicator extends Thread {

    private Socket socket;
    private MyServer server;
    private int ID;
    public String name;
    private DataInputStream in;
    private DataOutputStream out;
    private int p, g, a, key, x, y;
    private boolean rmi;
    private String keyLocked;

    /** 
     * Constructor to create a server by socket communication.
     * @param server Server reference
     * @param socket Socket to open the communication on
     * @param p p value
     * @param g g value
     * @param a a value
     * @param ID ID of client
     */
    public ServerCommunicator(MyServer server, Socket socket, int p, int g, int a, int ID) {
        super();

        this.socket = socket;
        setValues(p, g, a, server, ID, false);
    }

    /**
     * Constructor to create a server by RMI communication.
     * @param server Server reference
     * @param p p value
     * @param g g value
     * @param a a value
     * @param ID ID of client
     */
    public ServerCommunicator(MyServer server, int ID, int p, int g, int a) {
        super();
        setValues(p, g, a, server, ID, true);
    }

    private final void setValues(int p, int g, int a, MyServer server, int ID, boolean rmi) {
        this.ID = ID;
        this.rmi = rmi;
        this.server = server;
        this.p = p;
        this.g = g;
        this.a = a;
        x = powerModulo(1, g, a, p);
    }


    /**
     * This function is used to compute the modulo of a number multiplied by a
     * factor, power number of times.
     * <p>
     * Example:
     * <blockquote><pre>
     * powerModulo(2,3,4,5) = (2*3*3*3*3)%5
     * </pre></blockquote>
     *
     * @param number Initial number
     * @param factor Factor
     * @param power Power
     * @param modulo Mod
     * @return The result
     */
    
    public int powerModulo(int number, int factor, int power, int modulo) {
        int solution = number % modulo;
        for (int i = 1; i <= power; i++) {
            solution = solution * factor;
            solution = solution % modulo;
        }
        return solution;
    }

    /**
     * Connect server input and output.
     * @throws IOException
     */
    public void setIO() throws IOException {
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    @Override
    public void run() {

        if (rmi == false) {
            String toSend;
            toSend = Integer.toString(x) + " " + Integer.toString(g) + " " + Integer.toString(p) + " " + Integer.toString(ID);
            sendMessege(toSend);
            try {
                name = in.readUTF();
                String response = in.readUTF();
                y = Integer.parseInt(response);
                key = powerModulo(y, g, a, p);
                sendMessege(server.getCipher(name, key));
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    /**
     * Closes this part.
     * @throws IOException
     */
    public void close() throws IOException {
        socket.close();
        in.close();
        out.close();
    }

    /**
     * Sends a message to output channel.
     * @param msg Message to send
     */
    public void sendMessege(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error in printing the msg");
            server.removeUser(ID);
        }
    }
    
    /**
     * Locks a key.
     * @param key Key to be locked
     * @param lock Lock
     * @return 
     */
    private String lockKey(int key, int lock) {
        char[] stringKey = Integer.toString(key).toCharArray();
        String resultKey = "";
        for (int i = 0; i < stringKey.length; i++) {
            char temp = (char) (stringKey[stringKey.length - i - 1] + (char) lock);
            resultKey = resultKey.concat(Character.toString(temp));
        }

        return resultKey;
    }

    /**
     * Gets the primes from this client and sets it's name.
     * @param name Name of client.
     * @return Primes as a string.
     */
    public String getPrimes(String name) {
        this.name = name;
        return Integer.toString(x) + " " + Integer.toString(g) + " " + Integer.toString(p);
    }

    /**
     * Sets Y.
     * @param response Response from client
     * @param lock locker to crypt key.
     */
    public void setY(String response, int lock) {
        y = Integer.parseInt(response);
        keyLocked = lockKey(powerModulo(y, g, a, p), lock);
    }

    /**
     * Gets the locked key.
     * @return The locked key.
     */
    protected String getKey() {
        return keyLocked;
    }

    protected int getP() {
        return p;
    }

    protected int getG() {
        return g;
    }

}
