import java.net.*;
import java.io.*;
import java.util.*;


class Master {
    private int MIN_WORKERS;
    private int userGPXPort, userStatisticsPort, workerConnectionPort, workerInDataPort;

    private final Database database;
    private final ArrayList<Chunk[]> dataForProcessing;
    private final ArrayList<ObjectOutputStream> workerConnectionOuts;
    private final HashMap<Integer, UserGPXBroker> activeGPXUsers;

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
            workerInDataPort = Integer.parseInt(properties.getProperty("workerDataPort"));

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
                while (!Thread.currentThread().isInterrupted()) {
                    /* Assign data to workers using round-robin */
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
            } catch (Exception e) {
                System.err.println("Scheduler - ERROR: " + e.getMessage());
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
                try { if (workerServerSocket != null) workerServerSocket.close(); } catch (IOException ioException) { System.err.println("WorkerHandler - IOERROR while closing master's worker serverSocket: " + ioException.getMessage()); }
                System.err.println("WorkerHandler - Shutting down...");
            }
        }

        public void workerConnectionHandler() {
            Socket providerSocket = null;
            ArrayList<Socket> workerConnectionSockets = new ArrayList<>();
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    providerSocket = workerServerSocket.accept();
                    System.out.println("WorkerConnectionHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    workerConnectionSockets.add(providerSocket);
                    ObjectOutputStream out = new ObjectOutputStream(providerSocket.getOutputStream());
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
                    try { if (socket != null) socket.close(); } catch (IOException ioException) { System.err.println("WorkerConnectionHandler - IOERROR while closing a worker's connection socket: " + ioException.getMessage()); }
            }
        }

        public void workerInDataHandler() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket providerSocket = workerServerSocket.accept();
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

            /* TODO: Handle thread crash, user disconnects etc */
            } catch (Exception e) {
                System.err.println("UserHandler - ERROR: " + e.getMessage());
                throw new RuntimeException("UserHandler - ERROR: " + e.getMessage());
            } finally {
                try { if (userServerSocket != null) userServerSocket.close(); } catch (IOException ioException) { System.err.println("UserHandler - IOERROR while closing master's user serverSocket: " + ioException.getMessage()); }
                System.err.println("UserHandler - Shutting down...");
            }
        }

        private void userGPXHandler(){
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    /* Accept the connection */
                    Socket providerSocket = userServerSocket.accept();
                    System.out.println("UserGPXHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    /* Handle the request */
                    UserGPXBroker broker = new UserGPXBroker(providerSocket);
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
                    Socket providerSocket = userServerSocket.accept();
                    System.out.println("UserStatisticsHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    /* Handle the request */
                    UserStatisticsBroker broker = new UserStatisticsBroker(providerSocket);
                    broker.start();
                }
            } catch (IOException ioException) {
                System.err.println("UserHandler - userStatisticsHandler IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("UserHandler - userStatisticsHandler ERROR: " + e.getMessage());
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

                    chunks[--numChunks] = new Chunk(userID, numChunks, totalChunks, chunkWaypoints);
                }

                addDataForProcessing(chunks);

                System.out.println("UserGPXBroker " + this.getId() + " for DummyUser #" + userID + " waiting for data from worker...");

                synchronized (intermediateResults) {
                    while (intermediateResults.size() < totalChunks) {
                        intermediateResults.wait();
                    }
                }

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
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - ERROR while closing input stream: " + ioException.getMessage()); }
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - ERROR while closing output stream: " + ioException.getMessage()); }
                try { if (providerSocket != null) providerSocket.close(); } catch (IOException ioException) { System.err.println("UserGPXBroker for DummyUser #" + user.getID() + " - ERROR while closing providerSocket: " + ioException.getMessage()); }
                synchronized (activeGPXUsers){
                    activeGPXUsers.remove(user.getID());
                }
                System.out.println("UserGPXBroker for DummyUser #" + user.getID() + " shutting down...");
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
                totalDistance += intermediateChunk.getTotalDistance();    //km
                totalElevation += intermediateChunk.getTotalElevation();  //m
                totalTime += intermediateChunk.getTotalTime();            //ms
            }

            return new IntermediateChunk(localGPXID, 0, intermediateResults.size(), totalDistance, totalDistance / totalTime, totalElevation, totalTime);
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
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("UserStatisticsBroker for DummyUser #" + user.getID() + " - IOERROR while closing input stream: " + ioException.getMessage());}
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("UserStatisticsBroker for DummyUser #" + user.getID() + " - IOERROR while closing output stream: " + ioException.getMessage());}
                try { if (providerSocket != null) providerSocket.close(); } catch (IOException ioException) { System.err.println("UserStatisticsBroker for DummyUser #" + user.getID() + " - IOERROR while closing providerSocket: " + ioException.getMessage());}
                System.out.println("UserStatisticsBroker for DummyUser #" + user.getID() + " shutting down...");
            }
        }
    }

    private class Worker extends Thread {
        Socket workerSocket;
        ObjectInputStream in;
        Inet4Address workerAddress;

        // TODO: What should we keep??????????????
        public Worker(Socket workerSocket){
            this.workerSocket = workerSocket;
            this.workerAddress = (Inet4Address) workerSocket.getInetAddress();
            this.in = null;
        }

        public void addIntermediateResults(IntermediateChunk data){
            synchronized (activeGPXUsers){
                activeGPXUsers.get(data.getGPXID()).addIntermediateResult(data);
            }
        }

        @Override
        public void run() {
            try {
                in = new ObjectInputStream(workerSocket.getInputStream());

                IntermediateChunk data = (IntermediateChunk) in.readObject();
                System.out.println("Worker port: " + workerAddress.getAddress() + " received data for GPX: " + data.getGPXID());
                addIntermediateResults(data);
            }catch (IOException ioException) {
                System.err.println("Worker port: " + workerAddress.getAddress() + " - IOERROR: " + ioException.getMessage());
            }catch (ClassNotFoundException classNotFoundException) {
                System.err.println("Worker port: " + workerAddress.getAddress() + " - CASTERROR: " + classNotFoundException.getMessage());
            }catch (Exception e) {
                System.err.println("Worker port: " + workerAddress.getAddress() + " - ERROR: " + e.getMessage());
                e.printStackTrace();
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("Worker port: "+ workerAddress.getAddress() + " - IOERROR while closing input stream: " + ioException.getMessage()); }
                try { if (workerSocket != null) workerSocket.close(); } catch (IOException ioException) { System.err.println("Worker port: "+ workerAddress.getAddress() + " - IOERROR while closing worker's socket: " + ioException.getMessage()); }
            }
        }

        public Socket getSocket(){
            return workerSocket;
        }

        public Inet4Address getWorkerPort(){
            return workerAddress;
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