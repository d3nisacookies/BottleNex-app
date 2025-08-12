package com.example.bottlenex.ml;

/**
 * Data model class for Singapore traffic data
 * Contains static datasets for traffic visualization
 */
public class SingaporeTrafficData {
    
    // Time slots for daily traffic analysis
    public static final String[] TIME_SLOTS = {
        "6AM", "7AM", "8AM", "9AM", "10AM", "11AM", "12PM", 
        "1PM", "2PM", "3PM", "4PM", "5PM", "6PM", "7PM", "8PM"
    };
    
    // Singapore regions/areas
    public static final String[] REGIONS = {
        "CBD", "Orchard", "Marina Bay", "Chinatown", "Little India", 
        "Bugis", "Raffles Place", "Clarke Quay", "Sentosa", "Changi"
    };
    
    // Time periods for heatmap analysis
    public static final String[] TIME_PERIODS = {
        "Morning Rush (7-9AM)", "Mid-Morning (10AM-12PM)", 
        "Lunch Time (12-2PM)", "Afternoon (2-5PM)", 
        "Evening Rush (5-7PM)", "Night (7-9PM)"
    };
    
    // Congestion levels throughout the day (0.0 = no traffic, 1.0 = heavy traffic)
    public static final float[] CONGESTION_LEVELS = {
        0.2f, 0.4f, 0.8f, 0.9f, 0.7f, 0.6f, 0.5f,
        0.4f, 0.3f, 0.4f, 0.6f, 0.8f, 0.9f, 0.7f, 0.5f
    };
    
    // Average speeds throughout the day (in km/h)
    public static final float[] AVERAGE_SPEEDS = {
        45f, 35f, 15f, 10f, 25f, 30f, 35f,
        40f, 45f, 40f, 30f, 20f, 15f, 25f, 35f
    };
    
    // Traffic density data for different time periods and regions
    // Format: [timePeriod][timeSlot][region]
    public static final float[][][] TRAFFIC_DENSITY = {
        // Morning Rush (7-9AM)
        {{0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.9f, 0.3f, 0.2f, 0.3f},
         {0.8f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.8f, 0.4f, 0.3f, 0.4f},
         {0.7f, 0.8f, 0.9f, 0.8f, 0.7f, 0.6f, 0.7f, 0.5f, 0.4f, 0.5f}},
        
        // Mid-Morning (10AM-12PM)
        {{0.5f, 0.4f, 0.3f, 0.4f, 0.5f, 0.6f, 0.5f, 0.7f, 0.8f, 0.6f},
         {0.4f, 0.5f, 0.4f, 0.5f, 0.6f, 0.7f, 0.4f, 0.8f, 0.9f, 0.7f},
         {0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.3f, 0.9f, 0.8f, 0.8f}},
        
        // Lunch Time (12-2PM)
        {{0.7f, 0.8f, 0.9f, 0.8f, 0.7f, 0.6f, 0.7f, 0.5f, 0.4f, 0.5f},
         {0.8f, 0.9f, 0.8f, 0.9f, 0.8f, 0.7f, 0.8f, 0.6f, 0.5f, 0.6f},
         {0.9f, 0.8f, 0.7f, 0.8f, 0.9f, 0.8f, 0.9f, 0.7f, 0.6f, 0.7f}},
        
        // Afternoon (2-5PM)
        {{0.4f, 0.3f, 0.2f, 0.3f, 0.4f, 0.5f, 0.4f, 0.6f, 0.7f, 0.5f},
         {0.3f, 0.4f, 0.3f, 0.4f, 0.5f, 0.6f, 0.3f, 0.7f, 0.8f, 0.6f},
         {0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.2f, 0.8f, 0.9f, 0.7f}},
        
        // Evening Rush (5-7PM)
        {{0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.9f, 0.3f, 0.2f, 0.3f},
         {0.8f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.8f, 0.4f, 0.3f, 0.4f},
         {0.7f, 0.8f, 0.9f, 0.8f, 0.7f, 0.6f, 0.7f, 0.5f, 0.4f, 0.5f}},
        
        // Night (7-9PM)
        {{0.3f, 0.2f, 0.1f, 0.2f, 0.3f, 0.4f, 0.3f, 0.5f, 0.6f, 0.4f},
         {0.2f, 0.3f, 0.2f, 0.3f, 0.4f, 0.5f, 0.2f, 0.6f, 0.7f, 0.5f},
         {0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.1f, 0.7f, 0.8f, 0.6f}}
    };
    
    /**
     * Get congestion level for a specific time slot
     * @param timeIndex Index of the time slot
     * @return Congestion level (0.0 to 1.0)
     */
    public static float getCongestionLevel(int timeIndex) {
        if (timeIndex >= 0 && timeIndex < CONGESTION_LEVELS.length) {
            return CONGESTION_LEVELS[timeIndex];
        }
        return 0.0f;
    }
    
    /**
     * Get average speed for a specific time slot
     * @param timeIndex Index of the time slot
     * @return Average speed in km/h
     */
    public static float getAverageSpeed(int timeIndex) {
        if (timeIndex >= 0 && timeIndex < AVERAGE_SPEEDS.length) {
            return AVERAGE_SPEEDS[timeIndex];
        }
        return 0.0f;
    }
    
    /**
     * Get traffic density for a specific time period, time slot, and region
     * @param timePeriodIndex Index of the time period
     * @param timeSlotIndex Index of the time slot within the period
     * @param regionIndex Index of the region
     * @return Traffic density (0.0 to 1.0)
     */
    public static float getTrafficDensity(int timePeriodIndex, int timeSlotIndex, int regionIndex) {
        if (timePeriodIndex >= 0 && timePeriodIndex < TRAFFIC_DENSITY.length &&
            timeSlotIndex >= 0 && timeSlotIndex < TRAFFIC_DENSITY[0].length &&
            regionIndex >= 0 && regionIndex < TRAFFIC_DENSITY[0][0].length) {
            return TRAFFIC_DENSITY[timePeriodIndex][timeSlotIndex][regionIndex];
        }
        return 0.0f;
    }
    
    /**
     * Get the number of time periods available
     * @return Number of time periods
     */
    public static int getTimePeriodCount() {
        return TIME_PERIODS.length;
    }
    
    /**
     * Get the number of regions available
     * @return Number of regions
     */
    public static int getRegionCount() {
        return REGIONS.length;
    }
    
    /**
     * Get the number of time slots available
     * @return Number of time slots
     */
    public static int getTimeSlotCount() {
        return TIME_SLOTS.length;
    }
}
