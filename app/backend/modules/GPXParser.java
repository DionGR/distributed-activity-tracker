package app.backend.modules;

import java.util.Date;
import java.util.ArrayList;

/* GPXParser Class
 *
 * @authors: P3200098, P3200150, P3200262
 * @info: Made for the course of Distributed Systems @ Spring/Summer AUEB 2022-2023
 *
 * This class is used to parse a GPX file and extract the waypoints from it.
 */

public class GPXParser {

    public static ArrayList<Waypoint> parse(StringBuilder buffer) {
        ArrayList<Waypoint> waypoints = new ArrayList<>();

        // Remove header
        buffer.delete(0, buffer.indexOf("<wpt"));
        // Remove footer
        buffer.delete(buffer.indexOf("</gpx>"), buffer.length());

        // Split the GPX file into a list of strings, each string representing a waypoint
        String[] waypointsAsStrings = buffer.toString().split("</wpt>");

        int id = 0;

        // Extract the data from each waypoint
        for (String strWaypoint: waypointsAsStrings){
            double latitude = Double.parseDouble(strWaypoint.substring(strWaypoint.indexOf("lat=") + 5, strWaypoint.indexOf("lon=") - 2));
            double longitude = Double.parseDouble(strWaypoint.substring(strWaypoint.indexOf("lon=") + 5, strWaypoint.indexOf("<ele>") - 6));
            double elevation =  Double.parseDouble(strWaypoint.substring(strWaypoint.indexOf("<ele>") + 5, strWaypoint.indexOf("</ele>")));

            // Extract the date and time from the waypoint
            String time = strWaypoint.substring(strWaypoint.indexOf("<time>") + 6, strWaypoint.indexOf("</time>"));
            String[] timeSplit = time.split("T");
            String[] dateSplit = timeSplit[0].split("-");
            String[] timeSplit2 = timeSplit[1].split("Z");
            String[] timeSplit3 = timeSplit2[0].split(":");

            // Convert the date and time into the correct format\
            Date date = new Date(Integer.parseInt(dateSplit[0]) - 1900, Integer.parseInt(dateSplit[1]) - 1, Integer.parseInt(dateSplit[2]), Integer.parseInt(timeSplit3[0]), Integer.parseInt(timeSplit3[1]), Integer.parseInt(timeSplit3[2]));

            // Create a new Waypoint object and add it to the list of waypoints
            waypoints.add(new Waypoint(id, latitude, longitude, elevation, date));
            id++;
        }

        return waypoints;
    }
}
