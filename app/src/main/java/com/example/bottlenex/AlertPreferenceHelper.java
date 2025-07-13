package com.example.bottlenex;

import android.content.Context;
import android.content.SharedPreferences;

public class AlertPreferenceHelper {
    private static final String PREF_NAME = "alert_preferences";
    private static final String KEY_SPEED_LIMIT = "speed_limit_alert";
    private static final String KEY_ROAD_INCIDENT = "road_incident_alert";
    private static final String KEY_SPEED_CAMERA = "speed_camera_alert";

    private SharedPreferences sharedPreferences;

    public AlertPreferenceHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setSpeedLimitAlertEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_SPEED_LIMIT, enabled).apply();
    }

    public boolean isSpeedLimitAlertEnabled() {
        return sharedPreferences.getBoolean(KEY_SPEED_LIMIT, true);
    }

    public void setRoadIncidentAlertEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_ROAD_INCIDENT, enabled).apply();
    }

    public boolean isRoadIncidentAlertEnabled() {
        return sharedPreferences.getBoolean(KEY_ROAD_INCIDENT, true);
    }

    public void setSpeedCameraAlertEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_SPEED_CAMERA, enabled).apply();
    }

    public boolean isSpeedCameraAlertEnabled() {
        return sharedPreferences.getBoolean(KEY_SPEED_CAMERA, true);
    }
} 