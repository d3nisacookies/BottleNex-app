package com.example.bottlenex;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OSMMaxSpeedFetcher {
    public interface MaxSpeedCallback {
        void onMaxSpeedResult(Integer maxSpeedKmh);
    }

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final OkHttpClient client = new OkHttpClient();

    public static void fetchMaxSpeed(double lat, double lon, MaxSpeedCallback callback) {
        String query = "[out:json];way(around:20," + lat + "," + lon + ")[highway][maxspeed];out tags center 1;";
        String url = OVERPASS_URL + "?data=" + java.net.URLEncoder.encode(query);

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OSMMaxSpeedFetcher", "Overpass API request failed: " + e.getMessage());
                postResult(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("OSMMaxSpeedFetcher", "Overpass API response not successful: " + response.code());
                    postResult(null);
                    return;
                }
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    JSONArray elements = json.getJSONArray("elements");
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject element = elements.getJSONObject(i);
                        JSONObject tags = element.optJSONObject("tags");
                        if (tags != null && tags.has("maxspeed")) {
                            String maxspeedStr = tags.getString("maxspeed");
                            Integer maxspeed = parseMaxSpeed(maxspeedStr);
                            Log.d("OSMMaxSpeedFetcher", "Found maxspeed: " + maxspeed);
                            postResult(maxspeed);
                            return;
                        }
                    }
                    Log.d("OSMMaxSpeedFetcher", "No maxspeed found in Overpass response.");
                    postResult(null);
                } catch (Exception e) {
                    Log.e("OSMMaxSpeedFetcher", "Error parsing Overpass response: " + e.getMessage());
                    postResult(null);
                }
            }

            private void postResult(Integer maxSpeedKmh) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onMaxSpeedResult(maxSpeedKmh));
            }
        });
    }

    private static Integer parseMaxSpeed(String maxspeedStr) {
        try {
            // Handle values like "50", "50 km/h", "30 mph"
            if (maxspeedStr.contains("mph")) {
                String num = maxspeedStr.replaceAll("[^0-9]", "");
                return (int) Math.round(Integer.parseInt(num) * 1.60934); // convert mph to km/h
            } else {
                String num = maxspeedStr.replaceAll("[^0-9]", "");
                return Integer.parseInt(num);
            }
        } catch (Exception e) {
            Log.e("OSMMaxSpeedFetcher", "Failed to parse maxspeed: " + maxspeedStr);
            return null;
        }
    }
} 