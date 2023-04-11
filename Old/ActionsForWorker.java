package Old;

import java.net.*;
import java.io.*;

public class ActionsForWorker extends Thread{
    Socket providerSocket;
    ObjectOutputStream out;
    ObjectInputStream in;

    ActionsForWorker(Socket providerSocket) {
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

            message = message * 2;

            out.writeObject(message);
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}