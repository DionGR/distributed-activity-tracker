package app.backend.modules;

import java.util.ArrayList;
import java.util.HashMap;


/* User Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class is used to store a user's data.
 */


public class User {
    private final String id;
    private final Statistics statistics;
    private final ArrayList<Route> routes;
    private final HashMap<Integer, ArrayList<IntermediateChunk>> segmentStatistics;
    private final HashMap<Integer, String> segmentNames;

    public User(String id){
        this.id = id;
        this.statistics = new Statistics();
        this.routes = new ArrayList<>();
        this.segmentStatistics = new HashMap<>();
        this.segmentNames = new HashMap<>();
    }

    public void addRoute(Route route){
        routes.add(route);
        updateUserStatistics(route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());
    }

    public void initSegment(Segment segment, String segmentName) {
        segmentStatistics.put(segment.getSegmentID(), new ArrayList<>());
        segmentNames.put(segment.getSegmentID(), segmentName);
    }

    public void updateUserStatistics(double distance, double time, double elevation){
        statistics.update(true, distance, time, elevation);
    }

    /*foundSegments: segments found in user's GPX file
    * segmentStatistics: user's registered segments
    * updates registered only segments in hashmap
    * */
    public void updateSegmentHistory(ArrayList<IntermediateChunk> foundSegments){
        int segmentID;
        for(IntermediateChunk segment: foundSegments){
            segmentID = segment.getSegmentID();
            ArrayList<IntermediateChunk> segmentHistory = segmentStatistics.get(segmentID);
            if(segmentHistory != null){
                segmentStatistics.get(segmentID).add(segment); //i.e. User is interested in this segment (segment history exists for this segment)
            }
        }
    }

    public HashMap<Integer, ArrayList<IntermediateChunk>> getSegmentsStatistics(){
        return segmentStatistics;
    }

    public HashMap<Integer, String> getSegmentNames() { return segmentNames; }

    public String getID(){
        return id;
    }

    public ArrayList<Route> getRoutes(){
        return routes;
    }

    public Statistics getStatistics(){
        return statistics;
    }


}