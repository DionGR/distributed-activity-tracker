package modules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Segment {
    private final int segmentID;
    private final ArrayList<Waypoint> waypoints;
    private final HashMap<Integer, IntermediateChunk> leaderboard; //key: userID

    public Segment(int segmentID, ArrayList<Waypoint> waypoints){
        this.segmentID = segmentID;
        this.waypoints = waypoints;
        this.leaderboard = new HashMap<>();
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

    public  HashMap<Integer, IntermediateChunk> getLeaderboard(){ return leaderboard; }
}