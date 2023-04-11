import java.net.*;
import java.io.*;

public class UserThread extends Thread{
    Socket providerSocket;
    ObjectOutputStream out;
    ObjectInputStream in;

    UserThread(Socket providerSocket) {
        this.providerSocket = providerSocket;

        try {
            out = new ObjectOutputStream(providerSocket.getOutputStream());
            in = new ObjectInputStream(providerSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(){
        try {
            int message = (int) in.readObject();
            System.out.println("Received: " + message);

            message *= 2; //GPX parsing

            Master.addData(message);
            Master.workerData.put(message, 0);

            System.out.println("Waiting for data from worker");

            while (true){
                if (Master.workerData.get(message) > 0) {
                    out.writeInt(Master.workerData.get(message));
                    out.flush();
                    break;
                }
            }

            System.out.println("Sent data to user");

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}

