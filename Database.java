import java.util.HashMap;

public class Database {
    private final HashMap<Integer, Node> userData;
    private final Node totalData;

    public Database(){
        userData = new HashMap<>();
        totalData = new Node();
    }

    public void initUser(int id){
        userData.put(id, new Node());
    }

    public void addData(int id, double distance, double time, double elevation){
        userData.get(id).addData(distance, time, elevation);
        totalData.addData(distance, time, elevation);
    }

    public String getUserData(int id){
        Node node;
        node = userData.get(id);
        return "User: " + id + " Average Distance: " + node.avgDistance + " Average Time: " + node.avgTime + " Average Elevation: " + node.avgElevation;
    }

    public String getTotalData(){
        return "Total Average Distance: " + totalData.avgDistance + " Total Average Time: " + totalData.avgTime + " Total Average Elevation: " + totalData.avgElevation;
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

        public void addData(double distance, double time, double elevation){
            submissions++;
            avgDistance = ((submissions-1)*avgDistance + distance)/submissions;
            avgTime = ((submissions-1)*avgTime + time)/submissions;
            avgElevation = ((submissions-1)*avgElevation + elevation)/submissions;
        }
    }
}
