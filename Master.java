import java.net.*;
import java.io.*;
import java.util.*;


class Master {
    private int MIN_WORKERS;
    private int userGPXPort, userStatisticsPort, workerConnectionPort, workerInDataPort;

    private final Database database;
    private final ArrayList<Chunk[]> dataForProcessing;
    private final ArrayList<ObjectOutputStream> workerConnectionOuts;
    private final HashMap<Integer, UserGPXThread> activeGPXUsers;


    Master(){
        this.database = new Database();
        this.dataForProcessing = new ArrayList<>();
        this.workerConnectionOuts = new ArrayList<>();
        this.activeGPXUsers = new HashMap<>();
    }


    public void bootServer() {
        try {
            /* Initialize server */
            initDefaults();
            Scheduler scheduler = new Scheduler();
            WorkerHandler workerConnectionHandler = new WorkerHandler(workerConnectionPort);
            WorkerHandler workerDataHandler = new WorkerHandler(workerInDataPort);
            UserHandler userGPXHandler = new UserHandler(userGPXPort);
            UserHandler userStatisticsHandler = new UserHandler(userStatisticsPort);

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
        } catch (InterruptedException interruptedException) {
            System.err.println("Master - bootServer - InterruptedERROR while booting: " + interruptedException.getMessage());
        } catch (Exception e) {
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
            workerInDataPort = Integer.parseInt(properties.getProperty("workerInDataPort"));

            userGPXPort = Integer.parseInt(properties.getProperty("userGPXPort"));
            userStatisticsPort = Integer.parseInt(properties.getProperty("userStatisticsPort"));

            MIN_WORKERS = Integer.parseInt(properties.getProperty("minWorkers"));
        }catch (IOException ioException) {
            System.err.println("Master - initDefaults - IOERROR while initializing defaults: " + ioException.getMessage());
            throw new RuntimeException("initDefaults - IOERROR: " + ioException.getMessage());
        } catch (Exception e) {
            System.err.println("Master - initDefaults - ERROR while initializing defaults: " + e.getMessage());
            throw new RuntimeException("initDefaults - ERROR: " + e.getMessage());
        } finally {
            try { if (reader != null) reader.close(); } catch (IOException ioException) { System.err.println("Master - initDefaults - IOERROR while closing config file: " + ioException.getMessage()); throw new RuntimeException("initDefaults - ERROR: " + ioException.getMessage());  }
        }

        System.err.println("Master | Initializing Configuration |");
        System.err.println("Master | [VALUE] Min. workers: " + MIN_WORKERS);
        System.err.println("Master | [PORT] Worker Connection: " + workerConnectionPort);
        System.err.println("Master | [PORT] Worker Data: " + workerInDataPort);
        System.err.println("Master | [PORT] User GPX: " + userGPXPort);
        System.err.println("Master | [PORT] User Statistics: " + userStatisticsPort);
        System.err.println("Master | Initialization complete |");
    }

    private class Scheduler extends Thread{
        @Override
        public void run(){
            int nextWorker = 0;

            try {
                Random r = new Random();
                while (!Thread.currentThread().isInterrupted()) {
                    /* Assign data to workers using round-robin */
//                    synchronized (workerConnectionOuts) {
//                        for (ObjectOutputStream out : workerConnectionOuts) {
//                            if (r.nextDouble() <= 0.99) continue;
//                            try {
//
//                                System.out.println("BEFORE");
//                                out.write(1);
//                                System.out.println("AFTER");
//                                //out.flush();
//                                //out.reset();
//                            } catch (IOException ioException) {
//                                throw new RuntimeException("Scheduler - ERROR: " + ioException.getMessage());
//                            }
//                        }
//                    }
                    while (dataForProcessing.size() > 0) {
                        Chunk[] chunks;

                        synchronized (dataForProcessing) {
                            chunks = dataForProcessing.remove(0);
                        }

                        for (Chunk c: chunks) {
                            ObjectOutputStream out;

                            synchronized (workerConnectionOuts) {
                                out = workerConnectionOuts.get(nextWorker);
                            }

                            System.out.println("Assigning data to worker: " + nextWorker);

                            out.writeObject(c);
                            out.flush();

                            System.out.println("Data assigned to worker: " + c.toString());

                            nextWorker = (++nextWorker) % workerConnectionOuts.size();
                        }
                    }
                }
            } catch(IOException ioException) {
                System.err.println("Scheduler - IOERROR: " + ioException.getMessage());
                throw new RuntimeException("Scheduler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("Scheduler - ERROR: " + e.getMessage());
                throw new RuntimeException("Scheduler - ERROR: " + e.getMessage());
            } finally {
                System.err.println("Scheduler - Shutting down...");
            }
        }
    }

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

