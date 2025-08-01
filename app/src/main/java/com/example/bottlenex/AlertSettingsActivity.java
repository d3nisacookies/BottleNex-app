package com.example.bottlenex;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * AlertSettingsActivity allows users to configure their alert preferences
 * for Speed Limit, Road Incident, and Speed Camera alerts.
 */
public class AlertSettingsActivity extends AppCompatActivity {

    private Switch switchSpeedLimit;
    private Switch switchRoadIncident;
    private Switch switchSpeedCamera;
    private AlertPreferenceHelper alertPreferenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_settings_alert);

        // Initialize AlertPreferenceHelper
        alertPreferenceHelper = new AlertPreferenceHelper(this);

        // Initialize switches
        switchSpeedLimit = findViewById(R.id.switchSpeedLimit);
        switchRoadIncident = findViewById(R.id.switchRoadIncident);
        switchSpeedCamera = findViewById(R.id.switchSpeedCamera);

        // Load current settings
        loadCurrentSettings();

        // Set up switch listeners
        setupSwitchListeners();
    }

    private void loadCurrentSettings() {
        // Load and set current alert preferences
        switchSpeedLimit.setChecked(alertPreferenceHelper.isSpeedLimitAlertEnabled());
        switchRoadIncident.setChecked(alertPreferenceHelper.isRoadIncidentAlertEnabled());
        switchSpeedCamera.setChecked(alertPreferenceHelper.isSpeedCameraAlertEnabled());
    }

    private void setupSwitchListeners() {
        // Speed Limit Alert Switch
        switchSpeedLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alertPreferenceHelper.setSpeedLimitAlertEnabled(isChecked);
            showToast("Speed Limit Alert " + (isChecked ? "enabled" : "disabled"));
        });

        // Road Incident Alert Switch
        switchRoadIncident.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alertPreferenceHelper.setRoadIncidentAlertEnabled(isChecked);
            showToast("Road Incident Alert " + (isChecked ? "enabled" : "disabled"));
        });

        // Speed Camera Alert Switch
        switchSpeedCamera.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alertPreferenceHelper.setSpeedCameraAlertEnabled(isChecked);
            showToast("Speed Camera Alert " + (isChecked ? "enabled" : "disabled"));
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
} 