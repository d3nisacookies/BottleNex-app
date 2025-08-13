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

public class OSMSpeedCameraFetcher {
    public static class SpeedCamera {
        public final double lat;
        public final double lon;

        public SpeedCamera(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    public interface SpeedCameraCallback {
        void onSpeedCamerasResult(List<SpeedCamera> cameras);
    }

    // Using alternative Overpass API server due to overpass-api.de connectivity issues
    private static final String OVERPASS_URL = "https://overpass.kumi.systems/api/interpreter";
    private static final OkHttpClient client = new OkHttpClient();

    public static void fetchSpeedCameras(double lat, double lon, double radiusKm, SpeedCameraCallback callback) {
        String query = "[out:json];node(around:" + (int)(radiusKm * 1000) + "," + lat + "," + lon + ")[highway=speed_camera];out;";
        String url = OVERPASS_URL + "?data=" + java.net.URLEncoder.encode(query);

        Log.d("SpeedCameraFetcher", "Requesting: " + url);
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SpeedCameraFetcher", "Overpass API request failed: " + e.getMessage());
                postResult(new ArrayList<>());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<SpeedCamera> cameras = new ArrayList<>();
                if (!response.isSuccessful()) {
                    Log.e("SpeedCameraFetcher", "Overpass API response not successful: " + response.code());
                    postResult(cameras);
                    return;
                }
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    JSONArray elements = json.getJSONArray("elements");
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject element = elements.getJSONObject(i);
                        double clat = element.getDouble("lat");
                        double clon = element.getDouble("lon");
                        cameras.add(new SpeedCamera(clat, clon));
                    }
                    Log.d("SpeedCameraFetcher", "Found " + cameras.size() + " speed cameras.");
                    postResult(cameras);
                } catch (Exception e) {
                    Log.e("SpeedCameraFetcher", "Error parsing Overpass response: " + e.getMessage());
                    postResult(cameras);
                }
            }

            private void postResult(List<SpeedCamera> cameras) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onSpeedCamerasResult(cameras));
            }
        });
    }
} 