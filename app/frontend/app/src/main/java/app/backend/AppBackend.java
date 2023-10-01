package app.frontend.app.src.main.java.app.backend;

import android.os.Environment;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import app.backend.modules.Chart;
import app.backend.modules.IntermediateChunk;
import app.backend.modules.GPXResult;
import app.backend.modules.Leaderboard;
import app.backend.modules.Statistics;

/* AppBackend Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class is used to simulate a user's requests.
 */

public class AppBackend{
    private static AppBackend instance;
    private String host = "192.168.1.2";
    private final String userPath;
    private String id;
    private int gpxRequestPort = 44444, statsRequestPort = 44443, segRequestPort = 44442, segStatsRequestPort = 44441;

    private HashMap<String, List<GPXResult>> gpxResults;    // userID -> GPXResults
    private HashMap<String, List<Leaderboard>> leaderboards; // userID -> Leaderboards

    private AppBackend() {
        userPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/routes/";
        initDefaults();
        gpxResults = new HashMap<>();
        leaderboards = new HashMap<>();
    }


    public static AppBackend getInstance() {
        if(instance == null){
            instance = new AppBackend();
        }
        return instance;
    }

    public void setUserID(String id) {
        if (!gpxResults.containsKey(id)) {
            gpxResults.put(id, new ArrayList<>());
        }
        if (!leaderboards.containsKey(id)) {
            leaderboards.put(id, new ArrayList<>());
        }
        this.id = id;
    }
    public String getUserID() { return id; }



