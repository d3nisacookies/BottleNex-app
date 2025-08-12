package com.example.bottlenex.ml;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.example.bottlenex.R;
import java.util.ArrayList;
import java.util.List;

public class TrafficCongestionGraphsActivity extends AppCompatActivity {
    
    private LineChart lineChart;
    private BarChart barChart;
    private TextView tvTitle;
    private Button btnSwitchView;
    private boolean isLineChartVisible = true;
    
    // Use centralized Singapore traffic data
    private final String[] timeSlots = SingaporeTrafficData.TIME_SLOTS;
    private final float[] congestionLevels = SingaporeTrafficData.CONGESTION_LEVELS;
    private final float[] averageSpeed = SingaporeTrafficData.AVERAGE_SPEEDS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_congestion_graphs);
        
        initializeViews();
        setupCharts();
        setupClickListeners();
        showLineChart(); // Start with line chart
    }
    
    private void initializeViews() {
        lineChart = findViewById(R.id.lineChart);
        barChart = findViewById(R.id.barChart);
        tvTitle = findViewById(R.id.tvTitle);
        btnSwitchView = findViewById(R.id.btnSwitchView);
        
        tvTitle.setText("Singapore Traffic Congestion Analysis");
        btnSwitchView.setText("Switch to Bar Chart");
    }
    
    private void setupCharts() {
        // Setup line chart
        setupLineChart();
        
        // Setup bar chart
        setupBarChart();
        
        // Initially hide bar chart
        barChart.setVisibility(View.GONE);
    }
    
    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(Color.WHITE);
        
        // X-axis setup
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < timeSlots.length) {
                    return timeSlots[index];
                }
                return "";
            }
        });
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(true);
        
        // Y-axis setup
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1f);
        
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Legend
        lineChart.getLegend().setEnabled(true);
        lineChart.getLegend().setTextColor(Color.BLACK);
        
        // Data
        List<Entry> congestionEntries = new ArrayList<>();
        List<Entry> speedEntries = new ArrayList<>();
        
        for (int i = 0; i < timeSlots.length; i++) {
            congestionEntries.add(new Entry(i, congestionLevels[i]));
            speedEntries.add(new Entry(i, averageSpeed[i] / 50f)); // Normalize speed to 0-1 range
        }
        
        LineDataSet congestionDataSet = new LineDataSet(congestionEntries, "Congestion Level");
        congestionDataSet.setColor(Color.RED);
        congestionDataSet.setCircleColor(Color.RED);
        congestionDataSet.setLineWidth(3f);
        congestionDataSet.setCircleRadius(5f);
        congestionDataSet.setDrawValues(false);
        
        LineDataSet speedDataSet = new LineDataSet(speedEntries, "Average Speed (normalized)");
        speedDataSet.setColor(Color.BLUE);
        speedDataSet.setCircleColor(Color.BLUE);
        speedDataSet.setLineWidth(3f);
        speedDataSet.setCircleRadius(5f);
        speedDataSet.setDrawValues(false);
        
        LineData lineData = new LineData(congestionDataSet, speedDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }
    
    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
        barChart.setPinchZoom(true);
        barChart.setBackgroundColor(Color.WHITE);
        
        // X-axis setup
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < timeSlots.length) {
                    return timeSlots[index];
                }
                return "";
            }
        });
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(true);
        
        // Y-axis setup
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1f);
        
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Legend
        barChart.getLegend().setEnabled(true);
        barChart.getLegend().setTextColor(Color.BLACK);
        
        // Data
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < timeSlots.length; i++) {
            entries.add(new BarEntry(i, congestionLevels[i]));
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Congestion Level");
        dataSet.setColor(Color.parseColor("#FF6B6B"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.invalidate();
    }
    
    private void setupClickListeners() {
        btnSwitchView.setOnClickListener(v -> {
            if (isLineChartVisible) {
                showBarChart();
            } else {
                showLineChart();
            }
        });
    }
    
    private void showLineChart() {
        lineChart.setVisibility(View.VISIBLE);
        barChart.setVisibility(View.GONE);
        btnSwitchView.setText("Switch to Bar Chart");
        isLineChartVisible = true;
    }
    
    private void showBarChart() {
        lineChart.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);
        btnSwitchView.setText("Switch to Line Chart");
        isLineChartVisible = false;
    }
}
