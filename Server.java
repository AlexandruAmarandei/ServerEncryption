
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server extends Thread {

    private static Socket socket;

    private final int p = 191;
    private final int g = 131;
    private int a = 1, x, y, ks;
    private String message;
    public ServerSocket serverSocket;

    public void Server() throws IOException, UnknownHostException, NotBoundException {
        System.setProperty("java.security.policy", "mypolicy");
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
            Registry ctreg = LocateRegistry.getRegistry("svm-tjn1f15-comp2207.ecs.soton.ac.uk", 12345);
            CiphertextInterface ctstub = (CiphertextInterface) ctreg.lookup("CiphertextProvider");
            message = (String) ctstub.get("vc1g16", 84);
            System.out.println(message);
        }

        serverSocket = new ServerSocket(60301);
        System.out.println("Server started and listening to port 60301");

        start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                socket = serverSocket.accept();
                addClient(socket);
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
    public void addClient(Socket socket){
        
        //Aici creezi o noua clasa care se ocupa separat de client
        System.out.println("Connected with" + socket + ".");
        
            Runnable run = new Runnable() {
                public final int id = a;

                @Override
                public void run() {
                    try {
                        InputStream is = socket.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader br = new BufferedReader(isr);

                        OutputStream os = socket.getOutputStream();
                        OutputStreamWriter osw = new OutputStreamWriter(os);
                        BufferedWriter bw = new BufferedWriter(osw);

                        bw.write(message + " " + Integer.toString(p) + " " + Integer.toString(g) + " "
                                + Integer.toString(calculate(p, g, id)) + "\n");
                        bw.flush();

                        y = br.read();

                        ks = calculate(p, y, id);

                        socket.close();
                        System.out.println("Connection with " + a + " terminated.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            new Thread(run).start();
            a++;
    }
    
    public int calculate(int p, int g, int a) {
        int x = 1;
        for (int i = 0; i < a; i++) {
            x = (x * g) % p;
        }
        return x;
    }

    public static void main(String args[]) {
            Server myServer = new Server();
    
    }
}
