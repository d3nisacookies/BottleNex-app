package com.example.bottlenex.ml;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import org.osmdroid.util.GeoPoint;
import java.util.*;

/**
 * Traffic Bottleneck Identifier
 * Identifies the top-k traffic bottlenecks by analyzing the current navigation route
 * and identifying segments with highest traffic impact potential.
 */
public class TrafficBottleneckIdentifier {
    private static final String TAG = "TrafficBottleneckIdentifier";
    private static final int DEFAULT_K = 5; // Default number of bottlenecks to identify

    private Context context;
    private List<RoadEdge> roadNetwork;
    private List<TrafficBottleneck> identifiedBottlenecks;
    private List<GeoPoint> currentRoutePoints;

    /**
     * Represents a road edge in the network
     */
    public static class RoadEdge {
        public String edgeId;
        public String edgeName;
        public GeoPoint startPoint;
        public GeoPoint endPoint;
        public double congestionLevel; // 0.0 to 1.0
        public double influenceScore;
        public List<String> influencedEdges;

        public RoadEdge(String edgeId, String edgeName, GeoPoint start, GeoPoint end) {
            this.edgeId = edgeId;
            this.edgeName = edgeName;
            this.startPoint = start;
            this.endPoint = end;
            this.congestionLevel = 0.0;
            this.influenceScore = 0.0;
            this.influencedEdges = new ArrayList<>();
        }
    }

    /**
     * Represents an identified traffic bottleneck
     */
    public static class TrafficBottleneck {
        public RoadEdge edge;
        public double impactScore;
        public int influencedEdgeCount;
        public String severity;

        public TrafficBottleneck(RoadEdge edge, double impactScore, int influencedEdgeCount) {
            this.edge = edge;
            this.impactScore = impactScore;
            this.influencedEdgeCount = influencedEdgeCount;
            this.severity = determineSeverity(impactScore);
        }

        private String determineSeverity(double score) {
            if (score >= 0.8) return "Critical";
            else if (score >= 0.6) return "High";
            else if (score >= 0.4) return "Medium";
            else return "Low";
        }
    }

    public TrafficBottleneckIdentifier(Context context) {
        this.context = context;
        this.roadNetwork = new ArrayList<>();
        this.identifiedBottlenecks = new ArrayList<>();
        this.currentRoutePoints = new ArrayList<>();
    }

    /**
     * Set the current navigation route points
     */
    public void setCurrentRoute(List<GeoPoint> routePoints) {
        this.currentRoutePoints = routePoints != null ? new ArrayList<>(routePoints) : new ArrayList<>();
        Log.d(TAG, "Set current route with " + this.currentRoutePoints.size() + " points");
    }

    /**
     * Build road network from current navigation route
     * Creates road edges from consecutive route points
     */
    private void buildRoadNetworkFromRoute() {
        roadNetwork.clear();

        if (currentRoutePoints == null || currentRoutePoints.size() < 2) {
            Log.w(TAG, "Cannot build road network: insufficient route points");
            return;
        }

        Log.d(TAG, "Building road network from " + currentRoutePoints.size() + " route points");

        // Create road edges from consecutive route points
        for (int i = 0; i < currentRoutePoints.size() - 1; i++) {
            GeoPoint start = currentRoutePoints.get(i);
            GeoPoint end = currentRoutePoints.get(i + 1);

            // Calculate distance between points
            double distance = calculateDistance(start, end);

            // Create meaningful edge names based on distance and position
            String edgeName = generateEdgeName(i, distance, start, end);
            String edgeId = "ROUTE_SEGMENT_" + i;

            RoadEdge edge = new RoadEdge(edgeId, edgeName, start, end);
            roadNetwork.add(edge);

            Log.d(TAG, "Created road edge " + i + ": " + edgeName +
                    " (Distance: " + String.format("%.1f", distance * 1000) + "m)");
        }

        Log.d(TAG, "Built road network with " + roadNetwork.size() + " edges from current route");
    }

