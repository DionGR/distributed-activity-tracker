import java.util.HashMap;

public class Database {
    private final HashMap<Integer, UserData> userData;
    private final UserData totalData;

    public Database(){
        userData = new HashMap<>();
        totalData = new UserData();
    }

    public void initUser(int id){
        userData.put(id, new UserData());
    }

    public void addData(int id, double distance, double time, double elevation){
        userData.get(id).update(distance, time, elevation);
        totalData.update(distance, time, elevation);
    }

    public String getUserData(int id){
        UserData userData;
        userData = this.userData.get(id);
        return "User: " + id + " Average Distance: " + userData.avgDistance + " Average Time: " + userData.avgTime + " Average Elevation: " + userData.avgElevation;
    }

    public String getTotalData(){
        return "Total Average Distance: " + totalData.avgDistance + " Total Average Time: " + totalData.avgTime + " Total Average Elevation: " + totalData.avgElevation;
    }

    private class UserData {
        private double avgDistance;
        private double avgTime;
        private double avgElevation;
        private int submissions;

        UserData(){
            avgDistance = 0;
            avgTime = 0;
            avgDistance = 0;
            submissions = 0;
        }

        public void update(double distance, double time, double elevation){
            submissions++;
            avgDistance = ((submissions-1)*avgDistance + distance)/submissions;
            avgTime = ((submissions-1)*avgTime + time)/submissions;
            avgElevation = ((submissions-1)*avgElevation + elevation)/submissions;
        }
    }
}
