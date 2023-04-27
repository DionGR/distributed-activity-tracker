import java.io.Serializable;

public class Statistics implements Serializable {
    private final double avgDistance, avgTime, avgElevation;
    public double totalDistance, totalTime, totalElevation;

    //private double generalAvgDistance, generalAvgTime, generalAvgElevation;
    public final double meanTotalDistance, meanTotalTime, meanTotalElevation;

    public Statistics(double avgDistance, double avgTime, double avgElevation,
                      double totalDistance, double totalTime, double totalElevation,
                      double meanTotalDistance, double meanTotalTime, double meanTotalElevation){
        this.avgDistance = avgDistance;
        this.avgTime = avgTime;
        this.avgElevation = avgElevation;
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
        this.totalElevation = totalElevation;
        this.meanTotalDistance = meanTotalDistance;
        this.meanTotalTime = meanTotalTime;
        this.meanTotalElevation = meanTotalElevation;
    }
    
public double getAvgDistance(){
        return avgDistance;
    }

    public double getAvgTime(){
        return avgTime;
    }

    public double getAvgElevation(){
        return avgElevation;
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

    public double getMeanTotalDistance(){
        return meanTotalDistance;
    }

    public double getMeanTotalTime(){
        return meanTotalTime;
    }

    public double getMeanTotalElevation(){
        return meanTotalElevation;
    }

    @Override
    public String toString(){
        return String.format("Total Distance: %5.2f km | Total Time: %5.2f min | Mean Velocity: %5.2f km/h | Total Elevation: %5.2f m", totalDistance, (double) totalTime/1000/60, avgDistance*1000*60*60, totalElevation);
    }


}
