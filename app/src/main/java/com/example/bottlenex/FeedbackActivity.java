package com.example.bottlenex;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private EditText feedbackInput;
    private EditText categoryInput;
    private Button submitButton;
    private RatingBar ratingBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String reviewerName = "Anonymous";

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

        // Initialize views
        feedbackInput = findViewById(R.id.feedbackInput);
        categoryInput = findViewById(R.id.categoryInput);
        submitButton = findViewById(R.id.submitButton);
        ratingBar = findViewById(R.id.ratingBar);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup toolbar with back button
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Load current user's name
        loadCurrentUserName();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReview();
            }
        });
    }

    private void loadCurrentUserName() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        
                        if (firstName != null && lastName != null) {
                            reviewerName = firstName + " " + lastName;
                        } else if (firstName != null) {
                            reviewerName = firstName;
                        } else if (lastName != null) {
                            reviewerName = lastName;
                        }
                        Log.d(TAG, "Loaded reviewer name: " + reviewerName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user name", e);
                });
        }
    }

    private void submitReview() {
        String feedbackText = feedbackInput.getText().toString().trim();
        String categoryText = categoryInput.getText().toString().trim();
        float rating = ratingBar.getRating();

        if (feedbackText.isEmpty()) {
            Toast.makeText(FeedbackActivity.this, "Please enter your review", Toast.LENGTH_SHORT).show();
            return;
        }

        if (categoryText.isEmpty()) {
            Toast.makeText(FeedbackActivity.this, "Please enter a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rating == 0) {
            Toast.makeText(FeedbackActivity.this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());

        // Prepare review data matching the Firebase structure
        Map<String, Object> review = new HashMap<>();
        review.put("body", feedbackText);
        review.put("category", categoryText);
        review.put("date", currentDate);
        review.put("rating", (int) rating);
        review.put("reviewer", reviewerName);

        // First, get the current items to determine the next index
        db.collection("landingPage").document("reviews")
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get the current items array
                    Object itemsObj = documentSnapshot.get("items");
                    if (itemsObj instanceof java.util.List) {
                        java.util.List<Object> items = (java.util.List<Object>) itemsObj;
                        int nextIndex = items.size();
                        
                        // Create a new list with the existing items plus the new review
                        java.util.List<Object> newItems = new java.util.ArrayList<>(items);
                        newItems.add(review);
                        
                        // Update the items array with the new review added
                        db.collection("landingPage").document("reviews")
                            .update("items", newItems)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(FeedbackActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                                // Clear form
                                feedbackInput.setText("");
                                categoryInput.setText("");
                                ratingBar.setRating(5);
                                Log.d(TAG, "Review added as item " + nextIndex);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(FeedbackActivity.this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Error submitting review", e);
                            });
                    } else {
                        // If items doesn't exist, create it with the first review
                        db.collection("landingPage").document("reviews")
                            .set(java.util.Collections.singletonMap("items", java.util.Arrays.asList(review)))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(FeedbackActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                                // Clear form
                                feedbackInput.setText("");
                                categoryInput.setText("");
                                ratingBar.setRating(5);
                                Log.d(TAG, "Review added as first item");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(FeedbackActivity.this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Error submitting review", e);
                            });
                    }
                } else {
                    // Document doesn't exist, create it with the first review
                    db.collection("landingPage").document("reviews")
                        .set(java.util.Collections.singletonMap("items", java.util.Arrays.asList(review)))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(FeedbackActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                            // Clear form
                            feedbackInput.setText("");
                            categoryInput.setText("");
                            ratingBar.setRating(5);
                            Log.d(TAG, "Review added as first item");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(FeedbackActivity.this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error submitting review", e);
                        });
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(FeedbackActivity.this, "Failed to get current reviews: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error getting current reviews", e);
            });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
