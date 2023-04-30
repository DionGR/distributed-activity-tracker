import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Scanner;


public class DummyUser extends Thread{
    String host, userPath;
    int id;
    int gpxServerPort, statsServerPort;

    Scanner input = new Scanner(System.in);

    DummyUser(int id, String host, int gpxServerPort, int statsServerPort){
        this.id = id;
        this.host = host;
        this.gpxServerPort = gpxServerPort;
        this.statsServerPort = statsServerPort;
        this.userPath = System.getProperty("user.dir") + "\\data\\user-data\\user" + id + "\\";
    }

    @Override
    public void run() {
        try {
            initFolders();
            int option = 0;

//            while (option != 3) {
                System.out.print("DummyUser #" + id + ": 1.Send GPX, 2.Receive Statistics, 3.Exit\n\t-> ");
                option = input.nextInt();
                System.out.println();

                switch (option) {
                    case 1: {
                        new gpxThread().start();
                        break;
                    }
                    case 2: {
                        new StatisticsThread().start();
                        break;
                    }
                    case 3: {
                        System.out.println("DummyUser #" + id + " shutting down...");
                        break;
                    }
                }
//            }
        }
        catch (Exception e) {
            System.err.println("DummyUser #" + this.id + " - ERROR: " + e.getMessage());
        } finally {
//            try {
////                out.close(); in.close();
////                requestSocket.close();
//                System.out.println("DummyUser #" + this.id + " shutting down...");
//            } catch (IOException ioException) {
//                System.err.println("DummyUser #" + this.id + " - IOERROR while shutting down: " + ioException.getMessage());
//            }
        }
    }

    private class gpxThread extends Thread {
        private Socket gpxSocket;
        ObjectOutputStream out;
        ObjectInputStream in;

        gpxThread() {
            this.gpxSocket = null;
            this.out = null;
            this.in = null;
        }

        @Override
        public void run() {
            try {
                Scanner input = new Scanner(System.in);
                /* Create socket for contacting the server on port 54321 */
                gpxSocket = new Socket(host, gpxServerPort);

                /* Create the streams to send and receive data to/from the server */
                out = new ObjectOutputStream(gpxSocket.getOutputStream());
                in = new ObjectInputStream(gpxSocket.getInputStream());

                /* Send the ID of the user to the server */
                out.writeObject(id);
                out.flush();

                /* Find all available GPX files in the unprocessed folder */
                File[] files = new File(userPath + "unprocessed\\").listFiles();
                if (files == null) throw new Exception("No files to process");

                /* Print all the available GPX files and let the user pick one */
                for (int i = 0; i < files.length; i++)
                    System.out.println("File #" + (i+1) +": " + files[i].getName());

                System.out.print("Enter the file # to process: ");
                int fileID = input.nextInt() - 1;
                String fileName = files[fileID].getName();

                /* Send the file to the server */
                Files.move(Paths.get(userPath + "unprocessed\\" + fileName), Paths.get(userPath + "processed\\" + fileName));
                File gpxFile = new File(userPath + "processed\\" + fileName);
                BufferedReader br = new BufferedReader(new FileReader(gpxFile));
                String line;
                StringBuilder buffer = new StringBuilder();

                int routeID = Integer.parseInt(gpxFile.getName().replaceAll("[\\D]", ""));
                buffer.append(routeID).append("!");

                while((line = br.readLine()) != null) {
                    buffer.append(line);
                }

                /* Write gpx */
                out.writeObject(buffer);
                out.flush();

                /* Wait and receive result */
                Segment result = (Segment) in.readObject();

                /* Write results to file */
                File f2 = new File(userPath + "\\results.txt");
                BufferedWriter bw = new BufferedWriter(new FileWriter(f2, true));
                bw.append(result.toString()).append("\n");
                bw.close();

                /* Print the received result from server */
                System.out.println("DummyUser #" + id + " received GPX result for " + result);


//                Files.deleteIfExists(Paths.get(userPath + "unprocessed\\"));
            }catch (UnknownHostException unknownHostException) {
                System.err.println("DummyUser #" + id + " - GPXThread: you are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                System.err.println("DummyUser #" + id + " - GPXThread IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("DummyUser #" + id + " - GPXThread CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("DummyUser #" + id + " - GPXThread ERROR: " + e.getMessage());
            }finally {
                try {
                    in.close(); out.close();
                    gpxSocket.close();

                    for (File f: Objects.requireNonNull(new File(userPath + "unprocessed\\").listFiles())) {
                        Files.move(Paths.get(userPath + "unprocessed\\" + f.getName()), Paths.get(userPath + "processed\\" + f.getName()));
                    }

                    Files.deleteIfExists(Paths.get(userPath + "unprocessed\\"));
                } catch (IOException e) {
                    System.err.println("DummyUser #" + id + " - GPXThread IOERROR while finishing GPX request: " + e.getMessage());
                }
            }
        }
    }

    private class StatisticsThread extends Thread{
        private Socket statsSocket;
        ObjectOutputStream out;
        ObjectInputStream in;

        StatisticsThread() {
            this.statsSocket = null;
            this.out = null;
            this.in = null;
        }

        @Override
        public void run(){
            try {
                /* Create socket for contacting the server on port ____ */
                statsSocket = new Socket(host, statsServerPort);

                /* Create the streams to send and receive data from server */
                ObjectOutputStream out = new ObjectOutputStream(statsSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(statsSocket.getInputStream());

                /* Send the id of the user */
                out.writeObject(id);
                out.flush();

                /* Request statistics */
                Statistics statistics = (Statistics) in.readObject();

                /* Print received statistics from server */
                System.out.println("DummyUser #" + id + " received statistics: " + statistics);

            }catch (UnknownHostException unknownHostException) {
                System.err.println("DummyUser #" + id + " - StatisticsThread: you are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                System.err.println("DummyUser #" + id + " - StatisticsThread IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("DummyUser #" + id + " - StatisticsThread CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("DummyUser #" + id + " - StatisticsThread ERROR: " + e.getMessage());
            }finally {
                try {
//                    out.close();
//                    in.close();
                    statsSocket.close();
                } catch (IOException e) {
                    System.err.println("DummyUser #" + id + " - StatisticsThread IOERROR while finishing Statistics request: " + e.getMessage());
                }
            }
        }
    }

    private void initFolders() throws Exception{
        try {
            new File(userPath + "unprocessed\\").mkdir();
        } catch (Exception e) {
            throw new Exception("Could not create \"unprocessed\" folder!");
        }

        for (File f: Objects.requireNonNull(new File(userPath + "processed\\").listFiles())) {
            Files.move(Paths.get(userPath + "processed\\" + f.getName()), Paths.get(userPath + "unprocessed\\" + f.getName()));
        }

        Files.deleteIfExists(Paths.get(userPath + "results.txt"));
    }


    public static void main(String[] args) {
        String host = "localhost";
        int gpxServerPort = 54321;
        int statsServerPort = 65432;

        int numUsers = 1;

        // Count time
        long startTime = System.currentTimeMillis();

        // Create 12 dummyUsers and wait for them to finish
        DummyUser[] dummyUsers = new DummyUser[numUsers];
        for (int i = 1; i <= numUsers; i++) {
            dummyUsers[i-1] = new DummyUser(i, host, gpxServerPort, statsServerPort);
            dummyUsers[i-1].start();
        }
        for (int i = 1; i <= numUsers; i++) {
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
