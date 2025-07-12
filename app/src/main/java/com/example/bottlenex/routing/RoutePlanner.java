package com.example.bottlenex.routing;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class RoutePlanner {

    public interface RouteCallback {
        void onRouteReady(ArrayList<GeoPoint> routePoints, double duration, double distance);
        void onError(String errorMessage);
    }

    public static void getRoute(GeoPoint start, GeoPoint end, String apiKey, RouteCallback callback) {
        new AsyncTask<Void, Void, Void>() {
            ArrayList<GeoPoint> routePoints = new ArrayList<>();
            double duration = 0;
            double distance = 0;
            String error = null;

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    String urlString = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=" + apiKey +
                            "&start=" + start.getLongitude() + "," + start.getLatitude() +
                            "&end=" + end.getLongitude() + "," + end.getLatitude();

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
                    JSONObject summary = response.getJSONArray("features")
                            .getJSONObject(0).getJSONObject("properties").getJSONObject("summary");
                    duration = summary.getDouble("duration");
                    distance = summary.getDouble("distance");

                    JSONArray coords = response.getJSONArray("features")
                            .getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");

                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray point = coords.getJSONArray(i);
                        routePoints.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
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
                    callback.onRouteReady(routePoints, duration, distance);
                }
            }
        }.execute();
    }
}