    /**
     * Generate meaningful edge names based on route position and distance
     */
    private String generateEdgeName(int segmentIndex, double distance, GeoPoint start, GeoPoint end) {
        if (segmentIndex == 0) {
            return "Route Start Segment";
        } else if (segmentIndex == currentRoutePoints.size() - 2) {
            return "Route End Segment";
        } else if (distance > 0.01) { // > 1km
            return "Long Route Segment " + (segmentIndex + 1);
        } else if (distance > 0.005) { // > 500m
            return "Medium Route Segment " + (segmentIndex + 1);
        } else {
            return "Route Segment " + (segmentIndex + 1);
        }
    }

    /**
     * Identify top-k traffic bottlenecks from current navigation route
     * @param k Number of bottlenecks to identify
     * @return List of identified bottlenecks sorted by impact score
     */
    public List<TrafficBottleneck> identifyTopKBottlenecks(int k) {
        Log.d(TAG, "Starting identification of top-" + k + " traffic bottlenecks from current route");

        // Check if we have a valid route
        if (currentRoutePoints == null || currentRoutePoints.size() < 2) {
            Log.w(TAG, "No valid route available for bottleneck analysis");
            return new ArrayList<>();
        }

        // Build road network from current route
        buildRoadNetworkFromRoute();

        if (roadNetwork.isEmpty()) {
            Log.w(TAG, "Failed to build road network from route");
            return new ArrayList<>();
        }

        // Update current traffic conditions based on route segments
        updateCurrentTrafficConditions();

        // Calculate influence relationships between route segments
        calculateInfluenceRelationships();

        // Calculate impact scores
        calculateImpactScores();

        // Sort by impact score and return top-k
        Collections.sort(roadNetwork, (e1, e2) -> Double.compare(e2.influenceScore, e1.influenceScore));

        identifiedBottlenecks.clear();
        int count = Math.min(k, roadNetwork.size());

        for (int i = 0; i < count; i++) {
            RoadEdge edge = roadNetwork.get(i);
            double impactScore = edge.influenceScore;
            int influencedCount = edge.influencedEdges.size();

            TrafficBottleneck bottleneck = new TrafficBottleneck(edge, impactScore, influencedCount);
            identifiedBottlenecks.add(bottleneck);

            // Log detailed information about each bottleneck
            Log.d(TAG, "Bottleneck " + (i + 1) + ": " + edge.edgeName +
                    " (Score: " + String.format("%.2f", impactScore) +
                    ", Congestion: " + String.format("%.2f", edge.congestionLevel) +
                    ", Influenced: " + influencedCount +
                    ", Severity: " + bottleneck.severity + ")");

            // Log route-specific bottleneck information
            if (edge.edgeName.contains("Route Start")) {
                Log.d(TAG, "🚨 START SEGMENT BOTTLENECK: " + edge.edgeName + " - Beginning of route congestion!");
            } else if (edge.edgeName.contains("Route End")) {
                Log.d(TAG, "⚠️ END SEGMENT BOTTLENECK: " + edge.edgeName + " - Destination area congestion!");
            } else if (edge.edgeName.contains("Long Route")) {
                Log.d(TAG, "⚠️ LONG SEGMENT BOTTLENECK: " + edge.edgeName + " - Extended route congestion!");
            }
        }

        Log.d(TAG, "Identified " + identifiedBottlenecks.size() + " bottlenecks from current route");
        return identifiedBottlenecks;
    }

