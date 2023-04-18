import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Worker extends Thread{
    private Socket requestSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final int id;
    private final int port;

    Worker (int id, int port) {
        this.id = id;
        this.port = port;
        this.requestSocket = null;
        this.out = null;
        this.in = null;
    }

    @Override
    public void run(){
        out = null;
        in = null;
        try {
            requestSocket = new Socket("localhost", port);
            System.out.println("Worker #" + id + " with worker port: " + requestSocket.getLocalPort() + " connected to Master" );

            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());


            /* Create the streams to send and receive data from server */
            while(true) {
                Chunk data = (Chunk) in.readObject();

                WorkerThread workerThread = new WorkerThread(data);
                workerThread.start();

                System.out.println("Worker #" + id + " assigned data: " + data);
            }

        } catch (UnknownHostException unknownHostException) {
            System.err.println("Worker #" + id + " - UnknownHostERROR: " + unknownHostException.getMessage());
            // Retry connecting to host
        } catch (IOException ioException) {
            System.err.println("Worker #" + id + " - IOERROR: " + ioException.getMessage());
            // Retry opening streams
        } catch (ClassNotFoundException classNotFoundException) {
            System.err.println("Worker #" + id + " - CASTERROR: " + classNotFoundException.getMessage());
        } catch (Exception e) {
            System.err.println("Worker #" + id + " - ERROR: " + e.getMessage());
            throw new RuntimeException(e); // !!!
        }finally {
            try {
                out.close(); in.close();
                requestSocket.close();
                System.err.println("Worker #" + id + " shutting down...");
            } catch (IOException ioException) {
                System.err.println("Worker #" + id + " - IOERROR while shutting down: " + ioException.getMessage());
            }
        }
    }

    private class WorkerThread extends Thread {
        private final Chunk data;

        WorkerThread(Chunk data){
            this.data = data;
        }

        @Override
        public void run() {
            try {
                // Mapping




                synchronized (out) {
                    out.writeObject(result);
                    out.flush();
                }

                System.out.println("WorkerThread #" + id + " sent intermediate result: " + result);

            } catch (IOException ioException) {
                System.err.println("WorkerThread #" + id + " - IOERROR while sending intermediate result: " + ioException.getMessage());
                throw new RuntimeException(ioException); // !!!
            }catch (Exception e) {
                System.err.println("WorkerThread #" + id + " - ERROR: " + e.getMessage());
                throw new RuntimeException(e); // !!!
            }
        }
    }

    public static void main(String[] args) {
        Worker w1 = new Worker(1, 1234);
        Worker w2 = new Worker(2, 1234);
        Worker w3 = new Worker(3, 1234);
        Worker w4 = new Worker(4, 1234);
        Worker w5 = new Worker(5, 1234);

        w1.start();
        w2.start();
        w3.start();
        w4.start();
        w5.start();
    }
}







