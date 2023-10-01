package app.frontend.app.src.main.java.app.frontend.fragments;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;

import java.util.ArrayList;

import app.R;
import app.backend.AppBackend;
import app.backend.modules.Chart;

public class GpxStatisticsFragment extends Fragment {

    AppBackend appBackend;
    Button requestStatisticsBtn;
    BarChart timeChart, distanceChart, elevationChart;
    TextView timeDiff, distanceDiff, elevationDiff;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_gpx_statistics, container, false);
        appBackend = AppBackend.getInstance();
        requestStatisticsBtn = (Button) rootView.findViewById(R.id.requestStatisticsBtn);

        timeDiff = (TextView) rootView.findViewById(R.id.timeDiff);
        distanceDiff = (TextView) rootView.findViewById(R.id.distanceDiff);
        elevationDiff = (TextView) rootView.findViewById(R.id.elevationDiff);

        timeChart = (BarChart) rootView.findViewById(R.id.timeChart);
        distanceChart = (BarChart) rootView.findViewById(R.id.distanceChart);
        elevationChart = (BarChart) rootView.findViewById(R.id.elevationChart);
        requestStatisticsBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.e("STATS", "BEF TASK");

                @SuppressLint("StaticFieldLeak")
                AsyncTask<Void, Void, ArrayList<Chart>> task = new AsyncTask<Void, Void, ArrayList<Chart>>() {
                    @Override
                    protected ArrayList<Chart> doInBackground(Void... voids) {
                        Log.e("STATS", "BEF METHOD");

                        return appBackend.requestStatistics();
                    }

                    @SuppressLint("DefaultLocale")
                    @Override
                    protected void onPostExecute(ArrayList<Chart> charts) {
                        //super.onPostExecute(barData);
                        //Log.e("STATS", "Statistics fetched");

                        // Arrange bar data and user/total differences in corresponding data structures
                        ArrayList<BarData> barData = new ArrayList<>();
                        ArrayList<Double> diffData = new ArrayList<>();

                        for(int i=0; i<charts.size(); i++){
                            barData.add(charts.get(i).getBarData());
                            diffData.add(charts.get(i).getDifference());
                        }

                        // Apply settings for final view
                        timeDiff.setText(String.format("Your relative performance: %s%.2f%%", (diffData.get(0) >= 0 ? "+" : ""), diffData.get(0)));
                        distanceDiff.setText(String.format("Your relative performance: %s%.2f%%", (diffData.get(1) >= 0 ? "+" : ""), diffData.get(1)));
                        elevationDiff.setText(String.format("Your relative performance: %s%.2f%%", (diffData.get(2) >= 0 ? "+" : ""), diffData.get(2)));


                        for (BarData bData : barData) {
                            bData.setBarWidth(0.3f);
                        }

                        Description timeChartDesc = new Description();
                        timeChartDesc.setText("Time");
                        timeChart.setData(barData.get(0));
                        timeChart.setDescription(timeChartDesc);
                        timeChart.animateXY(1000, 1000);
                        timeChart.invalidate();

                        Description distanceChartDesc = new Description();
                        distanceChartDesc.setText("Distance");
                        distanceChart.setData(barData.get(1));
                        distanceChart.setDescription(distanceChartDesc);
                        distanceChart.animateXY(2000, 2000);
                        distanceChart.invalidate();

                        Description elevationChartDesc = new Description();
                        elevationChartDesc.setText("Elevation");
                        elevationChart.setData(barData.get(2));
                        elevationChart.setDescription(elevationChartDesc);
                        elevationChart.animateXY(3000, 3000);
                        elevationChart.invalidate();
                    }
                };
                task.execute();
            }
        });

        requestStatisticsBtn.callOnClick();

        return rootView;
    }

}

