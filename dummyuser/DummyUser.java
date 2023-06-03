package dummyuser;
import app.backend.modules.IntermediateChunk;
import app.backend.modules.Segment;
import app.backend.modules.Statistics;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

/* DummyUser Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class is used to simulate a user.
 */

public class DummyUser extends Thread{
    private String host;
    private final String userPath;
    private final int id;
    private int gpxRequestPort, statsRequestPort, segRequestPort, segStatsRequestPort;
    private boolean autorun, autorunNoMoreGPX, addedSegments, gotNormalStatistics, gotSegmentStatistics = false;

    DummyUser(int id){
        this.id = id;
        this.userPath = System.getProperty("user.dir") + "\\dummyuser\\data\\user" + id + "\\";
    }

    @Override
    public void run() {
        try {
            /* Ask whether new or existing user */
            login();

            /* Start the main menu */
            int option;
            do {

                /* Let the user choose what he wants to do */
                do{
                    // TODO: Remove this when frontend is ready
                    if (autorun && !autorunNoMoreGPX && !addedSegments && !gotNormalStatistics && !gotSegmentStatistics) {
                        option = 2;
                        break;
                    }else if (autorun && !autorunNoMoreGPX && addedSegments && !gotNormalStatistics && !gotSegmentStatistics) {
                        option = 1;
                        break;
                    }else if (autorun && autorunNoMoreGPX && addedSegments && !gotNormalStatistics && !gotSegmentStatistics) {
                        option = 3;
                        break;
                    } else if (autorun && autorunNoMoreGPX && addedSegments && gotNormalStatistics && !gotSegmentStatistics) {
                        option = 4;
                        break;
                    }else if (autorun && autorunNoMoreGPX && addedSegments && gotNormalStatistics && gotSegmentStatistics) {
                        option = 5;
                        break;
                    }

                    System.out.print("DummyUser #" + id + ": 1.Send GPX, 2.Send Segment, 3.Request General Statistics, 4. Request Segment Statistics\n\t-> ");
                    option = getInput();
                }while(option < 1 || option > 5);


                /* Start the thread for the chosen option */
                // TODO: Remove joins when frontend is ready
                switch (option) {
                    case 1: {
                        GPXThread gt = new GPXThread();
                        gt.start();
                        gt.join();
                        break;
                    }
                    case 2: {
                        SegmentThread st = new SegmentThread();
                        st.start();
                        st.join();
                        break;
                    } case 3: {
                        StatisticsThread statt = new StatisticsThread();
                        statt.start();
                        statt.join();
                        gotNormalStatistics = true;
                        break;
                    } case 4:{
                        SegmentStatisticsThread sstatt = new SegmentStatisticsThread();
                        sstatt.start();
                        sstatt.join();
                        gotSegmentStatistics = true;
                        if (autorun && autorunNoMoreGPX && addedSegments && gotNormalStatistics && gotSegmentStatistics)
                            option = 5;
                        break;
                    } case 5:{
                        break;
                    }
                }
            }while (option != 5);
        }
        catch (Exception e) {
            System.err.println("DummyUser #" + this.id + " - ERROR: " + e.getMessage());
        }finally {
            System.err.println("DummyUser #" + this.id + " - exiting...");
        }
    }

    private class GPXThread extends Thread {
        private Socket gpxSocket;
        ObjectOutputStream out;
        ObjectInputStream in;

        GPXThread() {
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
                    autorunNoMoreGPX = true; // TODO: Remove this when frontend is ready
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

                // TODO: Remove if else when frontend is ready
                String fileName;
                if (!autorun) {
                    /* Print all the available GPX files and let the user pick one */
                    for (int i = 0; i < unprocessedFiles.length; i++)
                        System.out.println("File #" + (i + 1) + ": " + unprocessedFiles[i].getName());
                    System.out.println();

                    int fileID = -1;
                    do {
                        System.out.print("Enter the file # to process: ");
                        fileID = getInput();
                    } while (fileID < 0 || fileID > unprocessedFiles.length);
                    fileName = unprocessedFiles[fileID - 1].getName();
                } else{
                    fileName = unprocessedFiles[0].getName();
                }

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
                // Close gpx file
                try { if (br != null) br.close(); } catch (IOException ioException) {System.err.println("DummyUser #" + id + " - GPXThread IOERROR while closing gpx file: " + ioException.getMessage()); }

                /* Send the file to the server */
                out.writeObject(buffer);
                out.flush();

                /* Wait and receive result */
                IntermediateChunk result = (IntermediateChunk) in.readObject();

                /* Write results to file */
                File resultFile = new File(userPath + "\\results.txt");
                BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile, true));
                bw.append(result.toString()).append("\n");
                // Close results file
                try { if (bw != null) bw.close(); } catch (IOException ioException) {System.err.println("DummyUser #" + id + " - GPXThread IOERROR while closing results file: " + ioException.getMessage()); };