    /**
     * Update current traffic conditions based on time and historical patterns
     */
    private void updateCurrentTrafficConditions() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);

        for (RoadEdge edge : roadNetwork) {
            // Base congestion on time of day and road type
            double baseCongestion = getBaseCongestion(hour, dayOfWeek, edge.edgeName);

            // Add some randomness to simulate real-world variations
            double randomFactor = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2

            edge.congestionLevel = Math.min(1.0, baseCongestion * randomFactor);
        }

        Log.d(TAG, "Updated traffic conditions for " + roadNetwork.size() + " road edges");
    }

    /**
     * Get base congestion level based on time and route segment characteristics
     * Creates realistic bottleneck patterns based on route position and time
     */
    private double getBaseCongestion(int hour, int dayOfWeek, String roadName) {
        // Weekend traffic is generally lighter
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            if (hour >= 10 && hour <= 16) return 0.4; // Medium
            return 0.2; // Low
        }

        // Create realistic bottleneck patterns based on route segments
        if (roadName.contains("Route Start")) {
            // START SEGMENT - Often congested due to traffic merging
            if (hour >= 7 && hour <= 9) return 0.85; // Morning rush - High
            if (hour >= 17 && hour <= 19) return 0.85; // Evening rush - High
            if (hour >= 10 && hour <= 16) return 0.7; // Mid-day - Medium-High
            return 0.4; // Night - Medium
        }

        if (roadName.contains("Route End")) {
            // END SEGMENT - Often congested due to destination traffic
            if (hour >= 7 && hour <= 9) return 0.8; // Morning rush - High
            if (hour >= 17 && hour <= 19) return 0.9; // Evening rush - Very High
            if (hour >= 10 && hour <= 16) return 0.6; // Mid-day - Medium
            return 0.3; // Night - Low
        }

        if (roadName.contains("Long Route")) {
            // LONG SEGMENTS - Extended congestion areas
            if (hour >= 7 && hour <= 9) return 0.75; // Morning rush - Medium-High
            if (hour >= 17 && hour <= 19) return 0.75; // Evening rush - Medium-High
            if (hour >= 10 && hour <= 16) return 0.5; // Mid-day - Medium
            return 0.3; // Night - Low
        }

        if (roadName.contains("Medium Route")) {
            // MEDIUM SEGMENTS - Moderate congestion
            if (hour >= 7 && hour <= 9) return 0.65; // Morning rush - Medium
            if (hour >= 17 && hour <= 19) return 0.65; // Evening rush - Medium
            if (hour >= 10 && hour <= 16) return 0.4; // Mid-day - Medium
            return 0.2; // Night - Low
        }

        // Default patterns for other route segments
        if (hour >= 7 && hour <= 9) { // Morning rush
            return 0.6; // Medium for other segments
        } else if (hour >= 17 && hour <= 19) { // Evening rush
            return 0.6; // Medium for other segments
        } else if (hour >= 10 && hour <= 16) { // Mid-day
            return 0.4; // Medium
        } else { // Night/early morning
            return 0.2; // Low
        }
    }

    /**
     * Calculate influence relationships between route segments
     * Models realistic traffic flow with cascading effects along the route
     */
    private void calculateInfluenceRelationships() {
        for (RoadEdge edge : roadNetwork) {
            edge.influencedEdges.clear();

            // Find edges that are influenced by this edge
            for (RoadEdge otherEdge : roadNetwork) {
                if (!edge.edgeId.equals(otherEdge.edgeId)) {
                    double distance = calculateDistance(edge.endPoint, otherEdge.startPoint);

                    // For the 2km main road, create realistic influence patterns
                    if (edge.congestionLevel > 0.6) { // Only congested edges can influence others

                        // Main road segments influence each other more strongly
                        boolean isMainRoadSegment = edge.edgeName.contains("Marina Bay") ||
                                edge.edgeName.contains("Raffles Place") ||
                                edge.edgeName.contains("Shenton Way") ||
                                edge.edgeName.contains("Tanjong Pagar") ||
                                edge.edgeName.contains("Outram");

                        boolean isOtherMainRoad = otherEdge.edgeName.contains("Marina Bay") ||
                                otherEdge.edgeName.contains("Raffles Place") ||
                                otherEdge.edgeName.contains("Shenton Way") ||
                                otherEdge.edgeName.contains("Tanjong Pagar") ||
                                otherEdge.edgeName.contains("Outram");

                        if (isMainRoadSegment && isOtherMainRoad) {
                            // Main road segments influence each other strongly
                            if (distance < 0.005) { // Within ~500m
                                edge.influencedEdges.add(otherEdge.edgeId);

                                // Strong influence between main road segments
                                double influenceStrength = edge.congestionLevel * 0.8;
                                otherEdge.congestionLevel = Math.min(1.0,
                                        otherEdge.congestionLevel + influenceStrength * 0.4);
                            }
                        } else if (isMainRoadSegment && !isOtherMainRoad) {
                            // Main road influences side roads
                            if (distance < 0.003) { // Within ~300m
                                edge.influencedEdges.add(otherEdge.edgeId);

                                // Moderate influence on side roads
                                double influenceStrength = edge.congestionLevel * 0.6;
                                otherEdge.congestionLevel = Math.min(1.0,
                                        otherEdge.congestionLevel + influenceStrength * 0.3);
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Calculated influence relationships for route segments with realistic cascading effects");
    }

    /**
     * Calculate impact scores for each route segment
     * Prioritizes bottlenecks based on route position and congestion
     */
    private void calculateImpactScores() {
        for (RoadEdge edge : roadNetwork) {
            // Impact score based on:
            // 1. Congestion level (50%)
            // 2. Number of influenced edges (30%)
            // 3. Road importance factor (20%)

            double congestionScore = edge.congestionLevel * 0.5;
            double influenceScore = Math.min(1.0, edge.influencedEdges.size() / 3.0) * 0.3;
            double importanceScore = getRoadImportance(edge.edgeName) * 0.2;

            // Bonus for critical route segments
            if (edge.edgeName.contains("Route Start") || edge.edgeName.contains("Route End")) {
                importanceScore += 0.1; // Extra importance for start/end segments
            }

            edge.influenceScore = congestionScore + influenceScore + importanceScore;
        }

        Log.d(TAG, "Calculated impact scores prioritizing route segment bottlenecks");
    }

    /**
     * Get route segment importance factor based on position and characteristics
     * Prioritizes start, end, and long segments as potential bottlenecks
     */
    private double getRoadImportance(String roadName) {
        // Route segments get importance based on position and characteristics
        if (roadName.contains("Route Start")) {
            return 1.0; // Start segment - highest importance (traffic merging)
        } else if (roadName.contains("Route End")) {
            return 0.95; // End segment - very high importance (destination traffic)
        } else if (roadName.contains("Long Route")) {
            return 0.9; // Long segments - high importance (extended congestion)
        } else if (roadName.contains("Medium Route")) {
            return 0.8; // Medium segments - medium-high importance
        } else if (roadName.contains("Route Segment")) {
            return 0.7; // Regular route segments - medium importance
        } else {
            return 0.5; // Other segments - lower importance
        }
    }

    /**
     * Calculate distance between two geographic points
     */
    private double calculateDistance(GeoPoint point1, GeoPoint point2) {
        double lat1 = Math.toRadians(point1.getLatitude());
        double lon1 = Math.toRadians(point1.getLongitude());
        double lat2 = Math.toRadians(point2.getLatitude());
        double lon2 = Math.toRadians(point2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return 6371 * c; // Earth's radius in km
    }

    /**
     * Get the identified bottlenecks
     */
    public List<TrafficBottleneck> getIdentifiedBottlenecks() {
        return identifiedBottlenecks;
    }

    /**
     * Get a summary of the current route bottlenecks
     */
    public String getRouteBottleneckSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("CURRENT ROUTE BOTTLENECK ANALYSIS\n");
        summary.append("==================================\n\n");

        // Find all route segments
        for (RoadEdge edge : roadNetwork) {
            summary.append(String.format("📍 %s\n", edge.edgeName));
            summary.append(String.format("   Congestion Level: %.1f%%\n", edge.congestionLevel * 100));
            summary.append(String.format("   Impact Score: %.2f\n", edge.influenceScore));
            summary.append(String.format("   Influenced Edges: %d\n", edge.influencedEdges.size()));

            // Add bottleneck classification
            if (edge.edgeName.contains("Route Start")) {
                summary.append("   🚨 CLASSIFICATION: START SEGMENT BOTTLENECK\n");
            } else if (edge.edgeName.contains("Route End")) {
                summary.append("   ⚠️ CLASSIFICATION: END SEGMENT BOTTLENECK\n");
            } else if (edge.edgeName.contains("Long Route")) {
                summary.append("   ⚠️ CLASSIFICATION: LONG SEGMENT BOTTLENECK\n");
            } else if (edge.edgeName.contains("Medium Route")) {
                summary.append("   ⚠️ CLASSIFICATION: MEDIUM SEGMENT BOTTLENECK\n");
            } else {
                summary.append("   📍 CLASSIFICATION: ROUTE SEGMENT\n");
            }
            summary.append("\n");
        }

        return summary.toString();
    }

    /**
     * Get road network for visualization
     */
    public List<RoadEdge> getRoadNetwork() {
        return roadNetwork;
    }

    /**
     * Clear identified bottlenecks
     */
    public void clearBottlenecks() {
        identifiedBottlenecks.clear();
        Log.d(TAG, "Cleared identified bottlenecks");
    }
}