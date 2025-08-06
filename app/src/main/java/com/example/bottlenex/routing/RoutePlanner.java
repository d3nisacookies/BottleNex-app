package com.example.bottlenex.routing;

//To commit

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RoutePlanner {

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
                    JSONArray segments = properties.getJSONArray("segments");
                    for (int i = 0; i < segments.length(); i++) {
                        JSONObject segment = segments.getJSONObject(i);
                        JSONArray steps = segment.getJSONArray("steps");
                        
                        for (int j = 0; j < steps.length(); j++) {
                            JSONObject step = steps.getJSONObject(j);
                            
                            String instruction = step.getString("instruction");
                            String streetName = step.optString("name", "");
                            double stepDistance = step.getDouble("distance");
                            double stepDuration = step.getDouble("duration");
                            String maneuver = step.optString("maneuver", "");
                            
                            // Get step location (use the first coordinate of the step)
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
}
