import java.io.Serializable;


public class Segment implements Serializable {
    private final int gpxID;
    private final int id;
    private final int totalSegments;
    private final double totalDistance;
    private final double meanVelocity;
    private final double totalElevation;
    private final long totalTime;

    public Segment(int gpxID, int id, int totalSegments, double totalDistance, double meanVelocity, double totalElevation, long totalTime){
        this.gpxID = gpxID;
        this.id = id;
        this.totalSegments =totalSegments;
        this.totalDistance = totalDistance;
        this.meanVelocity = meanVelocity;
        this.totalElevation = totalElevation;
        this.totalTime = totalTime;
    }


    public int getGPXID() {
        return gpxID;
    }

    public int getId() {
        return id;
    }

    public int getTotalSegments() { return totalSegments; }

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


    public String toString(){
        return "GPX: " + gpxID + " User: " + id + " Total Distance: " + totalDistance + " Mean Velocity: " + meanVelocity + " Total Elevation: " + totalElevation + " Total Time: " + totalTime;
    }

}

