package com.example.bottlenex;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TomTomIncidentsFetcher {
    public static class Incident {
        public final double lat;
        public final double lon;
        public final String type;
        public final String description;
        public final int distanceMeters;

        public Incident(double lat, double lon, String type, String description, int distanceMeters) {
            this.lat = lat;
            this.lon = lon;
            this.type = type;
            this.description = description;
            this.distanceMeters = distanceMeters;
        }
    }

    public interface IncidentsCallback {
        void onIncidentsResult(List<Incident> incidents);
    }

    private static final String API_KEY = "v0MHekYQmvieC8OOIosTag3grP8lSkmC";
    private static final String BASE_URL = "https://api.tomtom.com/traffic/services/5/incidentDetails";
    private static final OkHttpClient client = new OkHttpClient();

    public static void fetchIncidents(double lat, double lon, double radiusKm, IncidentsCallback callback) {
        // TomTom expects bounding box: minLat,minLon,maxLat,maxLon
        double latDelta = radiusKm / 111.0; // ~111km per degree latitude
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        double minLat = lat - latDelta;
        double minLon = lon - lonDelta;
        double maxLat = lat + latDelta;
        double maxLon = lon + lonDelta;

        // Format bounding box as required by TomTom API
        String bbox = String.format("%.6f,%.6f,%.6f,%.6f", minLat, minLon, maxLat, maxLon);

        // Using the correct fields parameter as per TomTom documentation
        String fields = "{incidents{type,geometry{type,coordinates},properties{iconCategory,from,to,length,delay,timeValidity,events{code,description,iconCategory}}}}";

        String url = BASE_URL + "?key=" + API_KEY +
                "&bbox=" + bbox +
                "&fields=" + fields +
                "&language=en-GB" +
                "&timeValidityFilter=present";

        Log.d("TomTomIncidents", "Requesting: " + url);
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TomTomIncidents", "API request failed: " + e.getMessage());
                postResult(new ArrayList<>());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<Incident> incidents = new ArrayList<>();
                
                // Log response headers for debugging
                Log.d("TomTomIncidents", "Response code: " + response.code());
                Log.d("TomTomIncidents", "Response headers:");
                for (String name : response.headers().names()) {
                    Log.d("TomTomIncidents", "  " + name + ": " + response.header(name));
                }
                
                if (!response.isSuccessful()) {
                    Log.e("TomTomIncidents", "API response not successful: " + response.code());
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e("TomTomIncidents", "Error response: " + errorBody);
                    postResult(incidents);
                    return;
                }
                String body = response.body().string();
                Log.d("TomTomIncidents", "Response body: " + body);
                try {
                    JSONObject json = new JSONObject(body);
                    JSONArray incidentsArr = json.getJSONArray("incidents");
                    for (int i = 0; i < incidentsArr.length(); i++) {
                        JSONObject incident = incidentsArr.getJSONObject(i);
                        JSONObject properties = incident.getJSONObject("properties");
                        JSONObject geometry = incident.getJSONObject("geometry");
                        
                        // Get coordinates (TomTom returns [lon, lat] format)
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        double ilon = coordinates.getDouble(0);
                        double ilat = coordinates.getDouble(1);
                        
                        // Get incident type and description
                        String type = properties.optString("iconCategory", "unknown");
                        String description = "";
                        
                        // Try to get description from events
                        if (properties.has("events")) {
                            JSONArray events = properties.getJSONArray("events");
                            if (events.length() > 0) {
                                JSONObject event = events.getJSONObject(0);
                                description = event.optString("description", "");
                            }
                        }
                        
                        // If no description from events, use type
                        if (description.isEmpty()) {
                            description = formatIncidentType(type);
                        }
                        
                        // Calculate distance from user location
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(lat, lon, ilat, ilon, results);
                        int distanceMeters = (int) results[0];
                        
                        incidents.add(new Incident(ilat, ilon, type, description, distanceMeters));
                    }
                    Log.d("TomTomIncidents", "Found " + incidents.size() + " incidents.");
                    postResult(incidents);
                } catch (Exception e) {
                    Log.e("TomTomIncidents", "Error parsing response: " + e.getMessage());
                    postResult(incidents);
                }
            }

            private void postResult(List<Incident> incidents) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onIncidentsResult(incidents));
            }
        });
    }

    private static String formatIncidentType(String type) {
        switch (type.toLowerCase()) {
            case "accident":
                return "Traffic Accident";
            case "congestion":
                return "Traffic Congestion";
            case "construction":
                return "Road Construction";
            case "disabledvehicle":
                return "Disabled Vehicle";
            case "masstransit":
                return "Mass Transit Issue";
            case "miscellaneous":
                return "Road Incident";
            case "othernews":
                return "Road Information";
            case "plannedevent":
                return "Planned Event";
            case "roadhazard":
                return "Road Hazard";
            case "weather":
                return "Weather Related";
            default:
                return "Traffic Incident";
        }
    }
} 