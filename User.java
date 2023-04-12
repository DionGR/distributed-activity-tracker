import java.io.*;
import java.net.*;

public class User extends Thread{
    int gpx;
    int id;

    User(int id, int gpx){
        this.id = id;
        this.gpx = gpx;
    }

    public void run(){
        ObjectOutputStream out= null ;
        ObjectInputStream in = null ;
        Socket requestSocket = null ;


        try {
            String host = "localhost";
            /* Create socket for contacting the server on port 4321*/
            requestSocket = new Socket(host, 4321);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(requestSocket.getOutputStream());


            /* Write the two integers */
            out.writeObject(gpx);
            out.flush();

            in = new ObjectInputStream(requestSocket.getInputStream());
            Chunk result = (Chunk) in.readObject();

            /* Print the received result from server */
            System.out.println("User #" + this.gpx + " Result:" + result.getData());

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                in.close(); out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        new User(1, 5).start();
        new User(2, 10).start();
//        new User(3, 15).start();
//        new User(4, 20).start();
//        new User(5, 25).start();
    }
}
