import java.net.*;
import java.io.*;

public class Worker extends Thread{
    private Socket requestSocket;
    private int id;
    private int port;

    Worker (int id, int port){
        this.id = id;
        this.port = port;
    }

    public void run(){
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            requestSocket = new Socket("localhost" , port);

            /* Create the streams to send and receive data from server */

            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());


            System.out.println("Worker #" + id + " is ready to work");

            int num = (int) in.readObject();

            System.out.println("Worker #" + id + " received: " + num);

            num = 2*id;

            out.writeObject(num);
            out.flush();

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                in.close();
                out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        Worker w1 = new Worker(1, 1234);
        Worker w2 = new Worker(2, 1234);
//        Worker w3 = new Worker(3, 1234);
//        Worker w4 = new Worker(4, 1234);

        w1.start();
        w2.start();
//        w3.start();
//        w4.start();
    }
}


