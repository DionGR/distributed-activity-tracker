import java.io.Serializable;
import java.util.ArrayList;


class Chunk implements Serializable {
    private final long user;
    private final ArrayList<Waypoint> data;
    private final int id;

    Chunk(long user, ArrayList<Waypoint> data, int id){
        this.user = user;
        this.data = data;
        this.id = id;
    }

    public long getUser() {
        return user;
    }

    public ArrayList<Waypoint> getData() {
        return data;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString(){
        return "Chunk: " + data;
    }
}
