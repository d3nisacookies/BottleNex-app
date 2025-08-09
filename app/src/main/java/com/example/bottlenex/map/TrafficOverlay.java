package com.example.bottlenex.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.*;

import com.example.bottlenex.ml.TensorFlowTrafficPredictor;

/**
 * Traffic overlay for displaying predicted traffic conditions on the map
 * Focuses on user-friendly visualization that drivers actually find useful
 */
public class TrafficOverlay extends Overlay {
    private static final String TAG = "TrafficOverlay";
    
    private Context context;
    private MapView mapView;
    private Paint trafficPaint;
    private Map<String, String> roadTrafficLevels;
    private List<TrafficSegment> trafficSegments;
    private boolean showTrafficOverlay = false;
    private TensorFlowTrafficPredictor mlPredictor;
    
    public TrafficOverlay(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.roadTrafficLevels = new HashMap<>();
        this.trafficSegments = new ArrayList<>();
        this.mlPredictor = new TensorFlowTrafficPredictor(context);
        
        setupPaint();
    }
    
    private void setupPaint() {
        trafficPaint = new Paint();
        trafficPaint.setStyle(Paint.Style.STROKE);
        trafficPaint.setStrokeWidth(22f); // Maximum visibility for traffic overlay
        trafficPaint.setAntiAlias(true);
        Log.d(TAG, "Traffic paint setup with stroke width: " + trafficPaint.getStrokeWidth());
    }
    
    /**
     * Generate user-friendly traffic visualization
     * Focus ONLY on showing traffic conditions along the selected route
     */
    private void generateTrafficData() {
        trafficSegments.clear();
        
        Log.d(TAG, "Starting user-friendly traffic analysis");
        
        // Get current route for analysis
        List<GeoPoint> routePoints = getCurrentRoutePoints();
        if (routePoints != null && !routePoints.isEmpty()) {
            Log.d(TAG, "Showing traffic conditions along your route with " + routePoints.size() + " points");
            
            // Show traffic ONLY along the selected route
            for (int i = 0; i < routePoints.size() - 1; i++) {
                GeoPoint start = routePoints.get(i);
                GeoPoint end = routePoints.get(i + 1);
                String trafficLevel = getMLTrafficPrediction(i, start, end);
                addTrafficSegment("Route Traffic " + i, start, end, trafficLevel);
            }
            Log.d(TAG, "Added " + (routePoints.size() - 1) + " route traffic segments");
            
        } else {
            Log.d(TAG, "No route selected - no traffic overlay to show");
            // Don't show any traffic when no route is selected
        }
        
        Log.d(TAG, "Generated " + trafficSegments.size() + " route-only traffic segments");
    }
    

    

    
    // Complex analysis removed - back to clean, simple traffic display

    private List<GeoPoint> getCurrentRoutePoints() {
        try {
            // Access the route points from MapManager through reflection
            // This is a workaround since we don't have direct access to MapManager's routeLine
            for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
                if (overlay instanceof org.osmdroid.views.overlay.Polyline) {
                    org.osmdroid.views.overlay.Polyline polyline = (org.osmdroid.views.overlay.Polyline) overlay;
                    List<GeoPoint> points = polyline.getPoints();
                    if (points != null && !points.isEmpty()) {
                        Log.d(TAG, "Found route polyline with " + points.size() + " points");
                        return points;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting route points", e);
        }
        return null;
    }
    
    /**
     * Get ML-based traffic prediction for a route segment
     */
    private String getMLTrafficPrediction(int segmentIndex, GeoPoint start, GeoPoint end) {
        try {
            // Use segment index as junction number (1-4)
            int junction = (segmentIndex % 4) + 1;
            
            // Get current time-based prediction
            String prediction = mlPredictor.getCurrentTrafficPrediction(junction);
            
            Log.d(TAG, "ML Prediction for segment " + segmentIndex + " (junction " + junction + "): " + prediction);
            return prediction;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting ML prediction: " + e.getMessage());
            // Fallback to time-based prediction
            return getTimeBasedTrafficPrediction(segmentIndex);
        }
    }
    
    /**
     * Fallback time-based traffic prediction
     */
    private String getTimeBasedTrafficPrediction(int segmentIndex) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        
        // Simple time-based logic
        if (hour >= 7 && hour <= 9) { // Morning peak
            return segmentIndex % 2 == 0 ? "High" : "Medium";
        } else if (hour >= 17 && hour <= 19) { // Evening peak
            return segmentIndex % 2 == 0 ? "High" : "Medium";
        } else if (hour >= 22 || hour <= 5) { // Night
            return "Low";
        } else {
            return segmentIndex % 3 == 0 ? "High" : segmentIndex % 3 == 1 ? "Medium" : "Low";
        }
    }
    
    private void addTrafficSegment(String roadName, GeoPoint start, GeoPoint end, String trafficLevel) {
        TrafficSegment segment = new TrafficSegment(roadName, start, end, trafficLevel);
        trafficSegments.add(segment);
        roadTrafficLevels.put(roadName, trafficLevel);
    }
    
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) {
            Log.d(TAG, "Drawing shadow - skipping");
            return;
        }
        
