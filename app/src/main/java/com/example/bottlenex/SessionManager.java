package com.example.bottlenex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Utility class for managing user sessions and authentication state
 */
public class SessionManager {
    
    private static final String PREF_NAME = "BottleNexSession";
    private static final String KEY_LAST_ACTIVITY = "last_activity";
    private static final String KEY_AUTO_LOGIN = "auto_login_enabled";
    
    // Session timeout: 30 days (in milliseconds)
    private static final long SESSION_TIMEOUT = 30L * 24 * 60 * 60 * 1000; // 30 days
    
    private SharedPreferences preferences;
    private Context context;
    
    public SessionManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if user session is valid and user should remain logged in
     */
    public boolean isValidSession() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            return false;
        }
        
        // Check if auto-login is enabled (default: true)
        boolean autoLoginEnabled = preferences.getBoolean(KEY_AUTO_LOGIN, true);
        if (!autoLoginEnabled) {
            return false;
        }
        
        // Check session timeout
        long lastActivity = preferences.getLong(KEY_LAST_ACTIVITY, 0);
        long currentTime = System.currentTimeMillis();
        
        if (lastActivity > 0 && (currentTime - lastActivity) > SESSION_TIMEOUT) {
            // Session has expired
            logoutUser();
            return false;
        }
        
        // Update last activity time
        updateLastActivity();
        return true;
    }
    
    /**
     * Update the last activity timestamp
     */
    public void updateLastActivity() {
        preferences.edit()
                .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
                .apply();
    }
    
    /**
     * Enable or disable auto-login feature
     */
    public void setAutoLoginEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_AUTO_LOGIN, enabled)
                .apply();
    }
    
    /**
     * Check if auto-login is enabled
     */
    public boolean isAutoLoginEnabled() {
        return preferences.getBoolean(KEY_AUTO_LOGIN, true);
    }
    
    /**
     * Logout user and clear session data
     */
    public void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        clearSessionData();
    }
    
    /**
     * Clear all session-related data
     */
    public void clearSessionData() {
        preferences.edit().clear().apply();
    }
    
    /**
     * Redirect to login activity
     */
    public void redirectToLogin(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
    
    /**
     * Handle successful login
     */
    public void onLoginSuccess() {
        updateLastActivity();
        setAutoLoginEnabled(true);
    }
    
    /**
     * Get current user info
     */
    public FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }
    
    /**
     * Check if user is logged in (simple check without session validation)
     */
    public boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
}
