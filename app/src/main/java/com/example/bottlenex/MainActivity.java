package com.example.bottlenex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bottlenex.databinding.ActivityMainBinding;
import com.example.bottlenex.map.MapManager;
import com.example.bottlenex.services.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.example.bottlenex.routing.RoutePlanner;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;



import org.osmdroid.util.GeoPoint;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements
        MapManager.OnMapClickListener,
        MapManager.OnLocationUpdateListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private ActivityMainBinding binding;

    @Inject
    MapManager mapManager;

    @Inject
    FirebaseService firebaseService;

    private GeoPoint selectedLocation;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase authentication
        initializeFirebaseAuth();

        // Setup map
        mapManager.setupMap(binding.mapView);
        mapManager.setOnMapClickListener(this);
        mapManager.setOnLocationUpdateListener(this);

        // Setup UI
        setupUI();

        // Check permissions
        if (checkPermissions()) {
            initializeMap();
        } else {
            requestPermissions();
        }
    }

    private void initializeFirebaseAuth() {
        currentUser = firebaseService.getCurrentUser();
        if (currentUser == null) {
            // Sign in anonymously
            firebaseService.signInAnonymously(task -> {
                if (task.isSuccessful()) {
                    currentUser = task.getResult().getUser();
                } else {
                    Toast.makeText(this, "Failed to initialize user", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar);

        // Setup map controls
        binding.btnZoomIn.setOnClickListener(v -> mapManager.zoomIn());
        binding.btnZoomOut.setOnClickListener(v -> mapManager.zoomOut());
        binding.btnMyLocation.setOnClickListener(v -> mapManager.centerOnMyLocation());

        // Setup navigation buttons
        binding.btnSearch.setOnClickListener(v -> onSearchClicked());
        binding.btnNavigate.setOnClickListener(v -> onNavigateClicked());
        binding.btnSettings.setOnClickListener(v -> onSettingsClicked());
        binding.btnBugReport.setOnClickListener(v -> {
            startActivity(new Intent(this, BugReportActivity.class));
        });
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    private void initializeMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("PERMISSION", "Fine location not granted");
            return;
        }
        mapManager.startLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initializeMap();
            } else {
                Toast.makeText(this, "Permissions required for map functionality",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onMapClick(GeoPoint point) {
        selectedLocation = point;

        // Clear previous markers and add new one
        mapManager.clearMarkers();
        mapManager.addMarker(point, "Selected Location");

        // Update location info
        String locationText = String.format("Lat: %.6f, Lon: %.6f",
                point.getLatitude(), point.getLongitude());
        binding.locationInfo.setText(locationText);

        // Enable navigation button
        binding.btnNavigate.setEnabled(true);
    }

    @Override
    public void onLocationUpdate(Location location) {
        Log.d("DEBUG_LOCATION", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
        // Update location info if no location is selected
        if (selectedLocation == null) {
            String locationText = String.format("My Location: %.6f, %.6f",
                    location.getLatitude(), location.getLongitude());
            binding.locationInfo.setText(locationText);
        }

        // Save location to Firebase if user is authenticated
        if (currentUser != null) {
            firebaseService.saveUserLocation(
                    currentUser.getUid(),
                    location.getLatitude(),
                    location.getLongitude(),
                    aVoid -> {
                        // Location saved successfully
                    }
            );
        }
    }

    private void onSearchClicked() {
        // TODO: Implement search functionality
        Toast.makeText(this, "Search functionality coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void onNavigateClicked() {
        if (selectedLocation != null) {
            Location current = mapManager.getLastKnownLocation();
            if (current == null) {
                Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
                return;
            }

            GeoPoint start = new GeoPoint(current.getLatitude(), current.getLongitude());
            GeoPoint end = selectedLocation;

            String apiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImE3NmQzMjQwZWU4MzRjYWFiNTllOWI0MWM2MmE5ODc3IiwiaCI6Im11cm11cjY0In0=";

            // LOGGING TO CHECK FOR ROUTING COORDS TO BE WITHIN SG
            Log.d("ROUTING_COORDS", "Start: " + start.getLatitude() + "," + start.getLongitude());
            Log.d("ROUTING_COORDS", "End: " + end.getLatitude() + "," + end.getLongitude());

            RoutePlanner.getRoute(start, end, apiKey, new RoutePlanner.RouteCallback() {
                @Override
                public void onRouteReady(ArrayList<GeoPoint> routePoints, double duration, double distance) {
                    mapManager.drawRoute(routePoints);
                    String info = String.format("ETA: %.1f min | Distance: %.1f km",
                            duration / 60.0, distance / 1000.0);
                    binding.locationInfo.setText(info);
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e("ROUTING_ERROR", "Routing failed: " + errorMessage);
                    Toast.makeText(MainActivity.this, "Routing failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "Please select a destination on the map first", Toast.LENGTH_SHORT).show();
        }
    }


    private void onSettingsClicked() {
        // TODO: Implement settings functionality
        Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapManager.stopLocationUpdates();
    }
}