        if (!showTrafficOverlay) {
            Log.d(TAG, "Traffic overlay not visible - skipping draw");
            return;
        }
        
        Log.d(TAG, "Drawing traffic overlay with " + trafficSegments.size() + " segments, visible: " + showTrafficOverlay);
        
        int drawnSegments = 0;
        for (TrafficSegment segment : trafficSegments) {
            try {
                boolean drawn = drawTrafficSegment(canvas, mapView, segment);
                if (drawn) {
                    drawnSegments++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error drawing traffic segment: " + segment.roadName, e);
            }
        }
        
        Log.d(TAG, "Successfully drew " + drawnSegments + " out of " + trafficSegments.size() + " segments");
    }
    
    private boolean drawTrafficSegment(Canvas canvas, MapView mapView, TrafficSegment segment) {
        // Convert GeoPoints to screen coordinates
        android.graphics.Point startPoint = mapView.getProjection().toPixels(segment.start, null);
        android.graphics.Point endPoint = mapView.getProjection().toPixels(segment.end, null);
        
        // Check if points are within visible area
        if (startPoint == null || endPoint == null) {
            Log.w(TAG, "Null screen coordinates for segment: " + segment.roadName);
            return false;
        }
        
        // Check if points are within canvas bounds
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        
        if (startPoint.x < 0 || startPoint.x > canvasWidth || startPoint.y < 0 || startPoint.y > canvasHeight ||
            endPoint.x < 0 || endPoint.x > canvasWidth || endPoint.y < 0 || endPoint.y > canvasHeight) {
            Log.d(TAG, "Segment " + segment.roadName + " outside canvas bounds: start(" + startPoint.x + "," + startPoint.y + 
                  ") end(" + endPoint.x + "," + endPoint.y + ") canvas(" + canvasWidth + "x" + canvasHeight + ")");
            return false;
        }
        
        // Set color based on traffic level
        int color = getTrafficLevelColor(segment.trafficLevel);
        trafficPaint.setColor(color);
        trafficPaint.setStrokeWidth(22f); // Maximum visibility for traffic overlay
        
        // Draw the road segment
        canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, trafficPaint);
        
        // Add traffic level indicator
        drawTrafficIndicator(canvas, startPoint, segment.trafficLevel);
        
