package app.backend.modules;

import java.util.ArrayList;
import java.util.HashMap;

public class Segment {
    private final int segmentID;
    private final ArrayList<Waypoint> waypoints;
    private final HashMap<String, IntermediateChunk> leaderboard; //key: userID

    public Segment(int segmentID, ArrayList<Waypoint> waypoints){
        this.segmentID = segmentID;
        this.waypoints = waypoints;
        this.leaderboard = new HashMap<>();
    }

    public void addIntermediateChunk(IntermediateChunk chunk) {
        String userid = chunk.getUserID();
        IntermediateChunk previousResult = leaderboard.get(userid);
        if (previousResult != null){
            if (previousResult.compareTo(chunk) > 0)
                leaderboard.put(userid, chunk);
        }
        else {
            leaderboard.put(userid,chunk);
        }
    }

    public ArrayList<Waypoint> getWaypoints() {
        return waypoints;
    }

    public Integer getSegmentID() { return segmentID; }

    public boolean equals(Segment segment) {
        if (segment == null) return false;
        if (segment == this) return true;
        return this.waypoints.equals(segment.getWaypoints());
    }

    public  HashMap<String, IntermediateChunk> getLeaderboard(){ return leaderboard; }
}