                /* Print the received result from server */
                System.out.println("\nDummyUser #" + id + " received GPX result: " + result + "\n");
                System.out.println("DummyUser #" + id + " - segments found in GPX file: " + result.getSegmentID() + "\n");

            }catch (UnknownHostException unknownHostException) {
                System.err.println("DummyUser #" + id + " - GPXThread: you are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                System.err.println("DummyUser #" + id + " - GPXThread IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("DummyUser #" + id + " - GPXThread CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("DummyUser #" + id + " - GPXThread ERROR: " + e.getMessage());
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - GPXThread IOERROR while closing input stream: " + ioException.getMessage()); }
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - GPXThread IOERROR while closing output stream: " + ioException.getMessage()); }
                try { if (gpxSocket != null) gpxSocket.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - GPXThread IOERROR while closing socket: " + ioException.getMessage()); }
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
                System.out.println("DummyUser #" + id + " compared to all users: Time Difference " + String.format("%.2f", timeDiff) + "%" + " | Distance Difference: " + String.format("%.2f", distDiff) + "%" + " | Elevation Difference: " + String.format("%.2f", eleDiff) + "%");
                System.out.println();

            }catch (UnknownHostException unknownHostException) {
                System.err.println("DummyUser #" + id + " - StatisticsThread: you are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                System.err.println("DummyUser #" + id + " - StatisticsThread IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("DummyUser #" + id + " - StatisticsThread CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("DummyUser #" + id + " - StatisticsThread ERROR: " + e.getMessage());
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - StatisticsThread IOERROR while closing input stream: " + ioException.getMessage()); }
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - StatisticsThread IOERROR while closing output stream: " + ioException.getMessage()); }
                try { if (statsSocket != null) statsSocket.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - StatisticsThread IOERROR while closing socket: " + ioException.getMessage()); }
            }
        }
    }

    private class SegmentThread extends Thread{
        private Socket segmentSocket;
        ObjectOutputStream out;
        ObjectInputStream in;

        SegmentThread(){
            this.segmentSocket = null;
            this.out = null;
            this.in = null;
        }

        @Override
        public void run() {
            System.out.println();
            try {
                /* Find all available Segment files in the unprocessed folder */
                File[] unprocessedFiles = new File(userPath + "unprocessedSeg\\").listFiles();
                if (unprocessedFiles == null || unprocessedFiles.length == 0) {
                    System.out.println("DummyUser #" + id + " - SegmentThread: no unprocessed Segment files found!\n");
                    addedSegments = true;
                    return;
                }

                /* Create socket for contacting the server on the specified port */
                segmentSocket = new Socket(host, segRequestPort);

                /* Create the streams to send and receive data to/from the server */
                out = new ObjectOutputStream(segmentSocket.getOutputStream());
                in = new ObjectInputStream(segmentSocket.getInputStream());

                /* Send the ID of the user to the server */
                out.writeObject(id);
                out.flush();

                // TODO: Remove if else when frontend is ready
                String fileName;
                if (!autorun) {
                    /* Print all the available Segment files and let the user pick one */
                    for (int i = 0; i < unprocessedFiles.length; i++)
                        System.out.println("Segment #" + (i + 1) + ": " + unprocessedFiles[i].getName());
                    System.out.println();

                    int fileID = -1;
                    do {
                        System.out.print("Enter the segment # to process: ");
                        fileID = getInput();
                    } while (fileID < 0 || fileID > unprocessedFiles.length);
                    fileName = unprocessedFiles[fileID - 1].getName();
                } else {
                    fileName = unprocessedFiles[0].getName();
                }

                /* Read the file */

                // Move the file to the processed folder
                Files.move(Paths.get(userPath + "unprocessedSeg\\" + fileName), Paths.get(userPath + "processedSeg\\" + fileName));

                // Start reading the file
                File segmentFile = new File(userPath + "processedSeg\\" + fileName);
                BufferedReader br = new BufferedReader(new FileReader(segmentFile));
                String line;
                StringBuilder buffer = new StringBuilder();

                // Add the route ID to the buffer TODO: ?????????
                int routeID = Integer.parseInt(segmentFile.getName().replaceAll("[\\D]", ""));
                buffer.append(routeID).append("!");

                // Read the file line by line and add it to the buffer
                while((line = br.readLine()) != null) {
                    buffer.append(line);
                }
                // Close gpx file
                try { if (br != null) br.close(); } catch (IOException ioException) {System.err.println("DummyUser #" + id + " - SegmentThread IOERROR while closing gpx file: " + ioException.getMessage()); }

                /* Send the file to the server */
                out.writeObject(buffer);
                out.flush();

//                int ack = (int) in.readObject();

                System.out.println("\nDummyUser #" + id + " uploaded segment!\n");
            }catch (UnknownHostException unknownHostException) {
                System.err.println("DummyUser #" + id + " - SegmentThread: you are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                System.err.println("DummyUser #" + id + " - SegmentThread IOERROR: " + ioException.getMessage());
            } catch (Exception e) {
                System.err.println("DummyUser #" + id + " - SegmentThread ERROR: " + e.getMessage());
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - SegmentThread IOERROR while closing input stream: " + ioException.getMessage()); }
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - SegmentThread IOERROR while closing output stream: " + ioException.getMessage()); }
                try { if (segmentSocket != null) segmentSocket.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - SegmentThread IOERROR while closing socket: " + ioException.getMessage()); }
            }
        }
    }

    private class SegmentStatisticsThread extends Thread{
        private Socket segStatsSocket;
        ObjectOutputStream out;
        ObjectInputStream in;

        SegmentStatisticsThread() {
            this.segStatsSocket = null;
            this.out = null;
            this.in = null;
        }

        @Override
        public void run(){
            System.out.println();
            try {
                /* Create socket for contacting the server */
                segStatsSocket = new Socket(host, segStatsRequestPort);

                /* Create the streams to send and receive data from server */
                ObjectOutputStream out = new ObjectOutputStream(segStatsSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(segStatsSocket.getInputStream());

                /* Send the id of the user */
                out.writeObject(id);
                out.flush();

                /* Request statistics TODO: ... */
                ArrayList<HashMap<Integer, IntermediateChunk>> leaderboard = (ArrayList<HashMap<Integer, IntermediateChunk>>) in.readObject();
                HashMap<Integer, ArrayList<IntermediateChunk>> segmentsStatistics = (HashMap<Integer, ArrayList<IntermediateChunk>>) in.readObject();

                System.out.println("\nDummyUser #" + id + " - SegmentStatisticsThread: received statistics!\n");

                // Print Leaderboard
                StringBuilder s = new StringBuilder();
                s.append("DummyUser #").append(id).append(" - Leaderboard\n");
                for (HashMap<Integer, IntermediateChunk> segment: leaderboard) {
                    int segID;
                    if (!segment.isEmpty()) {
                        segID = segment.get(segment.keySet().iterator().next()).getSegmentID();
                    }
                    else continue;
                    s.append("\tSegment #").append(segID).append("\n");

                    for (Integer userID: segment.keySet()) {
                        s.append("\t\tUser #").append(userID).append(String.format("| Time: %5.2f min\n", (double) segment.get(userID).getTotalTime()/1000/60));
                    }
                    s.append("\n");
                }
                System.out.println(s);

                // Print Segment Statistics
                s = new StringBuilder();
                for (Integer segID: segmentsStatistics.keySet()) {
                    s.append("DummyUser #").append(id).append(" - History of segmentID #").append(segID).append("\n");
                    for (IntermediateChunk route: segmentsStatistics.get(segID)) {
                        s.append(String.format("\tTime: %5.2f min\n", (double)route.getTotalTime()/1000/60));
                    }
                    s.append("\n");
                }
                System.out.println(s);


            }catch (UnknownHostException unknownHostException) {
                System.err.println("DummyUser #" + id + " - SegmentStatisticsThread: you are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                System.err.println("DummyUser #" + id + " - SegmentStatisticsThread IOERROR: " + ioException.getMessage());
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println("DummyUser #" + id + " - SegmentStatisticsThread CASTERROR: " + classNotFoundException.getMessage());
            } catch (Exception e) {
                System.err.println("DummyUser #" + id + " - SegmentStatisticsThread ERROR: " + e.getMessage());
            }finally {
                try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - SegmentStatisticsThread IOERROR while closing input stream: " + ioException.getMessage()); }
                try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - SegmentStatisticsThread IOERROR while closing output stream: " + ioException.getMessage()); }
                try { if (segStatsSocket != null) segStatsSocket.close(); } catch (IOException ioException) { System.err.println("DummyUser #" + id + " - SegmentStatisticsThread IOERROR while closing socket: " + ioException.getMessage()); }
            }
        }
    }

    /* Initialize the user */
    private void login(){
        try {
            // TODO: Move this under after autorun is removed
            /* Read the host and port from the config file */
            FileReader cfgReader = new FileReader(System.getProperty("user.dir") + "\\dummyuser\\data\\userCFG");
            Properties properties = new Properties();
            properties.load(cfgReader);

            this.host = properties.getProperty("host");
            this.gpxRequestPort = Integer.parseInt(properties.getProperty("gpxRequestPort"));
            this.statsRequestPort = Integer.parseInt(properties.getProperty("statsRequestPort"));
            this.segRequestPort = Integer.parseInt(properties.getProperty("segRequestPort"));
            this.segStatsRequestPort = Integer.parseInt(properties.getProperty("segStatsRequestPort"));
            this.autorun = Boolean.parseBoolean(properties.getProperty("autorun"));

            try { if (cfgReader != null) cfgReader.close(); } catch(IOException ioException) { System.err.println("DummyUser #" + id + " - login - IOERROR while closing config file: " + ioException.getMessage());}// Close the reader

            /* Ask if new or existing user */
            int answer;
            do {
                // TODO: Remove this when frontend is ready
                if (autorun){
                    answer = 1;
                    break;
                }
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
        }catch (Exception e){
            System.err.println("DummyUser #" + id + " - login ERROR: " + e.getMessage());
        }
    }

    /* Initialize the default folders and files */
    private void initDefaults() {
        try {
            /* Delete results file if it exists */
            Files.deleteIfExists(Paths.get(userPath + "results.txt"));

            /* Create unprocessed GPX folder if it does not exist */
            File unprocessedDir = new File(userPath + "unprocessed\\");
            if (!unprocessedDir.exists()) unprocessedDir.mkdir();

            /* Create processed GPX folder if it does not exist */
            File processedDir = new File(userPath + "processed\\");
            if (!processedDir.exists()) processedDir.mkdir();

            /* Create unprocessed segment folder if it does not exist */
            File unprocessedSegDir = new File(userPath + "unprocessedSeg\\");
            if (!unprocessedSegDir.exists()) unprocessedSegDir.mkdir();

            /* Create processed segment folder if it does not exist */
            File processedSegDir = new File(userPath + "processedSeg\\");
            if (!processedSegDir.exists()) processedSegDir.mkdir();

            /* Move all existing files from processed to unprocessed */
            File[] processedFiles = processedDir.listFiles();
            if (processedFiles == null) return;
            for (File pf: processedFiles)
                Files.move(Paths.get(processedDir + "\\" + pf.getName()), Paths.get(unprocessedDir + "\\" + pf.getName()));

            File[] processedSegs = processedSegDir.listFiles();
            if (processedSegs == null) return;
            for (File pf: processedSegs)
                Files.move(Paths.get(processedSegDir + "\\" + pf.getName()), Paths.get(unprocessedSegDir + "\\" + pf.getName()));
        } catch (IOException e) {
            System.err.println("DummyUser #" + id + " - initDefaults IOERROR: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("DummyUser #" + id + " - initDefaults ERROR: " + e.getMessage());
        }
    }

    /* Safely gets input from user */
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
        int numUsers = 3;

        long startTime = System.currentTimeMillis();

        /* Create and start all users */
        DummyUser[] dummyUsers = new DummyUser[numUsers];
        for (int i = 1; i <= numUsers; i++) {
            dummyUsers[i-1] = new DummyUser(i);
            dummyUsers[i-1].start();
        }

        /* Wait for all users to finish */
        for (int i = 1; i <= numUsers; i++) {
            try {
                dummyUsers[i-1].join();
            } catch (InterruptedException e) {
                System.err.println("DummyUser #" + i + " - main ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }

        long endTime = System.currentTimeMillis();
        System.err.println("Total time: " + (double) (endTime - startTime) / 1000 + " seconds");
    }
}