import javax.net.ssl.HandshakeCompletedEvent;
import java.io.*;
import java.net.*;

public class Master extends Thread{

    static int x = 0;

    ServerSocket userSocket;
    ServerSocket workerSocket;

    Socket providerSocket;

    void openServer() {
        try {
            /* Create Server Socket */
            x++;
            if (x == 1) {
                userSocket = new ServerSocket(4321, 10);

                while (true) {
                    /* Accept the connection */
                    System.out.println("1");
                    providerSocket = userSocket.accept();

                    /* Handle the request */
                    Thread d = new ActionsForUsers(providerSocket);
                    d.start();
                }
            }else if (x==2){
                workerSocket = new ServerSocket(1234, 10);

                while (true) {
                    /* Accept the connection */
                    System.out.println("2");
                    providerSocket = workerSocket.accept();

                    /* Handle the request */
                Thread d = new ActionsForClients(providerSocket);
                d.start();
                }
            }else {
                System.err.println();
            }


        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    @Override
    public void run(){
        try{
            openServer();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }


}

class HandleRequests extends Thread{
    ServerSocket socketToHandle;
    Socket providerSocket;

    HandleRequests(int port){
        this.socketToHandle = new ServerSocket(port, 10);
    }

    public void run(){
        try {
            while (true) {
                /* Accept the connection */

                providerSocket = socketToHandle.accept();

                /* Handle the request */
                Thread d = new ActionsForUsers(providerSocket);
                d.start();
            }
        }
        catch(Exception e){


            }
        }



}