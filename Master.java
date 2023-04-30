import java.lang.reflect.Array;
import java.net.*;
import java.io.*;
import java.util.*;

import static java.util.Collections.indexOfSubList;


class Master {
    private int MIN_WORKERS;
    private int userGPXPort, userStatisticsPort, workerConnectionPort, workerDataPort;

    private final ArrayList<UserGPXBroker> connectedUsers;
    //private final ArrayList<Worker> connectedWorkers;
    private final ArrayList<ObjectOutputStream> workerOutStreams;
    private final ArrayList<ObjectInputStream> workerInStreams;
    private final HashMap<Integer, ArrayList<Waypoint>> segments;


    private final ArrayList<Chunk[]> dataForProcessing;
    private final HashMap<Integer, ArrayList<Segment>> intermediateResults;

    private final Database database;
    private Integer g_gpxID;


    Master(){
        this.connectedUsers = new ArrayList<>();
        //this.connectedWorkers = new ArrayList<>();
        this.workerOutStreams = new ArrayList<>();
        this.workerInStreams = new ArrayList<>();
        this.dataForProcessing = new ArrayList<>();
        this.intermediateResults = new HashMap<>();
        this.segments = new HashMap<>();
        this.database = new Database();
        this.g_gpxID = 0;
    }

    public void bootServer() {
        try {
            initDefaults();
            WorkerHandler workerConnectionHandler = new WorkerHandler(workerConnectionPort);
            WorkerHandler workerDataHandler = new WorkerHandler(workerDataPort);
            UserHandler userGPXHandler = new UserHandler(userGPXPort); // TODO: Statistics/GPX Handlers
            UserHandler userStatisticsHandler = new UserHandler(userStatisticsPort);
            Scheduler scheduler = new Scheduler();

            workerConnectionHandler.start();
            workerDataHandler.start();

            synchronized (workerOutStreams){
                while (workerOutStreams.size() < MIN_WORKERS){
                    System.err.println("Master: Waiting for workers to connect...");
                    workerOutStreams.wait();
                }
                System.err.println("Master: All workers connected...");
            }

            userGPXHandler.start();
            userStatisticsHandler.start();
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


            workerConnectionPort = Integer.parseInt(properties.getProperty("workerConnectionPort"));
            workerDataPort = Integer.parseInt(properties.getProperty("workerDataPort"));

            userGPXPort = Integer.parseInt(properties.getProperty("userGPXPort"));
            userStatisticsPort = Integer.parseInt(properties.getProperty("userStatisticsPort"));

            MIN_WORKERS = Integer.parseInt(properties.getProperty("minWorkers"));

            System.err.println("Master | Configurations loaded |");
            System.err.println("Master | Worker Connection PORT: " + workerConnectionPort);
            System.err.println("Master | Worker Data PORT: " + workerDataPort);
            System.err.println("Master | Min workers: " + MIN_WORKERS);
            System.err.println("Master | User GPX PORT: " + userGPXPort);
            System.err.println("Master | User Statistics PORT: " + userStatisticsPort);


            /* Set default segments */
            File[] loadedSegments = new File(System.getProperty("user.dir") + "\\data\\segments\\").listFiles();
            if (loadedSegments != null) {
                for (int i = 0; i < loadedSegments.length; i++) {
                    BufferedReader br = new BufferedReader(new FileReader(loadedSegments[i]));
                    String line;
                    StringBuilder buffer = new StringBuilder();
                    while((line = br.readLine()) != null)
                        buffer.append(line);
//                    segments.put(i, GPXParser.parse(buffer));
                    database.initSegment(i, GPXParser.parse(buffer));
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
            try{
                if (workersSocketToHandle.getLocalPort() == workerConnectionPort)
                    workerConnectionHandler();
                else if (workersSocketToHandle.getLocalPort() == workerDataPort)
                    workerDataHandler();
            } catch (Exception e) {
                System.err.println("WorkerHandler ERROR: " + e.getMessage());
            }
        }

        public void workerConnectionHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket providerSocket = workersSocketToHandle.accept();
                    System.out.println("WorkerConnectionHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    ObjectOutputStream out = new ObjectOutputStream(providerSocket.getOutputStream());
                    synchronized (workerOutStreams) {
                        workerOutStreams.add(out);
                        if (workerOutStreams.size() >= MIN_WORKERS) workerOutStreams.notifyAll();
                    }
                }

            } catch (IOException ioException) {
                System.err.println("WorkerConnectionHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("WorkerConnectionHandler ERROR: " + e.getMessage());
            }finally {
                try {
                    for (ObjectOutputStream out: workerOutStreams)
                        out.close();
                    workersSocketToHandle.close();
                } catch (IOException ioException) {
                    System.err.println("WorkerConnectionHandler IOERROR while shutting down... " + ioException.getMessage());
                }
            }
        }

        public void workerDataHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket providerSocket = workersSocketToHandle.accept();
                    System.out.println("WorkerDataHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    new Worker(providerSocket).start();
                }
            } catch (IOException ioException) {
                System.err.println("WorkerDataHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("WorkerDataHandler ERROR: " + e.getMessage());
            } finally {
                try {
                    workersSocketToHandle.close();
                } catch (IOException ioException) {
                    System.err.println("WorkerDataHandler IOERROR while shutting down... " + ioException.getMessage());
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
                if (usersSocketToHandle.getLocalPort() == userGPXPort)
                    userGPXHandler();
                else if (usersSocketToHandle.getLocalPort() == userStatisticsPort)
                    userStatisticsHandler();


                /* TODO: Handle thread crash, user disconnects etc */
//            } catch (IOException ioException) {
//                System.err.println("UserHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("UserHandler ERROR: " + e.getMessage());
            }
//            } finally {
//                try {
//                    usersSocketToHandle.close();
//                } catch (IOException ioException) {
//                    System.err.println("UserHandler IOERROR while shutting down... " + ioException.getMessage());
//                }
//            }
        }

        private void userGPXHandler(){
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket providerSocket = usersSocketToHandle.accept();
                    System.out.println("UserGPXHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    /* Handle the request */
                    UserGPXBroker broker = new UserGPXBroker(providerSocket);
//                    connectedUsers.add(broker);
                    broker.start();
                }
            } catch (IOException ioException) {
                System.err.println("UserGPXHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("UserGPXHandler ERROR: " + e.getMessage());
            } finally {
                try {
                    usersSocketToHandle.close();
                } catch (IOException ioException) {
                    System.err.println("UserGPXHandler IOERROR while shutting down... " + ioException.getMessage());
                }
            }
        }

        private void userStatisticsHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket providerSocket = usersSocketToHandle.accept();
                    System.out.println("UserStatisticsHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    /* Handle the request */
                    UserStatisticsBroker broker = new UserStatisticsBroker(providerSocket);
//                    connectedUsers.add(broker);
                    broker.start();
                }
            } catch (IOException ioException) {
                System.err.println("UserStatisticsHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("UserStatisticsHandler ERROR: " + e.getMessage());
            }finally {
                try {
                    usersSocketToHandle.close();
                } catch (IOException ioException) {
                    System.err.println("UserStatisticsHandler IOERROR while shutting down... " + ioException.getMessage());
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

                            synchronized (workerOutStreams) {
                                out = workerOutStreams.get(nextWorker);
                            }

                            System.out.println("Assigning data to worker: " + nextWorker);

                            out.writeObject(c);
                            out.flush();

                            System.out.println("Data assigned to worker: " + c.toString());

                            nextWorker = (++nextWorker) % workerOutStreams.size();
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
        int workerPort;

        // TODO: What should we keep??????????????
        public Worker(Socket workerSocket){
            this.workerSocket = workerSocket;
            this.workerPort = workerSocket.getPort();
            System.out.println("Worker port: " + workerPort); // TODO: replace ports with something unique
            this.in = null;
        }

        public void addIntermediateResults(Segment data){
            synchronized (intermediateResults){
                intermediateResults.get(data.getGPXID()).add(data);
                if ((intermediateResults.get(data.getGPXID()).size() == data.getTotalSegments()) || data.getSegmentID() < 0)
                    intermediateResults.notifyAll();
            }
        }

        @Override
        public void run() {
            try {
                in = new ObjectInputStream(workerSocket.getInputStream());

                Segment data = (Segment) in.readObject();
                System.out.println("Worker port: "+ workerPort + " received data for GPX: " + data.getGPXID());
                addIntermediateResults(data);

            }catch (IOException ioException) {
                System.err.println("Worker port: "+ workerPort + " - IOERROR: " + ioException.getMessage());
            }catch (ClassNotFoundException classNotFoundException) {
                System.err.println("Worker port: "+ workerPort + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                System.err.println("Worker port: "+ workerPort + " - ERROR: " + e.getMessage());
                throw new RuntimeException(); // !!!!!!!!!
            }finally {
                try {
                    in.close();
                    workerSocket.close();
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
    }

    private class UserGPXBroker extends Thread {
        private final Socket providerSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;
        private int localGPXID, totalChunks;
        private ArrayList<Waypoint> waypoints;

        UserGPXBroker(Socket providerSocket) {
            this.providerSocket = providerSocket;
            this.out = null;
            this.in = null;
            this.totalChunks = 0;
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

                /* DummyUser registration */
                int userID = (int) in.readObject();


                synchronized (database) {
                    user = database.initUser(userID);
                }

                System.out.println("UserGPXBroker for User #" + userID + " started."); // TODO: Might remove


                /* Take GPX from DummyUser */
                StringBuilder buffer;
                buffer = (StringBuilder) in.readObject();

                System.out.println("UserGPXBroker for User #" + userID + " - GPX received.");

                synchronized (g_gpxID) {
                    g_gpxID++;
                    localGPXID = g_gpxID;
                }

                /* Convert the GPX file into a list of Waypoints */

                // Parse GPX
                waypoints = GPXParser.parse(buffer);

                /* Find segments */
                new SegmentFinder().start();


                /* Split waypoints into chunks */
                int numChunks = (int) ((waypoints.size() / 10) + 0.5);
                int chunkSize = waypoints.size() / (numChunks) + 1;
                totalChunks = numChunks;

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

                synchronized (intermediateResults) {
                    intermediateResults.put(localGPXID, new ArrayList<>());
                }

                addDataForProcessing(chunks);

                System.out.println("UserGPXBroker " + this.getId() + " for DummyUser #" + userID + " waiting for data from worker...");

                ArrayList<Segment> chunkedGPXs;
                synchronized (intermediateResults) {
                    while (intermediateResults.get(localGPXID).size() < totalChunks + segments.size()) {
                        intermediateResults.wait();
                    }
                    chunkedGPXs = intermediateResults.get(localGPXID);
                    //TODO intermediateResults.remove(localGPXID);
                }

                /* Get intermediate results */
                ArrayList<Segment> returnedSegments = new ArrayList<>();
                ArrayList<Segment> returnedChunks  = new ArrayList<>();
                for(Segment c: chunkedGPXs){
                    if(c.getSegmentID() < 0){
                        returnedSegments.add(c);
                    }else{
                        returnedChunks.add(c);
                    }
                }

                /* Reduce the results */
                IntermediateChunk result = reduce();

                Route route = new Route(user.getStatistics().getSubmissions() + 1, waypoints, result.getTotalDistance(), result.getTotalTime(), result.getMeanVelocity(), result.getTotalElevation());

                synchronized (database) {
                    database.addRoute(route, user.getID());
                }
                for (Segment s: returnedSegments) {
                    System.out.println("UserGPXBroker " + this.getId() + " for DummyUser #" + userID + " received segment #" + s.getSegmentID() + " from worker.");
                }
//                synchronized (out) {            //TODO: 2 handlers/ports or requests in userHandler
                out.writeObject(result);
                out.flush();
//                }

                System.out.println("UserGPXBroker " + this.getId() + " for DummyUser #" + userID + " sent final result to user.");

            } catch (IOException ioException) {
                System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - IOERROR: " + ioException.getMessage());
                // Retry opening streams
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - ERROR: " + e.getMessage());
                throw new RuntimeException(e); // !!!
            } finally {
                try {
                    in.close();
                    out.close();
                    providerSocket.close();
                    System.out.println("UserGPXBroker for DummyUser #" + user.getID() + " shutting down...");
                } catch (IOException ioException) {
                    System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - IOERROR while shutting down: " + ioException.getMessage());
                }
            }
        }

        private IntermediateChunk reduce() {
            // Reduce
            System.out.println("UserGPXBroker " + this.getId() + " for DummyUser #" + user.getID() + " reducing data for user...");


            double totalDistance = 0;
            double totalElevation = 0;
            long totalTime = 0;

            for (IntermediateChunk intermediateChunk : intermediateResults) {
                if(intermediateChunk.getSegmentID() < 0){
                    database.updateSegmentStats(user, intermediateChunk); //segment identifier (index in arraylist)
                }
                else {
                    totalDistance += intermediateChunk.getTotalDistance();    //km
                    totalElevation += intermediateChunk.getTotalElevation();  //m
                    totalTime += intermediateChunk.getTotalTime();            //ms
                }
            }

            return new IntermediateChunk(localGPXID, 0, intermediateResults.size(), totalDistance, totalDistance / totalTime, totalElevation, totalTime);
        }

        private class SegmentFinder extends Thread {
            @Override
            public void run() {
                try{
                    ArrayList<Waypoint> wps = new ArrayList<>(waypoints);
                    ArrayList<Chunk> chunks = new ArrayList<>();
                    int y = 0;

                    for (int i = 0; i < segments.size(); i++){
                        ArrayList<Waypoint> segment = segments.get(i);
                        int segmentStartIndex = indexOfSubList(wps, segment);
                        if (segmentStartIndex != -1) {
                            ArrayList<Waypoint> foundSegment = new ArrayList<>(wps.subList(segmentStartIndex, segmentStartIndex + segment.size()));
                            if (foundSegment.equals(segment)) {
                                System.out.println("UserGPXBroker " + this.getId() + " for DummyUser #" + user.getID() + " found segment #" + i + " in user's GPX.");
                                chunks.add(new Chunk(localGPXID, -i-1, totalChunks, foundSegment));
                            }else {
                                y -= 1;
                            }
                        }else{
                            y -= 1; //!!!!!!!!!!!!
                        }
                    }

                    Chunk[] foundSegments = new Chunk[chunks.size()];
                    chunks.toArray(foundSegments);
                    addDataForProcessing(foundSegments);
                    totalChunks += y;
                }catch (Exception e){
                    System.err.println("SegmentFinder for DummyUser #" + user.getID() + " - ERROR: " + e.getMessage());
                }
            }
        }


    }
    private class UserStatisticsBroker extends Thread{
        private final Socket providerSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;

        UserStatisticsBroker(Socket providerSocket) {
            this.providerSocket = providerSocket;
            this.out = null;
            this.in = null;
        }

        @Override
        public void run(){
            try {
                out = new ObjectOutputStream(providerSocket.getOutputStream());
                in = new ObjectInputStream(providerSocket.getInputStream());

                // DummyUser registration
                int userID = (int) in.readObject();
                Statistics totalData;


                synchronized (database) {
                    user = database.initUser(userID);
                    totalData = database.getTotalData();
                }

                out.writeObject(user.getStatistics());
                out.writeObject(totalData);
                out.flush();

            }catch (IOException ioException) {
                    System.err.println("UserStatisticsBroker for DummyUser #" + user.getID() + " - IOERROR: " + ioException.getMessage());
                    // Retry opening streams
            }catch (ClassNotFoundException classNotFoundException) {
                    System.err.println("UserStatisticsBroker for DummyUser #" + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                    System.err.println("UserStatisticsBroker for DummyUser #" + user.getID() + " - ERROR: " + e.getMessage());
                    throw new RuntimeException(e); // !!!
            }finally {
                try {
                    out.close(); in.close();
                    providerSocket.close();
                    System.out.println("UserStatisticsBroker for DummyUser #" + user.getID() + " shutting down...");
                } catch (IOException ioException) {
                    System.err.println("UserStatisticsBroker for DummyUser #" + user.getID() + " - IOERROR while shutting down: " + ioException.getMessage());
                }
            }
        }
    }


    public static void main(String[] args) {
        Master master = new Master();
        master.bootServer();
    }
}