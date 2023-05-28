package modules;

import java.util.ArrayList;
import java.util.HashMap;

/* Database Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class is used to store all the data of the users.
 */

public class Database {
    private final HashMap<Integer, User> users;
    private final Statistics totalData;
    private final ArrayList<Segment> segments;

    public Database(){
        users = new HashMap<>();
        totalData = new Statistics();
        segments = new ArrayList<>();
    }

    public User initUser(int id){
        if (users.containsKey(id)) return users.get(id);
        users.put(id, new User(id));
        return users.get(id);
    }

    public void addRoute(Route route, int userID){
        /* Find the relevant user */
        User user = users.get(userID);

        /* If this is the user's first GPX submission then the number of users (denominator of average statistics) is increased*/
        boolean hasSubmissions = user.getStatistics().getSubmissions() == 0;

        /* Add route and update statistics */
        user.addRoute(route);
        totalData.update(hasSubmissions, route.getTotalDistance(), route.getTotalTime(), route.getTotalElevation());
    }

    public void addSegment(Segment segment, int userID) {
        /* Find the relevant user */
        User user = users.get(userID);

        /* Add segment to database and user */
        segments.add(segment);
        user.addSegment(segment);
    }

    public Statistics getTotalData() {
        return totalData;
    }

    public ArrayList<Segment> getSegments() {
        return segments;
    }

}

