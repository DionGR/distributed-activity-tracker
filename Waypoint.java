import java.io.Serializable;
import java.sql.Time;
import java.sql.Date;

public class Waypoint implements Serializable{
    private final int id;
    private final double latitude;
    private final double  longitude;
    private final double  elevation;
    private final Date date;
    private final Time time;

    public Waypoint(int id, double  latitude, double  longitude, double  elevation, Date date, Time time) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.date = date;
        this.time = time;
    }


    public Waypoint(Waypoint waypoint) {
        this.id = 2*waypoint.id;
        this.latitude = waypoint.latitude;
        this.longitude = waypoint.longitude;
        this.elevation = waypoint.elevation;
        this.date = waypoint.date;
        this.time = waypoint.time;
    }

    public Time getTime() {
        return time;
    }

    public Date getDate() {
        return date;
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

    public String toString(){
        return "Waypoint: " + id + " " + latitude + " " + longitude + " " + elevation;
    }
}