        // Log detailed information about what was drawn
        Log.d(TAG, "Drew traffic segment: " + segment.roadName + " (" + segment.trafficLevel + ") from (" + 
              startPoint.x + "," + startPoint.y + ") to (" + endPoint.x + "," + endPoint.y + ") - Color: " + 
              String.format("#%06X", (0xFFFFFF & color)) + " - StrokeWidth: " + trafficPaint.getStrokeWidth());
        return true;
    }
    
    /**
     * Get color for traffic level
     */
    private int getTrafficLevelColor(String trafficLevel) {
        switch (trafficLevel.toLowerCase()) {
            case "high":
                return android.graphics.Color.RED;
            case "medium":
                return android.graphics.Color.YELLOW; // Yellow for better visibility
            case "low":
                return android.graphics.Color.GREEN;
            default:
                return android.graphics.Color.GRAY;
        }
    }
    
    private void drawTrafficIndicator(Canvas canvas, android.graphics.Point point, String trafficLevel) {
        Paint indicatorPaint = new Paint();
        indicatorPaint.setColor(getTrafficLevelColor(trafficLevel));
        indicatorPaint.setStyle(Paint.Style.FILL);
        indicatorPaint.setAlpha(150); // Less opaque for subtlety

        // Draw a smaller circle indicator
        canvas.drawCircle(point.x, point.y, 6f, indicatorPaint);

        // Draw traffic level text
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10f); // Smaller text
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true); // Bold text

        String levelText = trafficLevel.substring(0, 1); // "L", "M", "H"
        canvas.drawText(levelText, point.x - 3f, point.y + 3f, textPaint);
    }
    
    /**
     * Show or hide traffic overlay
     */
    public void setTrafficOverlayVisible(boolean visible) {
        Log.d(TAG, "Setting traffic overlay visible: " + visible + " (was: " + showTrafficOverlay + ")");
        this.showTrafficOverlay = visible;
        
        if (mapView != null) {
            if (visible) {
                // Regenerate traffic data based on current map center when enabling
                generateTrafficData();
                Log.d(TAG, "Traffic overlay enabled - " + trafficSegments.size() + " segments ready to draw");
                // Force refresh to ensure visibility
                forceRefresh();
            } else {
                Log.d(TAG, "Traffic overlay disabled");
            }
            
            // Force a complete redraw of the map
            mapView.invalidate();
            mapView.postInvalidate();
        } else {
            Log.w(TAG, "MapView is null, cannot invalidate");
        }
    }
    
    /**
     * Update traffic predictions for all road segments
     */
    public void updateTrafficPredictions() {
        Log.d(TAG, "Updating traffic predictions...");
        
        // Regenerate traffic data with current predictions
        generateTrafficData();
        
        if (mapView != null) {
            mapView.invalidate();
            mapView.postInvalidate();
        }
    }
    
    /**
     * Force refresh the traffic overlay
     */
    public void forceRefresh() {
        Log.d(TAG, "Forcing traffic overlay refresh");
        if (mapView != null) {
            mapView.invalidate();
            mapView.postInvalidate();
            
            // Also force a redraw after a short delay
            mapView.post(() -> {
                mapView.invalidate();
                Log.d(TAG, "Post-delay invalidate called");
            });
        }
    }
    
    /**
     * Get traffic level for a specific road
     */
    public String getTrafficLevelForRoad(String roadName) {
        return roadTrafficLevels.getOrDefault(roadName, "Low");
    }
    
    /**
     * Get traffic level for a specific location (nearest road)
     */
    public String getTrafficLevelForLocation(GeoPoint location) {
        // Find nearest road segment
        TrafficSegment nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (TrafficSegment segment : trafficSegments) {
            double distance = calculateDistanceToSegment(location, segment);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = segment;
            }
        }
        
        return nearest != null ? nearest.trafficLevel : "Low";
    }
    
    private double calculateDistanceToSegment(GeoPoint point, TrafficSegment segment) {
        // Simplified distance calculation
        double latDiff = point.getLatitude() - segment.start.getLatitude();
        double lonDiff = point.getLongitude() - segment.start.getLongitude();
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }
    
    /**
     * Inner class to represent a traffic segment
     */
    private static class TrafficSegment {
        String roadName;
        GeoPoint start;
        GeoPoint end;
        String trafficLevel;
        
        TrafficSegment(String roadName, GeoPoint start, GeoPoint end, String trafficLevel) {
            this.roadName = roadName;
            this.start = start;
            this.end = end;
            this.trafficLevel = trafficLevel;
        }
    }
    

}