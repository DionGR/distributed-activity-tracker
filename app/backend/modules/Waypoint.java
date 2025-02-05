package app.backend.modules;

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
    public boolean equals(Object obj){
        if (obj == this) return true;
        if (!(obj instanceof Waypoint)) return false;
        Waypoint waypoint = (Waypoint) obj;
        return distance(latitude, waypoint.latitude, longitude, waypoint.longitude) <= 0.015;
    }

    public static double distance(double lat1, double lat2, double lon1, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    @Override
    public String toString(){
        return "Waypoint: " + id + " " + latitude + " " + longitude + " " + elevation;
    }
}