    public boolean uploadGPX(String gpxPath) {
        Socket gpxSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        IntermediateChunk result = null;
        boolean success = false;

        try {
            /* Create socket for contacting the server on the specified port */
            gpxSocket = new Socket();
            gpxSocket.connect(new InetSocketAddress(host, gpxRequestPort), 2000);

            /* Create the streams to send and receive data to/from the server */
            out = new ObjectOutputStream(gpxSocket.getOutputStream());
            in = new ObjectInputStream(gpxSocket.getInputStream());

            /* Send the ID of the user to the server */
            out.writeObject(id);
            out.flush();


            /* Read the file */

            // Move the file to the processed folder
            File f = new File(gpxPath);
            String filename = f.getName();
            Files.move(Paths.get(userPath + "unprocessed/" + filename ), Paths.get(userPath + "processed/" + filename));

            // Start reading the file
            File gpxFile = new File(userPath + "processed/" + filename);
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
            try { if (br != null) br.close(); } catch (IOException ioException) {System.err.println("User " + id + " - GPXThread IOERROR while closing gpx file: " + ioException.getMessage()); }

            /* Send the file to the server */
            out.writeObject(buffer);
            out.flush();

            /* Wait and receive result */
            result = (IntermediateChunk) in.readObject();

            gpxResults.get(id).add(new GPXResult(result));

            success = true;

        }catch (UnknownHostException unknownHostException) {
            System.err.println("User " + id + " - GPXThread: you are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            System.err.println("User " + id + " - GPXThread IOERROR: " + ioException.getMessage());
        } catch (ClassNotFoundException classNotFoundException) {
            System.err.println("User " + id + " - GPXThread CASTERROR: " + classNotFoundException.getMessage());
        } catch (Exception e) {
            System.err.println("User " + id + " - GPXThread ERROR: " + e.getMessage());
        }finally {
            try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("User " + id + " - GPXThread IOERROR while closing input stream: " + ioException.getMessage()); }
            try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("User " + id + " - GPXThread IOERROR while closing output stream: " + ioException.getMessage()); }
            try { if (gpxSocket != null) gpxSocket.close(); } catch (IOException ioException) { System.err.println("User " + id + " - GPXThread IOERROR while closing socket: " + ioException.getMessage()); }
            return success;
        }
    }

    public List<GPXResult> getGpxResults(){
        return gpxResults.get(id);
    }



    public boolean uploadSegment(String segmentPath) {
        Socket segmentSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        boolean success = false;

        try {
            /* Create socket for contacting the server on the specified port */
            segmentSocket = new Socket();
            segmentSocket.connect(new InetSocketAddress(host, segRequestPort), 2000);

            /* Create the streams to send and receive data to/from the server */
            out = new ObjectOutputStream(segmentSocket.getOutputStream());
            in = new ObjectInputStream(segmentSocket.getInputStream());

            /* Send the ID of the user to the server */
            out.writeObject(id);
            out.flush();

            /* Read the file */
            // Move the file to the processed folder
            File f = new File(segmentPath);
            String filename = f.getName();
            Files.move(Paths.get(userPath + "unprocessedSeg/" + filename), Paths.get(userPath + "processedSeg/" + filename));

            // Start reading the file
            File segmentFile = new File(userPath + "processedSeg/" + filename);
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
            try { if (br != null) br.close(); } catch (IOException ioException) {System.err.println("User " + id + " - SegmentThread IOERROR while closing gpx file: " + ioException.getMessage()); }

            /* Send the file to the server */
            out.writeObject(filename);
            out.writeObject(buffer);
            out.flush();

            success = true;
        }catch (UnknownHostException unknownHostException) {
            System.err.println("User " + id + " - SegmentThread: you are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            System.err.println("User " + id + " - SegmentThread IOERROR: " + ioException.getMessage());
        } catch (Exception e) {
            System.err.println("User " + id + " - SegmentThread ERROR: " + e.getMessage());
        }finally {
            try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("User " + id + " - SegmentThread IOERROR while closing input stream: " + ioException.getMessage()); }
            try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("User " + id + " - SegmentThread IOERROR while closing output stream: " + ioException.getMessage()); }
            try { if (segmentSocket != null) segmentSocket.close(); } catch (IOException ioException) { System.err.println("User " + id + " - SegmentThread IOERROR while closing socket: " + ioException.getMessage()); }
            return success;
        }
    }

    public ArrayList<Chart> requestStatistics(){
        Socket statsSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        ArrayList<Chart> charts = null;

        try {
            /* Create socket for contacting the server */
            statsSocket = new Socket(host, statsRequestPort);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(statsSocket.getOutputStream());
            in = new ObjectInputStream(statsSocket.getInputStream());

            /* Send the id of the user */
            out.writeObject(id);
            out.flush();

            /* Request statistics */
            Statistics userStatistics = (Statistics) in.readObject();
            Statistics totalStatistics = (Statistics) in.readObject();

            Chart timeChart = new Chart((double) userStatistics.getTotalTime()/1000/60, (double) totalStatistics.getAvgTime()/1000/60);
            Chart distanceChart = new Chart(userStatistics.getTotalDistance(), totalStatistics.getAvgDistance());
            Chart elevationChart = new Chart(userStatistics.getTotalElevation(), totalStatistics.getAvgElevation());

            charts = new ArrayList<>();
            charts.add(timeChart); charts.add(distanceChart); charts.add(elevationChart);

        }catch (UnknownHostException unknownHostException) {
            System.err.println("User " + id + " - StatisticsThread: you are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            System.err.println("User " + id + " - StatisticsThread IOERROR: " + ioException.getMessage());
        } catch (ClassNotFoundException classNotFoundException) {
            System.err.println("User " + id + " - StatisticsThread CASTERROR: " + classNotFoundException.getMessage());
        } catch (Exception e) {
            System.err.println("User " + id + " - StatisticsThread ERROR: " + e.getMessage());
        }finally {
            try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("User " + id + " - StatisticsThread IOERROR while closing input stream: " + ioException.getMessage()); }
            try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("User " + id + " - StatisticsThread IOERROR while closing output stream: " + ioException.getMessage()); }
            try { if (statsSocket != null) statsSocket.close(); } catch (IOException ioException) { System.err.println("User " + id + " - StatisticsThread IOERROR while closing socket: " + ioException.getMessage()); }
            return charts;
        }
    }


    public void requestSegmentStatistics(){
        Socket segStatsSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        System.out.println();
        try {
            /* Create socket for contacting the server */
            segStatsSocket = new Socket(host, segStatsRequestPort);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(segStatsSocket.getOutputStream());
            in = new ObjectInputStream(segStatsSocket.getInputStream());

            /* Send the id of the user */
            out.writeObject(id);
            out.flush();

            ArrayList<String> leaderboardSegmentNamesFromMaster = (ArrayList<String>) in.readObject();
            ArrayList<HashMap<String, IntermediateChunk>> leaderboardsFromMaster = (ArrayList<HashMap<String, IntermediateChunk>>) in.readObject();
            //HashMap<Integer, ArrayList<IntermediateChunk>> segmentsStatistics = (HashMap<Integer, ArrayList<IntermediateChunk>>) in.readObject();

            clearLeaderboards();
            for (int i = 0; i < leaderboardSegmentNamesFromMaster.size(); i++) {
                leaderboards.get(id).add(new Leaderboard(leaderboardSegmentNamesFromMaster.get(i)));
                leaderboards.get(id).get(i).setLeaderboard(leaderboardsFromMaster.get(i));
            }

        }catch (UnknownHostException unknownHostException) {
            System.err.println("User " + id + " - SegmentStatisticsThread: you are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            System.err.println("User " + id + " - SegmentStatisticsThread IOERROR: " + ioException.getMessage());
        } catch (ClassNotFoundException classNotFoundException) {
            System.err.println("User " + id + " - SegmentStatisticsThread CASTERROR: " + classNotFoundException.getMessage());
        } catch (Exception e) {
            System.err.println("User " + id + " - SegmentStatisticsThread ERROR: " + e.getMessage());
        }finally {
            try { if (in != null) in.close(); } catch (IOException ioException) { System.err.println("User " + id + " - SegmentStatisticsThread IOERROR while closing input stream: " + ioException.getMessage()); }
            try { if (out != null) out.close(); } catch (IOException ioException) { System.err.println("User " + id + " - SegmentStatisticsThread IOERROR while closing output stream: " + ioException.getMessage()); }
            try { if (segStatsSocket != null) segStatsSocket.close(); } catch (IOException ioException) { System.err.println("User " + id + " - SegmentStatisticsThread IOERROR while closing socket: " + ioException.getMessage()); }
        }
    }


    public List<Leaderboard> getLeaderboards() {
        return leaderboards.get(id);
    }



    /* Initialize the default folders and files */
    private void initDefaults() {
        try {
            /* Delete results file if it exists */
            Files.deleteIfExists(Paths.get(userPath + "results.txt"));

            /* Create unprocessed GPX folder if it does not exist */
            File unprocessedDir = new File(userPath + "unprocessed");
            if (!unprocessedDir.exists()) unprocessedDir.mkdir();

            /* Create processed GPX folder if it does not exist */
            File processedDir = new File(userPath + "processed");
            if (!processedDir.exists()) processedDir.mkdir();

            /* Create unprocessed segment folder if it does not exist */
            File unprocessedSegDir = new File(userPath + "unprocessedSeg");
            if (!unprocessedSegDir.exists()) unprocessedSegDir.mkdir();

            /* Create processed segment folder if it does not exist */
            File processedSegDir = new File(userPath + "processedSeg");
            if (!processedSegDir.exists()) processedSegDir.mkdir();

            /* Move all existing files from processed to unprocessed */
            File[] processedFiles = processedDir.listFiles();
            if (processedFiles == null) return;
            for (File pf: processedFiles)
                Files.move(Paths.get(processedDir + "/" + pf.getName()), Paths.get(unprocessedDir + "/" + pf.getName()));

            File[] processedSegs = processedSegDir.listFiles();
            if (processedSegs == null) return;
            for (File pf: processedSegs)
                Files.move(Paths.get(processedSegDir + "/" + pf.getName()), Paths.get(unprocessedSegDir + "/" + pf.getName()));
        } catch (IOException e) {
            System.err.println("User " + id + " - initDefaults IOERROR: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("User " + id + " - initDefaults ERROR: " + e.getMessage());
        }
    }

    public void clearLeaderboards() {
        if (leaderboards.get(id) != null)
            leaderboards.get(id).clear();
    }

}