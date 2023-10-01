package app.frontend.app.src.main.java.app.backend.modules;

import android.graphics.Color;
//import android.support.v7.app.ActionBarActivity;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.util.ArrayList;


public class Chart {
    //BarChart barChart;

    private BarData barData;
    double userData, totalData;
    double difference;

    public Chart(double userData, double totalData) {
        this.userData = userData; this.totalData = totalData;
        this.difference = this.userData * 100 / this.totalData - 100;
        createChart();
    }

    private void createChart() {
        BarDataSet userDataSet = new BarDataSet(getDataSet(userData, 1), "You");
        BarDataSet totalDataSet = new BarDataSet(getDataSet(totalData, 2), "Others");
        userDataSet.setColor(Color.rgb(228, 104, 52));
        totalDataSet.setColor(Color.rgb(112, 35, 73));

        barData = new BarData((IBarDataSet) userDataSet, (IBarDataSet) totalDataSet);

    }
    private ArrayList<BarEntry> getDataSet(double data, int xPos) {
        ArrayList<BarEntry> valueSet = new ArrayList<>();

        BarEntry entry = new BarEntry(xPos, (float) data);
        valueSet.add(entry);

        return valueSet;
    }

    public BarData getBarData() { return barData; }

    public double getDifference() { return difference;}

//    private ArrayList<String> getXAxisValues() {
//        ArrayList<String> xAxis = new ArrayList<>();
//        xAxis.add("User stats");
//        xAxis.add("Total stats");
//        return xAxis;
//    }


}
