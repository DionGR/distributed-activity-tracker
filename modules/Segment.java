package modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

public class Segment {
    private final int segmentID;
    private final ArrayList<Waypoint> waypoints;
    private final HashMap<Integer, IntermediateChunk> leaderboard;

    public Segment(ArrayList<Waypoint> waypoints){
        this.waypoints = waypoints;
        this.leaderboard = new TreeMap<>()
    }

    public void addIntermediateChunk(IntermediateChunk chunk){
        int userid = chunk.getUserID();
        IntermediateChunk previousResult = leaderboard.get(userid);
        if(previousResult != null)
            if(previousResult.compareTo(chunk) > 0) leaderboard.put(userid, chunk);
        else leaderboard.put(userid,chunk);
    }

    public ArrayList<Waypoint> getWaypoints() {
        return waypoints;
    }

    public Integer getSegmentID() { return segmentID; }
}