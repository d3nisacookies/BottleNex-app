package com.example.bottlenex.ml;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.example.bottlenex.R;
import java.util.ArrayList;
import java.util.List;

public class TrafficHeatmapsActivity extends AppCompatActivity {
    
    private BarChart barChart;
    private TextView tvTitle;
    private TextView tvDescription;
    private Button btnSwitchTime;
    private int currentTimeIndex = 0;
    
    // Use centralized Singapore traffic data
    private final String[] timePeriods = SingaporeTrafficData.TIME_PERIODS;
    private final String[] regions = SingaporeTrafficData.REGIONS;
    private final float[][][] trafficData = SingaporeTrafficData.TRAFFIC_DENSITY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_heatmaps);
        
        initializeViews();
        setupBarChart();
        setupClickListeners();
        updateBarChart();
    }
    
    private void initializeViews() {
        barChart = findViewById(R.id.heatmapChart);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        btnSwitchTime = findViewById(R.id.btnSwitchTime);
        
        tvTitle.setText("Singapore Traffic Density Bar Chart");
        tvDescription.setText("Historical traffic density across different regions and time periods");
        btnSwitchTime.setText("Next Time Period");
    }
    
    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
        barChart.setPinchZoom(true);
        barChart.setBackgroundColor(Color.WHITE);
        
        // X-axis setup (regions)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < regions.length) {
                    return regions[index];
                }
                return "";
            }
        });
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(true);
        xAxis.setLabelRotationAngle(45);
        
        // Y-axis setup (traffic density)
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.1f", value);
            }
        });
        
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Legend
        barChart.getLegend().setEnabled(true);
        barChart.getLegend().setTextColor(Color.BLACK);
    }
    
    private void setupClickListeners() {
        btnSwitchTime.setOnClickListener(v -> {
            currentTimeIndex = (currentTimeIndex + 1) % timePeriods.length;
            updateBarChart();
        });
    }
    
    private void updateBarChart() {
        // Update description
        tvDescription.setText("Current: " + timePeriods[currentTimeIndex]);
        
        // Create bar chart data - show average traffic density for each region
        List<BarEntry> entries = new ArrayList<>();
        
        for (int x = 0; x < regions.length; x++) {
            float totalDensity = 0f;
            // Calculate average density across all time slots for this region
            for (int y = 0; y < 3; y++) {
                totalDensity += trafficData[currentTimeIndex][y][x];
            }
            float averageDensity = totalDensity / 3f;
            entries.add(new BarEntry(x, averageDensity));
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Average Traffic Density");
        dataSet.setColor(Color.parseColor("#FF6B6B"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(true);
        
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.invalidate();
        
        // Update button text
        btnSwitchTime.setText("Next: " + timePeriods[(currentTimeIndex + 1) % timePeriods.length]);
    }
}
