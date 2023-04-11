import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


class Master {
    private int workers;
    private int userPort, workerPort;
    protected static ArrayList<Socket> workerSockets = new ArrayList<>();
    protected static ArrayList<Integer> data = new ArrayList<>();
    protected static HashMap<Integer, Integer> workerData = new HashMap<>();

    Master(int userPort, int workerPort){
        this.userPort = userPort;
        this.workerPort = workerPort;
    }

    public void bootServer(){
        try {
            ServerSocket usersSocketToHandle = new ServerSocket(userPort, 10);
            ServerSocket workersSocketToHandle = new ServerSocket(workerPort, 10);

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

    public static void addData(int data){
        Master.data.add(data);
    }

    private class AssignData extends Thread{

        @Override
        public void run(){

            int nextWorker = 1;

            try {
                while (true) {
                    /* Assign data to workers using round-robin */
                    while (data.size() > 0) {
                        System.out.println("Assigning data to worker");
                        ObjectOutputStream out = new ObjectOutputStream(workerSockets.get(nextWorker).getOutputStream());
                        System.out.println("Assigned: " + data.get(0));
                        out.writeObject(data.remove(0));
                        out.flush();

                        System.out.println("Data assigned");

                        nextWorker = (++nextWorker) % workerSockets.size();
                    }
                }
            } catch (Exception ignored) {

            }

        }
    }

    private class ReceiveData extends Thread{

        @Override
        public void run(){
            try {
                while (true){
                    for (Socket workerSocket: workerSockets){
                        ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream());
                        int data = (int) in.readObject();

                        System.out.println("Received: " + data);

                        if (workerData.containsKey(data/2)){
                            workerData.put(data/2, workerData.get(data/2) + data);
                        }else {
                            workerData.put(data / 2, data);
                        }

                        if (workerData.get(data/2) > 0){
                            System.out.println("Data is ready");
                        }
                    }
                }
            } catch (Exception ignored) {
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

                while (true) {
                    /* Accept the connection */
                    Socket providerSocket = usersSocketToHandle.accept();
                    System.out.println("UserHandler: Connection received from " + providerSocket.getInetAddress().getHostName());

                    /* Handle the request */
                    UserThread d = new UserThread(providerSocket);
                    d.start();
                }
            } catch (Exception ignored) {

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
                    workerSockets.add(providerSocket);

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


    public static void main(String[] args) {
        Master master = new Master(4321, 1234);
        master.bootServer();
    }
}



//    public void openServer() {
//        try {
//
//            ServerSocket socketToHandle = new ServerSocket(userPort, 10);
//
//            while (true) {
//                /* Accept the connection */
//                Socket providerSocket = socketToHandle.accept();
//                System.out.println("Connection received from " + providerSocket.getInetAddress().getHostName());
//
//                /* Handle the request */
//                MasterThread d = new MasterThread(providerSocket);
//                d.start();
//            }
//        } catch (Exception ignored) {
//
//        }
//    }
//private class MasterThread extends Thread{
//    private final Socket providerSocket;
//    private ObjectOutputStream out;
//    private ObjectInputStream in;
//
//    MasterThread(Socket providerSocket){
//        this.providerSocket = providerSocket;
//        try {
//            this.out = new ObjectOutputStream(this.providerSocket.getOutputStream());
//            this.in = new ObjectInputStream(this.providerSocket.getInputStream());
//        } catch (IOException ioException){
//            ioException.printStackTrace();
//        }
//    }
//
//    @Override
//    public void run(){
//        try {
//            GPXParser gpx = new GPXParser(in.readObject());
//            ArrayList<Object> result = gpx.parse();
//
//
//
//
//
//
//        }catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        } finally{
//            try {
//                in.close(); out.close();
//                this.providerSocket.close();
//            } catch (IOException ioException) {
//                ioException.printStackTrace();
//            }
//        }
//    }

