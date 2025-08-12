package com.example.bottlenex;

//To commit

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bottlenex.ml.TrafficCongestionGraphsActivity;
import com.example.bottlenex.ml.TrafficHeatmapsActivity;

public class PersonalTools extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.personal_tools);

        Button btnOfflineMaps = findViewById(R.id.btnOfflineMaps);
        Button btnRouteHistory = findViewById(R.id.btnRouteHistory);
        Button btnFeedback = findViewById(R.id.btnFeedback);
        Button btnReportBug = findViewById(R.id.btnReportBug);

        btnOfflineMaps.setOnClickListener(v -> {
            startActivity(new Intent(PersonalTools.this, OfflineMapsActivity.class));
        });

        btnRouteHistory.setOnClickListener(v -> {
            startActivity(new Intent(PersonalTools.this, RouteHistoryActivity.class));
        });

        btnFeedback.setOnClickListener(v -> {
            startActivity(new Intent(PersonalTools.this, FeedbackActivity.class));
        });

        btnReportBug.setOnClickListener(v -> {
            startActivity(new Intent(PersonalTools.this, BugReportActivity.class));
        });

        Button btnAlertSettings = findViewById(R.id.btnAlertSettings);
        btnAlertSettings.setOnClickListener(v -> {
            startActivity(new Intent(PersonalTools.this, AlertSettingsActivity.class));
        });

        // Traffic Congestion Graphs button
        Button btnTrafficCongestionGraphs = findViewById(R.id.btnTrafficCongestionGraphs);
        btnTrafficCongestionGraphs.setOnClickListener(v -> {
            startActivity(new Intent(PersonalTools.this, TrafficCongestionGraphsActivity.class));
        });

        // Traffic Heatmaps button
        Button btnTrafficHeatmaps = findViewById(R.id.btnTrafficHeatmaps);
        btnTrafficHeatmaps.setOnClickListener(v -> {
            startActivity(new Intent(PersonalTools.this, TrafficHeatmapsActivity.class));
        });
    }
}