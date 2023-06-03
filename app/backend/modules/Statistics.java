package app.backend.modules;

import java.io.Serializable;

/* Statistics Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class represents a statistics object which can be used as a record or be updatable if needed.
 */

public class Statistics implements Serializable {
    private double totalDistance, totalElevation;
    private long totalTime;
    private int submissions;

    public Statistics(){
        this.totalTime = 0;
        this.totalDistance = 0;
        this.totalElevation = 0;
        this.submissions = 0;
    }

    public Statistics(double totalDistance, long totalTime, double totalElevation){
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
        this.totalElevation = totalElevation;
        this.submissions = 0;
    }

    public void update(boolean flag, double distance, double time, double elevation){
        if(flag) submissions++;
        totalDistance += distance;
        totalTime += time;
        totalElevation += elevation;
    }
    
    public double getAvgDistance() { return totalDistance / submissions; }

    public double getAvgTime(){
        return (double) totalTime / submissions;
    }

    public double getAvgElevation(){
        return totalElevation / submissions;
    }

    public double getTotalDistance(){
        return totalDistance;
    }

    public long getTotalTime(){
        return totalTime;
    }

    public double getTotalElevation(){
        return totalElevation;
    }

    public int getSubmissions() { return submissions; }

    @Override
    public String toString(){
        return String.format("Total Distance: %5.2f km | Total Time: %5.2f min | Mean Velocity: %5.2f km/h | Total Elevation: %5.2f m", totalDistance, (double) totalTime/1000/60, totalDistance*1000*60*60 / totalTime, totalElevation);
    }
}