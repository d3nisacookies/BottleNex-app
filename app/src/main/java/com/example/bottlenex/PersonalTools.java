package com.example.bottlenex;

//To commit

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
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

        // Historical Traffic Data button
        Button btnHistoricalTrafficData = findViewById(R.id.btnHistoricalTrafficData);
        btnHistoricalTrafficData.setOnClickListener(v -> {
            UserTypeChecker.checkPremiumAccess(this, "Historical Traffic Data", () -> {
                startActivity(new Intent(PersonalTools.this, HistoricalTrafficDataActivity.class));
            });
        });
        
        setupBottomNavigation();
    }
    
    private void setupBottomNavigation() {
        ImageButton btnNavigation = findViewById(R.id.btnNavigation);
        ImageButton btnBookmark = findViewById(R.id.btnBookmark);
        ImageButton btnCar = findViewById(R.id.btnCar);
        ImageButton btnPersonalTools = findViewById(R.id.btnPersonalTools);
        
        btnNavigation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        
        btnBookmark.setOnClickListener(v -> {
            Intent intent = new Intent(this, Bookmark.class);
            startActivity(intent);
        });
        
        btnCar.setOnClickListener(v -> {
            UserTypeChecker.checkPremiumAccess(this, "Pre-Route Traffic Prediction", () -> {
                Intent intent = new Intent(this, PrePlannedRouteActivity.class);
                startActivity(intent);
            });
        });
        
        // btnPersonalTools - current page, no action needed
    }
}