package com.example.bottlenex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bottlenex.databinding.ActivityMainBinding;
import com.example.bottlenex.map.MapManager;
import com.example.bottlenex.services.FirebaseService;
import com.google.firebase.auth.FirebaseUser;

import org.osmdroid.util.GeoPoint;

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

        binding.btnZoomIn.setOnClickListener(v -> mapManager.zoomIn());
        binding.btnZoomOut.setOnClickListener(v -> mapManager.zoomOut());
        binding.btnMyLocation.setOnClickListener(v -> mapManager.centerOnMyLocation());

        binding.btnSearchLeft.setOnClickListener(v ->
                Toast.makeText(this, "Search icon clicked", Toast.LENGTH_SHORT).show());

        binding.searchText.setOnClickListener(v ->
                Toast.makeText(this, "Search text clicked", Toast.LENGTH_SHORT).show());

        binding.btnSearchRight.setOnClickListener(v ->
                Toast.makeText(this, "Voice input clicked", Toast.LENGTH_SHORT).show());

        binding.btnNavigate.setOnClickListener(v -> onNavigateClicked());

        ImageButton btnHome = findViewById(R.id.btnHome);
        ImageButton btnSearch = findViewById(R.id.btnSearc);
        ImageButton btnProfile = findViewById(R.id.btnProfile);

        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SavedPlacesActivity.class);
            startActivity(intent);
        });

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        btnProfile.setOnClickListener(v ->
                Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show());
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
