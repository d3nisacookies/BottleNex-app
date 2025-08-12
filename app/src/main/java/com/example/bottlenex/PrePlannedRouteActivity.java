package com.example.bottlenex;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bottlenex.map.MapManager;
import com.example.bottlenex.ml.TensorFlowTrafficPredictor;
import com.example.bottlenex.routing.RoutePlanner;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PrePlannedRouteActivity extends AppCompatActivity {

    private EditText etStartLocation, etDestination;
    private MaterialButton btnPlanRoute, btnTrafficOverlay, btnTrafficAnalysis;
    private MapView mapView;
    private TextView tvRouteInfo;
    private LinearLayout routeInfoSection;

    private MapManager mapManager;
    private RoutePlanner.RouteData currentRouteData;
    private GeoPoint startLocation, destinationLocation;
    private TensorFlowTrafficPredictor mlPredictor;
    private Calendar selectedTrafficTime;
    private boolean showTrafficOverlay = false;
    private String originalRouteInfo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_planned_route);

        // Initialize OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());

        initializeViews();
        setupToolbar();
        setupMapView();
        setupClickListeners();

        // Initialize ML predictor
        mlPredictor = new TensorFlowTrafficPredictor(this);
    }

    private void initializeViews() {
        etStartLocation = findViewById(R.id.etStartLocation);
        etDestination = findViewById(R.id.etDestination);
        btnPlanRoute = findViewById(R.id.btnPlanRoute);
        btnTrafficOverlay = findViewById(R.id.btnTrafficOverlay);
        btnTrafficAnalysis = findViewById(R.id.btnTrafficAnalysis);
        mapView = findViewById(R.id.mapView);
        tvRouteInfo = findViewById(R.id.tvRouteInfo);
        routeInfoSection = findViewById(R.id.routeInfoSection);
    }

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Pre-Route Prediction");
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupMapView() {
        mapManager = new MapManager(this);
        mapManager.setupMap(mapView);
        
        // Set initial view to Singapore (this will be done by setupMap, but we can override if needed)
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(new GeoPoint(1.3521, 103.8198));
    }

    private void setupClickListeners() {
        btnPlanRoute.setOnClickListener(v -> planRoute());
        btnTrafficOverlay.setOnClickListener(v -> toggleTrafficOverlay());
        btnTrafficAnalysis.setOnClickListener(v -> showTrafficAnalysisDialog());
    }

    private void planRoute() {
        String startLocationText = etStartLocation.getText().toString().trim();
        String destinationText = etDestination.getText().toString().trim();

        if (startLocationText.isEmpty() || destinationText.isEmpty()) {
            Toast.makeText(this, "Please enter both start location and destination", Toast.LENGTH_SHORT).show();
            return;
        }

        // Geocode the locations
        geocodeLocations(startLocationText, destinationText);
    }

    private void geocodeLocations(String startLocationText, String destinationText) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                
                // Geocode start location
                List<Address> startAddresses = geocoder.getFromLocationName(startLocationText, 1);
                if (startAddresses == null || startAddresses.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "Start location not found", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                // Geocode destination
                List<Address> destAddresses = geocoder.getFromLocationName(destinationText, 1);
                if (destAddresses == null || destAddresses.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "Destination not found", Toast.LENGTH_SHORT).show());
                    return;
                }

                Address startAddress = startAddresses.get(0);
                Address destAddress = destAddresses.get(0);
                
                startLocation = new GeoPoint(startAddress.getLatitude(), startAddress.getLongitude());
                destinationLocation = new GeoPoint(destAddress.getLatitude(), destAddress.getLongitude());

                runOnUiThread(() -> {
                    // Clear previous route
                    mapManager.clearRoute();
                    
                    // Add markers for start and destination
                    mapManager.addMarker(startLocation, "Start: " + startLocationText);
                    mapManager.addMarker(destinationLocation, "Destination: " + destinationText);
                    
                    // Calculate and display route
                    calculateRoute();
                });

            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Geocoding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void calculateRoute() {
        if (startLocation == null || destinationLocation == null) {
            Toast.makeText(this, "Please set both start and destination locations", Toast.LENGTH_SHORT).show();
            return;
        }

        String apiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImE3NmQzMjQwZWU4MzRjYWFiNTllOWI0MWM2MmE5ODc3IiwiaCI6Im11cm11cjY0In0=";

        RoutePlanner.getRoute(startLocation, destinationLocation, apiKey, new RoutePlanner.RouteCallback() {
            @Override
            public void onRouteReady(RoutePlanner.RouteData routeData) {
                runOnUiThread(() -> {
                    currentRouteData = routeData;
                    mapManager.drawRoute(routeData.routePoints);
                    showRouteInfo(routeData);
                    
                    // Show route info section
                    routeInfoSection.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(PrePlannedRouteActivity.this, "Route calculation failed: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showRouteInfo(RoutePlanner.RouteData routeData) {
        double distanceKm = routeData.distance / 1000.0;
        int durationMinutes = (int) (routeData.duration / 60.0);
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;

        String routeInfo;
        if (hours > 0) {
            routeInfo = String.format(Locale.getDefault(), 
                "Distance: %.1f km • Duration: %dh %dm", 
                distanceKm, hours, minutes);
        } else {
            routeInfo = String.format(Locale.getDefault(), 
                "Distance: %.1f km • Duration: %d minutes", 
                distanceKm, minutes);
        }

        tvRouteInfo.setText(routeInfo);
        originalRouteInfo = routeInfo;
    }

    private void toggleTrafficOverlay() {
        if (!showTrafficOverlay) {
            showTrafficTimePickerDialog();
        } else {
            disableTrafficOverlay();
        }
    }

    private void showTrafficTimePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Traffic Prediction Time");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_traffic_time_picker, null);
        builder.setView(dialogView);

        RadioButton rbCurrentTime = dialogView.findViewById(R.id.rbCurrentTime);
        RadioButton rbCustomTime = dialogView.findViewById(R.id.rbCustomTime);
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);

        timePicker.setVisibility(View.GONE);

        rbCurrentTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                timePicker.setVisibility(View.GONE);
            }
        });

        rbCustomTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                timePicker.setVisibility(View.VISIBLE);
            }
        });

        builder.setPositiveButton("Predict", (dialog, which) -> {
            if (rbCurrentTime.isChecked()) {
                selectedTrafficTime = null;
                mapManager.setCustomTrafficTime(null);
                enableTrafficOverlay();
                Toast.makeText(this, "Predicting traffic for current time", Toast.LENGTH_SHORT).show();
            } else if (rbCustomTime.isChecked()) {
                Calendar now = Calendar.getInstance();
                Calendar selectedTime = Calendar.getInstance();
                selectedTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                selectedTime.set(Calendar.MINUTE, timePicker.getMinute());
                selectedTime.set(Calendar.SECOND, 0);
                selectedTime.set(Calendar.MILLISECOND, 0);

                if (selectedTime.before(now)) {
                    selectedTime.add(Calendar.DAY_OF_YEAR, 1);
                }

                selectedTrafficTime = selectedTime;
                mapManager.setCustomTrafficTime(selectedTime);
                enableTrafficOverlay();

                String timeStr = String.format("%02d:%02d", selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE));
                Toast.makeText(this, "Predicting traffic for " + timeStr, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        rbCurrentTime.setChecked(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void enableTrafficOverlay() {
        showTrafficOverlay = true;
        btnTrafficOverlay.setText("Hide Prediction");
        mapManager.showTrafficOverlay(true);
        mapManager.updateTrafficPredictions();
    }

    private void disableTrafficOverlay() {
        showTrafficOverlay = false;
        selectedTrafficTime = null;
        btnTrafficOverlay.setText("Predict Traffic");
        mapManager.showTrafficOverlay(false);
        mapManager.setCustomTrafficTime(null);
        Toast.makeText(this, "Traffic overlay disabled", Toast.LENGTH_SHORT).show();
    }

    private void showTrafficAnalysisDialog() {
        // Check if traffic analysis is already showing - if so, hide it
        String currentText = tvRouteInfo.getText().toString();
        if (currentText.contains("Traffic Analysis")) {
            tvRouteInfo.setText(originalRouteInfo);
            btnTrafficAnalysis.setText("Traffic Analysis");
            Toast.makeText(this, "Traffic analysis hidden", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Congestion Analysis Time");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_traffic_time_picker, null);
        builder.setView(dialogView);

        RadioButton rbCurrentTime = dialogView.findViewById(R.id.rbCurrentTime);
        RadioButton rbCustomTime = dialogView.findViewById(R.id.rbCustomTime);
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);

        timePicker.setVisibility(View.GONE);

        rbCurrentTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                timePicker.setVisibility(View.GONE);
            }
        });

        rbCustomTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                timePicker.setVisibility(View.VISIBLE);
            }
        });

        builder.setPositiveButton("Analyze Congestion", (dialog, which) -> {
            if (rbCurrentTime.isChecked()) {
                selectedTrafficTime = null;
                mapManager.setCustomTrafficTime(null);
                showTrafficAnalysisForRoute();
                Toast.makeText(this, "Analyzing congestion for current time", Toast.LENGTH_SHORT).show();
            } else if (rbCustomTime.isChecked()) {
                Calendar now = Calendar.getInstance();
                Calendar selectedTime = Calendar.getInstance();
                selectedTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                selectedTime.set(Calendar.MINUTE, timePicker.getMinute());
                selectedTime.set(Calendar.SECOND, 0);
                selectedTime.set(Calendar.MILLISECOND, 0);

                if (selectedTime.before(now)) {
                    selectedTime.add(Calendar.DAY_OF_YEAR, 1);
                }

                selectedTrafficTime = selectedTime;
                mapManager.setCustomTrafficTime(selectedTime);
                showTrafficAnalysisForRoute();

                String timeStr = String.format("%02d:%02d", selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE));
                Toast.makeText(this, "Analyzing congestion for " + timeStr, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        rbCurrentTime.setChecked(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showTrafficAnalysisForRoute() {
        if (currentRouteData == null || currentRouteData.routePoints == null) {
            Toast.makeText(this, "No route selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if we're currently showing traffic info - if so, restore original
        String currentText = tvRouteInfo.getText().toString();
        if (currentText.contains("Traffic Analysis")) {
            tvRouteInfo.setText(originalRouteInfo);
            Toast.makeText(this, "Route info restored", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store original route info if not already stored
        if (originalRouteInfo.isEmpty()) {
            originalRouteInfo = tvRouteInfo.getText().toString();
        }

        // Analyze traffic along the route
        StringBuilder trafficInfo = new StringBuilder();
        trafficInfo.append("Traffic Analysis\n\n");

        // Check traffic at start, middle, and end points using ML predictions
        List<GeoPoint> routePoints = currentRouteData.routePoints;
        if (!routePoints.isEmpty()) {
            // Use ML predictor for dynamic traffic analysis
            try {
                // Analyze traffic at key points along the route
                int totalPoints = routePoints.size();
                
                // Start point
                String startTraffic = getTrafficPredictionForPoint(0);
                trafficInfo.append("Start: ").append(startTraffic).append(" traffic\n");
                
                // Middle point
                if (totalPoints > 2) {
                    String midTraffic = getTrafficPredictionForPoint(totalPoints / 2);
                    trafficInfo.append("Middle: ").append(midTraffic).append(" traffic\n");
                }
                
                // End point
                String endTraffic = getTrafficPredictionForPoint(totalPoints - 1);
                trafficInfo.append("End: ").append(endTraffic).append(" traffic\n\n");
                
                // Overall assessment
                trafficInfo.append("Overall: Expect ").append(getOverallTrafficAssessment()).append(" conditions");
                
            } catch (Exception e) {
                trafficInfo.append("Traffic analysis unavailable");
            }
        }

        tvRouteInfo.setText(trafficInfo.toString());
        btnTrafficAnalysis.setText("Hide Analysis");
    }

    private String getTrafficPredictionForPoint(int pointIndex) {
        try {
            // Use point index to determine junction (1-4)
            int junction = (pointIndex % 4) + 1;
            
            if (selectedTrafficTime != null) {
                return mlPredictor.getTrafficPredictionForTime(junction, selectedTrafficTime);
            } else {
                return mlPredictor.getCurrentTrafficPrediction(junction);
            }
        } catch (Exception e) {
            // Fallback to time-based prediction
            Calendar now = selectedTrafficTime != null ? selectedTrafficTime : Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            
            if (hour >= 7 && hour <= 9 || hour >= 17 && hour <= 19) {
                return "High";
            } else if (hour >= 10 && hour <= 16) {
                return "Medium";
            } else {
                return "Low";
            }
        }
    }

    private String getOverallTrafficAssessment() {
        try {
            // Get predictions for multiple junctions and average them
            int highCount = 0, mediumCount = 0, lowCount = 0;
            
            for (int junction = 1; junction <= 4; junction++) {
                String prediction;
                if (selectedTrafficTime != null) {
                    prediction = mlPredictor.getTrafficPredictionForTime(junction, selectedTrafficTime);
                } else {
                    prediction = mlPredictor.getCurrentTrafficPrediction(junction);
                }
                
                switch (prediction.toLowerCase()) {
                    case "high":
                        highCount++;
                        break;
                    case "medium":
                        mediumCount++;
                        break;
                    case "low":
                        lowCount++;
                        break;
                }
            }
            
            if (highCount >= 2) return "heavy traffic";
            if (mediumCount >= 2) return "moderate traffic";
            return "light traffic";
            
        } catch (Exception e) {
            return "mixed traffic";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
