package com.example.bottlenex.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import com.example.bottlenex.ml.TrafficBottleneckIdentifier;
import java.util.List;

/**
 * Map overlay for displaying identified traffic bottlenecks
 * Shows bottlenecks in red color with edge names and severity indicators
 */
public class BottleneckOverlay extends Overlay {
    private static final String TAG = "BottleneckOverlay";

    private Context context;
    private MapView mapView;
    private Paint bottleneckPaint;
    private Paint textPaint;
    private Paint severityPaint;
    private List<TrafficBottleneckIdentifier.TrafficBottleneck> bottlenecks;
    private boolean showBottlenecks = false;

    public BottleneckOverlay(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.bottlenecks = null;
        setupPaints();
    }

    private void setupPaints() {
        // Paint for bottleneck lines
        bottleneckPaint = new Paint();
        bottleneckPaint.setStyle(Paint.Style.STROKE);
        bottleneckPaint.setStrokeWidth(8f);
        bottleneckPaint.setColor(Color.RED);
        bottleneckPaint.setAntiAlias(true);

        // Paint for text labels
        textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(24f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Paint for severity indicators
        severityPaint = new Paint();
        severityPaint.setStyle(Paint.Style.FILL);
        severityPaint.setAntiAlias(true);
    }

    /**
     * Set the bottlenecks to display
     */
    public void setBottlenecks(List<TrafficBottleneckIdentifier.TrafficBottleneck> bottlenecks) {
        this.bottlenecks = bottlenecks;
        Log.d(TAG, "Set " + (bottlenecks != null ? bottlenecks.size() : 0) + " bottlenecks for display");
    }

    /**
     * Show or hide the bottleneck overlay
     */
    public void setVisible(boolean visible) {
        this.showBottlenecks = visible;
        Log.d(TAG, "Bottleneck overlay visibility set to: " + visible);
    }

    /**
     * Check if overlay is visible
     */
    public boolean isVisible() {
        return showBottlenecks;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }

        if (!showBottlenecks || bottlenecks == null || bottlenecks.isEmpty()) {
            return;
        }

        Log.d(TAG, "Drawing " + bottlenecks.size() + " bottlenecks");

        for (TrafficBottleneckIdentifier.TrafficBottleneck bottleneck : bottlenecks) {
            try {
                drawBottleneck(canvas, mapView, bottleneck);
            } catch (Exception e) {
                Log.e(TAG, "Error drawing bottleneck: " + bottleneck.edge.edgeName, e);
            }
        }
    }

    /**
     * Draw a single bottleneck on the map
     */
    private void drawBottleneck(Canvas canvas, MapView mapView,
                                TrafficBottleneckIdentifier.TrafficBottleneck bottleneck) {
        TrafficBottleneckIdentifier.RoadEdge edge = bottleneck.edge;

        // Convert geographic coordinates to screen coordinates
        org.osmdroid.views.Projection projection = mapView.getProjection();
        android.graphics.Point startPoint = new android.graphics.Point();
        android.graphics.Point endPoint = new android.graphics.Point();

        projection.toPixels(edge.startPoint, startPoint);
        projection.toPixels(edge.endPoint, endPoint);

        // Draw the bottleneck line in red
        canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, bottleneckPaint);

        // Draw severity indicator (colored circle)
        int severityColor = getSeverityColor(bottleneck.severity);
        severityPaint.setColor(severityColor);

        // Draw circle at the middle of the edge
        int centerX = (startPoint.x + endPoint.x) / 2;
        int centerY = (startPoint.y + endPoint.y) / 2;
        int radius = 12;
        canvas.drawCircle(centerX, centerY, radius, severityPaint);

        // Draw edge name above the line
        String displayText = edge.edgeName;
        if (displayText.length() > 20) {
            displayText = displayText.substring(0, 17) + "...";
        }

        // Position text above the line
        int textX = centerX;
        int textY = centerY - 20;

        // Draw text with black outline for better visibility
        Paint outlinePaint = new Paint(textPaint);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(3f);
        outlinePaint.setColor(Color.BLACK);

        canvas.drawText(displayText, textX, textY, outlinePaint);
        canvas.drawText(displayText, textX, textY, textPaint);

        // Draw impact score below the line
        String scoreText = String.format("Score: %.2f", bottleneck.impactScore);
        int scoreY = centerY + 35;

        canvas.drawText(scoreText, textX, scoreY, outlinePaint);
        canvas.drawText(scoreText, textX, scoreY, textPaint);

        Log.d(TAG, "Drew bottleneck: " + edge.edgeName + " at (" + centerX + ", " + centerY + ")");
    }

    /**
     * Get color based on bottleneck severity
     */
    private int getSeverityColor(String severity) {
        switch (severity.toLowerCase()) {
            case "critical":
                return Color.parseColor("#FF0000"); // Bright red
            case "high":
                return Color.parseColor("#FF4444"); // Red
            case "medium":
                return Color.parseColor("#FF8888"); // Light red
            case "low":
                return Color.parseColor("#FFCCCC"); // Very light red
            default:
                return Color.RED;
        }
    }

    /**
     * Clear all bottlenecks
     */
    public void clearBottlenecks() {
        this.bottlenecks = null;
        Log.d(TAG, "Cleared all bottlenecks");
    }

    /**
     * Get current bottleneck count
     */
    public int getBottleneckCount() {
        return bottlenecks != null ? bottlenecks.size() : 0;
    }
}