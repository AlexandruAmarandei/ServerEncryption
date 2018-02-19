import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

/**
 * This represents my client class. It has 2 ways to communicate with the
 * server. First trough the required RMI interface with method calls to a remote
 * server object. Second one is trough socket communication. The second one is
 * faster and should be more stable. I've coded the socket communication as an
 * extra.
 *
 * @author Alexandru Amarandei Stanescu aas1u16 University of Southampton
 */
public class MyClient implements Runnable {

    //Those are the saved parapeters: the username that will be passed to the host on adress host on port port
    //If they don't receive any arguments, then they start with the default values
    private final String userName, host;
    private final int port;

    //Socket implementation variables
    private Socket socket;
    private DataOutputStream out;
    private Thread userThread;
    private ClientThread user;

    //RMI implementation variables
    private int y, x, p, g, b;
    private final Random random;
    //The key is obtained as in the CW spec. ID is given by the server in order to diferrentiate other users
    //This might represent a security thread of loss of data. If someone manages to pick up on the ID then it might pose as another user.
    private int key, ID;
    //Extra lock to keep the keys on the server safe
    private final int lock;

    /**
     * Creates a new object from:
     *
     * @param host Host name
     * @param userName Username to send to get cipher
     * @param port Port to call to
     */
    public MyClient(String host, String userName, int port) {
        random = new Random();
        //The lock is a relatively small value, the encryption is nothing too fancy, just an extra measure of security
        lock = random.nextInt(9) + 1;
        userThread = null;
        this.host = host;
        this.userName = userName;
        this.port = port;
        socket = null;
    }

    /**
     * This method stars the server on sockets. The methods of RMI and socket
     * connection are very similar.
     */
    private void connectAndStart() {
        try {
            //Connect on port port to host host :))
            socket = new Socket(host, port);
            //Get a random b for the computation of the key
            b = random.nextInt(4096) + 4096;
            //We want to be sure we make this a prime for extra security
            while (!isPrime(y)) {
                b = random.nextInt(4096) + 4096;
            }
            //Map output of this client to socket
            assignOut();
        } catch (IOException e) {
            System.err.println("Error in trying to reach a port");
            System.err.println(e);
        }
    }

    /**
     * This function obtains the RMI server object.
     */
    private void RMIServerConnect() {
        ServerInterface server = null;
        Registry reg;
        try {
            //First locates registry
            reg = LocateRegistry.getRegistry(host, port);
            //Then searches for a MyServer reference to obtain the stub class
            server = (ServerInterface) reg.lookup(" MyServer ");

        } catch (RemoteException | NotBoundException e) {
            System.err.println("Error while connecting to RMI server:");
            System.err.println(e);
        }
        if (server != null) {
            try {

                setPrimes(server.getPrimes(userName));
                server.setY(Integer.toString(y), lock, ID);

                System.out.println(resolveCipher(server.getCipher(lock, ID)));
            } catch (RemoteException e) {
                System.out.println("Something went wrong on server connection");
                System.out.println(e);
            }

        } else {
            System.out.println("Server not found");
        }
    }

