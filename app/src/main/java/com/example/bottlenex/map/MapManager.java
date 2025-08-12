package com.example.bottlenex.map;

//To commit

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
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.Polyline;

import com.example.bottlenex.routing.RoutePlanner;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MapManager {
    
    private static final String TAG = "MapManager";
    private static final int DEFAULT_ZOOM = 18; // Increased from 17 to 18 for even better default zoom
    private static final double DEFAULT_LAT = 1.3521; // SINGAPORE
    private static final double DEFAULT_LON = 103.8198;

    private final Context context;
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Polyline routeLine;
    private Polyline mainRouteLine;
    private Polyline alternativeRouteLine;
    private Location lastKnownLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private NavigationOverlay navigationOverlay;
    private TrafficOverlay trafficOverlay;
    private final List<Marker> markers = new ArrayList<>();
    private OnMapClickListener onMapClickListener;
    private OnLocationUpdateListener onLocationUpdateListener;
    
    // Auto-follow functionality
    private boolean isAutoFollowEnabled = false; // Only enabled during navigation
    private boolean isNavigating = false;
    private boolean hasInitialLocation = false; // Track if we've centered on initial location
    private long lastManualInteraction = 0;
    private static final long AUTO_FOLLOW_DELAY = 7000; // 7 seconds - longer pause for better user experience
    
    // Movement detection for auto-follow
    private Location previousLocation = null;
    private static final float MOVEMENT_THRESHOLD = 5.0f; // 5 meters - minimum distance to consider as movement
    
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
            
                    // Setup navigation overlay
        navigationOverlay = new NavigationOverlay(context, mapView);
        mapView.getOverlays().add(navigationOverlay);
        
        // Setup traffic overlay
        trafficOverlay = new TrafficOverlay(context, mapView);
        mapView.getOverlays().add(trafficOverlay);
        Log.d(TAG, "Traffic overlay added to map. Total overlays: " + mapView.getOverlays().size());
            
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

                if (newLocation != null) {
                    double lat = newLocation.getLatitude();
                    double lon = newLocation.getLongitude();

                    Log.d("LOCATION_DEBUG", "Received Location: " + lat + ", " + lon);

                    if (lat != 0.0 && !(lat == 37.4220936 && lon == -122.083922)) {
                        lastKnownLocation = newLocation;
                        
                        // Update navigation overlay with current location
                        if (navigationOverlay != null) {
                            navigationOverlay.updateCurrentLocation(newLocation);
                        }
                        
                        // Movement-based auto-follow logic
                        if (isAutoFollowEnabled && isNavigating) {
                            // Auto-follow during active navigation
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastManualInteraction > AUTO_FOLLOW_DELAY) {
                                centerOnLocation(newLocation);
                            }
                        } else if (!hasInitialLocation) {
                            // Center on location once at launch
                            centerOnLocation(newLocation);
                            hasInitialLocation = true;
                        } else if (!isNavigating) {
                            // Auto-follow when user is moving (outside navigation mode)
                            if (previousLocation != null) {
                                float distance = newLocation.distanceTo(previousLocation);
                                if (distance > MOVEMENT_THRESHOLD) {
                                    // User is moving, enable auto-follow
                                    long currentTime = System.currentTimeMillis();
                                    if (currentTime - lastManualInteraction > AUTO_FOLLOW_DELAY) {
                                        centerOnLocation(newLocation);
                                        Log.d(TAG, "Auto-following user movement: " + distance + "m");
                                    }
                                }
                            }
                        }
                        
                        // Update previous location for next comparison
                        previousLocation = newLocation;
                        
                        if (onLocationUpdateListener != null) {
                            onLocationUpdateListener.onLocationUpdate(newLocation);
                        }
                    } else {
                        Log.w("LOCATION_DEBUG", "This is the fallback location from Google.");
                    }
                } else {
                    Log.w("LOCATION_DEBUG", "Null location received.");
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
        // First try to get from our stored location
        if (lastKnownLocation != null) {
            Log.d("DEBUG_LOCATION", "Returning stored last known location: " + lastKnownLocation);
            return lastKnownLocation;
        }
        
        // Fallback: try to get from MyLocationNewOverlay if available
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint geoPoint = myLocationOverlay.getMyLocation();
            Location overlayLocation = new Location("MyLocationOverlay");
            overlayLocation.setLatitude(geoPoint.getLatitude());
            overlayLocation.setLongitude(geoPoint.getLongitude());
            overlayLocation.setTime(System.currentTimeMillis());
            Log.d("DEBUG_LOCATION", "Fallback: returning location from overlay: " + overlayLocation);
            return overlayLocation;
        }
        
        Log.d("DEBUG_LOCATION", "No location available from any source");
        return null;
    }
    
    public boolean isLocationAvailable() {
        return getLastKnownLocation() != null;
    }
    
    public boolean isLocationServicesEnabled() {
        if (myLocationOverlay != null) {
            return myLocationOverlay.isMyLocationEnabled();
        }
        return false;
    }

    public void drawRoute(List<GeoPoint> points) {
        if (routeLine != null && mapView != null) {
            mapView.getOverlays().remove(routeLine);
        }

        routeLine = new Polyline();
        routeLine.setPoints(points);
        routeLine.setColor(Color.BLUE);
        routeLine.setWidth(6f); // Visible route line thickness

        mapView.getOverlays().add(routeLine);

        fitToRoute(points, 12);

        mapView.invalidate();
    }

    public void drawDualRoutes(List<GeoPoint> mainRoutePoints, List<GeoPoint> alternativeRoutePoints) {
        clearAllRoutes();

        // main route blue line
        mainRouteLine = new Polyline();
        mainRouteLine.setPoints(mainRoutePoints);
        mainRouteLine.setColor(Color.BLUE);
        mainRouteLine.setWidth(8f);

        // alt route grey
        alternativeRouteLine = new Polyline();
        alternativeRouteLine.setPoints(alternativeRoutePoints);
        alternativeRouteLine.setColor(Color.MAGENTA);
        alternativeRouteLine.setWidth(4f);

        mapView.getOverlays().add(mainRouteLine);
        mapView.getOverlays().add(alternativeRouteLine);

        List<GeoPoint> combined = new ArrayList<>(mainRoutePoints);
        combined.addAll(alternativeRoutePoints);
        fitToRoute(combined, 12);

        mapView.invalidate();
    }

    public void selectMainRoute() {
        if (mainRouteLine != null && alternativeRouteLine != null) {
            mainRouteLine.setColor(Color.BLUE);
            mainRouteLine.setWidth(8f);
            alternativeRouteLine.setColor(Color.MAGENTA);
            alternativeRouteLine.setWidth(4f);
            mapView.invalidate();
        }
    }

    public void selectAlternativeRoute() {
        if (mainRouteLine != null && alternativeRouteLine != null) {
            mainRouteLine.setColor(Color.DKGRAY);
            mainRouteLine.setWidth(4f);
            alternativeRouteLine.setColor(Color.BLUE);
            alternativeRouteLine.setWidth(8f);
            mapView.invalidate();
        }
    }

    public void clearAlternativeRoutes() {
        if (mainRouteLine != null && mapView != null) {
            mapView.getOverlays().remove(mainRouteLine);
            mainRouteLine = null;
        }
        if (alternativeRouteLine != null && mapView != null) {
            mapView.getOverlays().remove(alternativeRouteLine);
            alternativeRouteLine = null;
        }
        mapView.invalidate();
    }

    private void clearAllRoutes() {
        if (routeLine != null && mapView != null) {
            mapView.getOverlays().remove(routeLine);
            routeLine = null;
        }
        if (mainRouteLine != null && mapView != null) {
            mapView.getOverlays().remove(mainRouteLine);
            mainRouteLine = null;
        }
        if (alternativeRouteLine != null && mapView != null) {
            mapView.getOverlays().remove(alternativeRouteLine);
            alternativeRouteLine = null;
        }
    }

    public void clearRoute() {
        try {
            clearAllRoutes();
            
            // Also clear traffic overlay when route is cleared
            if (trafficOverlay != null) {
                trafficOverlay.setTrafficOverlayVisible(false);
                Log.d(TAG, "Traffic overlay cleared along with route");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing route", e);
        }
    }

    public void startNavigation(List<RoutePlanner.NavigationStep> navigationSteps) {
        try {
            if (navigationOverlay != null) {
                navigationOverlay.setNavigationSteps(navigationSteps);
                mapView.invalidate();
            }
            // Enable auto-follow during navigation
            isNavigating = true;
            enableAutoFollow();
        } catch (Exception e) {
            Log.e(TAG, "Error starting navigation", e);
        }
    }

    public void stopNavigation() {
        try {
            if (navigationOverlay != null) {
                navigationOverlay.setNavigationSteps(null);
                mapView.invalidate();
            }
            // Disable auto-follow after navigation (user can explore freely)
            isNavigating = false;
            disableAutoFollow();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping navigation", e);
        }
    }

    public boolean isNearDestination() {
        if (navigationOverlay != null) {
            return navigationOverlay.isNearDestination();
        }
        return false;
    }
    
    // Auto-follow methods
    public void enableAutoFollow() {
        isAutoFollowEnabled = true;
        Log.d(TAG, "Auto-follow enabled");
    }
    
    public void disableAutoFollow() {
        isAutoFollowEnabled = false;
        Log.d(TAG, "Auto-follow disabled");
    }
    
    public boolean isAutoFollowEnabled() {
        return isAutoFollowEnabled;
    }
    
    public void resetAutoFollowState() {
        isAutoFollowEnabled = false;
        hasInitialLocation = false;
        lastManualInteraction = 0;
        previousLocation = null; // Reset movement tracking
        Log.d(TAG, "Auto-follow state reset to initial state");
    }
    
    public void onManualInteraction() {
        lastManualInteraction = System.currentTimeMillis();
        previousLocation = null; // Reset movement tracking to prevent immediate auto-follow
        Log.d(TAG, "Manual interaction detected, auto-follow paused and movement tracking reset");
    }
    
    private void centerOnLocation(Location location) {
        if (mapController != null && location != null) {
            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapController.animateTo(point);
        }
    }
    
    public void centerOnDestination(GeoPoint destination) {
        if (mapController != null && destination != null) {
            // Temporarily pause auto-follow to show destination
            onManualInteraction(); // This pauses auto-follow for 7 seconds
            mapController.animateTo(destination);
            
            // Auto-follow will resume automatically after 7 seconds
        }
    }

    // Traffic Overlay Methods
    public void showTrafficOverlay(boolean show) {
        Log.d(TAG, "showTrafficOverlay called with show=" + show + ", trafficOverlay null=" + (trafficOverlay == null));
        if (trafficOverlay != null) {
            trafficOverlay.setTrafficOverlayVisible(show);
            Log.d(TAG, "Traffic overlay visibility set to: " + show);
        } else {
            Log.w(TAG, "Traffic overlay is null, cannot set visibility");
        }
    }
    
    public void updateTrafficPredictions() {
        if (trafficOverlay != null) {
            trafficOverlay.updateTrafficPredictions();
        }
    }
    
    public void forceRefreshTrafficOverlay() {
        if (trafficOverlay != null) {
            trafficOverlay.forceRefresh();
        }
    }
    
    public String getTrafficLevelForLocation(GeoPoint location) {
        if (trafficOverlay != null) {
            return trafficOverlay.getTrafficLevelForLocation(location);
        }
        return "Low";
    }
    
    public String getTrafficLevelForRoad(String roadName) {
        if (trafficOverlay != null) {
            return trafficOverlay.getTrafficLevelForRoad(roadName);
        }
        return "Low";
    }
    
    public NavigationOverlay getNavigationOverlay() {
        return navigationOverlay;
    }
    
    public TrafficOverlay getTrafficOverlay() {
        return trafficOverlay;
    }
    
    /**
     * Set custom time for traffic prediction
     */
    public void setCustomTrafficTime(Calendar customTime) {
        if (trafficOverlay != null) {
            trafficOverlay.setCustomTrafficTime(customTime);
        }
    }

    // zoom out when route is selected to navigate from start to end
    private void fitToRoute(List<GeoPoint> points, int paddingPercent) {
        if (mapView == null || points == null || points.isEmpty()) return;
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (GeoPoint p : points) {
            if (p == null) continue;
            double lat = p.getLatitude();
            double lon = p.getLongitude();
            if (lat < minLat) minLat = lat;
            if (lat > maxLat) maxLat = lat;
            if (lon < minLon) minLon = lon;
            if (lon > maxLon) maxLon = lon;
        }
        if (minLat == Double.MAX_VALUE) return;
        // Expand bbox by padding percent
        double latSpan = Math.max(0.0005, maxLat - minLat);
        double lonSpan = Math.max(0.0005, maxLon - minLon);
        double padLat = latSpan * (paddingPercent / 100.0);
        double padLon = lonSpan * (paddingPercent / 100.0);
        BoundingBox bbox = new BoundingBox(maxLat + padLat, maxLon + padLon, minLat - padLat, minLon - padLon);
        mapView.zoomToBoundingBox(bbox, true);
    }

}