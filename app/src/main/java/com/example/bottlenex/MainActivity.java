package com.example.bottlenex;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.SearchView;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bottlenex.databinding.ActivityMainBinding;
import com.example.bottlenex.map.MapManager;
import com.example.bottlenex.services.FirebaseService;
import com.google.firebase.auth.FirebaseUser;
import com.example.bottlenex.routing.RoutePlanner;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import com.example.bottlenex.AlertPreferenceHelper;
import com.example.bottlenex.AlertsNotification;
import com.example.bottlenex.OSMMaxSpeedFetcher;
import com.example.bottlenex.MapQuestIncidentsFetcher;
import android.graphics.drawable.Drawable;
import java.util.HashSet;
import java.util.Set;
import com.example.bottlenex.OSMSpeedCameraFetcher;
import android.view.View;




@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements
        MapManager.OnMapClickListener,
        MapManager.OnLocationUpdateListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };

    private static final int REQUEST_CODE_BOOKMARK = 100;
    private ActivityMainBinding binding;

    @Inject
    MapManager mapManager;

    @Inject
    FirebaseService firebaseService;

    private GeoPoint selectedLocation;
    private FirebaseUser currentUser;
    private boolean hasAlertedSpeedLimit = false;
    private Integer currentSpeedLimit = null;
    private Set<String> alertedIncidentIds = new HashSet<>();
    private Set<String> alertedSpeedCameraIds = new HashSet<>();
    private boolean isNavigating = false;

    private void saveStarred(String name, double lat, double lon) {
        SharedPreferences prefs = getSharedPreferences("starred_places", MODE_PRIVATE);
        Set<String> starred = prefs.getStringSet("starred_places_list", new HashSet<>());
        Set<String> newStarred = new HashSet<>(starred);
        String entry = name + "|" + lat + "|" + lon;
        newStarred.add(entry);
        prefs.edit().putStringSet("starred_places_list", newStarred).apply();
    }

    private void loadStarredPlaces() {
        SharedPreferences prefs = getSharedPreferences("starred_places", MODE_PRIVATE);
        Set<String> starred = prefs.getStringSet("starred_places_list", new HashSet<>());
        for (String entry : starred) {
            String[] parts = entry.split("\\|");
            if (parts.length == 3) {
                String name = parts[0];
                double lat = Double.parseDouble(parts[1]);
                double lon = Double.parseDouble(parts[2]);

                GeoPoint point = new GeoPoint(lat, lon);
                Marker marker = new Marker(binding.mapView);
                marker.setPosition(point);
                marker.setTitle("⭐ Starred: " + name);
                Drawable icon = getResources().getDrawable(R.drawable.star);
                marker.setIcon(icon);
                binding.mapView.getOverlays().add(marker);
            }
        }
        binding.mapView.invalidate();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge implementation from first file
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Apply window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeFirebaseAuth();

        mapManager.setupMap(binding.mapView);
        loadStarredPlaces();
        mapManager.setOnMapClickListener(this);
        mapManager.setOnLocationUpdateListener(this);

        setupUI();

        if (checkPermissions()) {
            initializeMap();
        } else {
            requestPermissions();
        }
    }

    // Add missing method stubs
    private void initializeFirebaseAuth() {
        currentUser = firebaseService.getCurrentUser();
        if (currentUser == null) {
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
        setSupportActionBar(binding.toolbar);

        // Map controls
        binding.btnZoomIn.setOnClickListener(v -> mapManager.zoomIn());
        binding.btnZoomOut.setOnClickListener(v -> mapManager.zoomOut());
        binding.btnMyLocation.setOnClickListener(v -> mapManager.centerOnMyLocation());

        // SearchView setup
        SearchView searchView = binding.searchView;
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query == null || query.trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    performSearch(query.trim());
                    searchView.clearFocus();
                    return true;
                }
                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }

        // Old search buttons (optional)
        binding.btnSearchRight.setOnClickListener(v ->
                Toast.makeText(this, "Profile Clicked", Toast.LENGTH_SHORT).show());

        // Navigation button
        binding.btnNavigate.setOnClickListener(v -> onNavigateClicked());

        // Bottom bar buttons
        binding.btnPersonalTools.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PersonalTools.class);
            startActivity(intent);
        });

        // Bookmark button launches Bookmark activity for result
        binding.btnBookmark.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Bookmark.class);
            startActivityForResult(intent, REQUEST_CODE_BOOKMARK);
        });

        binding.btnCar.setOnClickListener(v ->
                Toast.makeText(this, "Car clicked", Toast.LENGTH_SHORT).show());

        binding.btnFavorite.setOnClickListener(v -> {
            String query = binding.searchView.getQuery().toString();
            if (query.isEmpty()) {
                Toast.makeText(this, "Search something before saving!", Toast.LENGTH_SHORT).show();
            } else {
                saveFavourite(query);
            }
        });

        binding.btnStarred.setOnClickListener(v -> {
            String query = binding.searchView.getQuery().toString();
            if (query.isEmpty()) {
                Toast.makeText(this, "Search something before starring!", Toast.LENGTH_SHORT).show();
            } else {
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocationName(query, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        double lat = address.getLatitude();
                        double lon = address.getLongitude();

                        GeoPoint point = new GeoPoint(lat, lon);

                        Marker marker = new Marker(binding.mapView);
                        marker.setPosition(point);
                        marker.setTitle("⭐ Starred: " + query);
                        Drawable icon = getResources().getDrawable(R.drawable.star); // your star icon

                        marker.setIcon(icon);
                        binding.mapView.getOverlays().add(marker);
                        binding.mapView.invalidate();

                        saveStarred(query, lat, lon);

                        Toast.makeText(this, "Starred place saved!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Geocoding failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.btnJourney.setOnClickListener(v -> {
            if (!isNavigating) {
                startJourney();
            } else {
                finishJourney();
            }
        });
    }

    private void saveFavourite(String query) {
        SharedPreferences prefs = getSharedPreferences("favourites", MODE_PRIVATE);
        Set<String> favourites = prefs.getStringSet("favourites_list", new HashSet<>());
        Set<String> newFavourites = new HashSet<>(favourites);
        newFavourites.add(query);
        prefs.edit().putStringSet("favourites_list", newFavourites).apply();
        Toast.makeText(this, "Saved to favourites!", Toast.LENGTH_SHORT).show();
    }

    private void performSearch(String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses == null || addresses.isEmpty()) {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Address address = addresses.get(0);
            GeoPoint point = new GeoPoint(address.getLatitude(), address.getLongitude());

            binding.mapView.getController().setZoom(15.0);
            binding.mapView.getController().setCenter(point);

            binding.mapView.getOverlays().clear();

            loadStarredPlaces();

            Marker marker = new Marker(binding.mapView);
            marker.setPosition(point);
            marker.setTitle(query);
            binding.mapView.getOverlays().add(marker);

            binding.mapView.invalidate();

            // Update selected location and UI
            selectedLocation = point;
            binding.locationInfo.setText(String.format("Found: %s\nLat: %.6f, Lon: %.6f",
                    query, point.getLatitude(), point.getLongitude()));
            binding.btnNavigate.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Geocoding failed, please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w("PermissionCheck", "Missing permission: " + permission);
                allGranted = false;
            }
        }
        return allGranted;
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
                Toast.makeText(this, "Permissions required for map functionality", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    public void onMapClick(GeoPoint point) {
        selectedLocation = point;

        mapManager.clearMarkers();
        mapManager.addMarker(point, "Selected Location");

        String locationText = String.format("Lat: %.6f, Lon: %.6f",
                point.getLatitude(), point.getLongitude());
        binding.locationInfo.setText(locationText);

        binding.btnNavigate.setEnabled(true);
    }
    @Override
    public void onLocationUpdate(Location location) {
        Log.d("SpeedAlert", "onLocationUpdate called. Location: " + location);
        Log.d("SpeedAlert", "location.hasSpeed(): " + location.hasSpeed());
        if (!location.hasSpeed()) {
            Log.d("SpeedAlert", "No speed data in this location update. Skipping speed alert logic.");
            return;
        }
        float speedMps = location.getSpeed(); // meters/second
        float speedKmh = speedMps * 3.6f; // convert to km/h
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        // --- Speed Camera Alert Integration ---
        AlertPreferenceHelper alertPreferenceHelper = new AlertPreferenceHelper(this);
        if (alertPreferenceHelper.isSpeedCameraAlertEnabled()) {
            Log.d("SpeedCameraAlert", "Speed Camera Alert is ENABLED");
            double radiusKm = 2.0; // Search within 2km
            OSMSpeedCameraFetcher.fetchSpeedCameras(lat, lon, radiusKm, cameras -> {
                // Remove old speed camera markers
                List<Marker> toRemove = new ArrayList<>();
                for (org.osmdroid.views.overlay.Overlay overlay : binding.mapView.getOverlays()) {
                    if (overlay instanceof Marker && "speed_camera".equals(((Marker) overlay).getSubDescription())) {
                        toRemove.add((Marker) overlay);
                    }
                }
                binding.mapView.getOverlays().removeAll(toRemove);

                for (OSMSpeedCameraFetcher.SpeedCamera camera : cameras) {
                    // Calculate distance to camera
                    float[] results = new float[1];
                    Location.distanceBetween(lat, lon, camera.lat, camera.lon, results);
                    int distanceMeters = (int) results[0];

                    // Only display marker and alert if within 400m
                    if (distanceMeters <= 400) {
                        // Add marker for each camera
                        Marker marker = new Marker(binding.mapView);
                        marker.setPosition(new GeoPoint(camera.lat, camera.lon));
                        marker.setTitle("Speed Camera");
                        marker.setSubDescription("speed_camera");
                        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_camera_red);

                        if (icon != null) marker.setIcon(icon);
                        binding.mapView.getOverlays().add(marker);

                        // Use lat/lon as a unique camera ID for this session
                        String cameraId = camera.lat + "," + camera.lon;

                        if (!alertedSpeedCameraIds.contains(cameraId)) {
                            Log.d("SpeedCameraAlert", "Speed camera detected: " + cameraId + " at " + distanceMeters + "m");
                            Log.d("SpeedCameraAlert", "Sending speed camera notification now.");
                            AlertsNotification.sendSpeedCameraAlert(
                                    this,
                                    "Speed Camera Ahead!",
                                    "A fixed speed camera is detected ahead."
                            );
                            alertedSpeedCameraIds.add(cameraId);
                        }
                    }
                }
                binding.mapView.invalidate();
            });
        } else {
            Log.d("SpeedCameraAlert", "Speed Camera Alert is DISABLED");
        }

        // --- Road Incident Alert Integration ---
        if (alertPreferenceHelper.isRoadIncidentAlertEnabled()) {
            Log.d("IncidentAlert", "Road Incident Alert is ENABLED");
            double radiusKm = 2.0; // Search within 2km
            MapQuestIncidentsFetcher.fetchIncidents(lat, lon, radiusKm, incidents -> {
                // Remove old incident markers
                List<Marker> toRemove = new ArrayList<>();
                for (org.osmdroid.views.overlay.Overlay overlay : binding.mapView.getOverlays()) {
                    if (overlay instanceof Marker && "incident".equals(((Marker) overlay).getSubDescription())) {
                        toRemove.add((Marker) overlay);
                    }
                }
                binding.mapView.getOverlays().removeAll(toRemove);

                for (MapQuestIncidentsFetcher.Incident incident : incidents) {
                    // Add marker for each incident
                    Marker marker = new Marker(binding.mapView);
                    marker.setPosition(new GeoPoint(incident.lat, incident.lon));
                    marker.setTitle("Incident");
                    marker.setSubDescription("incident");
                    marker.setSnippet(incident.description);
                    Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_warning_yellow);
                    if (icon != null) marker.setIcon(icon);
                    binding.mapView.getOverlays().add(marker);

                    // Calculate distance to incident
                    float[] results = new float[1];
                    Location.distanceBetween(lat, lon, incident.lat, incident.lon, results);
                    int distanceMeters = (int) results[0];

                    // Use lat/lon as a unique incident ID for this session
                    String incidentId = incident.lat + "," + incident.lon;

                    if (distanceMeters <= 800 && !alertedIncidentIds.contains(incidentId)) {
                        Log.d("IncidentAlert", "Incident ahead within 800m: " + incident.description);
                        AlertsNotification.sendRoadIncidentAlert(
                                this,
                                "Road Incident Ahead! (" + distanceMeters + "m)",
                                "Please be cautious."
                        );
                        alertedIncidentIds.add(incidentId);
                    }
                }
                binding.mapView.invalidate();
            });
        } else {
            Log.d("IncidentAlert", "Road Incident Alert is DISABLED");
        }

        // --- Existing Speed Limit Alert Logic ---
        OSMMaxSpeedFetcher.fetchMaxSpeed(lat, lon, maxSpeedKmh -> {
            if (maxSpeedKmh != null) {
                Log.d("SpeedAlert", "Fetched speed limit from OSM: " + maxSpeedKmh + " km/h");
                currentSpeedLimit = maxSpeedKmh;
                checkSpeedAndAlert(speedKmh, currentSpeedLimit);
            } else {
                Log.d("SpeedAlert", "No speed limit found from OSM. No alert will be triggered.");
                // Do not set a default speed limit or trigger alert
            }
        });
    }
    private void checkSpeedAndAlert(float speedKmh, int speedLimit) {
        AlertPreferenceHelper alertPreferenceHelper = new AlertPreferenceHelper(this);
        if (alertPreferenceHelper.isSpeedLimitAlertEnabled()) {
            Log.d("SpeedAlert", "Speed Limit Alert is ENABLED");
            if (speedKmh > speedLimit) {
                if (!hasAlertedSpeedLimit) {
                    String message = "Please slow down.\nCurrent Speed: " + String.format("%.1f", speedKmh) + " km/h";
                    Log.d("SpeedAlert", "Speed exceeds limit! Sending notification.");
                    AlertsNotification.sendSpeedLimitAlert(
                            this,
                            "Speed Alert! (>" + speedLimit + ")",
                            message
                    );
                    hasAlertedSpeedLimit = true;
                } else {
                    Log.d("SpeedAlert", "Already alerted for this overspeeding session. No new alert.");
                }
            } else {
                if (hasAlertedSpeedLimit) {
                    Log.d("SpeedAlert", "Speed dropped below or equals limit. Resetting alert flag.");
                }
                hasAlertedSpeedLimit = false;
                Log.d("SpeedAlert", "Speed is within limit. No alert. (speedKmh=" + speedKmh + ", limit=" + speedLimit + ")");
            }
        } else {
            Log.d("SpeedAlert", "Speed Limit Alert is DISABLED");
        }
    }
    private void onNavigateClicked() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show();
            return;
        }

        Location currentLocation = mapManager.getLastKnownLocation();
        if (currentLocation == null) {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        GeoPoint start = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
        GeoPoint end = selectedLocation;

        String apiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImE3NmQzMjQwZWU4MzRjYWFiNTllOWI0MWM2MmE5ODc3IiwiaCI6Im11cm11cjY0In0=";

        RoutePlanner.getRoute(start, end, apiKey, new RoutePlanner.RouteCallback() {
            @Override
            public void onRouteReady(ArrayList<GeoPoint> routePoints, double duration, double distance) {
                runOnUiThread(() -> {
                    mapManager.drawRoute(routePoints);
                    String info = String.format("Route: %.2f km, %.2f min", distance / 1000.0, duration / 60.0);
                    binding.locationInfo.setText(info);
                    binding.btnJourney.setVisibility(View.VISIBLE);
                    binding.btnJourney.setText("Start Navigation");
                    binding.tvJourneyState.setVisibility(View.GONE);
                });
            }
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Routing failed: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void startJourney() {
        isNavigating = true;
        binding.btnJourney.setText("Finish Navigation");
        binding.tvJourneyState.setText("You are currently in a journey");
        binding.tvJourneyState.setVisibility(View.VISIBLE);
        // Optionally, disable map interaction or other UI changes
    }

    private void finishJourney() {
        isNavigating = false;
        binding.btnJourney.setText("Start Navigation");
        binding.tvJourneyState.setText("");
        binding.tvJourneyState.setVisibility(View.GONE);
        // Optionally, re-enable map interaction or reset UI
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapManager.onResume();
        if (checkPermissions()) {
            mapManager.startLocationUpdates();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BOOKMARK && resultCode == RESULT_OK && data != null) {
            String name = data.getStringExtra("starred_name");
            double lat = data.getDoubleExtra("starred_lat", 0);
            double lon = data.getDoubleExtra("starred_lon", 0);

            if (name != null && lat != 0 && lon != 0) {
                GeoPoint point = new GeoPoint(lat, lon);

                binding.mapView.getController().setZoom(15.0);
                binding.mapView.getController().setCenter(point);

                binding.mapView.getOverlays().clear();
                loadStarredPlaces();

                Marker marker = new Marker(binding.mapView);
                marker.setPosition(point);
                marker.setTitle("⭐ Starred: " + name);
                Drawable icon = getResources().getDrawable(R.drawable.star);
                marker.setIcon(icon);
                binding.mapView.getOverlays().add(marker);

                binding.mapView.invalidate();

                binding.locationInfo.setText(String.format("Starred Place: %s\nLat: %.6f, Lon: %.6f", name, lat, lon));
                binding.btnNavigate.setEnabled(true);

                selectedLocation = point;

            } else {
                String placeName = data.getStringExtra("selected_location");
                if (placeName != null && !placeName.isEmpty()) {
                    performSearch(placeName);
                }
            }
        }
    }
}

