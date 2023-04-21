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
        this.out = null;
        this.in = null;
    }

    @Override
    public void run(){
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
        private final Chunk chunk;

        WorkerThread(Chunk chunk){
            this.chunk = chunk;
        }

        @Override
        public void run() {
            try {
                // Mapping
                ArrayList<Waypoint> waypoints = chunk.getData();

                double totalDistance = 0;
                double totalElevation = 0;
                long totalTime = 0;

                for(int i=1; i < waypoints.size(); i++) {
                    Waypoint curr = waypoints.get(i);
                    Waypoint prev = waypoints.get(i - 1);

                    totalDistance += distance(curr.getLatitude(), prev.getLatitude(), curr.getLongitude(), prev.getLongitude());
                    totalElevation += Math.max(0, curr.getElevation() - prev.getElevation());
                    totalTime += curr.getTime().getTime() - prev.getTime().getTime();
                }

                double meanVelocity = totalDistance / totalTime;  // v = delta_x / delta_t

                Segment result = new Segment(chunk.getGPXID(), chunk.getChunkID(), chunk.getTotalChunks(), totalDistance, meanVelocity, totalElevation, totalTime);

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

        private double distance(double lat1, double lat2, double lon1, double lon2) {
            final int R = 6371;
            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 2; i++) {
            Worker worker = new Worker(i, 12345);
            worker.start();
        }
    }
}
