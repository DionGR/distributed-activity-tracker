import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;


class Master {
    private int USERPORT, WORKERPORT;

    private final ArrayList<UserBroker> connectedUsers;
    private final ArrayList<Worker> connectedWorkers;
    private final HashMap<Integer, ArrayList<Waypoint>> segments;

    private final ArrayList<Chunk[]> dataForProcessing;
    private final HashMap<Integer, ArrayList<Segment>> intermediateResults;

    private final Database database;
    private int MIN_WORKERS;
    private Integer g_gpxID;


    Master(){
        this.connectedUsers = new ArrayList<>();
        this.connectedWorkers = new ArrayList<>();
        this.dataForProcessing = new ArrayList<>();
        this.intermediateResults = new HashMap<>();
        this.segments = new HashMap<>();
        this.database = new Database();
        this.g_gpxID = 0;
    }

    public void bootServer() {
        try {
            initDefaults();
            UserHandler userHandler = new UserHandler(USERPORT);
            WorkerHandler workerHandler = new WorkerHandler(WORKERPORT);
            Scheduler scheduler = new Scheduler();

            workerHandler.start();

            synchronized (connectedWorkers){
                while (connectedWorkers.size() < MIN_WORKERS){
                    System.err.println("Master: Waiting for workers to connect...");
                    connectedWorkers.wait();
                }
                System.err.println("Master: All workers connected...");
            }

            userHandler.start();
            scheduler.start();
    }catch (InterruptedException e) {
        System.err.println("Master: Interrupted while waiting for workers to connect: " + e.getMessage());
    }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initDefaults(){
        try {
            /* Set configurations */
            FileReader reader = new FileReader(System.getProperty("user.dir") + "\\data\\config");
            Properties properties = new Properties();
            properties.load(reader);


            WORKERPORT = Integer.parseInt(properties.getProperty("workerPort"));
            USERPORT = Integer.parseInt(properties.getProperty("userPort"));
            MIN_WORKERS = Integer.parseInt(properties.getProperty("minWorkers"));

            System.err.println("Master | Configurations loaded |");
            System.err.println("Master | Worker PORT: " + WORKERPORT);
            System.err.println("Master | User PORT: " + USERPORT);
            System.err.println("Master | Min workers: " + MIN_WORKERS);


            /* Set default segments */
            File[] loadedSegments = new File(System.getProperty("user.dir") + "\\data\\segments\\").listFiles();
            if (loadedSegments != null) {
                for (int i = 0; i < loadedSegments.length; i++) {
                    BufferedReader br = new BufferedReader(new FileReader(loadedSegments[i]));
                    String line;
                    StringBuilder buffer = new StringBuilder();
                    while((line = br.readLine()) != null)
                        buffer.append(line);
                    segments.put(i, GPXParser.parse(buffer));
                }
                System.err.println("Master | Segments loaded: " + loadedSegments.length);
            }else {
                System.err.println("Master | Segments loaded: 0");
            }
        }
        catch (IOException ioException) {
            System.err.println("Master IOERROR: " + ioException.getMessage());
        }catch (Exception e) {
            System.err.println("Master ERROR: " + e.getMessage());
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

                    if (workersSocketToHandle.getLocalPort() == workerConnectionPort){
                        ObjectOutputStream out = new ObjectOutputStream(providerSocket.getOutputStream());
                        synchronized (workerOutStreams) {
                            workerOutStreams.add(out);
                            if (workerOutStreams.size() >= MIN_WORKERS) workerOutStreams.notifyAll();
                        }

                    }else if (workersSocketToHandle.getLocalPort() == workerDataPort){
//                        ObjectOutputStream out = new ObjectOutputStream(providerSocket.getOutputStream());
//                        ObjectInputStream in = new ObjectInputStream(providerSocket.getInputStream());
//                        synchronized (workerInStreams) {
//                            workerInStreams.add(in);
//                        }
                        Worker worker = new Worker(providerSocket);
                        worker.start();
                    }

//                    synchronized (connectedWorkers){
//                        connectedWorkers.add(worker);
//                        if (connectedWorkers.size() >= MIN_WORKERS) connectedWorkers.notifyAll();
//                    }


                }

                /* TODO: Handle thread crash, worker disconnects etc */
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

                /* TODO: Handle thread crash, user disconnects etc */
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
    private class Scheduler extends Thread{

        @Override
        public void run(){

            int nextWorker = 0;

            try {
                while (!Thread.currentThread().isInterrupted()) {


                    /* Assign data to workers using round-robin */
                    while (dataForProcessing.size() > 0) {
                        Chunk[] chunks;

                        synchronized (dataForProcessing) {
                            chunks = dataForProcessing.get(0);
                            dataForProcessing.remove(0);
                        }

                        for (Chunk c : chunks) {
                            ObjectOutputStream out;

                            synchronized (connectedWorkers) {
                                out = connectedWorkers.get(nextWorker).getOutputStream();
                            }

                            System.out.println("Assigning data to worker: " + nextWorker);

                            out.writeObject(c);
                            out.flush();

                            System.out.println("Data assigned to worker: " + c.toString());

                            nextWorker = (++nextWorker) % connectedWorkers.size();
                        }
                    }
                }
                /* TODO: Handle thread crash, worker disconnects etc */
            }catch(IOException ioException){
                System.err.println("Scheduler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Scheduler ERROR: " + e.getMessage());
            }
        }
    }


    private class Worker extends Thread {
        Socket workerSocket;
        ObjectInputStream in;
        ObjectOutputStream out;
        int workerPort;

        // TODO: What should we keep??????????????
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

                //while (workerSocket.isConnected()) {
                    /* Waiting for intermediate result */
                    Segment data = (Segment) in.readObject();
                    System.out.println("Worker port: "+ workerPort + " received data for GPX: " + data.getGPXID());
                    addIntermediateResults(data);
                //}

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
//                    synchronized (connectedWorkers){
//                        connectedWorkers.remove(this);
//                        connectedWorkers.notifyAll();  //!!!! lathos logikh
//                  }
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



    private class UserBroker extends Thread {
        private final Socket providerSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;
        private int localGPXID;

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

                System.out.println("UserBroker for User #" + userID + " started."); // TODO: Might remove
                out.writeObject(1);

                while (providerSocket.isConnected()){

                    /* Take GPX from DummyUser */
                    StringBuilder buffer;
                    try {
                        buffer = (StringBuilder) in.readObject();
                    }
                    catch (IOException ioException){
                        System.err.println("DummyUser #" + user.getID() + " has disconnected!" );
                        break;
                    }

                    System.out.println("UserBroker for User #" + userID + " - GPX received.");

                    synchronized (g_gpxID){
                        g_gpxID++;
                        localGPXID = g_gpxID;
                    }

                    /* Convert the GPX file into a list of Waypoints */

                    // Parse GPX
                    ArrayList<Waypoint> waypoints = GPXParser.parse(buffer);

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
                       //TODO intermediateResults.remove(localGPXID);
                    }

                    /* Reduce the results */
                    Segment result = reduce(segments);

                    Route route = new Route(user.getSubmissions() + 1, waypoints, result.getTotalDistance(), result.getTotalTime(), result.getMeanVelocity(), result.getTotalElevation());

                    synchronized (database) {
                        database.addRoute(route, user.getID());
                    }

                    synchronized (out) {            //TODO: 2 handlers/ports or requests in userHandler
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

        private Segment reduce(ArrayList<Segment> segmentList){
            // Reduce
            System.out.println("UserBroker " + this.getId() + " for DummyUser #" + user.getID() + " reducing data for user...");


            double totalDistance = 0;
            double totalElevation = 0;
            long totalTime = 0;

            for (Segment segment : segmentList) {
                totalDistance += segment.getTotalDistance();    //km
                totalElevation += segment.getTotalElevation();  //m
                totalTime += segment.getTotalTime();            //ms
            }

            return new Segment(localGPXID, segments.size(), totalDistance, totalDistance / totalTime, totalElevation, totalTime);
        }
    }


    public static void main(String[] args) {
        Master master = new Master();
        master.bootServer();
    }
}