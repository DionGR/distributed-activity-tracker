import java.io.*;
import java.net.*;

import javax.management.RuntimeErrorException;


public class Client1 extends Thread {
    int a, b;

    Client1(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public static void main(String[] args) {
        new Client1(10, 5).start();
        new Client1(20, 5).start();
    }


    public void run() {
        ObjectOutputStream out= null ;
        ObjectInputStream in = null ;
        Socket requestSocket= null ;


        try {
            String host = "localhost";
            /* Create socket for contacting the server on port 4321*/
            requestSocket = new Socket(host, 4321);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            /* Write the two integers */
//            Test t = new Test(a,b);
//            out.writeObject(t);
//            out.flush();

             out.writeInt(a);
             out.flush();
             out.writeInt(b);
             out.flush();

//            Test t2 = (Test) in.readObject();
                System.out.println("Server to clt1>" + in.readInt());
//            System.out.println("System>" + t2.getA() + " " + t2.getB());
            /* Print the received result from server */

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                in.close(); out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}