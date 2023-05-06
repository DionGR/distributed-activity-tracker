import java.util.ArrayList;
import java.util.Date;

/* Route Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class is used to store a route and its statistics.
 */


public class Route {
    private final int routeID;
    private final Statistics routeStatistics;
    private final double meanVelocity;
    private final Date date;
    private final ArrayList<Waypoint> waypoints;

    public Route(int routeID, Date date, ArrayList<Waypoint> waypoints, double totalDistance, long totalTime, double meanVelocity, double totalElevation) {
        this.routeID = routeID;
        this.date = date;
        this.waypoints = waypoints;
        this.routeStatistics = new Statistics(totalDistance, totalTime, totalElevation);
        this.meanVelocity = meanVelocity;
    }

    public int getRouteID() {
        return routeID;
    }

    public Date getDate() {
        return date;
    }

    public double getTotalDistance() {
        return routeStatistics.getTotalDistance();
    }

    public long getTotalTime() {
        return routeStatistics.getTotalTime();
    }

    public double getMeanVelocity() {
        return meanVelocity;
    }

    public double getTotalElevation() {
        return routeStatistics.getTotalElevation();
    }

    public Date getDate() {
        return date;
    }

    public ArrayList<Waypoint> getWaypoints() {
        return waypoints;
    }

    @Override
    public String toString() {
        return String.format("[Route #%d][%s] Total Distance: %05.2f km | Total Time: %05.2f min | Mean Velocity: %05.2f km/h | Total Elevation: %05.2f m", routeID, date, getTotalDistance(), (double) getTotalTime()/1000/60, meanVelocity, getTotalElevation());
    }
}
