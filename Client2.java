import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client2 extends Thread{
    int a, b;

    Client2(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public static void main(String[] args) {
        new Client2(10, 5).start();
        new Client2(20, 5).start();
    }

    public void run() {
        ObjectOutputStream out= null ;
        ObjectInputStream in = null ;
        Socket requestSocket= null ;


        try {
            String host = "localhost";
            /* Create socket for contacting the server on port 4321*/
            requestSocket = new Socket(host , 1234);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            /* Write the two integers */
            out.writeInt(a);
            out.flush();
            out.writeInt(b);
            out.flush();
            //Test t = new Test (a,b);
            //out.writeObject(t);
            //out.flush();

            //Test t2 = (Test) in.readObject();
            System.out.println("Server to clt2>" + in.readInt());
            /* Print the received result from server */
            //System.out.println("Server>" + t2.getA() + " " + t2.getC());

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
