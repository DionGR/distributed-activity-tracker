/*import java.util.HashMap;


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
        return "Total Average Distance: " + totalData.getStatistics().getAvgDistance() + " Total Average Time: " + totalData.getStatistics().getAvgTime() + " Total Average Elevation: " + totalData.getStatistics().getAvgElevation();
    }
}*/

import java.util.HashMap;


public class Database {
    private final HashMap<Integer, User> users;
    //private final User totalData;
    //private double sumOfTotals_distance, sumOfTotals_time, sumOfTotals_elevation; //sumOfTotals value refers to all users
    private Statistics totalData;
//    private int usersSubmitted;

    public Database(){
        users = new HashMap<>();
        //totalData = new User(0);
        totalData = new Statistics();
//        usersSubmitted = 0;
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
        //totalData.updateUserStatistics(route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());

        /* # of users that have made submissions is updated only if needed (flag) */

        totalData.update(flag, route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());

    }

//    public double getAvgOfTotals_distance() {
//        return sumOfTotals_distance / usersSubmitted;
//    }
//
//    public double getAvgOfTotals_time() {
//        return sumOfTotals_time / usersSubmitted;
//    }
//
//    public double getAvgOfTotals_elevation() {
//        return sumOfTotals_elevation / usersSubmitted;
//    }



        public Statistics getTotalData() {
            return totalData;
    }

//    public String totalDataToString(){
//        return "Total Average Distance: " + totalData.getAvgDistance() + " Total Average Time: " + totalData.getAvgTime() + " Total Average Elevation: " + totalData.getAvgElevation();
//    }
}