                if (workerServerSocket.getLocalPort() == workerConnectionPort)
                    workerConnectionHandler();
                else if (workerServerSocket.getLocalPort() == workerInDataPort)
                    workerInDataHandler();

            } catch (IOException ioException) {
                System.err.println("WorkerHandler IOERROR: " + ioException.getMessage());
                throw new RuntimeException("WorkerHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("WorkerHandler ERROR: " + e.getMessage());
                throw new RuntimeException("WorkerHandler ERROR: " + e.getMessage());
            } finally {
                try { if (workerServerSocket != null) workerServerSocket.close(); } catch (IOException ioException) { System.err.println("WorkerHandler - IOERROR while closing workerServerSocket: " + ioException.getMessage());  throw new RuntimeException("WorkerHandler IOERROR while closing workerServerSocket:: " + ioException.getMessage()); }
                System.err.println("WorkerHandler - Shutting down...");
            }
        }

        public void workerConnectionHandler() {
            Socket workerConnectionSocket = null;
            ArrayList<Socket> workerConnectionSockets = new ArrayList<>();
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    workerConnectionSocket = workerServerSocket.accept();
                    System.out.println("WorkerConnectionHandler: Connection received from " + workerConnectionSocket.getRemoteSocketAddress());

                    workerConnectionSockets.add(workerConnectionSocket);
                    ObjectOutputStream out = new ObjectOutputStream(workerConnectionSocket.getOutputStream());
                    synchronized (workerConnectionOuts) {
                        workerConnectionOuts.add(out);
                        if (workerConnectionOuts.size() >= MIN_WORKERS) workerConnectionOuts.notifyAll();
                    }
                }

            } catch (IOException ioException) {
                System.err.println("WorkerHandler - workerConnectionHandler - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("WorkerHandler - workerConnectionHandler - ERROR: " + e.getMessage());
            } finally {
                for (ObjectOutputStream out: workerConnectionOuts)
                    try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("WorkerConnectionHandler - IOERROR while closing a worker's connection output stream: " + ioException.getMessage()); }
                for (Socket socket: workerConnectionSockets)
                    try { if (socket != null) socket.close(); } catch (IOException ioException) { System.err.println("WorkerConnectionHandler - IOERROR while closing a worker's workerConnectionSocket: " + ioException.getMessage()); }
            }
        }
        public void workerInDataHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket workerInDataSocket = workerServerSocket.accept();
                    System.out.println("WorkerDataHandler: Connection received from " + workerInDataSocket.getRemoteSocketAddress());
                    new Worker(workerInDataSocket).start();
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
        private ServerSocket userServerSocket;

        UserHandler(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                userServerSocket = new ServerSocket(port, 1000);

                if (userServerSocket.getLocalPort() == userGPXPort)
                    userGPXHandler();
                else if (userServerSocket.getLocalPort() == userStatisticsPort)
                    userStatisticsHandler();

            } catch (Exception e) {
                System.err.println("UserHandler - ERROR: " + e.getMessage());
                throw new RuntimeException("UserHandler - ERROR: " + e.getMessage());
            } finally {
                try { if (userServerSocket != null) userServerSocket.close(); } catch (IOException ioException) { System.err.println("UserHandler - IOERROR while closing userServerSocket: " + ioException.getMessage()); throw new RuntimeException("UserHandler - ERROR: " + ioException.getMessage()); }
                System.err.println("UserHandler - Shutting down...");
            }
        }

        private void userGPXHandler(){
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket userGPXSocket = userServerSocket.accept();
                    System.out.println("UserGPXHandler: Connection received from " + userGPXSocket.getRemoteSocketAddress());

                    /* Handle the request */
                    UserGPXThread broker = new UserGPXThread(userGPXSocket);
                    broker.start();
                }
            } catch (IOException ioException) {
                System.err.println("UserHandler - userGPXHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("UserHandler - userGPXHandler ERROR: " + e.getMessage());
            }
        }
        private void userStatisticsHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket userStatisticsSocket = userServerSocket.accept();
                    System.out.println("UserStatisticsHandler: Connection received from " + userStatisticsSocket.getRemoteSocketAddress());

                    /* Handle the request */
                    UserStatisticsThread broker = new UserStatisticsThread(userStatisticsSocket);
                    broker.start();
                }
            } catch (IOException ioException) {
                System.err.println("UserHandler - userStatisticsHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("UserHandler - userStatisticsHandler ERROR: " + e.getMessage());
            }
        }
    }

    private class UserGPXThread extends Thread {
        private final Socket userGPXSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User user;
        private Date date;
        private int totalChunks;
        private final ArrayList<IntermediateChunk> intermediateResults;

        UserGPXThread(Socket userGPXSocket) {
            this.userGPXSocket = userGPXSocket;
            this.out = null;
            this.in = null;
            this.intermediateResults = new ArrayList<>();
            this.totalChunks = 0;
            this.date = new Date();
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(userGPXSocket.getOutputStream());
                in = new ObjectInputStream(userGPXSocket.getInputStream());

                /* DummyUser registration */
                int userID = (int) in.readObject();

                synchronized (activeGPXUsers) {
                    activeGPXUsers.put(userID, this);
                }

                synchronized (database) {
                    user = database.initUser(userID);
                }


                /* Take GPX from DummyUser */
                StringBuilder buffer;
                buffer = (StringBuilder) in.readObject();

                System.out.println("UserGPXThread for User #" + userID + " - GPX received.");


                /* Convert the GPX file into a list of Waypoints */
                ArrayList<Waypoint> waypoints = GPXParser.parse(buffer);
                this.date = waypoints.get(0).getDate();

                /* Split the waypoints into chunks and send them for Mapping */
                addDataForProcessing(splitData(waypoints));

                System.out.println("UserGPXThread for DummyUser #" + userID + " waiting for data from worker...");

                synchronized (intermediateResults) {
                    while (intermediateResults.size() < totalChunks) {
                        intermediateResults.wait();
                    }
                }

                /* Reduce the results */
                IntermediateChunk result = reduce();

                Route route = new Route(user.getStatistics().getSubmissions() + 1, this.date, waypoints, result.getTotalDistance(), result.getTotalTime(), result.getMeanVelocity(), result.getTotalElevation());

                synchronized (database) {
                    database.addRoute(route, user.getID());
                }

                out.writeObject(result);
                out.flush();

                System.out.println("UserGPXThread for DummyUser #" + userID + " sent final result to user.");
            } catch (IOException ioException) {
                System.err.println("UserGPXThread for DummyUser #" + user.getID() + " - IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("UserGPXThread for DummyUser #" + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("UserGPXThread for DummyUser #" + user.getID() + " - ERROR: " + e.getMessage());
            } finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("UserGPXThread for DummyUser #" + user.getID() + " - ERROR while closing input stream: " + ioException.getMessage()); }
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("UserGPXThread for DummyUser #" + user.getID() + " - ERROR while closing output stream: " + ioException.getMessage()); }
                try { if (userGPXSocket != null) userGPXSocket.close(); } catch (IOException ioException) { System.err.println("UserGPXThread for DummyUser #" + user.getID() + " - ERROR while closing userGPXSocket: " + ioException.getMessage()); }
                synchronized (activeGPXUsers){
                    activeGPXUsers.remove(user.getID());
                }
                System.out.println("UserGPXThread for DummyUser #" + user.getID() + " shutting down...");
            }
        }

        private Chunk[] splitData(ArrayList<Waypoint> waypoints) {
            /* Split waypoints into chunks */
            int numChunks = (int) ((waypoints.size() / 10) + 0.5);
            int chunkSize = waypoints.size() / (numChunks) + 1;
            this.totalChunks = numChunks;

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

                chunks[--numChunks] = new Chunk(user.getID(), numChunks, chunks.length, chunkWaypoints);
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
                if (intermediateResults.size() == totalChunks)
                    intermediateResults.notifyAll();
            }
        }

        private IntermediateChunk reduce() {
            System.out.println("UserGPXThread for DummyUser #" + user.getID()  + " reducing data for user...");

            double totalDistance = 0;
            double totalElevation = 0;
            long totalTime = 0;

            for (IntermediateChunk intermediateChunk: intermediateResults) {
                totalDistance += intermediateChunk.getTotalDistance();    //km
                totalElevation += intermediateChunk.getTotalElevation();  //m
                totalTime += intermediateChunk.getTotalTime();            //ms
            }

            return new IntermediateChunk(user.getID(), intermediateResults.size(), totalDistance, totalDistance / totalTime, totalElevation, totalTime, this.date);
        }
    }

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
                    System.err.println("UserStatisticsThread for DummyUser #" + user.getID() + " - IOERROR: " + ioException.getMessage());
            }catch (ClassNotFoundException classNotFoundException) {
                    System.err.println("UserStatisticsThread for DummyUser #" + user.getID() + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                    System.err.println("UserStatisticsThread for DummyUser #" + user.getID() + " - ERROR: " + e.getMessage());
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("UserStatisticsThread for DummyUser #" + user.getID() + " - IOERROR while closing input stream: " + ioException.getMessage());}
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("UserStatisticsThread for DummyUser #" + user.getID() + " - IOERROR while closing output stream: " + ioException.getMessage());}
                try { if (userStatisticsSocket != null) userStatisticsSocket.close(); } catch (IOException ioException) { System.err.println("UserStatisticsThread for DummyUser #" + user.getID() + " - IOERROR while closing userStatisticsSocket: " + ioException.getMessage());}
                System.out.println("UserStatisticsThread for DummyUser #" + user.getID() + " shutting down...");
            }
        }
    }

    private class Worker extends Thread {
        Socket workerInDataSocket;
        ObjectInputStream in;
        SocketAddress workerInDataAddress;


        public Worker(Socket workerInDataSocket){
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
                System.out.println("Worker port: " + workerInDataAddress + " received data for DummyUser's #" + data.getUserID() + " GPX.");
                addIntermediateResults(data);
            }catch (IOException ioException) {
                System.err.println("Worker port: " + workerInDataAddress + " - IOERROR: " + ioException.getMessage());
            }catch (ClassNotFoundException classNotFoundException) {
                System.err.println("Worker port: " + workerInDataAddress + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                System.err.println("Worker port: " + workerInDataAddress + " - ERROR: " + e.getMessage());
                e.printStackTrace();
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("Worker port: "+ workerInDataAddress + " - IOERROR while closing input stream: " + ioException.getMessage()); }
                try { if (workerInDataSocket != null) workerInDataSocket.close(); } catch (IOException ioException) { System.err.println("Worker port: "+ workerInDataAddress + " - IOERROR while closing workerInDataSocket: " + ioException.getMessage()); }
            }
        }
    }



    public static void main(String[] args) {
        Master master = new Master();
        master.bootServer();
    }
}