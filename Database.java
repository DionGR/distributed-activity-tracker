import java.util.HashMap;

public class Database {
    private final HashMap<Integer, Node> userData;
    private final Node totalData;

    public Database(){
        userData = new HashMap<>();
        totalData = new Node();
    }

    public synchronized void initUser(int id){
        synchronized (userData) {
            userData.put(id, new Node());
        }
    }

    public synchronized void addData(int id, double distance, double time, double elevation){
        synchronized (totalData) {
            userData.get(id).addData(distance, time, elevation);
        }
        synchronized (userData) {
            totalData.addData(distance, time, elevation);
        }
    }

    public String getUserData(int id){
        Node node;
        synchronized (userData){
            node = userData.get(id);
        }
        return "User: " + id + " Average Distance: " + node.avgDistance + " Average Time: " + node.avgTime + " Average Elevation: " + node.avgElevation;
    }

    public String getTotalData(){
        synchronized (totalData){
            return "Total Average Distance: " + totalData.avgDistance + " Total Average Time: " + totalData.avgTime + " Total Average Elevation: " + totalData.avgElevation;
        }
    }

    private class Node{
        private double avgDistance;
        private double avgTime;
        private double avgElevation;
        private int submissions;

        Node(){
            avgDistance = 0;
            avgTime = 0;
            avgDistance = 0;
            submissions = 0;
        }

        public synchronized void addData(double distance, double time, double elevation){
            submissions++;
            avgDistance = ((submissions-1)*avgDistance + distance)/submissions;
            avgTime = ((submissions-1)*avgTime + time)/submissions;
            avgElevation = ((submissions-1)*avgElevation + elevation)/submissions;
        }
    }
}
