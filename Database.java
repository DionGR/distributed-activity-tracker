import java.util.HashMap;


public class Database {
    private final HashMap<Integer, User> users;
    private final Statistics totalData;

    public Database(){
        users = new HashMap<>();
        totalData = new Statistics();
    }

    public User initUser(int id){
        if (users.containsKey(id)) return users.get(id);
        users.put(id, new User(id));
        return users.get(id);
    }

    public void addRoute(Route route, int userID){
        /* Find the relevant user */
        User user = users.get(userID);

        boolean flag = user.getStatistics().getSubmissions() == 0;

        /* Add route and update statistics */
        user.addRoute(route);
        totalData.update(flag, route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());
    }

    public Statistics getTotalData() {
            return totalData;
    }
}

