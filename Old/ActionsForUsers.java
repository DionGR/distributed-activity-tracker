package Old;

import java.io.*;
import java.net.*;

public class ActionsForUsers extends Thread {
    ObjectInputStream in;
    ObjectOutputStream out;

    int gpx, c1, c2;
//    public int getResult() { return a+b; }

    public ActionsForUsers(Socket connection) {
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
//            Test t = (Test) in.readObject();
//            t.setA(t.getB()+t.getA());
//            t.setflag(true);
//            out.writeObject(t);
//            out.flush();
             gpx = in.readInt();
             c1 = gpx/2;
             c2 = gpx;
//             b = in.readInt();
//             out.writeInt(a + b);
//             out.flush(); // sends the data

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
