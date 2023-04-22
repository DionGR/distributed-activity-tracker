import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;



public class DummyUser extends Thread{
    int id;
    String host;
    int serverPort;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    Socket requestSocket = null;
    String userPath;


    DummyUser(int id, String host, int serverPort){
        this.id = id;
        this.host = host;
        this.serverPort = serverPort;
        this.userPath = System.getProperty("user.dir") + "\\data\\user-data\\user" + id + "\\";
    }

    @Override
    public void run() {
        try {
            try {
                new File(userPath + "unprocessed\\").mkdir();
            } catch (Exception e) {throw new Exception("DummyUser #" + id +" could not create \"unprocessed\" folder!"); }

            for (File f: Objects.requireNonNull(new File(userPath + "processed\\").listFiles())) {
                Files.move(Path.of(userPath + "processed\\" + f.getName()), Path.of(userPath + "unprocessed\\" + f.getName()));
            }

            Files.deleteIfExists(Path.of(userPath + "results.txt"));


            /* Create socket for contacting the server on port 54321 */
            requestSocket = new Socket(host, serverPort);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            /* Send the id of the user */
            out.writeObject(id);
            out.flush();

            /* Wait for ACK */
            int ack = (int) in.readObject();
            if (ack != 1){
                throw new Exception("Master did not acknowledge connection");
            }


            /* Find all GPX files in user folder */
            File[] files = new File(userPath + "unprocessed\\").listFiles();
            if (files == null) throw new Exception("No files to process");
            while(files.length != 0){

                for (File f: files) {
                    String fileName = f.getName();
                    Files.move(Path.of(userPath + "unprocessed\\" + fileName), Path.of(userPath + "processed\\" + fileName));

                    File gpxFile = new File(userPath + "processed\\" + fileName);

                    BufferedReader br = new BufferedReader(new FileReader(gpxFile));

                    String line;
                    StringBuilder buffer = new StringBuilder();

                    int routeID = Integer.parseInt(gpxFile.getName().replaceAll("[\\D]", ""));
                    buffer.append(routeID + "!");

                    while((line = br.readLine()) != null) {
                        buffer.append(line);
                    }

                    /* Write gpx */
                    out.writeObject(buffer);
                    out.flush();
                    Segment result = (Segment) in.readObject();


                    /* Write results to file */
                    File f2 = new File(userPath + "\\results.txt");
                    //f2.createNewFile();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(f2, true));
                    bw.append(result.toString()).append("\n");
                    bw.close();

                    /* Print the received result from server */
                    System.out.println("DummyUser #" + id + " received result for " + result);
                }
                files = new File(userPath + "unprocessed\\").listFiles();
                if (files == null) break;
            }

            System.out.println("DummyUser #" + this.id + " finished processing all files.");

            Files.deleteIfExists(Path.of(userPath + "unprocessed\\"));

        } catch (UnknownHostException unknownHostException) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            System.err.println("DummyUser #" + this.id + " - IOERROR: " + ioException.getMessage());
        } catch (ClassNotFoundException classNotFoundException) {
            System.err.println("DummyUser #" + this.id + " - CASTERROR: " + classNotFoundException.getMessage());
        } catch (Exception e) {
            System.err.println("DummyUser #" + this.id + " - ERROR: " + e.getMessage());
        } finally {
            try {
                out.close(); in.close();
                requestSocket.close();
                System.out.println("DummyUser #" + this.id + " shutting down...");
            } catch (IOException ioException) {
                System.err.println("DummyUser #" + this.id + " - IOERROR while shutting down: " + ioException.getMessage());
            }
        }
    }


    public static void main(String[] args) {
        String host = "localhost";
        int serverPort = 54321;

        // Count time
        long startTime = System.currentTimeMillis();

        // Create 12 dummyUsers and wait for them to finish
        DummyUser[] dummyUsers = new DummyUser[3];
        for (int i = 1; i <= 3; i++) {
            dummyUsers[i-1] = new DummyUser(i, host, serverPort);
            dummyUsers[i-1].start();
        }
        for (int i = 1; i <= 3; i++) {
            try {
                dummyUsers[i-1].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.currentTimeMillis();
        System.err.println("Time: " + (double)(endTime - startTime)/1000 + "s");
    }
}
