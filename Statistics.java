import java.io.Serializable;

public class Statistics implements Serializable {
    public double totalDistance, totalTime, totalElevation;
    private int submissions;

    public Statistics(){
        this.totalDistance = 0;
        this.totalTime = 0;
        this.totalElevation = 0;
        this.submissions = 0;
    }


    public void update(boolean flag, double distance, double time, double elevation){
        if(flag)
            submissions++;
        totalDistance += distance;
        totalTime += time;
        totalElevation += elevation;
    }
    
    public double getAvgDistance() { return totalDistance / submissions; }

    public double getAvgTime(){
        return totalTime / submissions;
    }

    public double getAvgElevation(){
        return totalElevation / submissions;
    }

    public double getTotalDistance(){
        return totalDistance;
    }

    public double getTotalTime(){
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

