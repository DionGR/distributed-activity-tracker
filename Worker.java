import java.net.*;
import java.io.*;

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
        try {
            requestSocket = new Socket("localhost", port);

            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());



            /* Create the streams to send and receive data from server */
            while(true) {
                Chunk data = (Chunk) in.readObject();
                System.out.println("Worker # " + id + " AAAAA ");
                WorkerThread workerThread = new WorkerThread(data);
                workerThread.start();
                System.out.println("Worker #" + id + " assigned data: " + data);
            }

        } catch (UnknownHostException e) {
            System.err.println("Worker #" + id + " - UnknownHostERROR: " + e.getMessage());
            // Retry connecting to host
        } catch (IOException e) {
            System.err.println("Worker #" + id + " - IOERROR: " + e.getMessage());
            // Retry opening streams
        } catch (Exception e) {
            System.err.println("Worker #" + id + " - ERROR: " + e.getMessage());
            throw new RuntimeException(e); // !!!
        }finally {
            try {
                in.close(); out.close();
                requestSocket.close();
                System.err.println("Worker #" + id + " shutting down...");
            } catch (Exception e) {
                System.err.println("Worker #" + id + " - ERROR while shutting down: " + e.getMessage());
            }
        }
    }

    private class WorkerThread extends Thread {
        private final Chunk data;

        WorkerThread(Chunk data)  {
            this.data = data;
        }

        @Override
        public void run() {
            try {
                // Mapping
                Chunk result = new Chunk(data.getUser(), data.getData() * 2, data.getId());

                synchronized (out) {
                    out.writeObject(result);
                    out.flush();
                }

                System.out.println("WorkerThread #" + id + " sent intermediate result: " + result);

            } catch (Exception e) {
                System.err.println("WorkerThread #" + id + " - ERROR while sending intermediate result");
                System.err.println(e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Worker w1 = new Worker(1, 1234);
        Worker w2 = new Worker(2, 1234);
        Worker w3 = new Worker(3, 1234);
        Worker w4 = new Worker(4, 1234);

        w1.start();
        w2.start();
        w3.start();
        w4.start();
    }
}


