package com.example.bottlenex;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class OfflineMapsActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_maps);
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        initializeViews();
        setupMap();
        setupClickListeners();
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        tvStatus = findViewById(R.id.tvStatus);
    }

    private void setupMap() {
        // Show map immediately - this is an offline maps viewer
        mapView.setVisibility(View.VISIBLE);
        mapView.setUseDataConnection(false); // Only use offline tiles
        
        // Set initial zoom and center (Singapore area)
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(new GeoPoint(1.3521, 103.8198));
        
        // Enable map interaction
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        
        // Show offline message
        tvStatus.setText("Offline Maps - No internet required");
    }

    private void setupClickListeners() {
        // Setup toolbar with back button
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }





    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
    }
}