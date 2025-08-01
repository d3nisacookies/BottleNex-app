package com.example.bottlenex.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
// Firebase Storage imports removed - using Base64 for now

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirebaseService {

    private static final String TAG = "FirebaseService";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    // FirebaseStorage removed - using Base64 for now

    @Inject
    public FirebaseService(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
        // FirebaseStorage removed - using Base64 for now
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
    public void saveBugReport(String title, String description, OnSuccessListener<Void> successListener, OnFailureListener failureListener, String screenshotData) {
        Map<String, Object> bugReport = new HashMap<>();
        bugReport.put("title", title);
        bugReport.put("description", description);
        bugReport.put("timestamp", System.currentTimeMillis());
        
        if (screenshotData != null) {
            // Check if it's a Base64 image or a URL
            if (screenshotData.startsWith("data:image") || screenshotData.length() > 100) {
                // It's a Base64 image
                bugReport.put("screenshot_base64", screenshotData);
                bugReport.put("has_screenshot", true);
            } else {
                // It's a URL (for future Firebase Storage use)
                bugReport.put("screenshot_url", screenshotData);
                bugReport.put("has_screenshot", true);
            }
        } else {
            bugReport.put("has_screenshot", false);
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

    // Convert image to Base64 for Firestore storage
    public void convertImageToBase64(Context context, Uri imageUri, OnSuccessListener<String> successListener, OnFailureListener failureListener) {
        Log.d(TAG, "Starting image conversion to Base64. URI: " + imageUri.toString());
        
        try {
            // Read the image from URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for image");
                failureListener.onFailure(new Exception("Failed to read image"));
                return;
            }

            // Decode the image
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from image");
                failureListener.onFailure(new Exception("Failed to decode image"));
                return;
            }

            // Compress and convert to Base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Compress with 70% quality to reduce size
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            
            // Convert to Base64
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            
            Log.d(TAG, "Image converted to Base64 successfully. Size: " + base64Image.length() + " characters");
            
            // Check if image is too large (Firestore has 1MB document limit)
            if (base64Image.length() > 800000) { // Leave some buffer
                Log.w(TAG, "Image is too large for Firestore. Size: " + base64Image.length() + " characters");
                Toast.makeText(context, "Image too large. Please select a smaller image.", Toast.LENGTH_LONG).show();
                failureListener.onFailure(new Exception("Image too large for storage"));
                return;
            }
            
            successListener.onSuccess(base64Image);
            
        } catch (Exception e) {
            Log.e(TAG, "Exception in convertImageToBase64", e);
            failureListener.onFailure(e);
        }
    }

    // Firebase Storage: Upload screenshot (kept for future use when Storage is available)
    public void uploadScreenshot(Context context, Uri imageUri, OnSuccessListener<String> successListener, OnFailureListener failureListener) {
        Log.d(TAG, "Firebase Storage not available. Using Base64 conversion instead.");
        convertImageToBase64(context, imageUri, successListener, failureListener);
    }

    // Note: Screenshot deletion not available with Base64 storage
    // When Firebase Storage is available, this method can be restored
}
