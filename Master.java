import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


class Master{
    int userPort;
    ServerSocket socketToHandle;


    Master(int userPort){
        this.userPort = userPort;
    }

    public void openServer(){
        try {

            ServerSocket socketToHandle = new ServerSocket(userPort, 10);

            while (true) {
                /* Accept the connection */
                Socket providerSocket = socketToHandle.accept();

                /* Handle the request */
                Thread d = new MasterThread(providerSocket);
                d.start();
            }
        }
        catch(Exception ignored){

            }
        }

    public static void main(String[] args) {
    }
}

class MasterThread extends Thread{
    Socket providerSocket;
    ObjectOutputStream out;
    ObjectInputStream in;

    MasterThread(Socket providerSocket){
        this.providerSocket = providerSocket;
        try {
            this.out = new ObjectOutputStream(this.providerSocket.getOutputStream());
            this.in = new ObjectInputStream(this.providerSocket.getInputStream());
        } catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    public void run(){
        try {
            GPXParser gpx = new GPXParser(in.readObject());
            ArrayList<Object> result = gpx.parse();






        }catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally{
            try {
                in.close(); out.close();
                this.providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}