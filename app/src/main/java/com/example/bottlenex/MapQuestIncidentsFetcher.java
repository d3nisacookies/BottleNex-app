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

public class MapQuestIncidentsFetcher {
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

    private static final String API_KEY = "U2Iefi1wix8SA9G6OILusOCPeR0BpWXZ";
    private static final String BASE_URL = "https://www.mapquestapi.com/traffic/v2/incidents";
    private static final OkHttpClient client = new OkHttpClient();

    public static void fetchIncidents(double lat, double lon, double radiusKm, IncidentsCallback callback) {
        // MapQuest expects bounding box: topLeftLat, topLeftLng, bottomRightLat, bottomRightLng
        double latDelta = radiusKm / 111.0; // ~111km per degree latitude
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        double topLeftLat = lat + latDelta;
        double topLeftLon = lon - lonDelta;
        double bottomRightLat = lat - latDelta;
        double bottomRightLon = lon + lonDelta;

        String url = BASE_URL + "?key=" + API_KEY +
                "&boundingBox=" + topLeftLat + "," + topLeftLon + "," + bottomRightLat + "," + bottomRightLon +
                "&filters=construction,incidents,congestion,disabledVehicle,accident,roadHazard,weather,other" +
                "&inFormat=kvp&outFormat=json";

        Log.d("MapQuestIncidents", "Requesting: " + url);
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MapQuestIncidents", "API request failed: " + e.getMessage());
                postResult(new ArrayList<>());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<Incident> incidents = new ArrayList<>();
                if (!response.isSuccessful()) {
                    Log.e("MapQuestIncidents", "API response not successful: " + response.code());
                    postResult(incidents);
                    return;
                }
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    JSONArray incidentsArr = json.getJSONArray("incidents");
                    for (int i = 0; i < incidentsArr.length(); i++) {
                        JSONObject obj = incidentsArr.getJSONObject(i);
                        double ilat = obj.getDouble("lat");
                        double ilon = obj.getDouble("lng");
                        String type = obj.optString("type", "");
                        String desc = obj.optString("fullDesc", obj.optString("shortDesc", ""));
                        int distance = obj.optInt("distance", 0);
                        incidents.add(new Incident(ilat, ilon, type, desc, distance));
                    }
                    Log.d("MapQuestIncidents", "Found " + incidents.size() + " incidents.");
                    postResult(incidents);
                } catch (Exception e) {
                    Log.e("MapQuestIncidents", "Error parsing response: " + e.getMessage());
                    postResult(incidents);
                }
            }

            private void postResult(List<Incident> incidents) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onIncidentsResult(incidents));
            }
        });
    }
} 