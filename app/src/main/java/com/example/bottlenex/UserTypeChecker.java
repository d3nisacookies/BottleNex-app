package com.example.bottlenex;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Utility class for checking user subscription status and enforcing premium feature restrictions
 */
public class UserTypeChecker {
    
    private static final String TAG = "UserTypeChecker";
    
    public interface UserTypeCallback {
        void onResult(boolean isPremium);
        void onError(String error);
    }
    
    /**
     * Check if the current user is a premium user
     */
    public static void checkUserType(Context context, UserTypeCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }
        
        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String userType = documentSnapshot.getString("userType");
                    boolean isPremium = "Premium".equals(userType);
                    callback.onResult(isPremium);
                } else {
                    // User document doesn't exist, assume free user
                    callback.onResult(false);
                }
            })
            .addOnFailureListener(e -> {
                callback.onError("Failed to check user type: " + e.getMessage());
            });
    }
    
    /**
     * Check if user has premium access and show upgrade dialog if not
     * @param context The activity context
     * @param featureName Name of the feature being accessed
     * @param onPremiumAccess Callback to execute if user has premium access
     */
    public static void checkPremiumAccess(Context context, String featureName, Runnable onPremiumAccess) {
        checkUserType(context, new UserTypeCallback() {
            @Override
            public void onResult(boolean isPremium) {
                if (isPremium) {
                    // User is premium, allow access
                    onPremiumAccess.run();
                } else {
                    // User is free, show upgrade dialog
                    showUpgradeDialog(context, featureName);
                }
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(context, "Unable to verify subscription status", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Show upgrade dialog for premium features
     */
    private static void showUpgradeDialog(Context context, String featureName) {
        String message = featureName + " is a premium feature.\n\n" +
                "Premium features include:\n" +
                "• Congestion Prediction Visualization\n" +
                "• Congestion Prediction Analysis\n" +
                "• Historical Traffic Graph Data\n" +
                "• Historical Traffic Heatmaps\n\n" +
                "Upgrade now to unlock all premium navigation features!";
        
        new AlertDialog.Builder(context)
            .setTitle("🚗 Premium Feature")
            .setMessage(message)
            .setPositiveButton("Upgrade to Premium", (dialog, which) -> {
                // Navigate to subscription/payment activity
                Intent intent = new Intent(context, ProfileActivity.class);
                context.startActivity(intent);
            })
            .setNegativeButton("Continue as Free", (dialog, which) -> {
                dialog.dismiss();
            })
            .setIcon(android.R.drawable.ic_dialog_info)
            .setCancelable(true)
            .show();
    }
    
    /**
     * Quick check method that returns true if user should have premium access
     * This is for immediate UI decisions (like hiding buttons)
     * Note: This doesn't make a network call, so it should be used after checkUserType
     */
    public static void checkPremiumAccessQuick(Context context, UserTypeCallback callback) {
        checkUserType(context, callback);
    }
    
    /**
     * Show a simple toast for premium feature restriction
     */
    public static void showPremiumRequiredToast(Context context) {
        Toast.makeText(context, "This feature requires a Premium subscription", Toast.LENGTH_LONG).show();
    }
    
    /**
     * Initialize premium restrictions for an activity
     * This can be called in onCreate to set up UI restrictions based on user type
     */
    public static void initializePremiumRestrictions(Context context, Runnable onInitComplete) {
        checkUserType(context, new UserTypeCallback() {
            @Override
            public void onResult(boolean isPremium) {
                if (onInitComplete != null) {
                    onInitComplete.run();
                }
            }
            
            @Override
            public void onError(String error) {
                // On error, assume free user for safety
                if (onInitComplete != null) {
                    onInitComplete.run();
                }
            }
        });
    }
}
