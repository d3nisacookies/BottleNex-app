package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button btnOfflineMaps = findViewById(R.id.btnOfflineMaps);
        Button btnRouteHistory = findViewById(R.id.btnRouteHistory);
        Button btnFeedback = findViewById(R.id.btnFeedback);
        Button btnReportBug = findViewById(R.id.btnReportBug);

        btnOfflineMaps.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, OfflineMapsActivity.class));
        });

        btnRouteHistory.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RouteHistoryActivity.class));
        });

        btnFeedback.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FeedbackActivity.class));
        });

        btnReportBug.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, BugReportActivity.class));
        });
    }
}