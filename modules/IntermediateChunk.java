package modules;

import java.io.Serializable;
import java.util.Date;

/* IntermediateChunk Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class represents an intermediate chunk of data, used to store the data of a part of the route after Mapping.
 */


public class IntermediateChunk implements Serializable {
    private final int userID;
    private final int totalChunks;
    private final double totalDistance;
    private final double meanVelocity;
    private final double totalElevation;
    private final long totalTime;
    private final Date date;

    public IntermediateChunk(int userID, int totalChunks, double totalDistance, double meanVelocity, double totalElevation, long totalTime, Date date){
        this.userID = userID;
        this.totalChunks = totalChunks;
        this.totalDistance = totalDistance;
        this.meanVelocity = meanVelocity;
        this.totalElevation = totalElevation;
        this.totalTime = totalTime;
        this.date = date;
    }

    public int getUserID() {
        return userID;
    }

    public int getTotalChunks() { return totalChunks; }

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

    public Date getDate() {
        return date;
    }

    @Override
    public String toString(){
        return String.format("[User #%d][%s] Total Distance: %5.2f km | Total Time: %5.2f min | Mean Velocity: %5.2f km/h | Total Elevation: %5.2f m", userID, date, totalDistance, (double) totalTime/1000/60, meanVelocity*1000*60*60, totalElevation);
    }
}

