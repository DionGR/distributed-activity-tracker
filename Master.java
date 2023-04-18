import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


class Master {
    private final int userPort, workerPort;

    private final ArrayList<UserThread> connectedUsers;
    private final ArrayList<ReceiveWorkerData> connectedWorkers;

    private final ArrayList<Chunk[]> dataForProcessing;
    private final HashMap<Long, ArrayList<Segment>> intermediateResults;


    Master(int userPort, int workerPort){
        this.userPort = userPort;
        this.workerPort = workerPort;
        this.connectedUsers = new ArrayList<>();
        this.connectedWorkers = new ArrayList<>();
        this.dataForProcessing = new ArrayList<>();
        this.intermediateResults = new HashMap<>();
    }

    public void bootServer(){
        try {
            UserHandler userHandler = new UserHandler(userPort);
            WorkerHandler workerHandler = new WorkerHandler(workerPort);
            AssignData assignData = new AssignData();

            userHandler.start();
            workerHandler.start();
            assignData.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected synchronized void addData(Chunk[] chunks){
        synchronized (dataForProcessing) {
            dataForProcessing.add(chunks);
        }
    }

    private class AssignData extends Thread{

        @Override
        public void run(){

            int nextWorker = 0;

            try {
                while (!Thread.currentThread().isInterrupted()) {

                    /* Assign data to workers using round-robin */
                    while (dataForProcessing.size() > 0 && connectedWorkers.size() > 0) {
                        Chunk[] chunks;

                        synchronized (dataForProcessing) {
                            chunks = dataForProcessing.get(0);
                            dataForProcessing.remove(0);
                        }

                        for (Chunk c : chunks) {
                            //ObjectOutputStream out = workerOutputStreams[nextWorker];
                            ObjectOutputStream out = connectedWorkers.get(nextWorker).getOutputStream();
                            System.out.println("Assigning data to worker: " + nextWorker);

                            out.writeObject(c);
                            out.flush();

                            System.out.println("Data assigned to worker: " + c.toString());

                            nextWorker = (++nextWorker) % connectedWorkers.size();
                        }
                    }
                }
            }catch(IOException ioException){
                System.err.println("AssignData IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("AssignData ERROR: " + e.getMessage());
            }
        }
    }

    private class UserHandler extends Thread {
        private final ServerSocket usersSocketToHandle;

        UserHandler(int port) throws IOException {
            this.usersSocketToHandle = new ServerSocket(port, 5000);
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket providerSocket = usersSocketToHandle.accept();
                    System.out.println("UserHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    /* Handle the request */
                    UserThread user = new UserThread(providerSocket);
                    connectedUsers.add(user);
                    user.start();
                }
            } catch (IOException ioException) {
                System.err.println("UserHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("UserHandler ERROR: " + e.getMessage());
            } finally {
                try {
                    usersSocketToHandle.close();
                } catch (IOException ioException) {
                    System.err.println("UserHandler IOERROR while shutting down... " + ioException.getMessage());
                }
            }
        }
    }

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

                int numChunks = connectedWorkers.size();
                Chunk[] chunks = new Chunk[numChunks];
                int chunkSize = (int) ((waypoints.size() / numChunks) + 1.5);


                // Split the list of waypoints into chunks
                while(waypoints.size() > 1) {
                    ArrayList<Waypoint> chunkWaypoints = new ArrayList<>();

                    int waysize = waypoints.size();

                    for (int i = 0; i < Math.min(chunkSize, waysize); i++) {
                        chunkWaypoints.add(waypoints.get(0));
                        waypoints.remove(0);
                    }

                    if(!waypoints.isEmpty())
                        chunkWaypoints.add(waypoints.get(0));

                    chunks[--numChunks] = new Chunk(this.getId(), chunkWaypoints, waypoints.size());
                }

                for (Chunk c : chunks) {
                    System.out.println("Chunk: ");
                    System.out.println(c.toString());
                }

//                int chunkSize = (int) ((waypoints.size() / numChunks) + 1.5);
//                ArrayList<Waypoint> chunkWaypoints = new ArrayList<>();
//                chunkWaypoints.add(waypoints.get(0));
//                for(int i=1; i < waypoints.size(); i++){
//                    if( i % chunkSize == 0 ){
//                            chunks[i / chunkSize] = new Chunk(this.getId(), chunkWaypoints, i / chunkSize);
//                            chunkWaypoints = new ArrayList<>();
//                            chunkWaypoints.add(waypoints.get(i-1));
//                    }
//                    chunkWaypoints.add(waypoints.get(i));
//                }
//                chunks[numChunks-1] = new Chunk(this.getId(), chunkWaypoints,  numChunks-1);

                addData(chunks);

                System.out.println("UserThread #" + this.getId() + " waiting for data from worker...");
                // Wait for data to be mapped by worker
                while (!intermediateResults.containsKey(this.getId())) {
                    Thread.sleep(1000);
                }


                System.out.println("UserThread #" + this.getId() + " has received first chunk from worker...");

                while (intermediateResults.get(this.getId()).size() < numChunks) {
                    Thread.sleep(1000);
                }

                // Reduce
                System.out.println("UserThread #" + this.getId() + " reducing data for user...");

                int sum = 0;

                ArrayList<Segment> segmentList = intermediateResults.get(this.getId());

                double totalDistance = 0;
                double totalElevation = 0;
                long totalTime = 0;

                for (Segment segment: segmentList) {
                    // REDUCE
                    totalDistance += segment.getTotalDistance();    //km
                    totalElevation += segment.getTotalElevation();  //m
                    totalTime += segment.getTotalTime() / 1000;     //sec
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

    private class WorkerHandler extends Thread {

        private final ServerSocket workersSocketToHandle;

        WorkerHandler(int port) throws IOException {
            this.workersSocketToHandle = new ServerSocket(port, 5000);
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket providerSocket = workersSocketToHandle.accept();
                    System.out.println("WorkerHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    ReceiveWorkerData workerData = new ReceiveWorkerData(providerSocket);
                    connectedWorkers.add(workerData);
                    workerData.start();
                }
            } catch (IOException ioException) {
                System.err.println("WorkerHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("WorkerHandler ERROR: " + e.getMessage());
            } finally {
                try {
                    workersSocketToHandle.close();
                } catch (IOException ioException) {
                    System.err.println("WorkerHandler IOERROR while shutting down... " + ioException.getMessage());
                }
            }
        }
    }

    private class ReceiveWorkerData extends Thread {
        Socket workerSocket;
        ObjectInputStream in;
        ObjectOutputStream out;
        int workerPort;

        public ReceiveWorkerData(Socket workerSocket){
            this.workerSocket = workerSocket;
            this.workerPort = workerSocket.getPort();
            System.out.println("Worker port: " + workerPort);
            this.in = null;
            this.out = null;
        }

        @Override
        public void run() {
            try {
                this.out = new ObjectOutputStream(workerSocket.getOutputStream());
                this.in = new ObjectInputStream(workerSocket.getInputStream());

                while (workerSocket.isConnected()) {
                    /* Waiting for intermediate result */
                    Chunk data = (Chunk) in.readObject();
                    System.out.println("Worker port: "+ workerPort + " received data for user: " + data.getUser());
                    addData(data);
                }

            }catch (IOException ioException) {
                System.err.println("Worker port: "+ workerPort + " - IOERROR: " + ioException.getMessage());
            }catch (ClassNotFoundException classNotFoundException) {
                System.err.println("Worker port: "+ workerPort + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                System.err.println("Worker port: "+ workerPort + " - ERROR: " + e.getMessage());
                throw new RuntimeException(); // !!!!!!!!!
            }finally {
                try {
                    out.close(); in.close();
                    workerSocket.close();
                } catch (IOException ioException) {
                    System.err.println("Worker port: "+ workerPort + " - IOERROR while shutting down... " + ioException.getMessage());
                }
            }
        }


        public synchronized void addData(Segment data){
            synchronized (intermediateResults){
                if (!intermediateResults.containsKey(data.getUser())) {
                    intermediateResults.put(data.getUser(), new ArrayList<>());
                }
                intermediateResults.get(data.getUser()).add(data);
            }
        }

        public Socket getSocket(){
            return workerSocket;
        }

        public int getWorkerPort(){
            return workerPort;
        }

        public ObjectInputStream getInputStream(){
            return in;
        }

        public ObjectOutputStream getOutputStream(){
            return out;
        }
    }

    public static void main(String[] args) {
        Master master = new Master(4321, 1234);
        master.bootServer();
    }
}
