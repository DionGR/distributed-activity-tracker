package app.backend.master;
import app.backend.modules.*;

import java.net.*;
import java.io.*;
import java.util.*;

import static java.util.Collections.indexOfSubList;

/* Master Class
*
* @authors: P3200098, P3200150, P3200262
* @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
*
* Handles user GPX/statistics requests, schedules them for mapping to workers and
* processes the results from workers, reducing them and sending them back to the user.
 */


class Master {
    private int MIN_WORKERS;
    private int userGPXPort, userStatisticsPort, userSegmentPort, userSegStatisticsPort, workerConnectionPort, workerInDataPort;

    private final Database database;
    private final ArrayList<Chunk[]> dataForProcessing;
    private final ArrayList<ObjectOutputStream> workerConnectionOuts;
    private final HashMap<String, UserGPXThread> activeGPXUsers;


    Master(){
        this.database = new Database();
        this.dataForProcessing = new ArrayList<>();
        this.workerConnectionOuts = new ArrayList<>();
        this.activeGPXUsers = new HashMap<>();
    }

    /* Initialize default values */
    public void bootServer() {
        try {
            /* Initialize server */
            initDefaults();
            Scheduler scheduler = new Scheduler();
            WorkerHandler workerConnectionHandler = new WorkerHandler(workerConnectionPort);
            WorkerHandler workerDataHandler = new WorkerHandler(workerInDataPort);
            UserHandler userGPXHandler = new UserHandler(userGPXPort);
            UserHandler userStatisticsHandler = new UserHandler(userStatisticsPort);
            UserHandler userSegmentHandler = new UserHandler(userSegmentPort);
            UserHandler userSegStatisticHandler = new UserHandler(userSegStatisticsPort);

            /* Start worker handlers */
            workerConnectionHandler.start();
            workerDataHandler.start();

            /* Wait for workers to connect */
            synchronized (workerConnectionOuts){
                while (workerConnectionOuts.size() < MIN_WORKERS){
                    System.err.println("Master | Waiting for workers to connect...");
                    workerConnectionOuts.wait();
                }
                System.err.println("Master | All workers connected!");
            }

            /* Start scheduler and user handlers */
            scheduler.start();
            userGPXHandler.start();
            userStatisticsHandler.start();
            userSegmentHandler.start();
            userSegStatisticHandler.start();

        } catch (InterruptedException interruptedException) {
            System.err.println("Master - bootServer - InterruptedERROR while booting: " + interruptedException.getMessage());
            throw new RuntimeException("Master - bootServer - InterruptedERROR: " + interruptedException.getMessage());
        } catch (Exception e) {
            System.err.println("Master - bootServer - ERROR while booting: " + e.getMessage());
            throw new RuntimeException("Master - bootServer - ERROR: " + e.getMessage());
        }
    }

    /* Initializes the default configuration from the serverCFG file */
    private void initDefaults() {
        FileReader reader = null;
        try {
            /* Set configurations */
            reader = new FileReader(System.getProperty("user.dir") + "\\app\\backend\\master\\data\\serverCFG");
            Properties properties = new Properties();
            properties.load(reader);

            workerConnectionPort = Integer.parseInt(properties.getProperty("workerConnectionPort"));
            workerInDataPort = Integer.parseInt(properties.getProperty("workerInDataPort"));

            userGPXPort = Integer.parseInt(properties.getProperty("userGPXPort"));
            userStatisticsPort = Integer.parseInt(properties.getProperty("userStatisticsPort"));
            userSegmentPort = Integer.parseInt(properties.getProperty("userSegmentPort"));
            userSegStatisticsPort = Integer.parseInt(properties.getProperty("userSegStatisticsPort"));

            MIN_WORKERS = Integer.parseInt(properties.getProperty("minWorkers"));
        }catch (IOException ioException) {
            System.err.println("Master - initDefaults - IOERROR while initializing defaults: " + ioException.getMessage());
            throw new RuntimeException("initDefaults - IOERROR: " + ioException.getMessage());
        } catch (Exception e) {
            System.err.println("Master - initDefaults - ERROR while initializing defaults: " + e.getMessage());
            throw new RuntimeException("initDefaults - ERROR: " + e.getMessage());
        } finally {
            try { if (reader != null) reader.close(); } catch (IOException ioException) { System.err.println("initDefaults - IOERROR while closing config file: " + ioException.getMessage()); throw new RuntimeException("initDefaults - ERROR: " + ioException.getMessage());  }
        }

        System.err.println("Master | Initializing Configuration |");
        System.err.println("Master | [VALUE] Min. workers: " + MIN_WORKERS);
        System.err.println("Master | [PORT] Worker Connection: " + workerConnectionPort);
        System.err.println("Master | [PORT] Worker Data: " + workerInDataPort);
        System.err.println("Master | [PORT] User GPX: " + userGPXPort);
        System.err.println("Master | [PORT] User Statistics: " + userStatisticsPort);
        System.err.println("Master | [PORT] User Segment: " + userSegmentPort);
        System.err.println("Master | [PORT] User Segment's Statistics: " + userSegStatisticsPort);
        System.err.println("Master | Initialization complete |");
    }

