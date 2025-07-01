package com.example.bottlenex;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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
import com.google.android.material.button.MaterialButton;

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
    
    private GeoPoint selectedLocation;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
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
        // Update location info if no location is selected
        if (selectedLocation == null) {
            String locationText = String.format("My Location: %.6f, %.6f", 
                    location.getLatitude(), location.getLongitude());
            binding.locationInfo.setText(locationText);
        }
    }
    
    private void onSearchClicked() {
        // TODO: Implement search functionality
        Toast.makeText(this, "Search functionality coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    private void onNavigateClicked() {
        if (selectedLocation != null) {
            // TODO: Implement navigation functionality
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