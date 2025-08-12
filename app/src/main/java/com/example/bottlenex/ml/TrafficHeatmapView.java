package com.example.bottlenex.ml;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Custom view for displaying traffic density as a color-coded grid heatmap
 */
public class TrafficHeatmapView extends View {
    private static final String TAG = "TrafficHeatmapView";
    
    private Paint cellPaint;
    private Paint textPaint;
    private Paint borderPaint;
    
    private String[] regions;
    private String[] timePeriods;
    private float[][] heatmapData;
    
    private int cellWidth;
    private int cellHeight;
    private int gridStartX = 120; // Space for region labels
    private int gridStartY = 80;  // Space for time period labels
    
    public TrafficHeatmapView(Context context) {
        super(context);
        init();
    }
    
    public TrafficHeatmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        cellPaint = new Paint();
        cellPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);
        
        borderPaint = new Paint();
        borderPaint.setColor(Color.GRAY);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        
        Log.d(TAG, "TrafficHeatmapView initialized");
    }
    
    /**
     * Set the data for the heatmap
     */
    public void setHeatmapData(String[] regions, String[] timePeriods, float[][] data) {
        if (regions == null || timePeriods == null || data == null) {
            Log.e(TAG, "Null data provided to setHeatmapData");
            return;
        }
        
        this.regions = regions;
        this.timePeriods = timePeriods;
        this.heatmapData = data;
        
        Log.d(TAG, "Heatmap data set: " + regions.length + " regions, " + timePeriods.length + " time periods");
        Log.d(TAG, "Data dimensions: " + data.length + "x" + (data.length > 0 ? data[0].length : 0));
        
        invalidate(); // Trigger redraw
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        if (regions != null && timePeriods != null) {
            // Calculate cell dimensions based on available space
            int availableWidth = w - gridStartX - 40; // 40px right margin
            int availableHeight = h - gridStartY - 40; // 40px bottom margin
            
            cellWidth = availableWidth / timePeriods.length;
            cellHeight = availableHeight / regions.length;
            
            Log.d(TAG, "Grid dimensions: " + cellWidth + "x" + cellHeight + 
                  " for " + timePeriods.length + "x" + regions.length + " grid");
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (heatmapData == null || regions == null || timePeriods == null) {
            Log.w(TAG, "No data to draw heatmap");
            return;
        }
        
        drawHeatmapGrid(canvas);
        drawLabels(canvas);
    }
    
    private void drawHeatmapGrid(Canvas canvas) {
        try {
            for (int regionIndex = 0; regionIndex < regions.length; regionIndex++) {
                for (int timeIndex = 0; timeIndex < timePeriods.length; timeIndex++) {
                    
                    // Safety check for array bounds
                    if (regionIndex >= heatmapData.length || timeIndex >= heatmapData[regionIndex].length) {
                        Log.w(TAG, "Data index out of bounds: region=" + regionIndex + ", time=" + timeIndex);
                        continue;
                    }
                    
                    // Get traffic density value (0.0 to 1.0)
                    float density = heatmapData[regionIndex][timeIndex];
                    
                    // Convert density to color
                    int color = getHeatmapColor(density);
                    cellPaint.setColor(color);
                    
                    // Calculate cell position
                    int left = gridStartX + (timeIndex * cellWidth);
                    int top = gridStartY + (regionIndex * cellHeight);
                    int right = left + cellWidth;
                    int bottom = top + cellHeight;
                    
                    // Draw filled cell
                    canvas.drawRect(left, top, right, bottom, cellPaint);
                    
                    // Draw cell border
                    canvas.drawRect(left, top, right, bottom, borderPaint);
                    
                    // Draw density value in cell
                    String valueText = String.format("%.2f", density);
                    float textX = left + cellWidth / 2f - textPaint.measureText(valueText) / 2f;
                    float textY = top + cellHeight / 2f + textPaint.getTextSize() / 3f;
                    
                    // Use white text for dark cells, black for light cells
                    textPaint.setColor(density > 0.5f ? Color.WHITE : Color.BLACK);
                    canvas.drawText(valueText, textX, textY, textPaint);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error drawing heatmap grid", e);
        }
    }
    
    private void drawLabels(Canvas canvas) {
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(20f);
        
        // Draw region labels (left side)
        for (int i = 0; i < regions.length; i++) {
            String region = regions[i];
            float textY = gridStartY + (i * cellHeight) + cellHeight / 2f + textPaint.getTextSize() / 3f;
            canvas.drawText(region, 10, textY, textPaint);
        }
        
        // Draw time period labels (top)
        textPaint.setTextSize(16f);
        for (int i = 0; i < timePeriods.length; i++) {
            String timePeriod = timePeriods[i];
            float textX = gridStartX + (i * cellWidth) + cellWidth / 2f - textPaint.measureText(timePeriod) / 2f;
            
            // Split long text into multiple lines if needed
            String[] lines = timePeriod.split(" ");
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                float textY = 30 + (lineIndex * 20);
                canvas.drawText(lines[lineIndex], textX, textY, textPaint);
            }
        }
    }
    
    /**
     * Convert traffic density (0.0-1.0) to heatmap color
     */
    private int getHeatmapColor(float density) {
        // Clamp density to valid range
        density = Math.max(0.0f, Math.min(1.0f, density));
        
        if (density < 0.3f) {
            // Low traffic - Green
            return Color.rgb(76, 175, 80); // Material Green
        } else if (density < 0.7f) {
            // Medium traffic - Yellow/Orange
            return Color.rgb(255, 193, 7); // Material Amber
        } else {
            // High traffic - Red
            return Color.rgb(244, 67, 54); // Material Red
        }
    }
}
