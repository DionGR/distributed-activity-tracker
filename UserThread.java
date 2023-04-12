import java.net.*;
import java.io.*;
import java.sql.Array;
import java.util.ArrayList;

public class UserThread extends Thread implements Serializable{

    transient Socket providerSocket;
    private final String ip;
    private final int port;
    private boolean mapped;

    UserThread(Socket providerSocket) {
        this.providerSocket = providerSocket;
        this.ip = providerSocket.getInetAddress().getHostAddress();
        this.port = providerSocket.getPort();
        this.mapped = false;
    }

    public void run(){
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            out = new ObjectOutputStream(providerSocket.getOutputStream());
            in = new ObjectInputStream(providerSocket.getInputStream());
            int message = (int) in.readObject(); // Takes GPX
            System.out.println("Received: " + message);

            Chunk c1 = new Chunk(this.getId(), message*1000, 1);
            Chunk c2 = new Chunk(this.getId(), message*2000, 2);

            Chunk[] chunks = {c1, c2};

            int numChunks = chunks.length;

            Master.addData(chunks);

            System.out.println(this.getId() + " has " + numChunks + " chunks");


            System.out.println("Waiting for data from worker...");

            // Wait for data to be mapped by worker
            while (!mapped)
            {
                sleep(1000);
            }

            // Reduce
            System.out.println("Reducing data for user...");
            int sum = 0;
            sum += Master.intermediateResults.get(this.getId()).get(0).getData();
            sum += Master.intermediateResults.get(this.getId()).get(1).getData();
            Chunk finalResult = new Chunk(this.getId(), sum, -1);

            out.writeObject(finalResult);
            out.flush();

            System.out.println("Sent final result to user.");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                in.close();
                out.close();
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void setMapped() {
        this.mapped = true;
    }
}

