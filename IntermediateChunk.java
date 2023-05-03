import java.io.Serializable;


public class IntermediateChunk implements Serializable {
    private final int gpxID;
    private final int segmentID;
    private final int totalSegments;
    private final double totalDistance;
    private final double meanVelocity;
    private final double totalElevation;
    private final long totalTime;

    public IntermediateChunk(int gpxID, int segmentID, int totalSegments, double totalDistance, double meanVelocity, double totalElevation, long totalTime){
        this.gpxID = gpxID;
        this.segmentID = segmentID;
        this.totalSegments = totalSegments;
        this.totalDistance = totalDistance;
        this.meanVelocity = meanVelocity;
        this.totalElevation = totalElevation;
        this.totalTime = totalTime;
    }

    public int getGPXID() {
        return gpxID;
    }

    public int getSegmentID() { return segmentID; }

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


    @Override
    public String toString(){
        return String.format("GPX #%d: Total Distance: %5.2f km | Total Time: %5.2f min | Mean Velocity: %5.2f km/h | Total Elevation: %5.2f m", gpxID, totalDistance, (double) totalTime/1000/60, meanVelocity*1000*60*60, totalElevation);
    }
}
