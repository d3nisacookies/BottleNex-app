package com.example.bottlenex.map;

//To commit

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.util.Log;
import android.view.View;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import com.example.bottlenex.routing.RoutePlanner;

import java.util.List;

public class NavigationOverlay extends Overlay {
    
    private Context context;
    private MapView mapView;
    private List<RoutePlanner.NavigationStep> navigationSteps;
    private Location currentLocation;
    private int currentStepIndex = 0;
    private Paint textPaint;
    private Paint backgroundPaint;
    private Paint arrowPaint;
    
    public NavigationOverlay(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        setupPaints();
    }
    
    private void setupPaints() {
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24); // Increased from 16 to 24
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setAntiAlias(true);
        
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#2196F3")); // Changed to blue for better visibility
        backgroundPaint.setAlpha(240); // Increased alpha for better visibility
        backgroundPaint.setAntiAlias(true);
        
        arrowPaint = new Paint();
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setTextSize(24);
        arrowPaint.setTypeface(Typeface.DEFAULT_BOLD);
        arrowPaint.setAntiAlias(true);
    }
    
    public void setNavigationSteps(List<RoutePlanner.NavigationStep> steps) {
        this.navigationSteps = steps;
        this.currentStepIndex = 0;
    }
    
    public void updateCurrentLocation(Location location) {
        this.currentLocation = location;
        Log.d("NavigationOverlay", "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
        updateCurrentStep();
    }
    
    private void updateCurrentStep() {
        if (currentLocation == null || navigationSteps == null || navigationSteps.isEmpty()) {
            return;
        }
        
        // Simple approach: find the closest step and advance if we're close enough
        double minDistance = Double.MAX_VALUE;
        int closestStep = currentStepIndex;
        
        // Look at current step and next few steps
        for (int i = currentStepIndex; i < Math.min(currentStepIndex + 2, navigationSteps.size()); i++) {
            RoutePlanner.NavigationStep step = navigationSteps.get(i);
            float[] results = new float[1];
            Location.distanceBetween(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                step.location.getLatitude(), step.location.getLongitude(),
                results
            );
            
            if (results[0] < minDistance) {
                minDistance = results[0];
                closestStep = i;
            }
        }
        
        // Simple rule: if we're within 80m of a step ahead of us, advance to it
        if (closestStep > currentStepIndex && minDistance < 80) {
            currentStepIndex = closestStep;
            Log.d("NavigationOverlay", "Step updated to: " + currentStepIndex + ", distance: " + minDistance);
            
            // Force redraw to update the display
            if (mapView != null) {
                mapView.invalidate();
            }
        }
    }
    
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        // Navigation instructions are now shown in the bottom panel only
        // No drawing on the map overlay to keep the map clean
        return;
    }
    
    private void drawNavigationInstruction(Canvas canvas, RoutePlanner.NavigationStep step, int screenWidth, int screenHeight) {
        String instruction = step.instruction;
        String distance = formatDistance(step.distance);
        
        // Create instruction text
        String displayText = instruction + "\n" + distance;
        
        // Measure text bounds
        Rect textBounds = new Rect();
        textPaint.getTextBounds(displayText, 0, displayText.length(), textBounds);
        
        // Calculate background dimensions
        int padding = 30; // Increased padding for larger background
        int backgroundWidth = textBounds.width() + (padding * 2);
        int backgroundHeight = textBounds.height() + (padding * 2);
        
        // Position at top center of screen
        int left = (screenWidth - backgroundWidth) / 2;
        int top = 150; // Moved down a bit more to avoid status bar
        
        // Draw background
        canvas.drawRoundRect(
            left, top, left + backgroundWidth, top + backgroundHeight,
            10, 10, backgroundPaint
        );
        
        // Draw text
        canvas.drawText(displayText, left + padding, top + padding + textBounds.height(), textPaint);
        
        // Draw next instruction if available
        if (currentStepIndex + 1 < navigationSteps.size()) {
            RoutePlanner.NavigationStep nextStep = navigationSteps.get(currentStepIndex + 1);
            String nextInstruction = "Then " + nextStep.instruction;
            
            // Draw smaller next instruction below
            Paint smallTextPaint = new Paint(textPaint);
            smallTextPaint.setTextSize(18); // Increased from 12 to 18
            smallTextPaint.setAlpha(200); // Increased alpha for better visibility
            
            Rect nextTextBounds = new Rect();
            smallTextPaint.getTextBounds(nextInstruction, 0, nextInstruction.length(), nextTextBounds);
            
            int nextLeft = (screenWidth - nextTextBounds.width()) / 2;
            int nextTop = top + backgroundHeight + 15; // Increased spacing
            
            canvas.drawText(nextInstruction, nextLeft, nextTop + nextTextBounds.height(), smallTextPaint);
        }
    }
    
    public RoutePlanner.NavigationStep getCurrentStep() {
        if (navigationSteps != null && currentStepIndex < navigationSteps.size()) {
            return navigationSteps.get(currentStepIndex);
        }
        return null;
    }
    
    public boolean isNearDestination() {
        if (navigationSteps == null || navigationSteps.isEmpty()) {
            return false;
        }
        
        RoutePlanner.NavigationStep lastStep = navigationSteps.get(navigationSteps.size() - 1);
        if (currentLocation == null || lastStep == null) {
            return false;
        }
        
        float[] results = new float[1];
        Location.distanceBetween(
            currentLocation.getLatitude(), currentLocation.getLongitude(),
            lastStep.location.getLatitude(), lastStep.location.getLongitude(),
            results
        );
        
        return results[0] < 50; // Within 50 meters of destination
    }
    
    private String formatDistance(double distanceMeters) {
        if (distanceMeters >= 1000) {
            // Show as kilometers with one decimal place
            return String.format("%.1f km", distanceMeters / 1000.0);
        } else if (distanceMeters >= 100) {
            // Show as meters, rounded to nearest 10m for distances 100m+
            int roundedMeters = (int) Math.round(distanceMeters / 10.0) * 10;
            return roundedMeters + " m";
        } else {
            // For distances under 100m, show exact distance
            return (int) distanceMeters + " m";
        }
    }
} 