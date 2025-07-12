package com.example.bottlenex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.SearchView;
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
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

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
            Manifest.permission.INTERNET // Added for Geocoder
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
        SearchView searchView = binding.searchView; // Assuming your binding has a SearchView with id 'searchView'
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query == null || query.trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    performSearch(query.trim());
                    // Optionally, clear focus to hide keyboard
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // No action needed for now
                    return false;
                }
            });
        }

        // Old search buttons, you can keep or remove if not used anymore
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

        binding.btnBookmark.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Bookmark.class);
            startActivity(intent);
        });

        binding.btnCar.setOnClickListener(v ->
                Toast.makeText(this, "Car clicked", Toast.LENGTH_SHORT).show());

        binding.btnPersonalTools.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PersonalTools.class);
            startActivity(intent);
        });
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

            // Clear old markers (if using osmdroid directly, else use your MapManager)
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
        if (selectedLocation == null) {
            String locationText = String.format("My Location: %.6f, %.6f",
                    location.getLatitude(), location.getLongitude());
            binding.locationInfo.setText(locationText);
        }

        if (currentUser != null) {
            firebaseService.saveUserLocation(
                    currentUser.getUid(),
                    location.getLatitude(),
                    location.getLongitude(),
                    aVoid -> {}
            );
        }
    }

    private void onNavigateClicked() {
        if (selectedLocation != null) {
            Toast.makeText(this, "Navigation to selected location", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show();
        }
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