    /**
     * This function checks if a number is prime.
     *
     * @param number Number to check
     * @return True if the number is prime, otherwise False
     */
    public boolean isPrime(int number) {
        double sqroot = Math.sqrt((double) number);
        for (int i = 2; i < sqroot; i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * This function sets the primes from which the key is calculated. The
     * values are contained in a string, each devided by a single empty space.
     *
     * @param primes Values split by 1 empty space
     */
    protected void setPrimes(String primes) {
        String[] numbers = primes.split(" ");
        try {
            x = Integer.parseInt(numbers[0]);
            g = Integer.parseInt(numbers[1]);
            p = Integer.parseInt(numbers[2]);
            ID = Integer.parseInt(numbers[3]);
            y = powerModulo(1, g, b, p);
            key = powerModulo(x, g, b, p);
            if (socket != null) {
                userThread.start();
            }
        } catch (Exception e) {
            System.err.println("Error while assigning primes");
            System.err.println(e);
        }

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
     * SOCKET IMPL: this methods communicates with the server.
     */
    @Override
    public void run() {
        try {
            //First it sends the username.
            out.writeUTF(userName);
            out.flush();
            //Then sends the y value
            String toSend = Integer.toString(y);
            out.writeUTF(toSend);
            out.flush();
        } catch (Exception e) {
            System.err.println("Problem while sending username or y value");
            System.err.println(e);
        }

        //Then tryes to send get the next cipher from a queue.
        String newCipher = user.getNextCipher();
        //When the client receives the cipher it resolves it and then prints it.
        String resolvedCipher = resolveCipher(newCipher);
        System.out.println(resolvedCipher);

    }

    /**
     * Simple function that call the decoder with our own key.
     *
     * @param message Message to decipher
     * @return Deciphered text
     */
    private String resolveCipher(String message) {
        return resolveCipher(message, key);
    }

    /**
     * This function deciphers a message. It can be improved by tweaking with
     * the values a bit.
     *
     * @param message Message to decipher
     * @param key The key used for deciphering
     * @return The new dechipered text.
     */
    public String resolveCipher(String message, int key) {
        String solved;
        int shift = key % 8;
        int subst = key % 26;
        //Here, if we want to do this faster we can simply double the key and then apply the modulo on it.
        //Because if we shift a chunk by 10 (5 two times) it's the same as shifting it just by just 2.
        //But for the sake of simplicity and cw corectness I will keep it this way.
        solved = uncrypt(message, shift, subst);
        solved = uncrypt(solved, shift, subst);
        return solved;
    }

    /**
     * This function simply decrypts a message by shifting and substracting from
     * it's message certain amounts.
     *
     * @param message Message to work on
     * @param shift Shift amount
     * @param subst Substraction amount
     * @return The new string
     */
    public String uncrypt(String message, int shift, int subst) {
        String solution;
        solution = substWord(message, subst);
        solution = shiftWord(solution, shift);
        return solution;
    }

    /**
     * This function substracts a certain amount from the ascii code of every
     * character in a string.
     *
     * @param message String to substract from
     * @param substAmount Substraction amount
     * @return The new String
     */
    public String substWord(String message, int substAmount) {
        String solution;
        char[] messageChar = message.toCharArray();
        for (int i = 0; i < messageChar.length; i++) {
            messageChar[i] = substChar(messageChar[i], substAmount);
        }
        solution = String.valueOf(messageChar);
        return solution;
    }

    /**
     * This function substracts an amount from the ascii code of a character.<p>
     * The new character is bounded from A to Z. If another character is
     * encountered the function returns the character.
     *
     * @param character Character to substract from
     * @param substAmount Substraction amount
     * @return The new character or the character in error case
     */
    public char substChar(char character, int substAmount) {
        if (character < 'A' || character > 'Z') {
            return character;
        }
        if (character - substAmount < 'A') {
            character += 26;
        }
        return (char) (character - substAmount);
    }

    /**
     * Shifts a whole text by a certain amount in chunks of 8.
     * <p>
     * Example:
     * <blockquote><pre>
     * shiftWord("ABCDEFGHIJKLMNOP", 1) returns "DEFGHABCLMNOPIJK"
     * </pre></blockquote>
     *
     * @param message Whole text to shift
     * @param shiftAmount Amount to shift the chunks of 8 by
     * @return The new shifted text
     */
    public String shiftWord(String message, int shiftAmount) {
        String solution = "";
        if (message.length() % 8 == 0 && message.length() >= 8) {
            for (int i = 0; i <= message.length() - 8; i += 8) {
                String currentChunk = message.substring(i, i + 8);
                solution = solution.concat(shiftChunk(currentChunk, shiftAmount));
            }
        }
        return solution;
    }

    /**
     * Shifts a chunk of text of size 8 by an amount.
     *
     * @param chunk Chunk of text to shift
     * @param shiftAmount Amount to shift by
     * @return Shifted amount
     */
    public String shiftChunk(String chunk, int shiftAmount) {
        String solution;
        solution = chunk.substring(shiftAmount);
        solution = solution.concat(chunk.substring(0, shiftAmount));
        return solution;
    }

    /**
     * Maps output of this client to server.
     */
    private void assignOut() {

        try {

            out = new DataOutputStream(socket.getOutputStream());
            if (userThread == null) {
                user = new ClientThread(this, socket);
                user.start();
                userThread = new Thread(this);
            }
        } catch (IOException ex) {
            System.err.println("Error in assigning in out streams");
            System.err.println(ex);
        }

    }

    /**
     * Starts the client
     *
     * @param args
     */
    public static void main(String args[]) {
        //Assign default values
        String host = "localhost";
        String username = "aas1u16";
        int port = 1099;
        //Change values accordingly
        if (args.length > 0) {
            if (args.length >= 2) {
                host = args[0];
                username = args[1];
            }
            if (args.length >= 3) {
                port = Integer.parseInt(args[2]);
            }
        }
        //Create the object
        MyClient client = new MyClient(host, username, port);
        //Choose what connection to go for
        boolean rmi = true;
        if (rmi == false) {
            client.connectAndStart();
        } else {
            client.RMIServerConnect();
        }
    }

    /**
     * Stops the client.
     */
    protected void stop() {
        if (userThread != null) {
            userThread.stop();
            userThread = null;
        }
        try {
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Problem in closing IO");
            System.err.println(e);
        }
        user.terminate();
        user.stop();
    }

}
