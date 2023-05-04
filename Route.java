import java.util.ArrayList;
import java.util.Date;


public class Route {
    private final int routeID;
    private final double totalDistance;
    private final double meanVelocity;
    private final double totalElevation;
    private final long totalTime;
    private final Date date;
    private final ArrayList<Waypoint> waypoints;

    public Route(int routeID, Date date, ArrayList<Waypoint> waypoints, double totalDistance, long totalTime, double meanVelocity, double totalElevation) {
        this.routeID = routeID;
        this.date = date;
        this.waypoints = waypoints;
        this.totalDistance = totalDistance;
        this.meanVelocity = meanVelocity;
        this.totalElevation = totalElevation;
        this.totalTime = totalTime;
    }

    public int getRouteID() {
        return routeID;
    }

    public Date getDate() {
        return date;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public double getMeanVelocity() {
        return meanVelocity;
    }

    public double getTotalElevation() {
        return totalElevation;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public ArrayList<Waypoint> getRoute() {
        return waypoints;
    }

    @Override
    public String toString() {
        return String.format("[Route #%d][%s] Total Distance: %05.2f km | Total Time: %05.2f min | Mean Velocity: %05.2f km/h | Total Elevation: %05.2f m", routeID, date, totalDistance, (double)totalTime/1000/60, meanVelocity*1000*60*60, totalElevation);
    }
}
