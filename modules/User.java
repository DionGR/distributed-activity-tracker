package modules;

import java.util.ArrayList;

/* User Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class is used to store a user's data.
 */


public class User {
    private final int id;
    private final Statistics statistics;
    private final ArrayList<Route> routes;
    private final ArrayList<Segment> segments;


    public User(int id){
        this.id = id;
        this.statistics = new Statistics();
        this.routes = new ArrayList<>();
        this.segments = new ArrayList<>();
    }

    public void addRoute(Route route){
        routes.add(route);
        updateUserStatistics(route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());
    }

    public void addSegment(Segment segment) {
        segments.add(segment);
    }

    public void updateUserStatistics(double distance, double time, double elevation){
        statistics.update(true, distance, time, elevation);
    }

    public int getID(){
        return id;
    }

    public ArrayList<Route> getRoutes(){
        return routes;
    }

    public ArrayList<Segment> getSegments() { return segments; }

    public Statistics getStatistics(){
        return statistics;
    }
}