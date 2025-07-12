package com.example.bottlenex.services;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirebaseService {

    private static final String TAG = "FirebaseService";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    @Inject
    public FirebaseService(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
        this.storage = FirebaseStorage.getInstance();
    }

    // Authentication methods
    public void signInAnonymously(OnCompleteListener<AuthResult> listener) {
        auth.signInAnonymously()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Anonymous sign-in failed", e));
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signOut() {
        auth.signOut();
    }

    // Firestore: Save Bug Report
    public void saveBugReport(String title, String description, OnSuccessListener<Void> successListener, OnFailureListener failureListener, String screenshotUrl) {
        Map<String, Object> bugReport = new HashMap<>();
        bugReport.put("title", title);
        bugReport.put("description", description);
        if (screenshotUrl != null) {
            bugReport.put("screenshots", screenshotUrl);
        }

        firestore.collection("bugs")
                .add(bugReport)
                .addOnSuccessListener(documentReference -> {
                    // Add bug_ID field (document ID)
                    documentReference.update("bug_ID", documentReference.getId())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Bug report saved successfully with ID: " + documentReference.getId());
                                successListener.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update bug_ID field", e);
                                failureListener.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save bug report", e);
                    failureListener.onFailure(e);
                });
    }

    // Firestore: Get bug reports by userId (optional, useful if you track user reports)
    public void getUserBugReports(String userId, OnSuccessListener<com.google.firebase.firestore.QuerySnapshot> successListener, OnFailureListener failureListener) {
        firestore.collection("bug_reports")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting bug reports", e);
                    failureListener.onFailure(e);
                });
    }

    // Firestore: Save user location
    public void saveUserLocation(String userId, double latitude, double longitude, OnSuccessListener<Void> successListener) {
        Map<String, Object> location = new HashMap<>();
        location.put("userId", userId);
        location.put("latitude", latitude);
        location.put("longitude", longitude);
        location.put("timestamp", System.currentTimeMillis());

        firestore.collection("user_locations")
                .add(location)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Location saved with ID: " + documentReference.getId());
                    successListener.onSuccess(null);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving location", e));
    }

    // Firebase Storage: Upload screenshot
    public void uploadScreenshot(Uri imageUri, OnSuccessListener<String> successListener, OnFailureListener failureListener) {
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child("screenshots/" + System.currentTimeMillis() + ".jpg");

        UploadTask uploadTask = imageRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Log.d(TAG, "Screenshot uploaded successfully: " + uri.toString());
                        successListener.onSuccess(uri.toString());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get screenshot download URL", e);
                        failureListener.onFailure(e);
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload screenshot", e);
            failureListener.onFailure(e);
        });
    }

    // Firebase Storage: Delete screenshot (optional)
    public void deleteScreenshot(String imageUrl, OnSuccessListener<Void> successListener) {
        StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
        imageRef.delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting screenshot", e));
    }
}
