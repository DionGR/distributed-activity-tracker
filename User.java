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
        this.counter = 0;
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
            requestSocket = new Socket(host, 4321);

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

            ResultTaker resultTaker = new ResultTaker();
            resultTaker.start();

            /* Find all GPX files in user folder */
            File[] files = new File(userPath + "unprocessed\\").listFiles();
            while(files != null){

                for (File f: files) {
                    String fileName = f.getName();
                    Files.move(Path.of(userPath + "unprocessed\\" + fileName), Path.of(userPath + "processed\\" + fileName));

                    File gpxFile = new File(userPath + "processed\\" + fileName);
                    synchronized (counter){
                        counter++;
                    }
                    FileThread gpxThread = new FileThread(gpxFile);
                    gpxThread.start();
                }
                files = new File(userPath + "unprocessed\\").listFiles();
            }

            /* Wait for all threads to finish */
            while (Thread.activeCount() > 1){
                Thread.sleep(1);
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

    private class FileThread extends Thread{
        private final File gpx;

        public FileThread(File gpx){
            this.gpx = gpx;
        }

        @Override
        public void run(){
            try{
                BufferedReader br = new BufferedReader(new FileReader(gpx));
                String line;
                StringBuilder buffer = new StringBuilder();
                int routeID = Integer.parseInt(gpx.getName().replaceAll("[\\D]", ""));

                buffer.append(routeID + "!");
                while((line = br.readLine()) != null) {
                    buffer.append(line);
                }

                /* Write gpx */
                synchronized (out) {
                    out.writeObject(buffer);
                    out.flush();
                }


            } catch (IOException ioException) {
                System.err.println("FileThread for User #" + id + " - IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("FileThread for User #" + id + " - ERROR: " + e.getMessage());
            }
        }
    }

    private class ResultTaker extends Thread{

        @Override
        public void run(){
            try{
                Segment result;

                while (requestSocket.isConnected()){
                    synchronized (in) {
                         result = (Segment) in.readObject();
                    }

                    /* Write results to file */
                    File f2 = new File(userPath + "\\results\\result" + result.getId() + ".gpx");
                    f2.createNewFile();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(f2));
                    bw.write(result.toString());
                    bw.close();

                    /* Print the received result from server */
                    System.out.println("User #" + id + " received result: " + result);

                    synchronized (counter){
                        counter--;
                        if (counter == 0){
                            break;
                        }
                    }
                }
            } catch (IOException ioException) {
                System.err.println("ResultTaker for User #" + id + " - IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("ResultTaker for User #" + id + " - CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("ResultTaker for User #" + id + " - ERROR: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 3; i++)
            new User(i).start();
    }
}
