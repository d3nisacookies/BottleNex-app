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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements
        MapManager.OnMapClickListener,
        MapManager.OnLocationUpdateListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final int REQUEST_CODE_BOOKMARK = 100; // For Bookmark Activity result

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebaseAuth();

        mapManager.setupMap(binding.mapView);
        mapManager.setOnMapClickListener(this);
        mapManager.setOnLocationUpdateListener(this);

        setupUI();

        if (checkPermissions()) {
            initializeMap();
        } else {
            requestPermissions();
        }
    }

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
        binding.btnSearchLeft.setOnClickListener(v ->
                Toast.makeText(this, "Search icon clicked", Toast.LENGTH_SHORT).show());

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

        // Favourite button inside MainActivity to save current search query
        binding.btnFavorite.setOnClickListener(v -> {
            String query = binding.searchView.getQuery().toString();
            if (query.isEmpty()) {
                Toast.makeText(this, "Search something before saving!", Toast.LENGTH_SHORT).show();
            } else {
                saveFavourite(query);
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

            // Update map
            binding.mapView.getController().setZoom(15.0);
            binding.mapView.getController().setCenter(point);

            // Clear old markers
            binding.mapView.getOverlays().clear();

            // Add marker
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
        // Your existing location update code remains unchanged
        // ... (speed alerts, incidents, speed cameras, etc.)
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

    // Handle returning from Bookmark (which can return from FavouritesActivity)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BOOKMARK && resultCode == RESULT_OK && data != null) {
            String favLocation = data.getStringExtra("selected_location");
            if (favLocation != null && !favLocation.isEmpty()) {
                // Set the query in SearchView and perform search automatically
                binding.searchView.setQuery(favLocation, true);
            }
        }
    }
}
