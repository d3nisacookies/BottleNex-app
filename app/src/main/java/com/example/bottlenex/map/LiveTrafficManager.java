package com.example.bottlenex.map;

import android.content.Context;
import android.util.Log;
import org.osmdroid.util.GeoPoint;
import com.example.bottlenex.routing.RoutePlanner;
import java.util.*;

/**
 * Manages live traffic simulation for major Singapore expressways
 * Uses RoutePlanner to get accurate route paths and applies smart traffic patterns
 */
public class LiveTrafficManager {
    private static final String TAG = "LiveTrafficManager";
    
    private Context context;
    private List<TrafficRoute> trafficRoutes;
    private String apiKey;
    
    public static class TrafficRoute {
        public String name;
        public GeoPoint start;
        public GeoPoint end;
        public String direction;
        public List<GeoPoint> routePoints;
        public String currentTrafficLevel;
        
        public TrafficRoute(String name, GeoPoint start, GeoPoint end, String direction) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.direction = direction;
            this.currentTrafficLevel = "Low";
        }
    }
    
    public LiveTrafficManager(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
        this.trafficRoutes = new ArrayList<>();
        
        initializeMajorRoutes();
    }
    
    /**
     * Initialize all major Singapore expressways with provided coordinates
     */
    private void initializeMajorRoutes() {
        Log.d(TAG, "Initializing major Singapore expressway routes");
        
        // BKE (Bukit Timah Expressway)
        trafficRoutes.add(new TrafficRoute("BKE North", 
            new GeoPoint(1.436111, 103.768644), new GeoPoint(1.348791, 103.792004), "North"));
        trafficRoutes.add(new TrafficRoute("BKE South", 
            new GeoPoint(1.348740, 103.791920), new GeoPoint(1.435766, 103.768463), "South"));
        
        // CTE (Central Expressway)
        trafficRoutes.add(new TrafficRoute("CTE North", 
            new GeoPoint(1.394247, 103.857962), new GeoPoint(1.277836, 103.825146), "North"));
        trafficRoutes.add(new TrafficRoute("CTE South", 
            new GeoPoint(1.278575, 103.824080), new GeoPoint(1.394726, 103.857759), "South"));
        
        // ECP (East Coast Parkway)
        trafficRoutes.add(new TrafficRoute("ECP East", 
            new GeoPoint(1.340024, 103.981314), new GeoPoint(1.287853, 103.861644), "East"));
        trafficRoutes.add(new TrafficRoute("ECP West", 
            new GeoPoint(1.288775, 103.861569), new GeoPoint(1.339107, 103.980696), "West"));
        
        // KJE (Kranji Expressway)
        trafficRoutes.add(new TrafficRoute("KJE East", 
            new GeoPoint(1.367620, 103.712233), new GeoPoint(1.390644, 103.772915), "East"));
        trafficRoutes.add(new TrafficRoute("KJE West", 
            new GeoPoint(1.389842, 103.773676), new GeoPoint(1.366928, 103.710834), "West"));
        
        // KPE (Kallang–Paya Lebar Expressway)
        trafficRoutes.add(new TrafficRoute("KPE East", 
            new GeoPoint(1.301323, 103.878062), new GeoPoint(1.380879, 103.916202), "East"));
        trafficRoutes.add(new TrafficRoute("KPE West", 
            new GeoPoint(1.380268, 103.915963), new GeoPoint(1.299259, 103.877711), "West"));
        
        // MCE (Marina Coastal Expressway)
        trafficRoutes.add(new TrafficRoute("MCE East", 
            new GeoPoint(1.273272, 103.849530), new GeoPoint(1.297121, 103.876736), "East"));
        trafficRoutes.add(new TrafficRoute("MCE West", 
            new GeoPoint(1.297102, 103.876809), new GeoPoint(1.273229, 103.849834), "West"));
        
        // SLE (Seletar Expressway)
        trafficRoutes.add(new TrafficRoute("SLE East", 
            new GeoPoint(1.420675, 103.770161), new GeoPoint(1.395024, 103.857863), "East"));
        trafficRoutes.add(new TrafficRoute("SLE West", 
            new GeoPoint(1.393793, 103.857879), new GeoPoint(1.423903, 103.774485), "West"));
        
        // TPE (Tampines Expressway)
        trafficRoutes.add(new TrafficRoute("TPE East", 
            new GeoPoint(1.399992, 103.857170), new GeoPoint(1.355566, 103.963438), "East"));
        trafficRoutes.add(new TrafficRoute("TPE West", 
            new GeoPoint(1.356414, 103.962918), new GeoPoint(1.400149, 103.857596), "West"));
        
        // AYE (Ayer Rajah Expressway)
        trafficRoutes.add(new TrafficRoute("AYE East", 
            new GeoPoint(1.321886, 103.665984), new GeoPoint(1.273062, 103.849158), "East"));
        trafficRoutes.add(new TrafficRoute("AYE West", 
            new GeoPoint(1.273068, 103.849518), new GeoPoint(1.322235, 103.663958), "West"));
        
        // PIE1 (Pan Island Expressway - Western Section)
        trafficRoutes.add(new TrafficRoute("PIE1 East", 
            new GeoPoint(1.327092, 103.666402), new GeoPoint(1.364379, 103.706116), "East"));
        trafficRoutes.add(new TrafficRoute("PIE1 West", 
            new GeoPoint(1.361565, 103.703948), new GeoPoint(1.326152, 103.665933), "West"));
        
        // PIE2 (Pan Island Expressway - Eastern Section)
        trafficRoutes.add(new TrafficRoute("PIE2 East", 
            new GeoPoint(1.361859, 103.709195), new GeoPoint(1.338168, 103.977174), "East"));
        trafficRoutes.add(new TrafficRoute("PIE2 West", 
            new GeoPoint(1.338079, 103.977065), new GeoPoint(1.361741, 103.709118), "West"));
        
        Log.d(TAG, "Initialized " + trafficRoutes.size() + " major expressway routes");
    }
    
    /**
     * Generate route paths for all traffic routes using RoutePlanner
     */
    public void generateRoutePathsAsync(Runnable onComplete) {
        Log.d(TAG, "Starting route path generation for " + trafficRoutes.size() + " routes");
        
        // Process routes in background to avoid blocking UI
        new Thread(() -> {
            int processedRoutes = 0;
            for (TrafficRoute route : trafficRoutes) {
                try {
                    // Use RoutePlanner static method to get actual route path
                    RoutePlanner.getRoute(route.start, route.end, apiKey, new RoutePlanner.RouteCallback() {
                        @Override
                        public void onRouteReady(RoutePlanner.RouteData routeData) {
                            if (routeData != null && routeData.routePoints != null) {
                                route.routePoints = routeData.routePoints;
                                Log.d(TAG, "Route path generated for " + route.name + ": " + 
                                      routeData.routePoints.size() + " points");
                            } else {
                                Log.w(TAG, "Failed to generate route for " + route.name);
                            }
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Route error for " + route.name + ": " + error);
                        }
                    });
                    
                    // Small delay to avoid overwhelming the routing service
                    Thread.sleep(200);
                    processedRoutes++;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error processing route " + route.name, e);
                }
            }
            
            Log.d(TAG, "Route path generation completed for " + processedRoutes + " routes");
            if (onComplete != null) {
                onComplete.run();
            }
        }).start();
    }
    
    /**
     * Get current traffic level for a route based on time and patterns
     */
    public String getSimulatedTrafficLevel(TrafficRoute route) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        
        // Weekend traffic is generally lighter
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            if (hour >= 10 && hour <= 16) return "Medium";
            return "Low";
        }
        
        // Weekday patterns with route-specific variations
        boolean isRushHour = (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19);
        boolean isMajorRoute = route.name.contains("CTE") || route.name.contains("ECP") || route.name.contains("BKE");
        
        if (isRushHour) {
            if (isMajorRoute) {
                return Math.random() > 0.2 ? "High" : "Medium"; // 80% high, 20% medium
            } else {
                return Math.random() > 0.4 ? "High" : "Medium"; // 60% high, 40% medium
            }
        } else if (hour >= 10 && hour <= 16) {
            return Math.random() > 0.6 ? "Medium" : "Low"; // 40% medium, 60% low
        } else {
            return "Low"; // Night/early morning
        }
    }
    
    /**
     * Update traffic levels for all routes
     */
    public void updateTrafficLevels() {
        for (TrafficRoute route : trafficRoutes) {
            route.currentTrafficLevel = getSimulatedTrafficLevel(route);
        }
        Log.d(TAG, "Updated traffic levels for all routes");
    }
    
    /**
     * Get all traffic routes with current levels
     */
    public List<TrafficRoute> getTrafficRoutes() {
        return trafficRoutes;
    }
}
