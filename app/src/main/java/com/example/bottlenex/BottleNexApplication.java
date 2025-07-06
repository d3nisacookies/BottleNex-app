package com.example.bottlenex;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class BottleNexApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
} 