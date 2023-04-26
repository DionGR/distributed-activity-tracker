import java.util.HashMap;



public class Database {
    private final HashMap<Integer, User> users;
    private final User totalData;

    public Database(){
        users = new HashMap<>();
        totalData = new User(0);
    }

    public User initUser(int id){
        if (users.containsKey(id)) return users.get(id);
        users.put(id, new User(id));
        return users.get(id);
    }

    public void addRoute(Route route, int userID){
        users.get(userID).addRoute(route);
        totalData.updateUserStatistics(route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());
    }

    public User getTotalData() {
        return totalData;
    }

    public String totalDataToString(){
        return "Total Average Distance: " + totalData.getAvgDistance() + " Total Average Time: " + totalData.getAvgTime() + " Total Average Elevation: " + totalData.getAvgElevation();
    }
}
