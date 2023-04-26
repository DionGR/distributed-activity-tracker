import java.util.ArrayList;

public class User {
    private final int id;
    private int submissions;
    private double avgDistance, avgTime, avgElevation;
    public double totalDistance, totalTime, totalElevation;
    private final ArrayList<Route> routes;


    public User(int id){
        this.id = id;
        this.avgDistance = 0;
        this.avgTime = 0;
        this.avgElevation = 0;
        this.totalDistance = 0;
        this.totalTime = 0;
        this.totalElevation = 0;
        this.submissions = 0;
        routes = new ArrayList<>();
    }

    public void addRoute(Route route){
        routes.add(route);
        updateUserStatistics(route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());
    }

    public void updateUserStatistics(double distance, double time, double elevation){
        submissions++;
        totalDistance += distance;
        totalTime += time;
        totalElevation += elevation;
        avgDistance = ((submissions - 1)*avgDistance + distance)/submissions;
        avgTime = ((submissions - 1)*avgTime + time)/submissions;
        avgElevation = ((submissions - 1)*avgElevation + elevation)/submissions;
    }

    public int getID(){
        return id;
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

    public int getSubmissions() {
        return submissions;
    }

    public ArrayList<Route> getRoutes(){
        return routes;
    }
}
