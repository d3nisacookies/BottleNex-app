package com.example.bottlenex;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class BottleNexApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize any app-wide configurations here
    }
} 