    /* Sends data to the workers for mapping using round-robin */
    private class Scheduler extends Thread{
        @Override
        public void run(){
            int nextWorker = 0;

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* While there is data to be processed */
                    while (dataForProcessing.size() > 0) {
                        Chunk[] chunks;

                        synchronized (dataForProcessing) {
                            chunks = dataForProcessing.remove(0);
                        }

                        /* Send all chunks belonging to a Chunk[] to workers */
                        for (Chunk c: chunks) {
                            ObjectOutputStream out;

                            synchronized (workerConnectionOuts) {
                                out = workerConnectionOuts.get(nextWorker);
                            }

                            System.out.println("Master - Scheduler - Assigning data to worker: " + nextWorker);

                            out.writeObject(c);
                            out.flush();

                            System.out.println("Master - Scheduler - Data assigned to worker: " + c.toString());

                            /* Calculate next worker using RR */
                            nextWorker = (++nextWorker) % workerConnectionOuts.size();
                        }
                    }
                }
            } catch(IOException ioException) {
                System.err.println("Master - Scheduler - IOERROR: " + ioException.getMessage());
                throw new RuntimeException("Scheduler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - Scheduler - ERROR: " + e.getMessage());
                throw new RuntimeException("Scheduler - ERROR: " + e.getMessage());
            } finally {
                System.err.println("Master - Scheduler - Shutting down...");
            }
        }
    }

    /* Handles the connection between the master and the workers */
    private class WorkerHandler extends Thread {
        private final int port;
        private ServerSocket workerServerSocket;

        WorkerHandler(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try{
                workerServerSocket = new ServerSocket(port, 100);

                if (port == workerConnectionPort)
                    workerConnectionHandler();
                else if (port == workerInDataPort)
                    workerInDataHandler();

            } catch (IOException ioException) {
                System.err.println("Master - WorkerHandler - IOERROR: " + ioException.getMessage());
                throw new RuntimeException("WorkerHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - WorkerHandler - ERROR: " + e.getMessage());
                throw new RuntimeException("WorkerHandler - ERROR: " + e.getMessage());
            } finally {
                try { if (workerServerSocket != null) workerServerSocket.close(); } catch (IOException ioException) { System.err.println("Master - WorkerHandler - IOERROR while closing workerServerSocket: " + ioException.getMessage());  throw new RuntimeException("WorkerHandler - IOERROR while closing workerServerSocket:: " + ioException.getMessage()); }
            }
        }

        /* Handles the outgoing connections to workers - for sending data */
        public void workerConnectionHandler() {
            Socket workerConnectionSocket = null;
            ArrayList<Socket> workerConnectionSockets = new ArrayList<>(); // Used to close all connections when shutting down
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    workerConnectionSocket = workerServerSocket.accept();
                    System.err.println("Master - WorkerHandler - WorkerConnectionHandler: Connection received from " + workerConnectionSocket.getRemoteSocketAddress());

                    workerConnectionSockets.add(workerConnectionSocket);
                    ObjectOutputStream out = new ObjectOutputStream(workerConnectionSocket.getOutputStream());
                    synchronized (workerConnectionOuts) {
                        workerConnectionOuts.add(out);
                        if (workerConnectionOuts.size() >= MIN_WORKERS) workerConnectionOuts.notifyAll();
                    }
                }

            } catch (IOException ioException) {
                System.err.println("Master - WorkerHandler - workerConnectionHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - WorkerHandler - workerConnectionHandler - ERROR: " + e.getMessage());
            } finally {
                for (ObjectOutputStream out: workerConnectionOuts)
                    try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("Master - WorkerHandler - WorkerConnectionHandler - IOERROR while closing a worker's connection output stream: " + ioException.getMessage()); }
                for (Socket socket: workerConnectionSockets)
                    try { if (socket != null) socket.close(); } catch (IOException ioException) { System.err.println("Master - WorkerHandler - WorkerConnectionHandler - IOERROR while closing a worker's workerConnectionSocket: " + ioException.getMessage()); }
                System.err.println("Master - WorkerHandler - workerConnectionHandler - Shutting down...");
            }
        }

        /* Handles the incoming connections from workers - for receiving mapped data */
        public void workerInDataHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket workerInDataSocket = workerServerSocket.accept();
                    System.out.println("Master - WorkerHandler - WorkerInDataHandler: Connection received from " + workerInDataSocket.getRemoteSocketAddress());
                    new ReceiveWorkerInData(workerInDataSocket).start();
                }
            } catch (IOException ioException) {
                System.err.println("Master - WorkerHandler - workerInDataHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - WorkerHandler - workerInDataHandler - ERROR: " + e.getMessage());
            } finally {
                System.err.println("Master - WorkerHandler - workerInDataHandler - Shutting down...");
            }
        }
    }

    /* Handles the connection between the master and the users */
    private class UserHandler extends Thread {
        private final int port;
        private ServerSocket userServerSocket;

        UserHandler(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                userServerSocket = new ServerSocket(port, 1000);

                if (port == userGPXPort)
                    userGPXHandler();
                else if (port == userStatisticsPort)
                    userStatisticsHandler();
                else if (port == userSegmentPort)
                    userSegmentHandler();
                else if (port == userSegStatisticsPort)
                    userSegStatisticsHandler();

            } catch (Exception e) {
                System.err.println("Master - UserHandler - ERROR: " + e.getMessage());
                throw new RuntimeException("UserHandler - ERROR: " + e.getMessage());
            } finally {
                try { if (userServerSocket != null) userServerSocket.close(); } catch (IOException ioException) { System.err.println("Master - UserHandler - IOERROR while closing userServerSocket: " + ioException.getMessage()); throw new RuntimeException("UserHandler - ERROR: " + ioException.getMessage()); }
            }
        }

        /* Accepts multiple GPX processing requests from users and creates a new thread for each one */
        private void userGPXHandler(){
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket userGPXSocket = userServerSocket.accept();
                    System.out.println("Master - UserHandler - UserGPXHandler: Connection received from " + userGPXSocket.getRemoteSocketAddress());

                    /* Handle the request */
                    UserGPXThread userGPXThread = new UserGPXThread(userGPXSocket);
                    userGPXThread.start();
                }
            } catch (IOException ioException) {
                System.err.println("Master - UserHandler - userGPXHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - UserHandler - userGPXHandler - ERROR: " + e.getMessage());
                System.err.println("Master - UserHandler - userGPXHandler - Shutting down...");
            }
        }

        /* Accepts multiple statistics requests from users and creates a new thread for each one */
        private void userStatisticsHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket userStatisticsSocket = userServerSocket.accept();
                    System.out.println("Master - UserHandler - UserStatisticsHandler: Connection received from " + userStatisticsSocket.getRemoteSocketAddress());

                    /* Handle the request */
                    UserStatisticsThread userStatisticsThread = new UserStatisticsThread(userStatisticsSocket);
                    userStatisticsThread.start();
                }
            } catch (IOException ioException) {
                System.err.println("Master - UserHandler - userStatisticsHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - UserHandler - userStatisticsHandler - ERROR: " + e.getMessage());
                System.err.println("Master - UserHandler - userStatisticsHandler - Shutting down...");
            }
        }

        /* */
        private void userSegmentHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket userSegmentSocket = userServerSocket.accept();
                    System.out.println("Master - UserHandler - UserSegmentHandler: Connection received from " + userSegmentSocket.getRemoteSocketAddress());

                    /* Handle the request */
                    UserSegmentThread userSegmentThread = new UserSegmentThread(userSegmentSocket);
                    userSegmentThread.start();
                }
            } catch (IOException ioException) {
                System.err.println("Master - UserHandler - userSegmentHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - UserHandler - userSegmentHandler - ERROR: " + e.getMessage());
                System.err.println("Master - UserHandler - userSegmentHandler - Shutting down...");
            }
        }


        private void userSegStatisticsHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket userSegStatisticsSocket = userServerSocket.accept();
                    System.out.println("Master - UserHandler - UserSegStatisticsHandler: Connection received from " + userSegStatisticsSocket.getRemoteSocketAddress());

                    /* Handle the request */
                    UserSegStatisticsThread userSegStatisticsThread = new UserSegStatisticsThread(userSegStatisticsSocket);
                    userSegStatisticsThread.start();
                }
            } catch (IOException ioException) {
                System.err.println("Master - UserHandler - userSegStatisticsHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - UserHandler - userSegStatisticsHandler - ERROR: " + e.getMessage());
                System.err.println("Master - UserHandler - userSegStatisticsHandler - Shutting down...");
            }
        }

    }

    /* Takes a GPX file from a user and splits it into chunks, which are then sent for mapping, and then reduces it */
    private class UserGPXThread extends Thread {
        private final Socket userGPXSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;
        private Date date;
        private int expectedChunks;
        private ArrayList<Waypoint> waypoints;
        private ArrayList<Segment> segments;
        private final ArrayList<IntermediateChunk> intermediateResults;

        UserGPXThread(Socket userGPXSocket) {
            this.userGPXSocket = userGPXSocket;
            this.out = null;
            this.in = null;
            this.intermediateResults = new ArrayList<>();
            this.expectedChunks = 0;
            this.date = new Date();
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(userGPXSocket.getOutputStream());
                in = new ObjectInputStream(userGPXSocket.getInputStream());

                /* DummyUser registration */
                String userID = (String) in.readObject();

                synchronized (activeGPXUsers) {
                    activeGPXUsers.put(userID, this);
                }

                synchronized (database) {
                    user = database.initUser(userID);
                    segments = database.getSegments();
                }

                /* Take GPX from DummyUser */
                StringBuilder buffer;
                buffer = (StringBuilder) in.readObject();
                System.out.println("Master - UserGPXThread for User #" + userID + " - GPX received.");

                /* Convert the GPX file into a list of Waypoints */
                this.waypoints = GPXParser.parse(buffer);
                this.date = waypoints.get(0).getDate();
                expectedChunks += segments.size();

                /* Find segments */
                new SegmentFinder().start();

                /* Split the waypoints into chunks and send them for Mapping */
                addDataForProcessing(splitData(new ArrayList<>(waypoints)));
                System.out.println("Master - UserGPXThread for DummyUser " + userID + " waiting for data from worker...");

                synchronized (intermediateResults) {
                    while (intermediateResults.size() < expectedChunks) {
                        intermediateResults.wait();
                    }
                }

                /* Get intermediate results */
                ArrayList<IntermediateChunk> returnedSegments = new ArrayList<>();
                ArrayList<IntermediateChunk> returnedChunks  = new ArrayList<>();

                for(IntermediateChunk c: intermediateResults){
                    if(c.getSegmentID() >= 0){
                        returnedSegments.add(c);
                    }else{
                        returnedChunks.add(c);
                    }
                }
                //System.err.println("Size of returnedSeg: "+returnedSegments.size());

                /* Reduce the results */
                IntermediateChunk result = reduce(returnedChunks);
                result.setSegmentID(returnedSegments.size());

                /* Save the result in the database */
                Route route = new Route(user.getStatistics().getSubmissions() + 1, this.date, waypoints, result.getTotalDistance(), result.getTotalTime(), result.getMeanVelocity(), result.getTotalElevation());
                synchronized (database) {
                    database.addRoute(route, user.getID());
                    if (returnedSegments.size() > 0) database.addSegmentResults(returnedSegments, user.getID());
                }

                /* Send the result to the user */
                out.writeObject(result);
                out.flush();
                System.out.println("Master - UserGPXThread for DummyUser " + userID + " sent final result to user.");

            } catch (IOException ioException) {
                System.err.println("Master - UserGPXThread for DummyUser " + user.getID() + " - IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("Master - UserGPXThread for DummyUser " + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - UserGPXThread for DummyUser " + user.getID() + " - ERROR: " + e.getMessage());
            } finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("Master - UserGPXThread for DummyUser " + user.getID() + " - ERROR while closing input stream: " + ioException.getMessage()); }
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("Master - UserGPXThread for DummyUser " + user.getID() + " - ERROR while closing output stream: " + ioException.getMessage()); }
                try { if (userGPXSocket != null) userGPXSocket.close(); } catch (IOException ioException) { System.err.println("Master - UserGPXThread for DummyUser " + user.getID() + " - ERROR while closing userGPXSocket: " + ioException.getMessage()); }
                synchronized (activeGPXUsers){
                    activeGPXUsers.remove(user.getID());
                }
                System.out.println("Master - UserGPXThread for DummyUser " + user.getID() + " shutting down...");
            }
        }

        /* Splits the waypoints into chunks */
        private Chunk[] splitData(ArrayList<Waypoint> waypoints) {
            int numChunks = (int) ((waypoints.size() / 10) + 0.5);
            int chunkSize = waypoints.size() / (numChunks) + 1;
            this.expectedChunks += numChunks;

            Chunk[] chunks = new Chunk[numChunks];

            while (waypoints.size() > 1) {
                ArrayList<Waypoint> chunkWaypoints = new ArrayList<>();

                int waypointsSize = waypoints.size();

                for (int i = 0; i < Math.min(chunkSize, waypointsSize); i++) {
                    chunkWaypoints.add(waypoints.get(0));
                    waypoints.remove(0);
                }

                if (!waypoints.isEmpty())
                    chunkWaypoints.add(waypoints.get(0));

                chunks[--numChunks] = new Chunk(user.getID(), -1, chunkWaypoints);
            }

            return chunks;
        }

        private void addDataForProcessing(Chunk[] chunks) {
            synchronized (dataForProcessing) {
                dataForProcessing.add(chunks);
            }
        }

        private void addIntermediateResult(IntermediateChunk s) {
            synchronized (intermediateResults){
                intermediateResults.add(s);
                if (intermediateResults.size() == expectedChunks)
                    intermediateResults.notifyAll();
            }
        }

        /* Reduces the intermediate results */
        private IntermediateChunk reduce(ArrayList<IntermediateChunk> returnedChunks) {
            System.out.println("Master - UserGPXThread for DummyUser " + user.getID()  + " reducing data for user...");

            double totalDistance = 0;
            double totalElevation = 0;
            long totalTime = 0;

            for (IntermediateChunk intermediateChunk: returnedChunks) {
                totalDistance += intermediateChunk.getTotalDistance();    //km
                totalElevation += intermediateChunk.getTotalElevation();  //m
                totalTime += intermediateChunk.getTotalTime();            //ms
            }


            return new IntermediateChunk(user.getID(), 0 ,totalDistance, totalDistance / totalTime, totalElevation, totalTime, this.date);
        }

        private class SegmentFinder extends Thread {
            @Override
            public void run() {
                try{
                    ArrayList<Chunk> chunks = new ArrayList<>();
                    int segmentsNotFound = 0;

                    for (int i = 0; i < segments.size(); i++){
                        ArrayList<Waypoint> segment = segments.get(i).getWaypoints();

                        int segmentStartIndex = indexOfSubList(waypoints, segment);
                        if (segmentStartIndex != -1) {
                            ArrayList<Waypoint> foundSegment = new ArrayList<>(waypoints.subList(segmentStartIndex, segmentStartIndex + segment.size()));
                            if (foundSegment.equals(segment)) {
                                System.err.println("Master - UserGPXThread" + " for DummyUser " + user.getID() + " found segment #" + i + " in user's GPX.");
                                chunks.add(new Chunk(user.getID(), i, foundSegment));
                            }else {
                                segmentsNotFound += 1;
                            }
                        } else {
                            segmentsNotFound += 1;
                        }
                    }

                    Chunk[] foundSegments = new Chunk[chunks.size()];
                    chunks.toArray(foundSegments);
                    addDataForProcessing(foundSegments);
                    expectedChunks -= segmentsNotFound;
                }catch (Exception e){
                    System.err.println("Master - SegmentFinder for DummyUser " + user.getID() + " - ERROR: " + e.getMessage());
                }
            }
        }
    }

    /* Take a request from the user and returns the statistics */
    private class UserStatisticsThread extends Thread{
        private final Socket userStatisticsSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;

        UserStatisticsThread(Socket providerSocket) {
            this.userStatisticsSocket = providerSocket;
            this.out = null;
            this.in = null;
        }

        @Override
        public void run(){
            try {
                out = new ObjectOutputStream(userStatisticsSocket.getOutputStream());
                in = new ObjectInputStream(userStatisticsSocket.getInputStream());

                // DummyUser registration
                String userID = (String) in.readObject();

                Statistics totalData;
                synchronized (database) {
                    user = database.initUser(userID);
                    totalData = database.getTotalData();
                }

                out.writeObject(user.getStatistics()); // Send the user's own statistics
                out.writeObject(totalData);            // Send the total statistics from all users
                out.flush();
            }catch (IOException ioException) {
                    System.err.println("Master - UserStatisticsThread for DummyUser " + user.getID() + " - IOERROR: " + ioException.getMessage());
            }catch (ClassNotFoundException classNotFoundException) {
                    System.err.println("Master - UserStatisticsThread for DummyUser " + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                    System.err.println("Master - UserStatisticsThread for DummyUser " + user.getID() + " - ERROR: " + e.getMessage());
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("Master - UserStatisticsThread for DummyUser " + user.getID() + " - IOERROR while closing input stream: " + ioException.getMessage());}
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("Master - UserStatisticsThread for DummyUser " + user.getID() + " - IOERROR while closing output stream: " + ioException.getMessage());}
                try { if (userStatisticsSocket != null) userStatisticsSocket.close(); } catch (IOException ioException) { System.err.println("Master - UserStatisticsThread for DummyUser " + user.getID() + " - IOERROR while closing userStatisticsSocket: " + ioException.getMessage());}
                System.out.println("Master - UserStatisticsThread for DummyUser " + user.getID() + " shutting down...");
            }
        }
    }

    /* */
    private class UserSegmentThread extends Thread{
        private final Socket userSegmentSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;

        public UserSegmentThread(Socket providerSocket){
            this.userSegmentSocket = providerSocket;
            this.out = null;
            this.in = null;
        }

        @Override
        public void run(){
            try{
                out = new ObjectOutputStream(userSegmentSocket.getOutputStream());
                in = new ObjectInputStream(userSegmentSocket.getInputStream());

                String userID = (String) in.readObject();

                synchronized (database) {
                    user = database.initUser(userID);
                }

                /* Take Segment from User */
                StringBuilder buffer;
                buffer = (StringBuilder) in.readObject();
                System.out.println("Master - UserSegmentThread for User #" + userID + " - Segment received.");

                /* Convert the Segment file into a list of Waypoints */
                ArrayList<Waypoint> waypoints = GPXParser.parse(buffer);


                /* Create and store segment into the Database */
                synchronized (database) {
                    database.initSegment(waypoints, user);
                }
            }catch (IOException ioException) {
                System.err.println("Master - UserSegmentThread for DummyUser " + user.getID() + " - IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("Master - UserSegmentThread for DummyUser " + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("Master - UserSegmentThread for DummyUser " + user.getID() + " - ERROR: " + e.getMessage());
            } finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("Master - UserSegmentThread for DummyUser " + user.getID() + " - ERROR while closing input stream: " + ioException.getMessage()); }
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("Master - UserSegmentThread for DummyUser " + user.getID() + " - ERROR while closing output stream: " + ioException.getMessage()); }
                try { if (userSegmentSocket != null) userSegmentSocket.close(); } catch (IOException ioException) { System.err.println("Master - UserSegmentThread for DummyUser " + user.getID() + " - ERROR while closing userGPXSocket: " + ioException.getMessage()); }
//                synchronized (activeGPXUsers){
//                    activeGPXUsers.remove(user.getID());
//                }
                System.out.println("Master - UserGPXThread for DummyUser " + user.getID() + " shutting down...");
            }
        }
    }

    /* */
    private class UserSegStatisticsThread extends Thread{
        private final Socket userSegStatisticsSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;

        UserSegStatisticsThread(Socket providerSocket) {
            this.userSegStatisticsSocket = providerSocket;
            this.out = null;
            this.in = null;
        }

        @Override
        public void run(){
            try {
                out = new ObjectOutputStream(userSegStatisticsSocket.getOutputStream());
                in = new ObjectInputStream(userSegStatisticsSocket.getInputStream());


                String userID = (String) in.readObject();


                ArrayList<Segment> segments;
                synchronized (database) {
                    user = database.initUser(userID);
                    segments = database.getSegments();
                }

                // user's history of registered segments
                HashMap<Integer, ArrayList<IntermediateChunk>> segmentsStatistics = new HashMap<>(user.getSegmentsStatistics());

                // Get the leaderboard for each segment in the user's history
                ArrayList<HashMap<String, IntermediateChunk>> leaderboardSegments = new ArrayList<>();
                for (Integer segmentID: segmentsStatistics.keySet()) {
                    //leaderboardSegments.add(leaderboards.get(segmentID).getLeaderboard());
                    leaderboardSegments.add(sorter(segments.get(segmentID).getLeaderboard()));
                }


                out.writeObject(leaderboardSegments);
                out.writeObject(segmentsStatistics);
                out.flush();

            }catch (IOException ioException) {
                System.err.println("Master - UserSegStatisticsThread for DummyUser " + user.getID() + " - IOERROR: " + ioException.getMessage());
            }catch (ClassNotFoundException classNotFoundException) {
                System.err.println("Master - UserSegStatisticsThread for DummyUser " + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                System.err.println("Master - UserSegStatisticsThread for DummyUser " + user.getID() + " - ERROR: " + e.getMessage());
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("Master - UserSegStatisticsThread for DummyUser " + user.getID() + " - IOERROR while closing input stream: " + ioException.getMessage());}
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("Master - UserSegStatisticsThread for DummyUser " + user.getID() + " - IOERROR while closing output stream: " + ioException.getMessage());}
                try { if (userSegStatisticsSocket != null) userSegStatisticsSocket.close(); } catch (IOException ioException) { System.err.println("Master - UserSegStatisticsThread for DummyUser " + user.getID() + " - IOERROR while closing userStatisticsSocket: " + ioException.getMessage());}
                System.out.println("Master - UserSegStatisticsThread for DummyUser " + user.getID() + " shutting down...");
            }
        }

        private HashMap<String, IntermediateChunk> sorter(HashMap<String, IntermediateChunk> leaderboard) {
            ArrayList<Map.Entry<String, IntermediateChunk>> listofEntries = new ArrayList<>(leaderboard.entrySet());
            Collections.sort(listofEntries, new Comparator<Map.Entry<String, IntermediateChunk>>() {
                @Override
                public int compare(Map.Entry<String, IntermediateChunk> o1, Map.Entry<String, IntermediateChunk> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });

            HashMap<String, IntermediateChunk> sortedMap = new LinkedHashMap<>(listofEntries.size());

            for (Map.Entry<String, IntermediateChunk> entry : listofEntries) {
                sortedMap.put(entry.getKey(), entry.getValue());
            }

            return sortedMap;
        }
    }

    /* Receives the intermediate results from the workers */
    private class ReceiveWorkerInData extends Thread {
        Socket workerInDataSocket;
        ObjectInputStream in;
        SocketAddress workerInDataAddress;

        public ReceiveWorkerInData(Socket workerInDataSocket){
            this.workerInDataSocket = workerInDataSocket;
            this.workerInDataAddress = workerInDataSocket.getRemoteSocketAddress();
            this.in = null;
        }

        public void addIntermediateResults(IntermediateChunk data){
            synchronized (activeGPXUsers){
                activeGPXUsers.get(data.getUserID()).addIntermediateResult(data);
            }
        }

        @Override
        public void run() {
            try {
                in = new ObjectInputStream(workerInDataSocket.getInputStream());

                IntermediateChunk data = (IntermediateChunk) in.readObject();
                System.out.println("Master - ReceiveWorkerInData port: " + workerInDataAddress + " received data for DummyUser's #" + data.getUserID() + " GPX.");
                addIntermediateResults(data);
            }catch (IOException ioException) {
                System.err.println("Master - ReceiveWorkerInData port: " + workerInDataAddress + " - IOERROR: " + ioException.getMessage());
            }catch (ClassNotFoundException classNotFoundException) {
                System.err.println("Master - ReceiveWorkerInData port: " + workerInDataAddress + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                System.err.println("Master - ReceiveWorkerInData port: " + workerInDataAddress + " - ERROR: " + e.getMessage());
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("Master - ReceiveWorkerInData port: "+ workerInDataAddress + " - IOERROR while closing input stream: " + ioException.getMessage()); }
                try { if (workerInDataSocket != null) workerInDataSocket.close(); } catch (IOException ioException) { System.err.println("Master - ReceiveWorkerInData port: "+ workerInDataAddress + " - IOERROR while closing workerInDataSocket: " + ioException.getMessage()); }
            }
        }
    }

    public static void main(String[] args) {
        Master master = new Master();
        master.bootServer();
    }
}