import java.io.Serializable;
import java.util.Date;

/* Waypoint Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class represents a Waypoint object - each marker on the map is a Waypoint.
 */


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
