import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


class Chunk implements Serializable{
    private final long user;
    private final int data;
    private final int id;

    Chunk(long user, int data, int id){
        this.user = user;
        this.data = data;
        this.id = id;
    }

    public long getUser() {
        return user;
    }

    public int getData() {
        return data;
    }

    public int getId() {
        return id;
    }

    public String toString(){
        return "Chunk: " + data;
    }
}

class Master {
    private int workers;
    private int userPort, workerPort;

    protected ArrayList<UserThread> connectedUsers = new ArrayList<>();
    protected ArrayList<ReceiveWorkerData> connectedWorkers = new ArrayList<>();

    protected static ArrayList<Chunk[]> dataForProcessing = new ArrayList<>();
    protected static HashMap<Long, ArrayList<Chunk>> intermediateResults = new HashMap<>();

    Master(int userPort, int workerPort){
        this.userPort = userPort;
        this.workerPort = workerPort;
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

    public synchronized static void addData(Chunk[] chunks){
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
                    Chunk data = (Chunk) in.readObject();
                    System.out.println("Received data for user: " + data.getUser());
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


        public synchronized void addData(Chunk data){
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
