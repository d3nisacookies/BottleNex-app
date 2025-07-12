package com.example.bottlenex;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class OfflineMapsActivity extends AppCompatActivity {

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_maps);

        mapView = findViewById(R.id.mapView);

        // Get zoom level passed from MainActivity, default to 6.0 if none
        double zoomLevel = getIntent().getDoubleExtra("EXTRA_MAP_ZOOM_LEVEL", 6.0);

        mapView.getController().setZoom(zoomLevel);

        // Optionally set a center point, e.g., somewhere neutral
        mapView.getController().setCenter(new GeoPoint(0.0, 0.0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume(); // needed for compass, zoom etc
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}