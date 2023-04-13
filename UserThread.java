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

            Chunk[] chunks = new Chunk[waypoints.size()/2];

            int j = 0;
            for (int i = 0; i < waypoints.size(); i += 2) {

                ArrayList<Waypoint> arr = new ArrayList<>();
                arr.add(waypoints.get(i));
                arr.add(waypoints.get(i+1));

                chunks[j] = new Chunk(this.getId(), arr, i);
                ++j;
            }

//            while(waypoints.size() > 0){
//                ArrayList<Waypoint> arr = new ArrayList<>();
//                arr.add(waypoints.get(0));
//                waypoints.remove(0);
//                if (waypoints.size() % 2 == 0) {
//                    chunks[j] = new Chunk(this.getId(), arr, j);
//                    arr.clear();
//                }
//                ++j;
//            }


            int numChunks = chunks.length;

            Master.addData(chunks);

            System.out.println("UserThread #" + this.getId() + " waiting for data from worker...");
            // Wait for data to be mapped by worker
            while (!Master.intermediateResults.containsKey(this.getId())) {
                Thread.sleep(1000);
            }


            System.out.println("UserThread #" + this.getId() + " has received first chunk from worker...");

            while (Master.intermediateResults.get(this.getId()).size() < numChunks) {
                Thread.sleep(1000);
            }

            // Reduce
            System.out.println("UserThread #" + this.getId() + " reducing data for user...");

            int sum = 0;

            ArrayList<Chunk> chunksList = Master.intermediateResults.get(this.getId());

            for (Chunk chunk: chunksList) {
                ArrayList<Waypoint> ws = chunk.getData();
                for (Waypoint w: ws) {
                    sum += w.getID();
                }
            }

            out.writeObject(sum);
            out.flush();

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

