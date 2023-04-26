import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Worker extends Thread{
    private Socket connectSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String host;
    private final int id;
    private final int serverConnectPort, serverRequestPort;


    Worker (int id, String host, int serverConnectPort, int serverRequestPort) {
        this.serverConnectPort = serverConnectPort;
        this.serverRequestPort = serverRequestPort;
        this.host = host;
        this.out = null;
        this.in = null;
        this.id = id;
    }

    @Override
    public void run(){
        try {
            connectSocket = new Socket(host, serverConnectPort);

            System.out.println("Worker #" + id + " with worker port: " + connectSocket.getLocalPort() + " connected to Master" );

            out = new ObjectOutputStream(connectSocket.getOutputStream());
            in = new ObjectInputStream(connectSocket.getInputStream());

            /* Create the streams to send and receive data from server */
            while(true) {
                Chunk data = (Chunk) in.readObject();
                Socket requestSocket = new Socket(host, serverRequestPort);

                WorkerThread workerThread = new WorkerThread(requestSocket, data);
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
                connectSocket.close();
                System.err.println("Worker #" + id + " shutting down...");
            } catch (IOException ioException) {
                System.err.println("Worker #" + id + " - IOERROR while shutting down: " + ioException.getMessage());
            }
        }
    }

    private class WorkerThread extends Thread {
        private final Socket requestSocket;
        private ObjectOutputStream out;
        private final Chunk chunk;


        WorkerThread(Socket requestSocket, Chunk chunk){
            this.requestSocket = requestSocket;
            this.out = null;
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

                Segment result = new Segment(chunk.getGPXID(), chunk.getTotalChunks(), totalDistance, meanVelocity, totalElevation, totalTime);

                this.out = new ObjectOutputStream(requestSocket.getOutputStream());
                this.out.writeObject(result);
                this.out.flush();

                System.out.println("WorkerThread #" + id + " sent intermediate result: " + result);

            } catch (IOException ioException) {
                System.err.println("WorkerThread #" + id + " - IOERROR while sending intermediate result: " + ioException.getMessage());
                throw new RuntimeException(ioException); // !!!
            }catch (Exception e) {
                System.err.println("WorkerThread #" + id + " - ERROR: " + e.getMessage());
                throw new RuntimeException(e); // !!!
            }finally{
//                try {
////                    this.out.close();
////                    requestSocket.close();
//                } catch (IOException ioException) {
//                    System.err.println("WorkerThread #" + id + " - IOERROR while closing request socket: " + ioException.getMessage());
//                }
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
        String host = "localhost";
        int serverConnectPort = 12345;
        int serverRequestPort = 23456;

        for (int i = 1; i <= 2; i++) {
            Worker worker = new Worker(i, host, serverConnectPort, serverRequestPort);
            worker.start();
        }
    }
}
