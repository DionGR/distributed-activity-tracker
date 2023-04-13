
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/* Reads the GPX file and converts it into a list of Waypoints */

public class GPXParser {
    StringBuilder buffer;

    public GPXParser(StringBuilder buffer) {
        this.buffer = buffer;
    }

    public ArrayList<Waypoint> parse() {
        List<String> waypointDelims = Arrays.asList("<wpt", "</wpt>");
        StringBuilder waypointBuffer = new StringBuilder();
        String buffer = this.buffer.toString();
        waypointBuffer.append("[");

        ArrayList<Waypoint> waypoints = new ArrayList<>();

        waypointDelims.forEach(delimiter -> waypointBuffer.append(delimiter).append("!"));
        waypointBuffer.append("]");

        String[] waypointStrings = waypointBuffer.toString().split("!");

        System.out.println(Arrays.toString(waypointStrings));


        return waypoints;


        //String[] waypointsAsStrings = buffer.toString().split("</wpt>");


    }
}
