package com.example.bottlenex;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private EditText feedbackInput;
    private Button submitButton;
    private FirebaseFirestore db;

    private static final String TAG = "FeedbackActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Check Firebase initialization
        if (FirebaseApp.getApps(this).isEmpty()) {
            Toast.makeText(this, "Firebase NOT initialized!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Firebase is not initialized.");
            return;
        } else {
            Log.d(TAG, "Firebase initialized successfully.");
        }

        // Optional: Enable Firestore debug logging
        FirebaseFirestore.setLoggingEnabled(true);

        feedbackInput = findViewById(R.id.feedbackInput);
        submitButton = findViewById(R.id.submitButton);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Setup toolbar with back button
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String feedbackText = feedbackInput.getText().toString().trim();

                if (feedbackText.isEmpty()) {
                    Toast.makeText(FeedbackActivity.this, "Please enter your feedback", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Prepare feedback data
                Map<String, Object> feedback = new HashMap<>();
                feedback.put("text", feedbackText);
                feedback.put("timestamp", System.currentTimeMillis());

                // Submit to Firestore
                db.collection("feedback")
                        .add(feedback)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(FeedbackActivity.this, "Feedback submitted!", Toast.LENGTH_SHORT).show();
                            feedbackInput.setText(""); // Clear input
                            Log.d(TAG, "Feedback submitted with ID: " + documentReference.getId());
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(FeedbackActivity.this, "Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error submitting feedback", e);
                        });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
