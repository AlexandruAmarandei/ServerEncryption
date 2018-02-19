
import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Client Thread mainly used in socket communication.
 * @author Alexandru Amarandei Stanescu aas1u16 University of Southampton
 */
public class ClientThread extends Thread {

    private MyClient user;
    private Socket socket;
    private DataInputStream in;
    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    /**
     * Creates the communicator on socket.
     * @param user User object creating this thread
     * @param socket Socket to create on
     */
    public ClientThread(MyClient user, Socket socket) {
        this.user = user;
        this.socket = socket;
        try {
            in = new DataInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            System.out.println("Problem in reading input");
        }
    }

    @Override
    public void run() {
        try {
            user.setPrimes(in.readUTF());
        } catch (IOException e) {
        }
        int numberOfDecodes = 0;
        while (numberOfDecodes < 1) {
            try {
                queue.add(in.readUTF());
                numberOfDecodes++;
            } catch (IOException e) {
                System.out.println("Error in reading the output");
                user.stop();
            }
        }
    }

    /**
     * Terminates this
     */
    public void terminate() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.out.println("Error in closing the input");
        }
    }

    /**
     * Gets next cipher while trough a queue that simply wait for the server to respond.
     * @return
     */
    public String getNextCipher() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            System.err.println(e);
            return "";
        }
    }
}
