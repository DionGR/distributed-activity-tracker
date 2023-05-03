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
    //private final HashMap<Integer, ArrayList<IntermediateChunk>> intermediateResults;
    private final HashMap<Integer, UserGPXBroker> activeGPXUsers;

    private final Database database;


    Master(){
        this.connectedUsers = new ArrayList<>();
        //this.connectedWorkers = new ArrayList<>();
        this.workerOutStreams = new ArrayList<>();
        this.workerInStreams = new ArrayList<>();
        this.dataForProcessing = new ArrayList<>();
        //this.intermediateResults = new HashMap<>();
        this.segments = new HashMap<>();
        this.activeGPXUsers = new HashMap<>();
        this.database = new Database();
    }

    public void bootServer() {
        try {
            /* Initialize server */
            initDefaults();
            Scheduler scheduler = new Scheduler();
            WorkerHandler workerConnectionHandler = new WorkerHandler(workerConnectionPort);
            WorkerHandler workerDataHandler = new WorkerHandler(workerDataPort);
            UserHandler userGPXHandler = new UserHandler(userGPXPort);
            UserHandler userStatisticsHandler = new UserHandler(userStatisticsPort);

            workerConnectionHandler.start();
            workerDataHandler.start();

            /* Wait for workers to connect */
            synchronized (workerOutStreams){
                while (workerOutStreams.size() < MIN_WORKERS){
                    System.err.println("Master | Waiting for workers to connect...");
                    workerOutStreams.wait();
                }
                System.err.println("Master | All workers connected!");
            }

            /* Start scheduler and user handlers */
            scheduler.start();
            userGPXHandler.start();
            userStatisticsHandler.start();
        }catch (InterruptedException interruptedException) {
            System.err.println("Master - bootServer - InterruptedERROR while booting: " + interruptedException.getMessage());
        }catch (Exception e) {
            System.err.println("Master - bootServer - ERROR while booting: " + e.getMessage());
        }
    }

    private void initDefaults() {
        FileReader reader = null;
        try {
            /* Set configurations */
            reader = new FileReader(System.getProperty("user.dir") + "\\data\\server-data\\serverCFG");
            Properties properties = new Properties();
            properties.load(reader);

            workerConnectionPort = Integer.parseInt(properties.getProperty("workerConnectionPort"));
            workerDataPort = Integer.parseInt(properties.getProperty("workerDataPort"));

            userGPXPort = Integer.parseInt(properties.getProperty("userGPXPort"));
            userStatisticsPort = Integer.parseInt(properties.getProperty("userStatisticsPort"));

            MIN_WORKERS = Integer.parseInt(properties.getProperty("minWorkers"));

            System.err.println("Master | Initializing Configuration |");
            System.err.println("Master | [VALUE] Min. workers: " + MIN_WORKERS);
            System.err.println("Master | [PORT] Worker Connection: " + workerConnectionPort);
            System.err.println("Master | [PORT] Worker Data: " + workerDataPort);
            System.err.println("Master | [PORT] User GPX: " + userGPXPort);
            System.err.println("Master | [PORT] User Statistics: " + userStatisticsPort);


            /* Set default segments */
            File[] loadedSegments = new File(System.getProperty("user.dir") + "\\data\\server-data\\segments\\").listFiles();
            if (loadedSegments != null) {
                for (int i = 0; i < loadedSegments.length; i++) {
                    BufferedReader br = new BufferedReader(new FileReader(loadedSegments[i]));
                    String line;
                    StringBuilder buffer = new StringBuilder();
                    while((line = br.readLine()) != null)
                        buffer.append(line);
//                    segments.put(i, GPXParser.parse(buffer));
                    database.initSegment(i, GPXParser.parse(buffer));
                    br.close();
                }
                System.err.println("Master | [DATA] Segments: " + loadedSegments.length);
            }else {
                System.err.println("Master | [DATA] Segments: 0");
            }

            System.err.println("Master | Initialization complete |");
        }
        catch (IOException ioException) {
            System.err.println("Master - initDefaults - IOERROR while initializing defaults: " + ioException.getMessage());
            throw new RuntimeException("initDefaults - IOERROR: " + ioException.getMessage());
        }catch (Exception e) {
            System.err.println("Master - initDefaults - ERROR while initializing defaults: " + e.getMessage());
            throw new RuntimeException("initDefaults - ERROR: " + e.getMessage());
        }finally{
            try{
                reader.close();
            }catch (IOException e) {
                System.err.println("Master - initDefaults - IOERROR while closing reader: " + e.getMessage());
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
            } finally {
                System.err.println("Scheduler - Shutting down...");
            }
        }
    }
    private class WorkerHandler extends Thread {
        private final int port;
        private ServerSocket serverSocket;

        WorkerHandler(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try{
                serverSocket = new ServerSocket(port, 100);

                if (serverSocket.getLocalPort() == workerConnectionPort)
                    workerConnectionHandler();
                else if (serverSocket.getLocalPort() == workerDataPort)
                    workerDataHandler();

            } catch (IOException ioException) {
                System.err.println("WorkerHandler IOERROR: " + ioException.getMessage());
                throw new RuntimeException("WorkerHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("WorkerHandler ERROR: " + e.getMessage());
                throw new RuntimeException("WorkerHandler ERROR: " + e.getMessage());
            } finally {
                try {
                    System.err.println("WorkerHandler - Shutting down...");
                    serverSocket.close();
                } catch (IOException ioException) {
                    System.err.println("WorkerHandler - IOERROR while shutting down: " + ioException.getMessage());
                }
            }
        }

        public void workerConnectionHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket providerSocket = serverSocket.accept();
                    System.out.println("WorkerConnectionHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    ObjectOutputStream out = new ObjectOutputStream(providerSocket.getOutputStream());

                    synchronized (workerOutStreams) {
                        workerOutStreams.add(out);
                        if (workerOutStreams.size() >= MIN_WORKERS) workerOutStreams.notifyAll();
                    }
                }

            } catch (IOException ioException) {
                System.err.println("WorkerConnectionHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("WorkerConnectionHandler - ERROR: " + e.getMessage());
            }finally {
                try {
                    for (ObjectOutputStream out: workerOutStreams) out.close();
                } catch (IOException ioException) {
                    System.err.println("WorkerConnectionHandler - IOERROR while shutting down... " + ioException.getMessage());
                }
            }
        }

        public void workerDataHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket providerSocket = serverSocket.accept();
                    System.out.println("WorkerDataHandler: Connection received from " + providerSocket.getInetAddress().getHostName());
                    new Worker(providerSocket).start();
                }
            } catch (IOException ioException) {
                System.err.println("WorkerHandler - workerDataHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("WorkerHandler - workerDataHandler - ERROR: " + e.getMessage());
            }
        }
    }
    private class UserHandler extends Thread {
        private final int port;
        private ServerSocket usersSocketToHandle;

        UserHandler(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                usersSocketToHandle = new ServerSocket(port, 1000);
                if (usersSocketToHandle.getLocalPort() == userGPXPort)
                    userGPXHandler();
                else if (usersSocketToHandle.getLocalPort() == userStatisticsPort)
                    userStatisticsHandler();


            /* TODO: Handle thread crash, user disconnects etc */
            } catch (Exception e) {
                System.err.println("UserHandler ERROR: " + e.getMessage());
                throw new RuntimeException("UserHandler ERROR: " + e.getMessage());
            } finally {
                try {
                    System.err.println("UserHandler - Shutting down...");
                    usersSocketToHandle.close();
                } catch (IOException ioException) {
                    System.err.println("UserHandler IOERROR while shutting down... " + ioException.getMessage());
                }
            }
        }

        private void userGPXHandler(){
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket providerSocket = usersSocketToHandle.accept();
                    System.out.println("UserGPXHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    /* Handle the request */
                    UserGPXBroker broker = new UserGPXBroker(providerSocket);
                    broker.start();
                }
            } catch (IOException ioException) {
                System.err.println("UserGPXHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("UserGPXHandler ERROR: " + e.getMessage());
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
            }
        }
    }


    private class UserGPXBroker extends Thread {
        private final Socket providerSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;
        private int localGPXID, totalChunks;
        private ArrayList<Waypoint> waypoints;
        private final ArrayList<IntermediateChunk> intermediateResults;

        UserGPXBroker(Socket providerSocket) {
            this.providerSocket = providerSocket;
            this.out = null;
            this.in = null;
            this.totalChunks = 0;
            this.intermediateResults = new ArrayList<>();
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

                synchronized (activeGPXUsers) {
                    activeGPXUsers.put(userID, this);
                }


                synchronized (database) {
                    user = database.initUser(userID);
                }

                System.out.println("UserGPXBroker for User #" + userID + " started."); // TODO: Might remove


                /* Take GPX from DummyUser */
                StringBuilder buffer;
                buffer = (StringBuilder) in.readObject();

                System.out.println("UserGPXBroker for User #" + userID + " - GPX received.");


                /* Convert the GPX file into a list of Waypoints */

                // Parse GPX
                waypoints = GPXParser.parse(buffer);

                /* Find segments */
                new SegmentFinder().start();


                /* Split waypoints into chunks */
                int numChunks = (int) ((waypoints.size() / 10) + 0.5);
                int chunkSize = waypoints.size() / (numChunks) + 1;
                totalChunks = numChunks + segments.size();

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

                    chunks[--numChunks] = new Chunk(userID, numChunks, totalChunks, chunkWaypoints);
                }

//                synchronized (intermediateResults) {
//                    intermediateResults.put(localGPXID, new ArrayList<>());
//                }

                addDataForProcessing(chunks);

                System.out.println("UserGPXBroker " + this.getId() + " for DummyUser #" + userID + " waiting for data from worker...");

                ArrayList<IntermediateChunk> chunkedGPXs;
                synchronized (intermediateResults) {
                    while (intermediateResults.size() < totalChunks) {
                        intermediateResults.wait();
                    }
                }

                /* Get intermediate results */
//                ArrayList<IntermediateChunk> returnedSegments = new ArrayList<>();
//                ArrayList<IntermediateChunk> returnedChunks  = new ArrayList<>();
//                for(IntermediateChunk c: intermediateResults){
//                    if(c.getSegmentID() < 0){
//                        returnedSegments.add(c);
//                    }else{
//                        returnedChunks.add(c);
//                    }
//                }

                /* Reduce the results */
                IntermediateChunk result = reduce();

                Route route = new Route(user.getStatistics().getSubmissions() + 1, waypoints, result.getTotalDistance(), result.getTotalTime(), result.getMeanVelocity(), result.getTotalElevation());

                synchronized (database) {
                    database.addRoute(route, user.getID());
                }
                out.writeObject(result);
                out.flush();


                System.out.println("UserGPXBroker " + this.getId() + " for DummyUser #" + userID + " sent final result to user.");

            } catch (IOException ioException) {
                System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - IOERROR: " + ioException.getMessage());
                // Retry opening streams
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - ERROR: " + e.getMessage());
                //throw new RuntimeException(e); // !!!
            } finally {
                try {
                    in.close();
                    out.close();
                    providerSocket.close();
                    synchronized (activeGPXUsers){
                        activeGPXUsers.remove(user.getID());
                    }
                    System.out.println("UserGPXBroker for DummyUser #" + user.getID() + " shutting down...");
                } catch (IOException ioException) {
                    System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - IOERROR while shutting down: " + ioException.getMessage());
                }
            }
        }

        public void addIntermediateResult(IntermediateChunk s) {
            synchronized (intermediateResults){
                intermediateResults.add(s);
                if (intermediateResults.size() >= totalChunks)
                    intermediateResults.notifyAll();
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
                                chunks.add(new Chunk(i, -i-1, totalChunks, foundSegment));
                            }else {
                                y -= 1;
                            }
                        } else {
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

        public void addIntermediateResults(IntermediateChunk data){
//            synchronized (intermediateResults){
//                intermediateResults.get(data.getGPXID()).add(data);
//                if ((intermediateResults.get(data.getGPXID()).size() >= data.getTotalSegments()))
//                    intermediateResults.notifyAll();
//            }
            synchronized (activeGPXUsers){
                activeGPXUsers.get(data.getGPXID()).addIntermediateResult(data);
            }
        }

        @Override
        public void run() {
            try {
                in = new ObjectInputStream(workerSocket.getInputStream());

                IntermediateChunk data = (IntermediateChunk) in.readObject();
                System.out.println("Worker port: "+ workerPort + " received data for GPX: " + data.getGPXID());
                addIntermediateResults(data);

            }catch (IOException ioException) {
                System.err.println("Worker port: "+ workerPort + " - IOERROR: " + ioException.getMessage());
            }catch (ClassNotFoundException classNotFoundException) {
                System.err.println("Worker port: "+ workerPort + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                System.err.println("Worker port: "+ workerPort + " - ERROR: " + e.getMessage());
                e.printStackTrace();
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


    public static void main(String[] args) {
        Master master = new Master();
        master.bootServer();
    }
}