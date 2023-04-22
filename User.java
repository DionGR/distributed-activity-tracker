import java.util.ArrayList;

public class User {
    private final int id;
    private double avgDistance;
    private double avgTime;
    private double avgElevation;
    private final ArrayList<Route> routes;


    public User(int id){
        this.id = id;
        routes = new ArrayList<>();
    }

    public void addRoute(Route route){
        routes.add(route);
        updateUserStatistics(route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());
    }

    public void updateUserStatistics(double distance, double time, double elevation){
        int submissions = getNumOfRoutes();
        avgDistance = ((submissions-1)*avgDistance + distance)/submissions;
        avgTime = ((submissions-1)*avgTime + time)/submissions;
        avgElevation = ((submissions-1)*avgElevation + elevation)/submissions;
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

    public int getNumOfRoutes() {
        return routes.size();
    }

    public ArrayList<Route> getRoutes(){
        return routes;
    }
}
