import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
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

    protected static ArrayList<UserThread> connectedUsers = new ArrayList<>();
    protected static ArrayList<ReceiveWorkerData> connectedWorkers = new ArrayList<>();

    protected volatile static ArrayList<Chunk[]> dataForProcessing = new ArrayList<>();
    protected static HashMap<Long, Chunk[]> intermediateResults = new HashMap<>();

    ServerSocket usersSocketToHandle, workersSocketToHandle;

    Master(int userPort, int workerPort){
        this.userPort = userPort;
        this.workerPort = workerPort;
    }

    public void bootServer(){
        try {
            usersSocketToHandle = new ServerSocket(userPort, 10);
            workersSocketToHandle = new ServerSocket(workerPort, 10);

            UserHandler userHandler = new UserHandler(usersSocketToHandle);
            WorkerHandler workerHandler = new WorkerHandler(workersSocketToHandle);
            AssignData assignData = new AssignData();
            ReceiveData receiveData = new ReceiveData();


            userHandler.start();
            workerHandler.start();
            assignData.start();
            receiveData.start();

            while (true){
                continue;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void addData(Chunk[] chunks){
        Master.dataForProcessing.add(chunks);
    }

    private class AssignData extends Thread{

        @Override
        public void run(){

            int nextWorker = 0;

            try {
                while (true) {
                    /* Assign data to workers using round-robin */
                    while (dataForProcessing.size() > 0) {
                        for (Chunk c: dataForProcessing.get(0)){
                            System.out.println("Assigning data to worker");

                            ObjectOutputStream out = new ObjectOutputStream(connectedWorkers.get(nextWorker).getSocket().getOutputStream());

                            out.writeObject(c);
                            out.flush();

                            System.out.println("Data assigned to worker: " + c.toString());

                            nextWorker = (++nextWorker) % connectedWorkers.size();
                        }
                        dataForProcessing.remove(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

//    private class ReceiveData extends Thread{
//
//        @Override
//        public void run(){
//            try {
//                while (!Thread.currentThread().isInterrupted()) {
//
//                    for (Socket workerSocket : workerSockets) {
//                        ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream());
//                        if (in.available() <= 0) {
//                            continue;
//                        }
//                        int data = (int) in.readObject();
//
//                        //System.out.println("Received from worker: " + data);
//
//                        if (workerData.containsKey(data / 4)) {
//                            workerData.put(data / 4, data);
//                        } else {
//                            workerData.put(data / 4, data);
//                        }
//
//                        System.out.println("Data received from worker: " + workerData.get(data / 4));
//
//                        if (workerData.get(data / 4) > 0) {
//                            System.out.println("Data is ready");
//                        }
//
//                    }
//                }
//            }catch (Exception ignored) {
//
//            }
//
//        }
//    }

    private class UserHandler extends Thread {
        private final ServerSocket usersSocketToHandle;

        UserHandler(ServerSocket usersSocketToHandle) {
            this.usersSocketToHandle = usersSocketToHandle;
        }

        public void run() {
            try {

                while (true) {
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
                while (true) {
                    /* Accept the connection */
                    Socket providerSocket = workersSocketToHandle.accept();
                    System.out.println("WorkerHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    ReceiveWorkerData workerData = new ReceiveWorkerData(providerSocket);
                    connectedWorkers.add(workerData);
                    workerData.start();
                    /* Handle the request */
//                    ActionsForWorker d1 = new ActionsForWorker(providerSocket);
//                    d1.start();
                    /* Send the worker the number of workers */
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }


    private class ReceiveWorkerData extends Thread{
        Socket workerSocket;
        ObjectInputStream in;

        public ReceiveWorkerData(Socket workerSocket){
            this.workerSocket = workerSocket;
        }

        @Override
        public void run(){
            try {
                in = new ObjectInputStream(workerSocket.getInputStream());

                while (true) {
                    Chunk data = (Chunk) in.readObject();
                    addData(data);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }

        public synchronized void addData(Chunk data){
            intermediateResults.get(data.getUser())[data.getId() - 1] = data;
        }

        public Socket getSocket(){
            return workerSocket;
        }
    }



    public static void main(String[] args) {
        Master master = new Master(4321, 1234);
        master.bootServer();
    }
}
