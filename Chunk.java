import java.io.Serializable;
import java.util.ArrayList;


class Chunk implements Serializable {
    private final int gpxID;
    private final int chunkID;
    private final int totalChunks;
    private final ArrayList<Waypoint> data;

    Chunk(int gpxID, int chunkID, int totalChunks, ArrayList<Waypoint> data){
        this.gpxID = gpxID;
        this.chunkID = chunkID;
        this.totalChunks = totalChunks;
        this.data = data;
    }

    public int getGPXID() {
        return gpxID;
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
