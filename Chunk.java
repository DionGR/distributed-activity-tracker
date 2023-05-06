import java.io.Serializable;
import java.util.ArrayList;

/* Chunk Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class is used to store a chunk of waypoints.
 */


public class Chunk implements Serializable {
    private final int userID;
    private final int chunkID;
    private final int totalChunks;
    private final ArrayList<Waypoint> data;

    Chunk(int userID, int chunkID, int totalChunks, ArrayList<Waypoint> data){
        this.userID = userID;
        this.chunkID = chunkID;
        this.totalChunks = totalChunks;
        this.data = data;
    }

    public int getUserID() {
        return userID;
    }

    public int getChunkID() {
        return chunkID;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public ArrayList<Waypoint> getData() {
        return data;
    }

    @Override
    public String toString(){
        return "Chunk: " + data;
    }
}
