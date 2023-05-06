import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

/* Worker Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class maps the data assigned from the master and sends it back to the master.
 */

public class Worker extends Thread{
    private final int id;
    private Socket connectSocket;
    private ObjectInputStream in;
    private String host;
    private int masterConnectionPort, masterOutDataPort;


    Worker (int id) {
        this.id = id;
        this.in = null;
    }

    @Override
    public void run(){
        try {
            initDefaults();

            /* Connect to server and receive data */
            connectSocket = new Socket(host, masterConnectionPort);

            System.err.println("Worker #" + id + " with worker port: " + connectSocket.getLocalPort() + " connected to Master" );

            /* Create the stream that receives data from server */
            in = new ObjectInputStream(connectSocket.getInputStream());

            /* Create the streams to send and receive data from server */
            while(!Thread.currentThread().isInterrupted()) {
                Chunk data = (Chunk) in.readObject();

                WorkerThread workerThread = new WorkerThread(data);
                workerThread.start();

                System.out.println("Worker #" + id + " assigned data: " + data);
            }

        } catch (UnknownHostException unknownHostException) {
            System.err.println("Worker #" + id + " - UnknownHostERROR: " + unknownHostException.getMessage());
        } catch (IOException ioException) {
            System.err.println("Worker #" + id + " - IOERROR: " + ioException.getMessage());
        } catch (ClassNotFoundException classNotFoundException) {
            System.err.println("Worker #" + id + " - CASTERROR: " + classNotFoundException.getMessage());
        } catch (Exception e) {
            System.err.println("Worker #" + id + " - ERROR: " + e.getMessage());
        }finally {
            try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("Worker #" + id + " - IOERROR while closing input stream: " + ioException.getMessage()); }
            try { if (connectSocket != null) connectSocket.close(); } catch (IOException ioException) { System.err.println("Worker #" + id + " - IOERROR while closing connection socket: " + ioException.getMessage()); }
            System.err.println("Worker #" + id + " shutting down...");
        }
    }


    private class WorkerThread extends Thread {
        private Socket requestSocket;
        private ObjectOutputStream out;
        private final Chunk chunk;


        WorkerThread(Chunk chunk){
            this.chunk = chunk;
            this.out = null;
        }

        @Override
        public void run() {
            try {
                /* New socket for each task; To send mapped data to master */
                requestSocket = new Socket(host, masterOutDataPort);

                this.out = new ObjectOutputStream(requestSocket.getOutputStream());

                // Mapping
                ArrayList<Waypoint> waypoints = chunk.getData();

                double totalDistance = 0;
                double totalElevation = 0;
                long totalTime = 0;

                for(int i = 1; i < waypoints.size(); i++) {
                    Waypoint curr = waypoints.get(i);
                    Waypoint prev = waypoints.get(i - 1);

                    totalDistance += distance(curr.getLatitude(), prev.getLatitude(), curr.getLongitude(), prev.getLongitude());
                    totalElevation += Math.max(0, curr.getElevation() - prev.getElevation());
                    totalTime += curr.getDate().getTime() - prev.getDate().getTime();
                }

                double meanVelocity = totalDistance / totalTime;  // v = delta_x / delta_t

                IntermediateChunk result = new IntermediateChunk(chunk.getUserID(), chunk.getTotalChunks(), totalDistance, meanVelocity, totalElevation, totalTime, waypoints.get(0).getDate());

                this.out.writeObject(result);
                this.out.flush();

                System.out.println("Worker - WorkerThread #" + id + " sent intermediate result: " + result);
            } catch (UnknownHostException unknownHostException) {
                System.err.println("Worker - WorkerThread #" + id + " - UnknownHostERROR: " + unknownHostException.getMessage());
            } catch (IOException ioException) {
                System.err.println("Worker - WorkerThread #" + id + " - IOERROR while sending intermediate result: " + ioException.getMessage());
                throw new RuntimeException("workerThread - IOERROR: " + ioException.getMessage());
            }catch (Exception e) {
                System.err.println("Worker - WorkerThread #" + id + " - ERROR: " + e.getMessage());
                throw new RuntimeException("workerThread - ERROR: " + e.getMessage());
            }finally{
                try { if (this.out != null) this.out.close(); } catch (IOException ioException) { System.err.println("Worker - WorkerThread #" + id + " - IOERROR while closing output stream: " + ioException.getMessage()); }
                try { if (requestSocket != null) requestSocket.close(); } catch (IOException ioException) { System.err.println("Worker - WorkerThread #" + id + " - IOERROR while closing request socket: " + ioException.getMessage()); }
            }
        }

        /* Calculates distance between two coordinates */
        private double distance(double lat1, double lat2, double lon1, double lon2) {
            final int RADIUS = 6371;

            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);

            double haversine = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                       Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                       Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));

            return RADIUS * angularDistance;
        }
    }

    /* Initialize default values */
    private void initDefaults() {
        FileReader cfgReader = null;
        try {
            cfgReader = new FileReader(System.getProperty("user.dir") + "\\data\\workerCFG");
            Properties properties = new Properties();
            properties.load(cfgReader);

            this.host = properties.getProperty("host");
            this.masterConnectionPort = Integer.parseInt(properties.getProperty("masterConnectionPort"));
            this.masterOutDataPort = Integer.parseInt(properties.getProperty("masterOutDataPort"));
        }catch (IOException ioException) {
            System.err.println("Worker - initDefaults - IOERROR while initializing defaults: " + ioException.getMessage());
            throw new RuntimeException("initDefaults - IOERROR: " + ioException.getMessage());
        } catch (Exception e) {
            System.err.println("Worker - initDefaults - ERROR while initializing defaults: " + e.getMessage());
            throw new RuntimeException("initDefaults - ERROR: " + e.getMessage());
        } finally {
            try { if (cfgReader != null) cfgReader.close(); } catch (IOException ioException) { System.err.println("Worker - initDefaults - IOERROR while closing config file: " + ioException.getMessage()); throw new RuntimeException("initDefaults - ERROR: " + ioException.getMessage());  }
        }
    }

    public static void main(String[] args) {

        for (int i = 1; i <= 2; i++) {
            Worker worker = new Worker(i);
            worker.start();
        }
    }
}
