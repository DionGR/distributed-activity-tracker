import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;


public class DummyUser extends Thread{
    String host, userPath;
    int id;
    int gpxRequestPort, statsRequestPort;

    DummyUser(int id){
        this.id = id;
        this.userPath = System.getProperty("user.dir") + "\\data\\user-data\\user" + id + "\\";
    }

    @Override
    public void run() {
        try {
            /* Ask whether new or existing user */
            login();

            System.out.println(this.host + " " + this.gpxRequestPort + " " + this.statsRequestPort);

            /* Start the main menu */
            int option;
            do {
                /* Let the user choose what he wants to do */
                do{
                    System.out.print("DummyUser #" + id + ": 1.Send GPX, 2.Receive Statistics, 3.Exit\n\t-> ");
                    option = getInput();
                }while(option < 1 || option > 3);

                /* Start the thread for the chosen option */
                // TODO: Remove joins when frontend is ready
                switch (option) {
                    case 1: {
                        gpxThread gt = new gpxThread();
                        gt.start();
                        gt.join();
                        break;
                    }
                    case 2: {
                        StatisticsThread st = new StatisticsThread();
                        st.start();
                        st.join();
                        break;
                    } case 3: {
                        break;
                    }
                }
            }while (option != 3);
        }
        catch (Exception e) {
            System.err.println("DummyUser #" + this.id + " - ERROR: " + e.getMessage());
        }finally {
            System.err.println("DummyUser #" + this.id + " - exiting...");
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
            System.out.println();
            try {
                /* Find all available GPX files in the unprocessed folder */
                File[] unprocessedFiles = new File(userPath + "unprocessed\\").listFiles();
                if (unprocessedFiles == null || unprocessedFiles.length == 0) {
                    System.out.println("DummyUser #" + id + " - GPXThread: no unprocessed GPX files found!\n");
                    return;
                }

                /* Create socket for contacting the server on the specified port */
                gpxSocket = new Socket(host, gpxRequestPort);

                /* Create the streams to send and receive data to/from the server */
                out = new ObjectOutputStream(gpxSocket.getOutputStream());
                in = new ObjectInputStream(gpxSocket.getInputStream());

                /* Send the ID of the user to the server */
                out.writeObject(id);
                out.flush();


                /* Print all the available GPX files and let the user pick one */
                for (int i = 0; i < unprocessedFiles.length; i++)
                    System.out.println("File #" + (i+1) +": " + unprocessedFiles[i].getName());
                System.out.println();

                int fileID = -1;
                do {
                    System.out.print("Enter the file # to process: ");
                    fileID = getInput();
                }while(fileID < 0 || fileID > unprocessedFiles.length);
                String fileName = unprocessedFiles[fileID - 1].getName();

                /* Read the file */

                // Move the file to the processed folder
                Files.move(Paths.get(userPath + "unprocessed\\" + fileName), Paths.get(userPath + "processed\\" + fileName));

                // Start reading the file
                File gpxFile = new File(userPath + "processed\\" + fileName);
                BufferedReader br = new BufferedReader(new FileReader(gpxFile));
                String line;
                StringBuilder buffer = new StringBuilder();

                // Add the route ID to the buffer
                int routeID = Integer.parseInt(gpxFile.getName().replaceAll("[\\D]", ""));
                buffer.append(routeID).append("!");

                // Read the file line by line and add it to the buffer
                while((line = br.readLine()) != null) {
                    buffer.append(line);
                }

                /* Send the file to the server */
                out.writeObject(buffer);
                out.flush();

                /* Wait and receive result */
                IntermediateChunk result = (IntermediateChunk) in.readObject();

                /* Write results to file */
                File resultFile = new File(userPath + "\\results.txt");
                BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile, true));
                bw.append(result.toString()).append("\n");
                bw.close();

                /* Print the received result from server */
                System.out.println("\nDummyUser #" + id + " received GPX result for " + result + "\n");

            }catch (UnknownHostException unknownHostException) {
                System.err.println("DummyUser #" + id + " - GPXThread: you are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                System.err.println("DummyUser #" + id + " - GPXThread IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("DummyUser #" + id + " - GPXThread CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("DummyUser #" + id + " - GPXThread ERROR: " + e.getMessage());
            }//finally {
//                try {
//                    in.close(); out.close();
//                    gpxSocket.close();
//                } catch (IOException e) {
//                    System.err.println("DummyUser #" + id + " - GPXThread IOERROR while finishing GPX request: " + e.getMessage());
//                }
//            }
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
            System.out.println();
            try {
                /* Create socket for contacting the server */
                statsSocket = new Socket(host, statsRequestPort);

                /* Create the streams to send and receive data from server */
                ObjectOutputStream out = new ObjectOutputStream(statsSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(statsSocket.getInputStream());

                /* Send the id of the user */
                out.writeObject(id);
                out.flush();

                /* Request statistics */
                Statistics userStatistics = (Statistics) in.readObject();
                Statistics totalStatistics = (Statistics) in.readObject();

                double timeDiff = userStatistics.getTotalTime() * 100 / totalStatistics.getAvgTime() - 100;
                double distDiff = userStatistics.getTotalDistance() * 100 / totalStatistics.getAvgDistance() - 100;
                double eleDiff = userStatistics.getTotalElevation() * 100 / totalStatistics.getAvgElevation() - 100;

                /* GUI - Print received statistics from server */
                System.out.println("DummyUser #" + id + " received statistics: " + userStatistics);
                System.out.println("DummyUser #" + id + " received total statistics: " + totalStatistics);
                System.out.println("DummyUser #" + id + " compared to all users: Time Difference " + timeDiff + "%" + " Distance Difference: " + distDiff + "%" + " Elevation Difference: " + eleDiff + "%");
                System.out.println("Average Time: "+totalStatistics.getAvgTime() + "| Average Distance: "+totalStatistics.getAvgDistance() + " | Average Eelevation "+totalStatistics.getAvgElevation());
                System.out.println();

            }catch (UnknownHostException unknownHostException) {
                System.err.println("DummyUser #" + id + " - StatisticsThread: you are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                System.err.println("DummyUser #" + id + " - StatisticsThread IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("DummyUser #" + id + " - StatisticsThread CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("DummyUser #" + id + " - StatisticsThread ERROR: " + e.getMessage());
            }//finally {
//                try {
//                    in.close(); out.close();
//                    statsSocket.close();
//                } catch (IOException e) {
//                    System.err.println("DummyUser #" + id + " - StatisticsThread IOERROR while finishing Statistics request: " + e.getMessage());
//                }
//            }
        }
    }

    private void login(){
        try {
            /* Ask if new or existing user */
            int answer;
            do {
                System.out.print("DummyUser #" + id + ": 1.New User, 2.Existing User\n\t-> ");
                answer = getInput();
            } while (answer != 1 && answer != 2);

            switch (answer) {
                case 1: {
                    initDefaults();
                    System.out.println("DummyUser #" + id + " created new user!");
                    break;
                }
                case 2: {
                    System.out.println("DummyUser #" + id + " welcome back!");
                    break;
                }
            }

            /* Read the host and port from the config file */
            FileReader cfgReader = new FileReader(System.getProperty("user.dir") + "\\data\\user-data\\userCFG");
            Properties properties = new Properties();
            properties.load(cfgReader);

            this.host = "localhost";
            this.gpxRequestPort = Integer.parseInt(properties.getProperty("gpxRequestPort"));
            this.statsRequestPort = Integer.parseInt(properties.getProperty("statsRequestPort"));
        }catch (Exception e){
            System.err.println("DummyUser #" + id + " - login ERROR: " + e.getMessage());
        }
    }

    private void initDefaults() {
        try {
            /* Delete results file if it exists */
            Files.deleteIfExists(Paths.get(userPath + "results.txt"));

            /* Create unprocessed folder if it does not exist */
            File unprocessedDir = new File(userPath + "unprocessed\\");
            if (!unprocessedDir.exists()) unprocessedDir.mkdir();

            /* Move all files from processed to unprocessed */
            File processedDir = new File(userPath + "processed\\");
            if (!processedDir.exists()) processedDir.mkdir();

            /* Move all existing files from processed to unprocessed */
            File[] processedFiles = processedDir.listFiles();
            if (processedFiles == null) return;
            for (File pf: processedFiles)
                Files.move(Paths.get(processedDir + "\\" + pf.getName()), Paths.get(unprocessedDir + "\\" + pf.getName()));
        } catch (IOException e) {
            System.err.println("DummyUser #" + id + " - initDefaults IOERROR: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("DummyUser #" + id + " - initDefaults ERROR: " + e.getMessage());
        }
    }

    private int getInput(){
        Scanner input = new Scanner(System.in);

        int prompt = 0;
        try{
            prompt = input.nextInt();
            return prompt;
        }catch (Exception e){
            prompt = -1;
        }

        input.close();
        return prompt;
    }

    public static void main(String[] args) {
        int numUsers = 1;

        DummyUser[] dummyUsers = new DummyUser[numUsers];
        for (int i = 1; i <= numUsers; i++) {
            dummyUsers[i-1] = new DummyUser(i);
            dummyUsers[i-1].start();
        }

        for (int i = 1; i <= numUsers; i++) {
            try {
                dummyUsers[i-1].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
