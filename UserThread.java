import java.net.*;
import java.io.*;

public class UserThread extends Thread{

    transient Socket providerSocket;
    private final String ip;
    private final int port;

    UserThread(Socket providerSocket) {
        this.providerSocket = providerSocket;
        this.ip = providerSocket.getInetAddress().getHostAddress();
        this.port = providerSocket.getPort();
    }

    public void run(){
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            out = new ObjectOutputStream(providerSocket.getOutputStream());
            in = new ObjectInputStream(providerSocket.getInputStream());
            int message = (int) in.readObject(); // Takes GPX
            System.out.println("Received: " + message);

            Chunk c1 = new Chunk(this.getId(), message*1000, 1);
            Chunk c2 = new Chunk(this.getId(), message*2000, 2);

            Chunk[] chunks = {c1, c2};

            int numChunks = chunks.length;

            Master.intermediateResults.put(this.getId(), new Chunk[numChunks]);
            Master.addData(chunks);



            System.out.println("Waiting for data from worker...");

            // Reduce
            System.out.println("Reducing data for user...");
            while (true){
                int sum = 0;

                // if there are no null values in the array, then we can reduce

                if (Master.intermediateResults.get(this.getId())[0] != null && Master.intermediateResults.get(this.getId())[1] != null) {
                    //System.out.println("Reducing chunk " + 1);
                    sum += Master.intermediateResults.get(this.getId())[0].getData();
                    sum += Master.intermediateResults.get(this.getId())[1].getData();
                }

//                for (int i = 0; i < numChunks; i++) {
//
//                }

                Chunk finalResult = new Chunk(this.getId(), sum, -1);

                out.writeObject(finalResult);
                out.flush();
                break;
            }

            System.out.println("Sent final result to user.");

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                in.close();
                out.close();
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }


    }
}

