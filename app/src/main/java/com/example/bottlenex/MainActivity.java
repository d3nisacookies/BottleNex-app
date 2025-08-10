package com.example.bottlenex;

//To commit

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
import com.google.firebase.auth.FirebaseAuth;
import com.example.bottlenex.routing.RoutePlanner;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import com.example.bottlenex.AlertPreferenceHelper;
import com.example.bottlenex.AlertsNotification;
import com.example.bottlenex.OSMMaxSpeedFetcher;
import com.example.bottlenex.TomTomIncidentsFetcher;
import android.graphics.drawable.Drawable;
import java.util.HashSet;
import java.util.Set;
import com.example.bottlenex.OSMSpeedCameraFetcher;
import com.example.bottlenex.RouteHistory;
import com.example.bottlenex.ml.TensorFlowTrafficPredictor;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TimePicker;
import androidx.appcompat.app.AlertDialog;




@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements
        MapManager.OnMapClickListener,
        MapManager.OnLocationUpdateListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
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
    private Location lastSpeedAlertLocation = null; // Track location for 100m alerts
    private RoutePlanner.RouteData currentRouteData = null;
    
    // Route History tracking stuff
    private DatabaseHelper databaseHelper;
    private long journeyStartTime;
    private Location journeyStartLocation;
    private GeoPoint journeyDestination;
    private String journeyStartAddress;
    private String journeyEndAddress;
    private double journeyDistance;
    private static final int REQUEST_CODE_ROUTE_HISTORY = 101;
    
    // Search suggestions and autocomplete
    private List<Address> searchSuggestions = new ArrayList<>();
    private boolean isShowingSuggestions = false;
    private android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;
    private android.widget.ListView suggestionListView;
    private android.widget.ArrayAdapter<String> suggestionAdapter;

    // Traffic prediction variables
    private boolean showTrafficOverlay = false;
    private android.os.Handler trafficUpdateHandler = new android.os.Handler();
    private Runnable trafficUpdateRunnable;
    private Calendar selectedTrafficTime = null; // Store user-selected time for traffic prediction

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

        // Check if user is authenticated
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

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
        mapManager.resetAutoFollowState(); // Reset auto-follow to initial state
        loadStarredPlaces();
        mapManager.setOnMapClickListener(this);
        mapManager.setOnLocationUpdateListener(this);

        // ini dbhelper for rh
        databaseHelper = new DatabaseHelper(this);
        
        // Initialize ML predictor for traffic analysis
        mlPredictor = new TensorFlowTrafficPredictor(this);
        
        // test db conn
        try {
            databaseHelper.getReadableDatabase();
            Log.d("RouteHistory", "Database initialized successfully");
        } catch (Exception e) {
            Log.e("RouteHistory", "Database initialization failed: " + e.getMessage());
        }

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
        binding.btnZoomIn.setOnClickListener(v -> {
            mapManager.zoomIn();
            mapManager.onManualInteraction(); // Pause auto-follow
        });
        binding.btnZoomOut.setOnClickListener(v -> {
            mapManager.zoomOut();
            mapManager.onManualInteraction(); // Pause auto-follow
        });
        binding.btnMyLocation.setOnClickListener(v -> mapManager.centerOnMyLocation());

        // SearchView setup with autocomplete
        SearchView searchView = binding.searchView;
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query == null || query.trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    // Hide suggestions when submitting
                    hideSearchSuggestions();
                    performSearch(query.trim());
                    searchView.clearFocus();
                    return true;
                }
                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText == null || newText.trim().isEmpty()) {
                        hideSearchSuggestions();
                        return false;
                    }
                    
                    // Don't show suggestions during navigation
                    if (isNavigating) {
                        hideSearchSuggestions();
                        return false;
                    }
                    
                    // Cancel previous search
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    
                    // Debounce search suggestions
                    searchRunnable = () -> {
                        if (newText.trim().length() >= 2) {
                            performSearchSuggestions(newText.trim());
                        } else {
                            hideSearchSuggestions();
                        }
                    };
                    searchHandler.postDelayed(searchRunnable, 300); // 300ms delay
                    return false;
                }
            });
            
            // Handle search view focus changes
            searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    // Hide suggestions when search view loses focus
                    hideSearchSuggestions();
                }
            });
        }
        
        // Setup suggestion dropdown
        setupSuggestionDropdown();

        // Profile button - navigate to ProfileActivity
        binding.btnSearchRight.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

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

        // Cancel route button
        binding.btnCancelRoute.setOnClickListener(v -> cancelRoute());

        // Traffic prediction button handlers
        binding.btnTrafficOverlay.setOnClickListener(v -> toggleTrafficOverlay());
        binding.btnRouteTraffic.setOnClickListener(v -> {
            // Check if traffic analysis is already displayed
            String currentText = binding.locationInfo.getText().toString();
            if (currentText.contains("Traffic Analysis")) {
                // If traffic analysis is already shown, just close it
                showTrafficPredictionForRoute();
            } else {
                // If no traffic analysis is shown, show the time picker dialog
                showCongestionInfoTimePickerDialog();
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
        // Always hide suggestions when performing search
        hideSearchSuggestions();
        
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 5); // Get up to 5 results
            if (addresses == null || addresses.isEmpty()) {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                return;
            }

            if (addresses.size() == 1) {
                // Single result - proceed directly
                selectLocationFromAddress(addresses.get(0), query);
            } else {
                // Multiple results - show selection dialog
                showMultipleResultsDialog(addresses, query);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Geocoding failed, please try again", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void performSearchSuggestions(String query) {
        Log.d("SearchSuggestions", "Performing search suggestions for: " + query);
        
        if (query == null || query.trim().isEmpty()) {
            hideSearchSuggestions();
            return;
        }
        
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                
                // Check if Geocoder is available
                if (!Geocoder.isPresent()) {
                    Log.e("SearchSuggestions", "Geocoder is not available on this device");
                    runOnUiThread(() -> hideSearchSuggestions());
                    return;
                }
                
                List<Address> addresses = geocoder.getFromLocationName(query, 5);
                
                Log.d("SearchSuggestions", "Query: " + query + ", Found addresses: " + (addresses != null ? addresses.size() : 0));
                
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        searchSuggestions = addresses;
                        showSearchSuggestions(addresses);
                    } else {
                        Log.d("SearchSuggestions", "No addresses found for query: " + query);
                        hideSearchSuggestions();
                    }
                });
            } catch (Exception e) {
                Log.e("SearchSuggestions", "Error getting suggestions: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> hideSearchSuggestions());
            }
        }).start();
    }
    
    private void setupSuggestionDropdown() {
        // Get the ListView from the layout
        suggestionListView = binding.suggestionListView;
        
        // Create a simple drawable for the divider
        android.graphics.drawable.ColorDrawable dividerDrawable = new android.graphics.drawable.ColorDrawable(android.graphics.Color.LTGRAY);
        suggestionListView.setDivider(dividerDrawable);
        suggestionListView.setDividerHeight(1);
        
        // Create adapter
        suggestionAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        suggestionListView.setAdapter(suggestionAdapter);
        
        // Handle item clicks
        suggestionListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < searchSuggestions.size()) {
                Address selectedAddress = searchSuggestions.get(position);
                String query = getReadableAddress(selectedAddress);
                binding.searchView.setQuery(query, false);
                hideSearchSuggestions();
                performSearch(query);
            }
        });
    }
    
    private void showSearchSuggestions(List<Address> addresses) {
        Log.d("SearchSuggestions", "Showing " + addresses.size() + " suggestions");
        if (suggestionListView == null || suggestionAdapter == null) {
            Log.e("SearchSuggestions", "Suggestion views not initialized");
            return;
        }
        
        searchSuggestions = addresses;
        suggestionAdapter.clear();
        
        for (Address address : addresses) {
            String suggestion = getReadableAddress(address);
            suggestionAdapter.add(suggestion);
            Log.d("SearchSuggestions", "Added suggestion: " + suggestion);
            
            // Debug: Log address details
            Log.d("SearchSuggestions", "Address details - Locality: " + address.getLocality() + 
                  ", Country: " + address.getCountryName() + 
                  ", AddressLine0: " + address.getAddressLine(0));
        }
        
        suggestionListView.setVisibility(View.VISIBLE);
        isShowingSuggestions = true;
        Log.d("SearchSuggestions", "Suggestion dropdown should now be visible");
    }
    
    private void hideSearchSuggestions() {
        if (suggestionListView != null) {
            suggestionListView.setVisibility(View.GONE);
        }
        if (suggestionAdapter != null) {
            suggestionAdapter.clear();
        }
        isShowingSuggestions = false;
        searchSuggestions.clear();
    }
    
    private void showMultipleResultsDialog(List<Address> addresses, String originalQuery) {
        String[] options = new String[addresses.size()];
        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            String readableAddress = getReadableAddress(address);
            // Add country info if available
            if (address.getCountryName() != null) {
                options[i] = readableAddress + " (" + address.getCountryName() + ")";
            } else {
                options[i] = readableAddress;
            }
        }
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Multiple locations found for \"" + originalQuery + "\"")
               .setItems(options, (dialog, which) -> {
                   selectLocationFromAddress(addresses.get(which), getReadableAddress(addresses.get(which)));
               })
               .setNegativeButton("Cancel", null)
               .show();
    }
    
    private void selectLocationFromAddress(Address address, String displayName) {
        // Hide suggestions when location is selected
        hideSearchSuggestions();
        
        GeoPoint point = new GeoPoint(address.getLatitude(), address.getLongitude());

        // Center on destination and temporarily disable auto-follow
        mapManager.centerOnDestination(point);

        // Clear only markers, not the location overlay
        mapManager.clearMarkers();
        loadStarredPlaces();

        // Use MapManager's addMarker method to ensure it's tracked
        mapManager.addMarker(point, displayName);

        binding.mapView.invalidate();

        // Update selected location and UI
        selectedLocation = point;
        binding.locationInfo.setText(displayName + " • Tap Navigate to start route");
        
        // Enable navigate button when a location is found
        binding.btnNavigate.setEnabled(true);
        
        // Only update other UI elements if we're not already in navigation mode
        if (!isNavigating) {
            binding.btnCancelRoute.setVisibility(View.VISIBLE);  // Show Cancel button to clear location
            binding.btnJourney.setVisibility(View.GONE);
        }
        // If we're navigating, keep the current navigation state intact
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

        // Get the address of the selected location using reverse geocoding
        getLocationAddress(point);

        // Enable navigate button when a location is selected
        binding.btnNavigate.setEnabled(true);
        
        // Only update other UI elements if we're not already in navigation mode
        if (!isNavigating) {
            binding.btnCancelRoute.setVisibility(View.VISIBLE);  // Show Cancel button to clear location
            binding.btnJourney.setVisibility(View.GONE);
        }
        // If we're navigating, keep the current navigation state intact
    }
    @Override
    public void onLocationUpdate(Location location) {
        Log.d("SpeedAlert", "onLocationUpdate called. Location: " + location);
        Log.d("SpeedAlert", "location.hasSpeed(): " + location.hasSpeed());
        
        // Check if we've reached the destination during navigation
        if (isNavigating && mapManager.isNearDestination()) {
            runOnUiThread(() -> {
                Toast.makeText(this, "You have reached your destination!", Toast.LENGTH_LONG).show();
                // Automatically finish the journey and clear everything
                finishJourney();
            });
        }
        
        // Update navigation info during active navigation
        if (isNavigating && currentRouteData != null) {
            updateNavigationInfo();
        }
        
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
            TomTomIncidentsFetcher.fetchIncidents(lat, lon, radiusKm, incidents -> {
                // Remove old incident markers
                List<Marker> toRemove = new ArrayList<>();
                for (org.osmdroid.views.overlay.Overlay overlay : binding.mapView.getOverlays()) {
                    if (overlay instanceof Marker && "incident".equals(((Marker) overlay).getSubDescription())) {
                        toRemove.add((Marker) overlay);
                    }
                }
                binding.mapView.getOverlays().removeAll(toRemove);

                for (TomTomIncidentsFetcher.Incident incident : incidents) {
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
                // Get current location for distance tracking
                Location currentLocation = mapManager.getLastKnownLocation();
                if (currentLocation != null) {
                    float distanceFromLastAlert = 0f;
                    
                    if (lastSpeedAlertLocation != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                            lastSpeedAlertLocation.getLatitude(), lastSpeedAlertLocation.getLongitude(),
                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                            results
                        );
                        distanceFromLastAlert = results[0];
                    }
                    
                    // Alert every 400m while above speed limit
                    if (!hasAlertedSpeedLimit || distanceFromLastAlert >= 400f) {
                        String message = "Please slow down.\nCurrent Speed: " + String.format("%.1f", speedKmh) + " km/h";
                        Log.d("SpeedAlert", "Speed exceeds limit! Sending notification. Distance from last alert: " + distanceFromLastAlert + "m");
                        AlertsNotification.sendSpeedLimitAlert(
                                this,
                                "Speed Alert! (" + speedLimit + ")",
                                message
                        );
                        hasAlertedSpeedLimit = true;
                        lastSpeedAlertLocation = new Location(currentLocation);
                    } else {
                        Log.d("SpeedAlert", "Already alerted recently. Distance from last alert: " + distanceFromLastAlert + "m");
                    }
                }
            } else {
                if (hasAlertedSpeedLimit) {
                    Log.d("SpeedAlert", "Speed dropped below or equals limit. Resetting alert flag.");
                }
                hasAlertedSpeedLimit = false;
                lastSpeedAlertLocation = null; // Reset location tracking
                Log.d("SpeedAlert", "Speed is within limit. No alert. (speedKmh=" + speedKmh + ", limit=" + speedLimit + ")");
            }
        } else {
            Log.d("SpeedAlert", "Speed Limit Alert is DISABLED");
        }
    }
    private void onNavigateClicked() {
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
            public void onRouteReady(RoutePlanner.RouteData routeData) {
                runOnUiThread(() -> {
                    currentRouteData = routeData;
                    mapManager.drawRoute(routeData.routePoints);
                    showEnhancedRouteSummary(routeData);
                    
                    // Store journey distance for route history
                    journeyDistance = routeData.distance;
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

    private void showEnhancedRouteSummary(RoutePlanner.RouteData routeData) {
        // Calculate route information
        String timeText = formatTimeForDisplay(routeData.duration);
        
        // Calculate arrival time
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MINUTE, (int) Math.ceil(routeData.duration / 60.0));
        String arrivalTime = sdf.format(cal.getTime());
        
        // Display route summary in the bottom panel
        String routeSummary = String.format("Route: %.1f km • %s • Arrive @ %s", 
            routeData.distance / 1000.0, timeText, arrivalTime);
        binding.locationInfo.setText(routeSummary);
        
        // Show the "Start Navigation" button
        binding.btnJourney.setVisibility(View.VISIBLE);
        binding.btnJourney.setText("Start Navigation");
        
        // Update UI state
        binding.btnCancelRoute.setVisibility(View.VISIBLE);
        binding.btnNavigate.setVisibility(View.GONE);
        binding.trafficButtonsLayout.setVisibility(View.VISIBLE);
    }





    private void startJourney() {
        if (currentRouteData != null && currentRouteData.navigationSteps != null) {
            isNavigating = true;
            binding.btnJourney.setText("Finish Navigation");
            binding.tvJourneyState.setVisibility(View.GONE); // Hide the redundant message
            
            // Hide search suggestions during navigation
            hideSearchSuggestions();
            
            // Start tracking journey for route history
            journeyStartTime = System.currentTimeMillis();
            journeyStartLocation = mapManager.getLastKnownLocation();
            journeyStartAddress = getAddressFromLocation(journeyStartLocation);
            
            // Store the selected destination
            journeyDestination = selectedLocation;
            
            Log.d("RouteHistory", "Journey started from: " + journeyStartAddress);
            Log.d("RouteHistory", "Journey destination: " + (journeyDestination != null ? 
                journeyDestination.getLatitude() + ", " + journeyDestination.getLongitude() : "null"));
            
            // Start turn-by-turn navigation
            mapManager.startNavigation(currentRouteData.navigationSteps);
            
            // Show remaining route info during navigation
            updateNavigationInfo();
        }
    }

    private void finishJourney() {
        isNavigating = false;
        binding.btnJourney.setText("Start Navigation");
        binding.tvJourneyState.setVisibility(View.GONE);
        
        // Hide navigation instruction
        binding.navigationInstruction.setVisibility(View.GONE);
        
        // Search suggestions will be automatically re-enabled since isNavigating = false
        
        // Save journey to route history
        saveJourneyToHistory();
        
        // Stop turn-by-turn navigation
        mapManager.stopNavigation();
        
        // Clear the route from the map
        mapManager.clearRoute();
        
        // Clear the selected location marker
        mapManager.clearMarkers();
        
        // Reset the selected location and route data
        selectedLocation = null;
        currentRouteData = null;
        
        // Clear stored traffic analysis info
        originalLocationInfo = "";
        
        // Reset UI to initial state
        binding.locationInfo.setText("Tap on map to get location");
        binding.btnNavigate.setEnabled(false);
        binding.btnNavigate.setVisibility(View.VISIBLE);
        binding.btnCancelRoute.setVisibility(View.GONE);
        binding.btnJourney.setVisibility(View.GONE);
        binding.trafficButtonsLayout.setVisibility(View.GONE);
        
        // Clear search view
        if (binding.searchView != null) {
            binding.searchView.setQuery("", false);
        }
        
        Toast.makeText(this, "Navigation finished", Toast.LENGTH_SHORT).show();
    }

    private void updateNavigationInfo() {
        if (currentRouteData == null || !isNavigating) {
            return;
        }
        
        // Get current location
        Location currentLocation = mapManager.getLastKnownLocation();
        if (currentLocation == null) {
            return;
        }
        
        // Ensure we're on the UI thread
        if (!isFinishing() && !isDestroyed()) {
        
        // Calculate remaining distance to destination
        double remainingDistance = calculateRemainingDistance(currentLocation);
        double remainingTime = calculateRemainingTime(remainingDistance);
        
        // Calculate estimated arrival time
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MINUTE, (int) (remainingTime / 60.0));
        String arrivalTime = sdf.format(cal.getTime());
        
        // Format time in a user-friendly way
        String timeText = formatTimeForDisplay(remainingTime);
        
        // Format and display the info with arrival time (shorter format)
        String info = String.format("%.1fkm • %s • %s", 
            remainingDistance / 1000.0, timeText, arrivalTime);
        
        // Only update location info if we're not currently showing traffic analysis
        String currentText = binding.locationInfo.getText().toString();
        if (!currentText.contains("Traffic Analysis")) {
            binding.locationInfo.setText(info);
        }
        
        // Show navigation instruction if available
        updateNavigationInstruction();
        }
    }
    
    private void updateNavigationInstruction() {
        if (!isNavigating || mapManager == null) {
            binding.navigationInstruction.setVisibility(View.GONE);
            return;
        }
        
        // Get current navigation step from NavigationOverlay
        try {
            if (mapManager.getNavigationOverlay() != null) {
                com.example.bottlenex.routing.RoutePlanner.NavigationStep currentStep = 
                    mapManager.getNavigationOverlay().getCurrentStep();
                
                if (currentStep != null) {
                    String instruction = currentStep.instruction + " • " + 
                        formatDistance(currentStep.distance);
                    binding.navigationInstruction.setText(instruction);
                    binding.navigationInstruction.setVisibility(View.VISIBLE);
                } else {
                    binding.navigationInstruction.setVisibility(View.GONE);
                }
            } else {
                binding.navigationInstruction.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e("Navigation", "Error updating navigation instruction: " + e.getMessage());
            binding.navigationInstruction.setVisibility(View.GONE);
        }
    }
    
    private String formatDistance(double distanceMeters) {
        if (distanceMeters >= 1000) {
            return String.format("%.1f km", distanceMeters / 1000.0);
        } else if (distanceMeters >= 100) {
            int roundedMeters = (int) Math.round(distanceMeters / 10.0) * 10;
            return roundedMeters + "m";
        } else {
            return (int) distanceMeters + "m";
        }
    }
    
    private double calculateRemainingDistance(Location currentLocation) {
        if (currentRouteData == null || currentRouteData.navigationSteps == null || 
            currentRouteData.navigationSteps.isEmpty()) {
            return 0;
        }
        
        // Get the last step (destination)
        RoutePlanner.NavigationStep destination = currentRouteData.navigationSteps.get(
            currentRouteData.navigationSteps.size() - 1);
        
        float[] results = new float[1];
        Location.distanceBetween(
            currentLocation.getLatitude(), currentLocation.getLongitude(),
            destination.location.getLatitude(), destination.location.getLongitude(),
            results
        );
        
        return results[0];
    }
    
    private double calculateRemainingTime(double remainingDistance) {
        if (currentRouteData == null) {
            return 0;
        }
        
        // Calculate time based on original route speed
        double originalSpeed = currentRouteData.distance / currentRouteData.duration; // m/s
        return remainingDistance / originalSpeed; // seconds
    }
    
    private String formatTimeForDisplay(double timeInSeconds) {
        // Always show in minutes, minimum 1 minute
        int minutes = Math.max(1, (int) Math.ceil(timeInSeconds / 60.0));
        return minutes + " min";
    }
    
    private void preserveNavigationState() {
        // If we're currently navigating, make sure navigation info is displayed
        // Only preserve state if we have valid route data and are actually navigating
        if (isNavigating && currentRouteData != null && selectedLocation != null) {
            updateNavigationInfo();
        } else {
            // If state is inconsistent, reset to safe state
            isNavigating = false;
            currentRouteData = null;
            selectedLocation = null;
        }
    }
    
    private void getLocationAddress(GeoPoint point) {
        // Don't update location info if we're currently navigating
        if (isNavigating) {
            return;
        }
        
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(point.getLatitude(), point.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String locationName = getReadableAddress(address);
                binding.locationInfo.setText(locationName + " • Tap Navigate to start route");
            } else {
                // Fallback if geocoding fails
                binding.locationInfo.setText("Location Selected • Tap Navigate to start route");
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback if geocoding fails
            binding.locationInfo.setText("Location Selected • Tap Navigate to start route");
        }
    }
    
    private String getReadableAddress(Address address) {
        StringBuilder readableAddress = new StringBuilder();
        
        // First, check if we have a meaningful address line (for manually created addresses)
        if (address.getAddressLine(0) != null && !address.getAddressLine(0).trim().isEmpty()) {
            String addressLine = address.getAddressLine(0).trim();
            // If the address line contains more than just a city name, use it
            if (addressLine.contains(",") || addressLine.length() > 15) {
                readableAddress.append(addressLine);
            }
        }
        
        // If we don't have a meaningful address line, try to build from components
        if (readableAddress.length() == 0) {
            if (address.getThoroughfare() != null) {
                readableAddress.append(address.getThoroughfare()); // Street name
            }
            
            if (address.getSubThoroughfare() != null) {
                if (readableAddress.length() > 0) {
                    readableAddress.append(", ");
                }
                readableAddress.append(address.getSubThoroughfare()); // Street number
            }
            
            if (address.getSubLocality() != null) {
                if (readableAddress.length() > 0) {
                    readableAddress.append(", ");
                }
                readableAddress.append(address.getSubLocality()); // Neighborhood
            }
            
            if (address.getLocality() != null) {
                if (readableAddress.length() > 0) {
                    readableAddress.append(", ");
                }
                readableAddress.append(address.getLocality()); // City
            }
        }
        
        // If still empty, use a generic message
        if (readableAddress.length() == 0) {
            readableAddress.append("Selected Location");
        }
        
        return readableAddress.toString();
    }
    
    private void cancelRoute() {
        // Clear the route from the map
        mapManager.clearRoute();
        
        // Stop navigation if active
        if (isNavigating) {
            mapManager.stopNavigation();
        }
        
        // Hide navigation instruction
        binding.navigationInstruction.setVisibility(View.GONE);
        
        // Clear the selected location marker
        mapManager.clearMarkers();
        
        // Reset the selected location
        selectedLocation = null;
        currentRouteData = null;
        
        // Clear stored traffic analysis info
        originalLocationInfo = "";
        
        // Reset UI to initial state
        binding.locationInfo.setText("Tap on map to get location");
        binding.btnNavigate.setEnabled(false);
        binding.btnNavigate.setVisibility(View.VISIBLE);
        binding.btnCancelRoute.setVisibility(View.GONE);
        binding.btnJourney.setVisibility(View.GONE);
        binding.tvJourneyState.setVisibility(View.GONE);
        binding.trafficButtonsLayout.setVisibility(View.GONE);
        
        // Clear search view
        if (binding.searchView != null) {
            binding.searchView.setQuery("", false);
        }
        
        Toast.makeText(this, "Route cancelled", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapManager.onResume();
        if (checkPermissions()) {
            mapManager.startLocationUpdates();
        }
        
        // Restore navigation state if we were navigating
        preserveNavigationState();
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
        
        // Clean up search handler
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    private String getAddressFromLocation(Location location) {
        if (location == null) return "Unknown Location";
        return getAddressFromLocation(location.getLatitude(), location.getLongitude());
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressLine = address.getAddressLine(0);
                return addressLine != null ? addressLine : "Unknown Location";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown Location";
    }

    // Traffic Prediction Methods
    private void toggleTrafficOverlay() {
        Log.d("TrafficOverlay", "toggleTrafficOverlay called, current state: " + showTrafficOverlay);
        
        if (!showTrafficOverlay) {
            // Show time picker dialog when enabling traffic overlay
            showTrafficTimePickerDialog();
        } else {
            // Disable traffic overlay
            showTrafficOverlay = false;
            selectedTrafficTime = null; // Reset selected time
            disableTrafficOverlay();
        }
    }
    
    private void showTrafficTimePickerDialog() {
        // Create a custom dialog with time picker options
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Traffic Prediction Time");
        
        // Create custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_traffic_time_picker, null);
        builder.setView(dialogView);
        
        // Get references to dialog elements
        RadioButton rbCurrentTime = dialogView.findViewById(R.id.rbCurrentTime);
        RadioButton rbCustomTime = dialogView.findViewById(R.id.rbCustomTime);
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        
        // Initially hide time picker
        timePicker.setVisibility(View.GONE);
        
        // Show/hide time picker based on radio button selection
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
        
        // Set up dialog buttons
        builder.setPositiveButton("Predict", (dialog, which) -> {
            if (rbCurrentTime.isChecked()) {
                // Use current time
                selectedTrafficTime = null;
                
                // Clear the custom time in MapManager
                mapManager.setCustomTrafficTime(null);
                
                enableTrafficOverlay();
                Toast.makeText(this, "Predicting traffic for current time", Toast.LENGTH_SHORT).show();
            } else if (rbCustomTime.isChecked()) {
                // Use selected time
                Calendar now = Calendar.getInstance();
                Calendar selectedTime = Calendar.getInstance();
                selectedTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                selectedTime.set(Calendar.MINUTE, timePicker.getMinute());
                selectedTime.set(Calendar.SECOND, 0);
                selectedTime.set(Calendar.MILLISECOND, 0);
                
                // If selected time is in the past, add a day
                if (selectedTime.before(now)) {
                    selectedTime.add(Calendar.DAY_OF_YEAR, 1);
                }
                
                selectedTrafficTime = selectedTime;
                
                // Set the custom time in MapManager so TrafficOverlay can use it
                mapManager.setCustomTrafficTime(selectedTime);
                
                enableTrafficOverlay();
                
                String timeStr = String.format("%02d:%02d", selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE));
                Toast.makeText(this, "Predicting traffic for " + timeStr, Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        // Set current time as default
        rbCurrentTime.setChecked(true);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showCongestionInfoTimePickerDialog() {
        // Create a custom dialog with time picker options
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Congestion Analysis Time");
        
        // Create custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_traffic_time_picker, null);
        builder.setView(dialogView);
        
        // Get references to dialog elements
        RadioButton rbCurrentTime = dialogView.findViewById(R.id.rbCurrentTime);
        RadioButton rbCustomTime = dialogView.findViewById(R.id.rbCustomTime);
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        
        // Initially hide time picker
        timePicker.setVisibility(View.GONE);
        
        // Show/hide time picker based on radio button selection
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
        
        // Set up dialog buttons
        builder.setPositiveButton("Analyze Congestion", (dialog, which) -> {
            if (rbCurrentTime.isChecked()) {
                // Use current time
                selectedTrafficTime = null;
                
                // Clear the custom time in MapManager
                mapManager.setCustomTrafficTime(null);
                
                showTrafficPredictionForRoute();
                Toast.makeText(this, "Analyzing congestion for current time", Toast.LENGTH_SHORT).show();
            } else if (rbCustomTime.isChecked()) {
                // Use selected time
                Calendar now = Calendar.getInstance();
                Calendar selectedTime = Calendar.getInstance();
                selectedTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                selectedTime.set(Calendar.MINUTE, timePicker.getMinute());
                selectedTime.set(Calendar.SECOND, 0);
                selectedTime.set(Calendar.MILLISECOND, 0);
                
                // If selected time is in the past, add a day
                if (selectedTime.before(now)) {
                    selectedTime.add(Calendar.DAY_OF_YEAR, 1);
                }
                
                selectedTrafficTime = selectedTime;
                
                // Set the custom time in MapManager so TrafficOverlay can use it
                mapManager.setCustomTrafficTime(selectedTime);
                
                showTrafficPredictionForRoute();
                
                String timeStr = String.format("%02d:%02d", selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE));
                Toast.makeText(this, "Analyzing congestion for " + timeStr, Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        // Set current time as default
        rbCurrentTime.setChecked(true);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void enableTrafficOverlay() {
        showTrafficOverlay = true;
        mapManager.showTrafficOverlay(true);
        startTrafficUpdates();
        Log.d("TrafficOverlay", "Traffic overlay enabled");
        mapManager.forceRefreshTrafficOverlay();
        
        // Additional debugging
        Log.d("TrafficOverlay", "MapView null check: " + (binding.mapView == null));
        if (binding.mapView != null) {
            binding.mapView.getWidth();
            binding.mapView.getHeight();
        }
        
        // Force map refresh to ensure overlay is visible
        if (binding.mapView != null) {
            binding.mapView.invalidate();
            binding.mapView.postInvalidate();
            Log.d("TrafficOverlay", "Map invalidate called");
        } else {
            Log.w("TrafficOverlay", "MapView is null, cannot invalidate");
        }
    }
    
    private void disableTrafficOverlay() {
        mapManager.showTrafficOverlay(false);
        stopTrafficUpdates();
        Toast.makeText(this, "Traffic overlay disabled", Toast.LENGTH_SHORT).show();
        Log.d("TrafficOverlay", "Traffic overlay disabled");
        
        // Force map refresh to ensure overlay is hidden
        if (binding.mapView != null) {
            binding.mapView.invalidate();
            binding.mapView.postInvalidate();
            Log.d("TrafficOverlay", "Map invalidate called");
        } else {
            Log.w("TrafficOverlay", "MapView is null, cannot invalidate");
        }
    }
    
    private void startTrafficUpdates() {
        // Update traffic predictions every 5 minutes
        trafficUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (showTrafficOverlay) {
                    Log.d("TrafficUpdates", "Updating traffic predictions, selectedTrafficTime: " + 
                          (selectedTrafficTime != null ? 
                           String.format("%02d:%02d", selectedTrafficTime.get(Calendar.HOUR_OF_DAY), selectedTrafficTime.get(Calendar.MINUTE)) : 
                           "null (current time)"));
                    mapManager.updateTrafficPredictions();
                    trafficUpdateHandler.postDelayed(this, 5 * 60 * 1000); // 5 minutes
                }
            }
        };
        trafficUpdateHandler.post(trafficUpdateRunnable);
    }
    
    private void stopTrafficUpdates() {
        if (trafficUpdateRunnable != null) {
            trafficUpdateHandler.removeCallbacks(trafficUpdateRunnable);
        }
    }
    
    private String originalLocationInfo = ""; // Store original location info
    private TensorFlowTrafficPredictor mlPredictor; // ML-based traffic predictor
    
    private void showTrafficPredictionForRoute() {
        if (currentRouteData == null || currentRouteData.routePoints == null) {
            Toast.makeText(this, "No route selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if we're currently showing traffic info - if so, restore original
        String currentText = binding.locationInfo.getText().toString();
        if (currentText.contains("Traffic Analysis")) {
            restoreOriginalLocationInfo();
            Toast.makeText(this, "Route info restored", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Store original location info if not already stored
        if (originalLocationInfo.isEmpty()) {
            originalLocationInfo = binding.locationInfo.getText().toString();
        }
        
        // Analyze traffic along the route
        StringBuilder trafficInfo = new StringBuilder();
        trafficInfo.append("Traffic Analysis\n\n");
        
        // Check traffic at start, middle, and end points using ML predictions
        List<GeoPoint> routePoints = currentRouteData.routePoints;
        if (!routePoints.isEmpty()) {
            // Use ML predictor for dynamic traffic analysis
            String startTraffic = getMLTrafficPrediction(routePoints.get(0), 1);
            String endTraffic = getMLTrafficPrediction(routePoints.get(routePoints.size() - 1), 2);
            
            // Get traffic level colors
            int startColor = getTrafficLevelColor(startTraffic);
            int endColor = getTrafficLevelColor(endTraffic);
            
            trafficInfo.append("Start: ").append(startTraffic).append(" Traffic\n");
            trafficInfo.append("End: ").append(endTraffic).append(" Traffic\n");
            
            // Check middle point if route is long enough
            if (routePoints.size() > 2) {
                String middleTraffic = getMLTrafficPrediction(routePoints.get(routePoints.size() / 2), 3);
                trafficInfo.append("Middle: ").append(middleTraffic).append(" Traffic\n");
            }
            
            // Add overall route assessment
            String overallTraffic = getOverallRouteTraffic(startTraffic, endTraffic);
            trafficInfo.append("\nOverall Route: ").append(overallTraffic).append(" Traffic");
            
            // Add instruction to restore original view
            trafficInfo.append("\n\nTap 'Traffic Analysis' again to return");
        }
        
        // Display traffic info in the existing location info area
        binding.locationInfo.setText(trafficInfo.toString());
        
        // Show a brief toast to confirm the action
        Toast.makeText(this, "Traffic analysis displayed", Toast.LENGTH_SHORT).show();
    }
    
    private void restoreOriginalLocationInfo() {
        if (!originalLocationInfo.isEmpty()) {
            binding.locationInfo.setText(originalLocationInfo);
            originalLocationInfo = ""; // Clear stored info
        }
    }
    
    private String getOverallRouteTraffic(String startTraffic, String endTraffic) {
        // Simple logic to determine overall route traffic
        if (startTraffic.equals("High") || endTraffic.equals("High")) {
            return "High";
        } else if (startTraffic.equals("Medium") || endTraffic.equals("Medium")) {
            return "Medium";
        } else {
            return "Low";
        }
    }
    
    private int getTrafficLevelColor(String trafficLevel) {
        switch (trafficLevel) {
            case "Low":
                return 0xFF4CAF50; // Green
            case "Medium":
                return 0xFFFF9800; // Orange
            case "High":
                return 0xFFF44336; // Red
            default:
                return 0xFF4CAF50; // Default green
        }
    }
    
    /**
     * Get ML-based traffic prediction for a location
     */
    private String getMLTrafficPrediction(GeoPoint location, int junctionNumber) {
        try {
            if (mlPredictor != null) {
                String prediction;
                if (selectedTrafficTime != null) {
                    // Use selected time for prediction
                    prediction = mlPredictor.getTrafficPredictionForTime(junctionNumber, selectedTrafficTime);
                    Log.d("TrafficAnalysis", "ML Prediction for junction " + junctionNumber + " at " + 
                          String.format("%02d:%02d", selectedTrafficTime.get(Calendar.HOUR_OF_DAY), selectedTrafficTime.get(Calendar.MINUTE)) + 
                          ": " + prediction);
                } else {
                    // Use current time for prediction
                    prediction = mlPredictor.getCurrentTrafficPrediction(junctionNumber);
                    Log.d("TrafficAnalysis", "ML Prediction for junction " + junctionNumber + " (current time): " + prediction);
                }
                return prediction;
            } else {
                Log.w("TrafficAnalysis", "ML predictor not initialized, using fallback");
                return getFallbackTrafficPrediction(junctionNumber);
            }
        } catch (Exception e) {
            Log.e("TrafficAnalysis", "Error getting ML prediction: " + e.getMessage());
            return getFallbackTrafficPrediction(junctionNumber);
        }
    }
    
    /**
     * Fallback traffic prediction when ML is not available
     */
    private String getFallbackTrafficPrediction(int junctionNumber) {
        java.util.Calendar cal;
        if (selectedTrafficTime != null) {
            cal = selectedTrafficTime;
        } else {
            cal = java.util.Calendar.getInstance();
        }
        
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        
        // Simple time-based logic
        if (hour >= 7 && hour <= 9) { // Morning peak
            return junctionNumber % 2 == 0 ? "High" : "Medium";
        } else if (hour >= 17 && hour <= 19) { // Evening peak
            return junctionNumber % 2 == 0 ? "High" : "Medium";
        } else if (hour >= 22 || hour <= 5) { // Night
            return "Low";
        } else {
            return junctionNumber % 3 == 0 ? "High" : junctionNumber % 3 == 1 ? "Medium" : "Low";
        }
    }

    private void saveJourneyToHistory() {
        if (journeyStartLocation == null) {
            Log.w("RouteHistory", "Cannot save journey: no start location");
            return;
        }

        if (journeyDestination == null) {
            Log.w("RouteHistory", "Cannot save journey: no destination");
            return;
        }

        // Calculate journey duration
        long journeyEndTime = System.currentTimeMillis();
        double durationSeconds = (journeyEndTime - journeyStartTime) / 1000.0;

        journeyEndAddress = getAddressFromLocation(journeyDestination.getLatitude(), journeyDestination.getLongitude());

        if (journeyDistance == 0.0) {
            float[] results = new float[1];
            Location.distanceBetween(
                journeyStartLocation.getLatitude(), journeyStartLocation.getLongitude(),
                journeyDestination.getLatitude(), journeyDestination.getLongitude(),
                results
            );
            journeyDistance = results[0];
        }

        // Format times
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        String startTime = timeFormat.format(new java.util.Date(journeyStartTime));
        String endTime = timeFormat.format(new java.util.Date(journeyEndTime));
        String date = dateFormat.format(new java.util.Date(journeyStartTime));

        // Create route history object
        RouteHistory routeHistory = new RouteHistory(
            journeyStartLocation.getLatitude(),
            journeyStartLocation.getLongitude(),
            journeyDestination.getLatitude(),
            journeyDestination.getLongitude(),
            journeyStartAddress,
            journeyEndAddress,
            journeyDistance,
            durationSeconds,
            startTime,
            endTime,
            date
        );

        // save to db
        long result = databaseHelper.insertRouteHistory(routeHistory);
        if (result != -1) {
            Log.d("RouteHistory", "Journey saved successfully with ID: " + result);
            Log.d("RouteHistory", "Route details: " + routeHistory.getFormattedRoute());
            Log.d("RouteHistory", "Distance: " + routeHistory.getFormattedDistance());
            Log.d("RouteHistory", "Duration: " + routeHistory.getFormattedDuration());
            Toast.makeText(this, "Journey saved to history", Toast.LENGTH_SHORT).show();
        } else {
            Log.e("RouteHistory", "Failed to save journey");
            Toast.makeText(this, "Failed to save journey", Toast.LENGTH_SHORT).show();
        }

        journeyStartLocation = null;
        journeyDestination = null;
        journeyStartAddress = null;
        journeyEndAddress = null;
        journeyDistance = 0.0;
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

                // Clear only markers, not the location overlay
                mapManager.clearMarkers();
                loadStarredPlaces();

                Marker marker = new Marker(binding.mapView);
                marker.setPosition(point);
                marker.setTitle("⭐ Starred: " + name);
                Drawable icon = getResources().getDrawable(R.drawable.star);
                marker.setIcon(icon);
                binding.mapView.getOverlays().add(marker);

                binding.mapView.invalidate();

                binding.locationInfo.setText(String.format("Starred Place: %s\nLat: %.6f, Lon: %.6f", name, lat, lon));
                
                // Enable navigate button when a starred place is selected
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

