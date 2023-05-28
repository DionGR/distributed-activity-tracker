package modules;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

public class Segment {
    private final int segmentID;
    private final ArrayList<Waypoint> waypoints;
    private final TreeSet<IntermediateChunk> leaderboard;

    public Segment(ArrayList<Waypoint> waypoints){
        this.waypoints = waypoints;
        this.leaderboard = new TreeMap<>()
    }

    public void addIntermediateChunk(IntermediateChunk chunk){
        leaderboard.put(chunk.getUserID(), chunk);
    }

    public ArrayList<Waypoint> getWaypoints() {
        return waypoints;
    }
}