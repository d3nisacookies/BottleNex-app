package com.example.bottlenex;

//To commit

import java.text.SimpleDateFormat;

public class RouteHistory {
    private int id;
    private double startLat;
    private double startLon;
    private double endLat;
    private double endLon;
    private String startAddress;
    private String endAddress;
    private double distance; // in meters
    private double duration; // in seconds
    private String startTime;
    private String endTime;
    private String date;

    public RouteHistory() {
        // Default constructor
    }

    public RouteHistory(double startLat, double startLon, double endLat, double endLon,
                       String startAddress, String endAddress, double distance, double duration,
                       String startTime, String endTime, String date) {
        this.startLat = startLat;
        this.startLon = startLon;
        this.endLat = endLat;
        this.endLon = endLon;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.distance = distance;
        this.duration = duration;
        this.startTime = startTime;
        this.endTime = endTime;
        this.date = date;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getStartLat() {
        return startLat;
    }

    public void setStartLat(double startLat) {
        this.startLat = startLat;
    }

    public double getStartLon() {
        return startLon;
    }

    public void setStartLon(double startLon) {
        this.startLon = startLon;
    }

    public double getEndLat() {
        return endLat;
    }

    public void setEndLat(double endLat) {
        this.endLat = endLat;
    }

    public double getEndLon() {
        return endLon;
    }

    public void setEndLon(double endLon) {
        this.endLon = endLon;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    // Helper methods for formatted display
    public String getFormattedDistance() {
        if (distance >= 1000) {
            return String.format("%.1f km", distance / 1000.0);
        } else {
            return String.format("%.0f m", distance);
        }
    }

    public String getFormattedDuration() {
        long hours = (long) (duration / 3600);
        long minutes = (long) ((duration % 3600) / 60);
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    public String getFormattedRoute() {
        String start = startAddress != null && !startAddress.isEmpty() ? startAddress : 
                      String.format("%.4f, %.4f", startLat, startLon);
        String end = endAddress != null && !endAddress.isEmpty() ? endAddress : 
                    String.format("%.4f, %.4f", endLat, endLon);
        return start + " → " + end;
    }
} 