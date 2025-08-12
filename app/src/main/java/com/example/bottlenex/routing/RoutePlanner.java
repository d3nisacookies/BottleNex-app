package com.example.bottlenex.routing;

//To commit

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RoutePlanner {

    private static final String TAG = "AltRoutes";

    public static class NavigationStep {
        public String instruction;
        public String streetName;
        public double distance;
        public double duration;
        public String maneuver;
        public GeoPoint location;

        public NavigationStep(String instruction, String streetName, double distance, double duration, String maneuver, GeoPoint location) {
            this.instruction = instruction;
            this.streetName = streetName;
            this.distance = distance;
            this.duration = duration;
            this.maneuver = maneuver;
            this.location = location;
        }
    }

    public static class RouteData {
        public ArrayList<GeoPoint> routePoints;
        public List<NavigationStep> navigationSteps;
        public double duration;
        public double distance;

        public RouteData(ArrayList<GeoPoint> routePoints, List<NavigationStep> navigationSteps, double duration, double distance) {
            this.routePoints = routePoints;
            this.navigationSteps = navigationSteps;
            this.duration = duration;
            this.distance = distance;
        }
    }

    public interface RouteCallback {
        void onRouteReady(RouteData routeData);
        void onError(String errorMessage);
    }

    public interface DualRouteCallback {
        void onRoutesReady(RouteData mainRoute, RouteData alternativeRoute);
        void onError(String errorMessage);
    }

    public static void getRoute(GeoPoint start, GeoPoint end, String apiKey, RouteCallback callback) {
        new AsyncTask<Void, Void, Void>() {
            ArrayList<GeoPoint> routePoints = new ArrayList<>();
            List<NavigationStep> navigationSteps = new ArrayList<>();
            double duration = 0;
            double distance = 0;
            String error = null;

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // Request detailed instructions in the API call
                    String urlString = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=" + apiKey +
                            "&start=" + start.getLongitude() + "," + start.getLatitude() +
                            "&end=" + end.getLongitude() + "," + end.getLatitude() +
                            "&instructions=true&geometry=true";

                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject response = new JSONObject(result.toString());
                    JSONObject feature = response.getJSONArray("features").getJSONObject(0);
                    JSONObject properties = feature.getJSONObject("properties");
                    JSONObject summary = properties.getJSONObject("summary");
                    
                    duration = summary.getDouble("duration");
                    distance = summary.getDouble("distance");

                    // Extract route coordinates
                    JSONArray coords = feature.getJSONObject("geometry").getJSONArray("coordinates");
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray point = coords.getJSONArray(i);
                        routePoints.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
                    }

                    // Extract navigation steps
                    JSONArray segments = properties.optJSONArray("segments");
                    if (segments != null) {
                        for (int i = 0; i < segments.length(); i++) {
                            JSONObject segment = segments.getJSONObject(i);
                            JSONArray steps = segment.optJSONArray("steps");
                            if (steps == null) continue;
                            for (int j = 0; j < steps.length(); j++) {
                                JSONObject step = steps.getJSONObject(j);
                                String instruction = step.getString("instruction");
                                String streetName = step.optString("name", "");
                                double stepDistance = step.getDouble("distance");
                                double stepDuration = step.getDouble("duration");
                                String maneuver = step.optString("maneuver", "");
                                JSONArray stepCoords = step.getJSONArray("way_points");
                                int startIndex = stepCoords.getInt(0);
                                if (startIndex < routePoints.size()) {
                                    GeoPoint stepLocation = routePoints.get(startIndex);
                                    NavigationStep navStep = new NavigationStep(
                                        instruction, streetName, stepDistance, stepDuration, maneuver, stepLocation
                                    );
                                    navigationSteps.add(navStep);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    error = e.getMessage();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    RouteData routeData = new RouteData(routePoints, navigationSteps, duration, distance);
                    callback.onRouteReady(routeData);
                }
            }
        }.execute();
    }

    public static void getDualRoutes(GeoPoint start, GeoPoint end, String apiKey, DualRouteCallback callback) {
        new AsyncTask<Void, Void, Void>() {
            RouteData mainRoute = null;
            RouteData alternativeRoute = null;
            String error = null;

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // Get main route (fastest by default)
                    mainRoute = getRouteData(start, end, apiKey, "driving-car", null, null);

                    // Try alternative with 'shortest'
                    alternativeRoute = getRouteData(start, end, apiKey, "driving-car", "shortest", null);

                    // If alt is null or too similar, try 'recommended'
                    if (alternativeRoute == null || isSimilarRoute(mainRoute, alternativeRoute)) {
                        alternativeRoute = getRouteData(start, end, apiKey, "driving-car", "recommended", null);
                    }

                } catch (Exception e) {
                    error = e.getMessage();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (error != null) {
                    callback.onError(error);
                } else if (mainRoute != null && alternativeRoute != null) {
                    callback.onRoutesReady(mainRoute, alternativeRoute);
                } else {
                    callback.onError("Failed to get routes");
                }
            }
        }.execute();
    }

    public static void getDualRoutesPost(GeoPoint start, GeoPoint end, String apiKey, DualRouteCallback callback) {
        new AsyncTask<Void, Void, Void>() {
            RouteData mainRoute = null;
            RouteData altRoute = null;
            String error = null;

            @Override
            protected Void doInBackground(Void... voids) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL("https://api.openrouteservice.org/v2/directions/driving-car?api_key=" + apiKey);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty("Accept", "application/geo+json");
                    conn.setRequestProperty("Authorization", apiKey);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(20000);
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    JSONArray coords = new JSONArray();
                    JSONArray startCoord = new JSONArray();
                    startCoord.put(start.getLongitude());
                    startCoord.put(start.getLatitude());
                    JSONArray endCoord = new JSONArray();
                    endCoord.put(end.getLongitude());
                    endCoord.put(end.getLatitude());
                    coords.put(startCoord);
                    coords.put(endCoord);
                    body.put("coordinates", coords);

                    JSONObject alt = new JSONObject();
                    alt.put("share_factor", 0.6);
                    alt.put("target_count", 3);
                    alt.put("weight_factor", 1.4);
                    body.put("alternative_routes", alt);
                    body.put("instructions", true);
                    body.put("format", "geojson");

                    String bodyStr = body.toString();
                    Log.d(TAG, "POST /directions body: " + bodyStr);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = bodyStr.getBytes(StandardCharsets.UTF_8);
                        os.write(input);
                        os.flush();
                    }

                    int status = conn.getResponseCode();
                    Log.d(TAG, "HTTP status: " + status);
                    InputStream is = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    String resp = sb.toString();
                    logLong(TAG, "Response: " + resp);

                    if (status < 200 || status >= 300) {
                        error = "HTTP " + status;
                        return null;
                    }

                    JSONObject response = new JSONObject(resp);
                    if (!response.has("features")) {
                        if (response.has("routes")) {
                            Log.w(TAG, "No features, found 'routes' array. Parsing JSON format.");
                            List<RouteData> routes = parseJsonRoutes(response);
                            if (routes.isEmpty()) {
                                error = "No parsed routes (json)";
                                return null;
                            }
                            mainRoute = routes.get(0);
                            for (int i = 1; i < routes.size(); i++) {
                                if (!isSimilarRoute(mainRoute, routes.get(i))) { altRoute = routes.get(i); break; }
                            }
                            if (altRoute == null && routes.size() > 1) altRoute = routes.get(1);
                            return null;
                        }
                        error = "No features in response";
                        return null;
                    }

                    JSONArray features = response.getJSONArray("features");
                    Log.d(TAG, "features.length(): " + features.length());
                    if (features.length() == 0) {
                        error = "Empty features";
                        return null;
                    }

                    List<RouteData> routes = new ArrayList<>();
                    for (int i = 0; i < features.length(); i++) {
                        JSONObject feature = features.getJSONObject(i);
                        RouteData rd = parseRouteFeature(feature);
                        if (rd != null) {
                            routes.add(rd);
                        } else {
                            Log.w(TAG, "Skipping route index=" + i + " due to parse failure");
                        }
                    }

                    Log.d(TAG, "Parsed routes count: " + routes.size());
                    if (routes.isEmpty()) {
                        error = "No parsed routes";
                        return null;
                    }

                    mainRoute = routes.get(0);

                    for (int i = 1; i < routes.size(); i++) {
                        if (!isSimilarRoute(mainRoute, routes.get(i))) {
                            altRoute = routes.get(i);
                            break;
                        }
                    }

                    if (altRoute == null && routes.size() > 1) {
                        altRoute = routes.get(1);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Exception during POST: " + e.getMessage(), e);
                    error = e.getMessage();
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (error != null) {
                    Log.e(TAG, "Alt route error: " + error);
                    callback.onError(error);
                } else if (mainRoute != null && altRoute != null) {
                    Log.d(TAG, "Routes ready. mainDistance=" + mainRoute.distance + ", altDistance=" + altRoute.distance);
                    callback.onRoutesReady(mainRoute, altRoute);
                } else {
                    Log.e(TAG, "Failed to get routes (null results)");
                    callback.onError("Failed to get routes");
                }
            }
        }.execute();
    }

    private static RouteData getRouteData(GeoPoint start, GeoPoint end, String apiKey, String profile, String preference, Boolean continueStraight) {
        try {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("https://api.openrouteservice.org/v2/directions/")
                    .append(profile)
                    .append("?api_key=").append(apiKey)
                    .append("&start=").append(start.getLongitude()).append(",").append(start.getLatitude())
                    .append("&end=").append(end.getLongitude()).append(",").append(end.getLatitude())
                    .append("&instructions=true&geometry=true");

            if (preference != null && !preference.isEmpty()) {
                urlBuilder.append("&preference=").append(preference);
            }
            if (continueStraight != null) {
                urlBuilder.append("&continue_straight=").append(continueStraight);
            }

            URL url = new URL(urlBuilder.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            JSONObject response = new JSONObject(result.toString());
            JSONObject feature = response.getJSONArray("features").getJSONObject(0);
            JSONObject properties = feature.getJSONObject("properties");
            JSONObject summary = properties.getJSONObject("summary");

            double duration = summary.getDouble("duration");
            double distance = summary.getDouble("distance");

            // get route coords
            ArrayList<GeoPoint> routePoints = new ArrayList<>();
            JSONArray coords = feature.getJSONObject("geometry").getJSONArray("coordinates");
            for (int i = 0; i < coords.length(); i++) {
                JSONArray point = coords.getJSONArray(i);
                routePoints.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
            }

            List<NavigationStep> navigationSteps = new ArrayList<>();
            JSONArray segments = properties.optJSONArray("segments");
            if (segments != null) {
                for (int i = 0; i < segments.length(); i++) {
                    JSONObject segment = segments.getJSONObject(i);
                    JSONArray steps = segment.optJSONArray("steps");
                    if (steps == null) continue;
                    for (int j = 0; j < steps.length(); j++) {
                        JSONObject step = steps.getJSONObject(j);
                        String instruction = step.getString("instruction");
                        String streetName = step.optString("name", "");
                        double stepDistance = step.getDouble("distance");
                        double stepDuration = step.getDouble("duration");
                        String maneuver = step.optString("maneuver", "");
                        JSONArray stepCoords = step.getJSONArray("way_points");
                        int startIndex = stepCoords.getInt(0);
                        if (startIndex < routePoints.size()) {
                            GeoPoint stepLocation = routePoints.get(startIndex);
                            NavigationStep navStep = new NavigationStep(
                                    instruction, streetName, stepDistance, stepDuration, maneuver, stepLocation
                            );
                            navigationSteps.add(navStep);
                        }
                    }
                }
            }

            return new RouteData(routePoints, navigationSteps, duration, distance);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isSimilarRoute(RouteData route1, RouteData route2) {
        if (route1 == null || route2 == null) return true;
        double distanceDiff = Math.abs(route1.distance - route2.distance);
        double avgDistance = (route1.distance + route2.distance) / 2;
        return distanceDiff / avgDistance < 0.1; // Less than 10% difference
    }

    private static RouteData parseRouteFeature(JSONObject feature) {
        try {
            JSONObject properties = feature.getJSONObject("properties");
            JSONObject summary = properties.getJSONObject("summary");
            double duration = summary.getDouble("duration");
            double distance = summary.getDouble("distance");

            ArrayList<GeoPoint> routePoints = new ArrayList<>();
            JSONArray coords = feature.getJSONObject("geometry").getJSONArray("coordinates");
            for (int i = 0; i < coords.length(); i++) {
                JSONArray point = coords.getJSONArray(i);
                routePoints.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
            }

            List<NavigationStep> navigationSteps = new ArrayList<>();
            JSONArray segments = properties.optJSONArray("segments");
            if (segments != null) {
                for (int i = 0; i < segments.length(); i++) {
                    JSONObject segment = segments.getJSONObject(i);
                    JSONArray steps = segment.optJSONArray("steps");
                    if (steps == null) continue;
                    for (int j = 0; j < steps.length(); j++) {
                        JSONObject step = steps.getJSONObject(j);
                        String instruction = step.getString("instruction");
                        String streetName = step.optString("name", "");
                        double stepDistance = step.getDouble("distance");
                        double stepDuration = step.getDouble("duration");
                        String maneuver = step.optString("maneuver", "");
                        JSONArray stepCoords = step.getJSONArray("way_points");
                        int startIndex = stepCoords.getInt(0);
                        if (startIndex < routePoints.size()) {
                            GeoPoint stepLocation = routePoints.get(startIndex);
                            NavigationStep navStep = new NavigationStep(
                                    instruction, streetName, stepDistance, stepDuration, maneuver, stepLocation
                            );
                            navigationSteps.add(navStep);
                        }
                    }
                }
            }
            return new RouteData(routePoints, navigationSteps, duration, distance);
        } catch (Exception e) {
            Log.e(TAG, "parseRouteFeature error: " + e.getMessage(), e);
            return null;
        }
    }

    private static List<RouteData> parseJsonRoutes(JSONObject response) {
        List<RouteData> result = new ArrayList<>();
        try {
            JSONArray routes = response.getJSONArray("routes");
            for (int r = 0; r < routes.length(); r++) {
                JSONObject route = routes.getJSONObject(r);
                JSONObject summary = route.getJSONObject("summary");
                double duration = summary.getDouble("duration");
                double distance = summary.getDouble("distance");

                String encoded = route.optString("geometry", null);
                ArrayList<GeoPoint> routePoints = new ArrayList<>();
                if (encoded != null && !encoded.isEmpty()) {
                    routePoints.addAll(decodePolyline(encoded));
                }

                List<NavigationStep> navigationSteps = new ArrayList<>();
                JSONArray segments = route.optJSONArray("segments");
                if (segments != null) {
                    for (int i = 0; i < segments.length(); i++) {
                        JSONObject segment = segments.getJSONObject(i);
                        JSONArray steps = segment.optJSONArray("steps");
                        if (steps == null) continue;
                        for (int j = 0; j < steps.length(); j++) {
                            JSONObject step = steps.getJSONObject(j);
                            String instruction = step.optString("instruction", "");
                            String streetName = step.optString("name", "");
                            double stepDistance = step.optDouble("distance", 0);
                            double stepDuration = step.optDouble("duration", 0);
                            String maneuver = String.valueOf(step.optInt("type", 0));
                            GeoPoint stepLocation = routePoints.isEmpty() ? new GeoPoint(0, 0) : routePoints.get(Math.min(routePoints.size()-1, 0));
                            navigationSteps.add(new NavigationStep(instruction, streetName, stepDistance, stepDuration, maneuver, stepLocation));
                        }
                    }
                }

                result.add(new RouteData(routePoints, navigationSteps, duration, distance));
            }
        } catch (Exception e) {
            Log.e(TAG, "parseJsonRoutes error: " + e.getMessage(), e);
        }
        return result;
    }

    private static List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> path = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < len);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < len);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double latitude = lat / 1E5;
            double longitude = lng / 1E5;
            path.add(new GeoPoint(latitude, longitude));
        }
        return path;
    }

    private static void logLong(String tag, String message) {
        if (message == null) return;
        final int maxLogSize = 3500;
        for (int i = 0; i <= message.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = Math.min((i + 1) * maxLogSize, message.length());
            if (start < end) {
                Log.d(tag, message.substring(start, end));
            }
        }
    }
}
