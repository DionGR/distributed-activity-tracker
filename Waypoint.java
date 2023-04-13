import java.io.Serializable;
import java.sql.Time;

public class Waypoint implements Serializable{
    private final int id;
    private final double latitude;
    private final double longitude;
    private final double elevation;
    private final Time time;

    public Waypoint(int id, double latitude, double longitude, double elevation, Time time) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.time = time;
    }

    public Time getTime() {
        return time;
    }

    public int getID() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getElevation() {
        return elevation;
    }

    public String toString(){
        return "Waypoint: " + id + " " + latitude + " " + longitude + " " + elevation;
    }
}
