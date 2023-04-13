import java.io.*;
import java.net.*;


public class User extends Thread{
    int gpx;
    int id;

    User(int id, int gpx){
        this.id = id;
        this.gpx = gpx;
    }

    @Override
    public void run(){
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        Socket requestSocket = null;

        try {
            String host = "localhost";
            /* Create socket for contacting the server on port 4321*/
            requestSocket = new Socket(host, 4321);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(requestSocket.getOutputStream());


            /* Write the integer */
            out.writeObject(gpx);
            out.flush();

            /* Wait for result */
            in = new ObjectInputStream(requestSocket.getInputStream());
            Chunk result = (Chunk) in.readObject();

            /* Print the received result from server */
            System.out.println("User #" + this.id + " Result: " + result.getData());

        } catch (UnknownHostException unknownHostException) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            System.err.println("User #" + this.id + " - IOERROR: " + ioException.getMessage());
        } catch (ClassNotFoundException classNotFoundException) {
            System.err.println("User #" + this.id + " - CASTERROR: " + classNotFoundException.getMessage());
        } catch (Exception e) {
            System.err.println("User #" + this.id + " - ERROR: " + e.getMessage());
        } finally {
            try {
                out.close(); in.close();
                requestSocket.close();
                System.out.println("User #" + this.id + " shutting down...");
            } catch (IOException ioException) {
                System.err.println("User #" + this.id + " - IOERROR while shutting down: " + ioException.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 5; i++) {
            new User(i, i).start();
        }
    }
}
