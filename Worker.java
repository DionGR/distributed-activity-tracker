import java.net.*;
import java.io.*;

public class Worker extends Thread{
    Socket requestSocket;
    private int id;
    private int port;

    Worker (int id, int port){
        this.id = id;
        this.port = port;
    }

    public void run(){
        ObjectInputStream in = null;

        try {
            requestSocket = new Socket("localhost", port);
            ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());

            /* Create the streams to send and receive data from server */

            while(true) {
                in = new ObjectInputStream(requestSocket.getInputStream());

                Chunk data = (Chunk) in.readObject();

                WorkerThread workerThread = new WorkerThread(requestSocket, data, out);
                workerThread.start();
                System.out.println(data + " assigned to worker #" + id);
            }

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                in.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }

    private class WorkerThread extends Thread {
        ObjectOutputStream out;
        private Socket requestSocket;
        private Chunk data;


        WorkerThread(Socket requestSocket, Chunk data, ObjectOutputStream out)  {
            this.requestSocket = requestSocket;
            this.data = data;
            this.out = out;
        }

        public void run() {
            try {

                // Mapping
                Chunk result = new Chunk(data.getUser(), data.getData() * 2, data.getId());

                out.writeObject(result);
                out.flush();

                System.out.println("Worker #" + id + " sent intermediate result: " + result);
            } catch (Exception e) {
                e.printStackTrace();
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


