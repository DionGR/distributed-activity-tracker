import java.io.Serializable;


public class Segment implements Serializable {
    private final long user;
    private final int id;
    private final double totalDistance;
    private final double meanVelocity;
    private final double totalElevation;
    private final long totalTime;

    public Segment(long user, int id, double totalDistance, double meanVelocity, double totalElevation, long totalTime){
        this.user = user;
        this.id = id;
        this.totalDistance = totalDistance;
        this.meanVelocity = meanVelocity;
        this.totalElevation = totalElevation;
        this.totalTime = totalTime;
    }


    public long getUser() {
        return user;
    }

    public int getId() {
        return id;
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

    public String toString(){
        return "User: " + user + " Segment: " + id + " Total Distance: " + totalDistance + " Mean Velocity: " + meanVelocity + " Total Elevation: " + totalElevation + " Total Time: " + totalTime;
    }

}

