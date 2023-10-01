package app.frontend.app.src.main.java.app.backend.modules;

import android.annotation.SuppressLint;

import app.R;
import app.backend.modules.IntermediateChunk;

public class GPXResult {

    private final String date, distance, time, elevation, speed;


    @SuppressLint("DefaultLocale")
    public GPXResult(IntermediateChunk result) {
        this.date = result.getDate().toString().substring(0, 19);
        this.distance = String.format("%.2f km", result.getTotalDistance());
        this.time = String.format("%.2f min", (double) result.getTotalTime() / 1000 / 60);
        this.speed = String.format("%.2f km/h", result.getMeanVelocity() * 1000 * 60 * 60);
        this.elevation = String.format("%.2f m", result.getTotalElevation());
    }


    public String getDate() {
        return date;
    }

    public String getDistance() {
        return distance;
    }

    public String getTime() {
        return time;
    }

    public String getElevation() {
        return elevation;
    }

    public String getSpeed() {
        return speed;
    }

    public int getImage() {
        return R.drawable.gpx_img;
    }

}
