package com.example.bottlenex.map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import java.util.List;

/**
 * Overlay for displaying live traffic congestion on major Singapore expressways
 * Uses route points from LiveTrafficManager to draw colored traffic lines
 */
public class LiveTrafficOverlay extends Overlay {
    private static final String TAG = "LiveTrafficOverlay";
    
    private Paint trafficPaint;
    private List<LiveTrafficManager.TrafficRoute> trafficRoutes;
    private boolean isVisible = false;
    
    public LiveTrafficOverlay() {
        setupPaint();
    }
    
    private void setupPaint() {
        trafficPaint = new Paint();
        trafficPaint.setStyle(Paint.Style.STROKE);
        trafficPaint.setStrokeWidth(8f); // Thick enough to be visible but not overwhelming
        trafficPaint.setAntiAlias(true);
        trafficPaint.setStrokeCap(Paint.Cap.ROUND);
        Log.d(TAG, "Live traffic paint setup completed");
    }
    
    /**
     * Set the traffic routes to display
     */
    public void setTrafficRoutes(List<LiveTrafficManager.TrafficRoute> routes) {
        this.trafficRoutes = routes;
        Log.d(TAG, "Set " + (routes != null ? routes.size() : 0) + " traffic routes");
    }
    
    /**
     * Show or hide the live traffic overlay
     */
    public void setVisible(boolean visible) {
        this.isVisible = visible;
        Log.d(TAG, "Live traffic overlay visibility set to: " + visible);
    }
    
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow || !isVisible || trafficRoutes == null) {
            return;
        }
        
        Log.d(TAG, "Drawing live traffic overlay with " + trafficRoutes.size() + " routes");
        
        int drawnRoutes = 0;
        for (LiveTrafficManager.TrafficRoute route : trafficRoutes) {
            if (route.routePoints != null && !route.routePoints.isEmpty()) {
                drawTrafficRoute(canvas, mapView, route);
                drawnRoutes++;
            }
        }
        
        Log.d(TAG, "Drew " + drawnRoutes + " traffic routes on map");
    }
    
    /**
     * Draw a single traffic route with appropriate color based on congestion level
     */
    private void drawTrafficRoute(Canvas canvas, MapView mapView, LiveTrafficManager.TrafficRoute route) {
        List<GeoPoint> routePoints = route.routePoints;
        if (routePoints.size() < 2) {
            return;
        }
        
        // Set color based on traffic level
        int color = getTrafficColor(route.currentTrafficLevel);
        trafficPaint.setColor(color);
        
        // Draw line segments between consecutive route points
        for (int i = 0; i < routePoints.size() - 1; i++) {
            GeoPoint startPoint = routePoints.get(i);
            GeoPoint endPoint = routePoints.get(i + 1);
            
            // Convert to screen coordinates
            android.graphics.Point startScreen = mapView.getProjection().toPixels(startPoint, null);
            android.graphics.Point endScreen = mapView.getProjection().toPixels(endPoint, null);
            
            if (startScreen != null && endScreen != null) {
                // Check if line segment is visible on screen
                if (isLineVisible(canvas, startScreen, endScreen)) {
                    canvas.drawLine(startScreen.x, startScreen.y, endScreen.x, endScreen.y, trafficPaint);
                }
            }
        }
        
        Log.d(TAG, "Drew traffic route: " + route.name + " (" + route.currentTrafficLevel + ") with " + 
              routePoints.size() + " points");
    }
    
    /**
     * Check if a line segment is visible within the canvas bounds
     */
    private boolean isLineVisible(Canvas canvas, android.graphics.Point start, android.graphics.Point end) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        
        // Simple bounds check - if either point is on screen, draw the line
        return (start.x >= -50 && start.x <= width + 50 && start.y >= -50 && start.y <= height + 50) ||
               (end.x >= -50 && end.x <= width + 50 && end.y >= -50 && end.y <= height + 50);
    }
    
    /**
     * Get color for traffic congestion level
     */
    private int getTrafficColor(String trafficLevel) {
        switch (trafficLevel.toLowerCase()) {
            case "high":
                return Color.rgb(244, 67, 54); // Material Red - Heavy traffic
            case "medium":
                return Color.rgb(255, 193, 7); // Material Amber - Moderate traffic
            case "low":
                return Color.rgb(76, 175, 80); // Material Green - Light traffic
            default:
                return Color.GRAY; // Unknown traffic level
        }
    }
    
    /**
     * Update traffic levels for all routes (called periodically for "live" effect)
     */
    public void updateTrafficLevels(LiveTrafficManager liveTrafficManager) {
        if (trafficRoutes != null && liveTrafficManager != null) {
            liveTrafficManager.updateTrafficLevels();
            Log.d(TAG, "Updated traffic levels for live display");
        }
    }
}
