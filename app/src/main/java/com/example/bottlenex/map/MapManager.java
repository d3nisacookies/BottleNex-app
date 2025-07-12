package com.example.bottlenex.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import android.os.Looper;


import androidx.core.app.ActivityCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.Polyline;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;


import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MapManager {
    
    private static final String TAG = "MapManager";
    private static final int DEFAULT_ZOOM = 15;
    private static final double DEFAULT_LAT = 1.3521; // SINGAPORE
    private static final double DEFAULT_LON = 103.8198;

    private final Context context;
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Polyline routeLine;
    private Location lastKnownLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;




    private final List<Marker> markers = new ArrayList<>();
    private OnMapClickListener onMapClickListener;
    private OnLocationUpdateListener onLocationUpdateListener;
    
    @Inject
    public MapManager(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context); // ✅ add this line
        try {
            initializeOSMDroid();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OSMDroid", e);
        }
    }
    
    private void initializeOSMDroid() {
        try {
            // Set user agent to prevent getting banned from the OSM servers
            Configuration.getInstance().setUserAgentValue(context.getPackageName());
        } catch (Exception e) {
            Log.e(TAG, "Error setting user agent", e);
        }
    }
    
    public void setupMap(MapView mapView) {
        try {
            this.mapView = mapView;
            
            if (mapView == null) {
                Log.e(TAG, "MapView is null");
                return;
            }
            
            // Set tile source
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            
            // Enable multi-touch gestures
            mapView.setMultiTouchControls(true);
            
            // Get map controller
            mapController = mapView.getController();
            
            // Set initial position and zoom
            mapController.setZoom(DEFAULT_ZOOM);
            mapController.setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LON));
            
            // Setup location overlay
            setupLocationOverlay();
            
            // Setup map click listener
            setupMapClickListener();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up map", e);
        }
    }
    
    private void setupLocationOverlay() {
        try {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), mapView);
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.runOnFirstFix(() -> {
                Location fix = new Location(LocationManager.GPS_PROVIDER);
                fix.setLatitude(myLocationOverlay.getMyLocation().getLatitude());
                fix.setLongitude(myLocationOverlay.getMyLocation().getLongitude());

                lastKnownLocation = fix;

                if (onLocationUpdateListener != null) {
                    onLocationUpdateListener.onLocationUpdate(fix);
                }

                Log.d("OVERLAY_FIX", "First fix from overlay: " + fix.getLatitude() + ", " + fix.getLongitude());
            });

            mapView.getOverlays().add(myLocationOverlay);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up location overlay", e);
        }
    }
    
    private void setupMapClickListener() {
        try {
            mapView.getOverlays().add(new org.osmdroid.views.overlay.Overlay() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent event, MapView mapView) {
                    try {
                        if (onMapClickListener != null) {
                            org.osmdroid.api.IGeoPoint iGeoPoint = mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
                            GeoPoint point = new GeoPoint(iGeoPoint.getLatitude(), iGeoPoint.getLongitude());
                            onMapClickListener.onMapClick(point);
                            return true;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling map click", e);
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up map click listener", e);
        }
    }
    
    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location newLocation = locationResult.getLastLocation();
                double lat = newLocation != null ? newLocation.getLatitude() : 0.0;
                double lon = newLocation != null ? newLocation.getLongitude() : 0.0;

                if (newLocation != null && lat != 0.0 && !(lat == 37.4220936 && lon == -122.083922)) {
                    lastKnownLocation = newLocation;
                    Log.d("LOCATION_UPDATE", "Lat: " + lat + ", Lon: " + lon);
                    if (onLocationUpdateListener != null) {
                        onLocationUpdateListener.onLocationUpdate(newLocation);
                    }
                } else {
                    Log.w("LOCATION_UPDATE", "Received fallback/default location, ignoring.");
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }



    public void zoomIn() {
        try {
            if (mapController != null) {
                mapController.zoomIn();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error zooming in", e);
        }
    }
    
    public void zoomOut() {
        try {
            if (mapController != null) {
                mapController.zoomOut();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error zooming out", e);
        }
    }
    
    public void centerOnMyLocation() {
        try {
            if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                mapController.animateTo(myLocationOverlay.getMyLocation());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error centering on location", e);
        }
    }
    
    public void addMarker(GeoPoint point, String title) {
        try {
            if (mapView != null) {
                Marker marker = new Marker(mapView);
                marker.setPosition(point);
                marker.setTitle(title);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
                markers.add(marker);
                mapView.invalidate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding marker", e);
        }
    }
    
    public void clearMarkers() {
        try {
            if (mapView != null) {
                for (Marker marker : markers) {
                    mapView.getOverlays().remove(marker);
                }
                markers.clear();
                mapView.invalidate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing markers", e);
        }
    }
    
    public void setOnMapClickListener(OnMapClickListener listener) {
        this.onMapClickListener = listener;
    }
    
    public void setOnLocationUpdateListener(OnLocationUpdateListener listener) {
        this.onLocationUpdateListener = listener;
    }
    
    public void onResume() {
        try {
            if (mapView != null) {
                mapView.onResume();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resuming map", e);
        }
    }
    
    public void onPause() {
        try {
            if (mapView != null) {
                mapView.onPause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pausing map", e);
        }
    }

    // <-- Add this method to expose zoom level -->
    public double getZoomLevel() {
        if (mapView != null) {
            return mapView.getZoomLevelDouble();
        }
        return 0;
    }

    public interface OnMapClickListener {
        void onMapClick(GeoPoint point);
    }
    
    public interface OnLocationUpdateListener {
        void onLocationUpdate(Location location);
    }
    public Location getLastKnownLocation() {
        Log.d("DEBUG_LOCATION", "Returning last known location: " + lastKnownLocation);
        return lastKnownLocation;
    }

    public void drawRoute(List<GeoPoint> points) {
        if (routeLine != null && mapView != null) {
            mapView.getOverlays().remove(routeLine);
        }

        routeLine = new Polyline();
        routeLine.setPoints(points);
        routeLine.setColor(Color.BLUE);
        routeLine.setWidth(8f);

        mapView.getOverlays().add(routeLine);
        mapView.invalidate();
    }

}