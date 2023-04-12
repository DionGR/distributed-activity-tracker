import java.net.*;
import java.io.*;
import java.sql.Array;
import java.util.ArrayList;

public class UserThread extends Thread {
    private Socket providerSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    UserThread(Socket providerSocket) {
        this.providerSocket = providerSocket;
        this.out = null;
        this.in = null;
    }

    @Override
    public void run(){

        try {
            out = new ObjectOutputStream(providerSocket.getOutputStream());
            in = new ObjectInputStream(providerSocket.getInputStream());

            // Read GPX from User
            int message = (int) in.readObject();
            System.out.println("Thread #" + this.getId() + " received: " + message);

            Chunk c1 = new Chunk(this.getId(), message * 5, 1);
            Chunk c2 = new Chunk(this.getId(), message * 10, 2);

            Chunk[] chunks = {c1, c2};

            int numChunks = chunks.length;

            Master.addData(chunks);


            System.out.println("Thread #" + this.getId() + " waiting for data from worker...");
            // Wait for data to be mapped by worker
            while (!Master.intermediateResults.containsKey(this.getId())) {
                Thread.sleep(1000);
            }


            System.out.println("Thread #" + this.getId() + " has received first chunk from worker...");

            while (Master.intermediateResults.get(this.getId()).size() < numChunks) {
                Thread.sleep(1000);
            }

            // Reduce
            System.out.println("Thread #" + this.getId() + " reducing data for user...");
            int sum = 0;
            sum += Master.intermediateResults.get(this.getId()).get(0).getData();
            sum += Master.intermediateResults.get(this.getId()).get(1).getData();
            Chunk finalResult = new Chunk(this.getId(), sum, -1);

            out.writeObject(finalResult);
            out.flush();

            System.out.println("UserThread #" + this.getId() + " sent final result to user.");

        }catch (IOException e) {
            System.err.println("UserThread #" + this.getId() + " - IOERROR: " + e.getMessage());
            // Retry opening streams
        }catch (Exception e){
            System.err.println("UserThread #" + this.getId() + " - ERROR: " + e.getMessage());
            throw new RuntimeException(e); // !!!
        }finally {
            try {
                in.close(); out.close();
                providerSocket.close();
                System.out.println("UserThread #" + this.getId() + " shutting down...");
            } catch (IOException ioException) {
                System.err.println("UserThread #" + this.getId() + " - IOERROR while shutting down: " + ioException.getMessage());
            }
        }
    }
}

