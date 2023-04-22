import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


class Master {
    private final int USERPORT, WORKERPORT;

    private final ArrayList<UserBroker> connectedUsers;
    private final ArrayList<Worker> connectedWorkers;

    private final ArrayList<Chunk[]> dataForProcessing;
    private final HashMap<Integer, ArrayList<Segment>> intermediateResults;

    private final Database database;
    private final int NEEDED_WORKERS;
    private Integer g_gpxID;


    Master(int USERPORT, int WORKERPORT, int NEEDED_WORKERS){
        this.USERPORT = USERPORT;
        this.WORKERPORT = WORKERPORT;
        this.NEEDED_WORKERS = NEEDED_WORKERS;
        this.connectedUsers = new ArrayList<>();
        this.connectedWorkers = new ArrayList<>();
        this.dataForProcessing = new ArrayList<>();
        this.intermediateResults = new HashMap<>();
        this.database = new Database();
        this.g_gpxID = 0;
    }

    public void bootServer() {
        try {
            UserHandler userHandler = new UserHandler(USERPORT);
            WorkerHandler workerHandler = new WorkerHandler(WORKERPORT);
            Scheduler scheduler = new Scheduler();

            userHandler.start();
            workerHandler.start();
            scheduler.start();

            synchronized (connectedWorkers) {
                while (true) {
                    if (connectedWorkers.size() == NEEDED_WORKERS && workerHandler.isAlive()) {
                        workerHandler.interrupt();
                        connectedWorkers.wait();
                    } else if (connectedWorkers.size() < NEEDED_WORKERS && !workerHandler.isAlive()) {
                        workerHandler = new WorkerHandler(WORKERPORT);
                        workerHandler.start();
                    }
                }
            }

    }catch (InterruptedException e) {
        System.err.println("Master: Interrupted while waiting for workers to connect: " + e.getMessage());
    }catch (IOException e) {
            e.printStackTrace();
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

                    Worker worker = new Worker(providerSocket);
                    connectedWorkers.add(worker);

                    worker.start();
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
                    UserBroker broker = new UserBroker(providerSocket);
                    connectedUsers.add(broker);
                    broker.start();
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

    private class Worker extends Thread {
        Socket workerSocket;
        ObjectInputStream in;
        ObjectOutputStream out;
        int workerPort;

        public Worker(Socket workerSocket){
            this.workerSocket = workerSocket;
            this.workerPort = workerSocket.getPort();
            System.out.println("Worker port: " + workerPort); // TODO: replace ports with something unique
            this.in = null;
            this.out = null;
        }

        public void addIntermediateResults(Segment data){
            synchronized (intermediateResults){
                intermediateResults.get(data.getGPXID()).add(data);
                if (intermediateResults.get(data.getGPXID()).size() == data.getTotalSegments())
                    intermediateResults.notifyAll();
            }
        }

        @Override
        public void run() {
            try {
                this.out = new ObjectOutputStream(workerSocket.getOutputStream());
                this.in = new ObjectInputStream(workerSocket.getInputStream());

                while (workerSocket.isConnected()) {
                    /* Waiting for intermediate result */
                    Segment data = (Segment) in.readObject();
                    System.out.println("Worker port: "+ workerPort + " received data for GPX: " + data.getGPXID());
                    addIntermediateResults(data);
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
                    connectedWorkers.remove(this);
                    synchronized (connectedWorkers){
                        connectedWorkers.notifyAll();
                    }
                } catch (IOException ioException) {
                    System.err.println("Worker port: "+ workerPort + " - IOERROR while shutting down... " + ioException.getMessage());
                }
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

    private class Scheduler extends Thread{

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
                System.err.println("Scheduler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Scheduler ERROR: " + e.getMessage());
            }
        }
    }

    private class UserBroker extends Thread {
        private final Socket providerSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;

        UserBroker(Socket providerSocket) {
            this.providerSocket = providerSocket;
            this.out = null;
            this.in = null;
        }

        protected synchronized void addDataForProcessing(Chunk[] chunks) {
            synchronized (dataForProcessing) {
                dataForProcessing.add(chunks);
            }
        }

        @Override
        public void run() {

            try {
                out = new ObjectOutputStream(providerSocket.getOutputStream());
                in = new ObjectInputStream(providerSocket.getInputStream());

                // DummyUser registration
                int userID = (int) in.readObject();
                //this.userID = userID;

                synchronized (database) {
                    user = database.initUser(userID);
                }

                System.out.println("UserBroker for User #" + userID + " started.");
                out.writeObject(1);

                int localGPXID;

                while (providerSocket.isConnected()){
                    // Take GPX from DummyUser
                    StringBuilder buffer = (StringBuilder) in.readObject();
                    System.out.println("UserBroker for User #" + userID + " - GPX received.");

                    synchronized (g_gpxID){
                        g_gpxID++;
                        localGPXID = g_gpxID;
                    }

                    /* Convert the GPX file into a list of Waypoints */

                    // Parse GPX
                    //gpxID = Integer.parseInt(buffer.substring(0, buffer.indexOf("!")));
                    GPXParser parser = new GPXParser(buffer);
                    ArrayList<Waypoint> waypoints = parser.parse();

                    int numChunks = (int) ((waypoints.size() / 10) + 0.5);
                    int totalChunks = numChunks;
                    int chunkSize = waypoints.size() / (numChunks) + 1;

                    Chunk[] chunks = new Chunk[numChunks];

                    // Split the list of waypoints into chunks
                    while (waypoints.size() > 1) {
                        ArrayList<Waypoint> chunkWaypoints = new ArrayList<>();

                        int waypointsSize = waypoints.size();

                        for (int i = 0; i < Math.min(chunkSize, waypointsSize); i++) {
                            chunkWaypoints.add(waypoints.get(0));
                            waypoints.remove(0);
                        }

                        if (!waypoints.isEmpty())
                            chunkWaypoints.add(waypoints.get(0));

                        chunks[--numChunks] = new Chunk(localGPXID, numChunks, totalChunks, chunkWaypoints);
                    }

                    synchronized (intermediateResults){
                        intermediateResults.put(localGPXID, new ArrayList<>());
                    }

                    addDataForProcessing(chunks);

                    System.out.println("UserBroker " + this.getId() + " for DummyUser #" + userID + " waiting for data from worker...");

                    ArrayList<Segment> segments;
                    synchronized (intermediateResults) {
                        while (intermediateResults.get(localGPXID).size() < totalChunks) {
                            intermediateResults.wait();
                        }
                        segments = intermediateResults.get(localGPXID);
                    }

                    /* Reduce the results */
                    Segment result = reduce(localGPXID, segments);

                    Route route = new Route(user.getNumOfRoutes() + 1, waypoints, result.getTotalDistance(), result.getTotalTime(), result.getMeanVelocity(), result.getTotalElevation());

                    synchronized (database) {
                        database.addRoute(route, user.getID());
                    }

                    synchronized (out) {
                        out.writeObject(result);
                        out.flush();
                    }

                    System.out.println("UserBroker " + this.getId() + " for DummyUser #" + userID + " sent final result to user.");
                }
            }
            catch (IOException ioException) {
                System.err.println("UserBroker for DummyUser #" + user.getID() + " - IOERROR: " + ioException.getMessage());
                // Retry opening streams
            }
            catch (ClassNotFoundException classNotFoundException) {
                System.err.println("UserBroker for DummyUser #" + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            }
            catch (Exception e) {
                System.err.println("UserBroker for DummyUser #" + user.getID() + " - ERROR: " + e.getMessage());
                throw new RuntimeException(e); // !!!
            }
            finally {
                try {
                    out.close();
                    in.close();
                    providerSocket.close();
                    System.out.println("UserBroker for DummyUser #" + user.getID() + " shutting down...");
                } catch (IOException ioException) {
                    System.err.println("UserBroker for DummyUser #" + user.getID() + " - IOERROR while shutting down: " + ioException.getMessage());
                }
            }
        }

        private Segment reduce(int localGPXID, ArrayList<Segment> segmentList){
            // Reduce
            System.out.println("UserThread " + this.getId() + " for DummyUser #" + user.getID() + " reducing data for user...");


            double totalDistance = 0;
            double totalElevation = 0;
            long totalTime = 0;

            for (Segment segment : segmentList) {
                totalDistance += segment.getTotalDistance();    //km
                totalElevation += segment.getTotalElevation();  //m
                totalTime += segment.getTotalTime();            //ms
            }

            return new Segment(0, 0, 0, totalDistance, totalDistance / totalTime, totalElevation, totalTime);
        }
    }


    public static void main(String[] args) {
        int workers = 2;

        try {
            workers = Integer.parseInt(args[0]);
            workers = Math.max(2, workers);
        }catch (Exception e){
            System.err.println("Invalid number of workers in arguments. Using default value: 2");
        }

        Master master = new Master(54321, 12345, workers);
        master.bootServer();
    }
}
