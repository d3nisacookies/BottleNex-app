package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.example.bottlenex.ml.TrafficCongestionGraphsActivity;
import com.example.bottlenex.ml.TrafficHeatmapsActivity;

public class HistoricalTrafficDataActivity extends AppCompatActivity {
    private static final String TAG = "HistoricalTrafficData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_traffic_data);

        Log.d(TAG, "Historical Traffic Data Activity created");

        setupToolbar();
        setupButtons();
    }

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupButtons() {
        MaterialButton btnTrafficCongestionGraphs = findViewById(R.id.btnTrafficCongestionGraphs);
        MaterialButton btnTrafficHeatmaps = findViewById(R.id.btnTrafficHeatmaps);

        btnTrafficCongestionGraphs.setOnClickListener(v -> {
            Log.d(TAG, "Traffic Congestion Graphs clicked");
            Intent intent = new Intent(this, TrafficCongestionGraphsActivity.class);
            startActivity(intent);
        });

        btnTrafficHeatmaps.setOnClickListener(v -> {
            Log.d(TAG, "Traffic Heatmaps clicked");
            Intent intent = new Intent(this, TrafficHeatmapsActivity.class);
            startActivity(intent);
        });
    }
}
