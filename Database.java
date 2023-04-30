import java.util.ArrayList;
import java.util.HashMap;


public class Database {
    private final HashMap<Integer, User> users;
    private final ArrayList<Segment> segments;
    private Statistics totalData;


    public Database(){
        users = new HashMap<>();
        segments = new ArrayList<>();
        totalData = new Statistics();
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
        totalData.update(flag, route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());

    }

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

