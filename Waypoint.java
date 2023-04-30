import java.io.Serializable;
import java.sql.Time;
import java.sql.Date;

public class Waypoint implements Serializable {
    private final int id;
    private final double latitude;
    private final double longitude;
    private final double elevation;
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
        this.id = waypoint.id;
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

    @Override
    public boolean equals(Object obj){
        if (obj == null) return false;
        if (!(obj instanceof Waypoint)) return false;
        if (obj == this) return true;
        Waypoint waypoint = (Waypoint) obj;
        System.out.println(distance(this.latitude, waypoint.latitude, this.longitude, waypoint.longitude));
        return distance(this.latitude, waypoint.latitude, this.longitude, waypoint.longitude) < 0.005;
    }

    @Override
    public int hashCode(){
        return (int) (latitude + longitude);
    }

    private double distance(double lat1, double lat2, double lon1, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.abs(R * c);
    }

    @Override
    public String toString(){
        return "Waypoint: " + id + " " + latitude + " " + longitude + " " + elevation;
    }
}
