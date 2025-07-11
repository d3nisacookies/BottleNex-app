package com.example.bottlenex.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MapManager {

    private static final String TAG = "MapManager";
    private static final int DEFAULT_ZOOM = 15;
    private static final double DEFAULT_LAT = 37.7749; // San Francisco
    private static final double DEFAULT_LON = -122.4194;

    private final Context context;
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private final List<Marker> markers = new ArrayList<>();
    private OnMapClickListener onMapClickListener;
    private OnLocationUpdateListener onLocationUpdateListener;

    @Inject
    public MapManager(Context context) {
        this.context = context;
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
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show();
                return;
            }

            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (onLocationUpdateListener != null) {
                        onLocationUpdateListener.onLocationUpdate(location);
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000, // 5 seconds
                    10,   // 10 meters
                    locationListener
            );
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates", e);
        }
    }

    public void stopLocationUpdates() {
        try {
            if (locationManager != null && locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location updates", e);
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
}
