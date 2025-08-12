package com.example.bottlenex.ml;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bottlenex.R;

public class TrafficHeatmapsActivity extends AppCompatActivity {
    
    private TrafficHeatmapView heatmapView;
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
        
        setupToolbar();
        initializeViews();
        setupHeatmap();
        setupClickListeners();
    }
    
    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void initializeViews() {
        heatmapView = findViewById(R.id.heatmapView);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        btnSwitchTime = findViewById(R.id.btnSwitchTime);
        
        tvTitle.setText("Historical Traffic Heatmaps");
        tvDescription.setText("Visual heatmap showing traffic density across Singapore regions and time periods");
        btnSwitchTime.setText("Next: " + timePeriods[currentTimeIndex]);
    }
    

    
    private void setupClickListeners() {
        btnSwitchTime.setOnClickListener(v -> {
            currentTimeIndex = (currentTimeIndex + 1) % timePeriods.length;
            updateHeatmapData();
        });
    }
    

    
    private void setupHeatmap() {
        // Initialize the heatmap with current time period data
        updateHeatmapData();
    }
    
    private void updateHeatmapData() {
        // Get data for current time period
        float[][] currentTimeData = trafficData[currentTimeIndex];
        
        // Transform data from [timeSlot][region] to [region][timeSlot]
        float[][] transformedData = new float[regions.length][currentTimeData.length];
        for (int timeSlot = 0; timeSlot < currentTimeData.length; timeSlot++) {
            for (int region = 0; region < regions.length && region < currentTimeData[timeSlot].length; region++) {
                transformedData[region][timeSlot] = currentTimeData[timeSlot][region];
            }
        }
        
        // Create time slot labels for this period
        String[] timeSlotLabels = new String[currentTimeData.length];
        for (int i = 0; i < timeSlotLabels.length; i++) {
            timeSlotLabels[i] = "Slot " + (i + 1);
        }
        
        // Set the heatmap data
        heatmapView.setHeatmapData(regions, timeSlotLabels, transformedData);
        
        // Update current time display
        tvDescription.setText("Current: " + timePeriods[currentTimeIndex]);
        btnSwitchTime.setText("Next: " + timePeriods[(currentTimeIndex + 1) % timePeriods.length]);
    }
}
