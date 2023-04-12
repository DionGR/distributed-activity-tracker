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

    ServerSocket usersSocketToHandle, workersSocketToHandle;

    Master(int userPort, int workerPort){
        this.userPort = userPort;
        this.workerPort = workerPort;
    }

    public void bootServer(){
        try {
            usersSocketToHandle = new ServerSocket(userPort, 5000);
            workersSocketToHandle = new ServerSocket(workerPort, 5000);

            UserHandler userHandler = new UserHandler(usersSocketToHandle);
            WorkerHandler workerHandler = new WorkerHandler(workersSocketToHandle);
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

                        synchronized (dataForProcessing){
                            chunks = dataForProcessing.get(0);
                            dataForProcessing.remove(0);
                        }

                        for (Chunk c: chunks){
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
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private class UserHandler extends Thread {
        private final ServerSocket usersSocketToHandle;

        UserHandler(ServerSocket usersSocketToHandle) {
            this.usersSocketToHandle = usersSocketToHandle;
        }

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class WorkerHandler extends Thread {

        private final ServerSocket workersSocketToHandle;

        WorkerHandler(ServerSocket workersSocketToHandle) {
            this.workersSocketToHandle = workersSocketToHandle;
        }

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
                ioException.printStackTrace();
            }
        }
    }

    private class ReceiveWorkerData extends Thread{
        Socket workerSocket;
        ObjectInputStream in;
        ObjectOutputStream out;

        public ReceiveWorkerData(Socket workerSocket){
            this.workerSocket = workerSocket;
            try{
                this.out = new ObjectOutputStream(workerSocket.getOutputStream());
                this.in = new ObjectInputStream(workerSocket.getInputStream());
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void run(){
            try {
                while (workerSocket.isConnected()) {
                    Chunk data = (Chunk) in.readObject();
                    System.out.println("Received data for user: " + data.getUser());
                    addData(data);
                }
            }catch(Exception e){
                e.printStackTrace();
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
