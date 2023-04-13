import java.net.*;
import java.io.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.stream.Stream;

public class UserThread extends Thread {
    private final Socket providerSocket;
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
            StringBuilder buffer = (StringBuilder) in.readObject();

            System.out.println("UserThread #" + this.getId() + " received.");

            /* Convert the GPX file into a list of Waypoints */

            // Parse GPX
            GPXParser parser = new GPXParser(buffer);
            ArrayList<Waypoint> waypoints = parser.parse();

            for (Waypoint w : waypoints) {
                System.out.println("UserThread #" + this.getId() + " - " + w.toString());
            }





//            Chunk c1 = new Chunk(this.getId(), message * 5, 1);
//            Chunk c2 = new Chunk(this.getId(), message * 10, 2);
//
//            Chunk[] chunks = {c1, c2};

//            int numChunks = chunks.length;
//
//            Master.addData(chunks);


//            System.out.println("UserThread #" + this.getId() + " waiting for data from worker...");
//            // Wait for data to be mapped by worker
//            while (!Master.intermediateResults.containsKey(this.getId())) {
//                Thread.sleep(1000);
//            }
//
//
//            System.out.println("UserThread #" + this.getId() + " has received first chunk from worker...");
//
//            while (Master.intermediateResults.get(this.getId()).size() < numChunks) {
//                Thread.sleep(1000);
//            }

//            // Reduce
//            System.out.println("UserThread #" + this.getId() + " reducing data for user...");
//            int sum = 0;
//            sum += Master.intermediateResults.get(this.getId()).get(0).getData();
//            sum += Master.intermediateResults.get(this.getId()).get(1).getData();
//            Chunk finalResult = new Chunk(this.getId(), sum, -1);
//
//            out.writeObject(finalResult);
//            out.flush();

            System.out.println("UserThread #" + this.getId() + " sent final result to user.");
        }catch (IOException ioException) {
            System.err.println("UserThread #" + this.getId() + " - IOERROR: " + ioException.getMessage());
            // Retry opening streams
        }catch (ClassNotFoundException classNotFoundException) {
            System.err.println("UserThread #" + this.getId() + " - CASTERROR: " + classNotFoundException.getMessage());
        } catch (Exception e){
            System.err.println("UserThread #" + this.getId() + " - ERROR: " + e.getMessage());
            throw new RuntimeException(e); // !!!
        }finally {
            try {
                out.close(); in.close();
                providerSocket.close();
                System.out.println("UserThread #" + this.getId() + " shutting down...");
            } catch (IOException ioException) {
                System.err.println("UserThread #" + this.getId() + " - IOERROR while shutting down: " + ioException.getMessage());
            }
        }
    }
}

