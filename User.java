import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;



public class User extends Thread{
    int id;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    Socket requestSocket = null;
    String userPath;
    Integer counter;


    User(int id){
        this.id = id;
        this.userPath = System.getProperty("user.dir") + "\\data\\user-data\\user" + id + "\\";
    }

    @Override
    public void run() {
        try {
            for (File f: Objects.requireNonNull(new File(userPath + "processed\\").listFiles())) {
                Files.move(Path.of(userPath + "processed\\" + f.getName()), Path.of(userPath + "unprocessed\\" + f.getName()));
            }
            for (File f: Objects.requireNonNull(new File(userPath + "results\\").listFiles())) {
                Files.delete(Path.of(userPath + "results\\" + f.getName()));
            }

            String host = "localhost";
            /* Create socket for contacting the server on port 4321 */
            requestSocket = new Socket(host, 54321);

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
            while(files != null){

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
                    File f2 = new File(userPath + "\\results\\result" + result.getId() + ".txt");
                    f2.createNewFile();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(f2));
                    bw.write(result.toString());
                    bw.close();

                    /* Print the received result from server */
                    System.out.println("User #" + id + " received result for " + result);
                }
                files = new File(userPath + "unprocessed\\").listFiles();
            }

            System.out.println("User #" + this.id + " finished processing all files.");

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
        for (int i = 1; i <= 3; i++)
            new User(i).start();
    }
}
