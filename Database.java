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

    public void initSegment(int id, ArrayList<Waypoint> waypoints) {
        segments.add(new Segment(id, waypoints));
    }

    public synchronized void updateSegmentStats(User user, IntermediateChunk intermediateChunk) {
        Segment segment = segments.get(intermediateChunk.getGPXID()); //segment identifier (index in arraylist)!!!!!!!!
        segment.updateLeaderboard(user, intermediateChunk);

        //TODO: Update segment statistics and leaderboard (see reduce in Master - UserGPXBroker)
        //TODO: How to wrap statistics? Statistics object??

        //segment.updateLeaderboard(user, );
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

    private class Segment {
        private final int segmentID;
        private final ArrayList<Waypoint> waypoints;
        private final HashMap<User, Statistics> leaderboard;

        public Segment(int segmentID, ArrayList<Waypoint> waypoints) {
            this.segmentID = segmentID;
            this.waypoints = waypoints;
            this.leaderboard = new HashMap<>();
        }

        /* Set Getters */
        public int getSegmentID() {
            return segmentID;
        }

        public ArrayList<Waypoint> getWaypoints() {
            return waypoints;
        }

        public HashMap<User, Statistics> getLeaderboard() {
            return leaderboard;
        }

        public void addLeaderboard(User user, Statistics statistics) {
            leaderboard.put(user, statistics);
        }

        public void removeLeaderboard(User user) {
            leaderboard.remove(user);
        }

        public void updateLeaderboard(User user, IntermediateChunk intermediateChunk) {

            if(!leaderboard.containsKey(user)){
                leaderboard.put(user, new Statistics());
            }

            //leaderboard.replace(user, statistics);
        }
    }
}

