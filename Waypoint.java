import java.io.Serializable;
import java.util.Date;

public class Waypoint implements Serializable {
    private final int id;
    private final double latitude;
    private final double longitude;
    private final double elevation;
    private final Date date;

    public Waypoint(int id, double  latitude, double  longitude, double  elevation, Date date) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.date = date;
    }


    public Waypoint(Waypoint waypoint) {
        this.id = waypoint.id;
        this.latitude = waypoint.latitude;
        this.longitude = waypoint.longitude;
        this.elevation = waypoint.elevation;
        this.date = waypoint.date;
    }

    public int getID() {
        return id;
    }

    public double  getLatitude() {
        return latitude;
    }

    public double  getLongitude() {
        return longitude;
    }

    public double  getElevation() {
        return elevation;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString(){
        return "Waypoint: " + id + " " + latitude + " " + longitude + " " + elevation;
    }
}
