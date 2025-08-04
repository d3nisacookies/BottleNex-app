package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bottlenex.EditProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//import com.example.bottlenex.PaymentActivity;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvPlanName1, tvPlanPrice1, tvBillingPeriod1;
    private TextView tvPlanName2, tvPlanPrice2, tvBillingPeriod2;
    private TextView tvPlanName3, tvPlanPrice3, tvBillingPeriod3;
    private Button btnSubscribe1, btnSubscribe2, btnSubscribe3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        


        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        TextView tvName = findViewById(R.id.tvName);
        TextView tvBod = findViewById(R.id.tvBod);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvPostcode = findViewById(R.id.tvPostcode);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnPaymentDetails = findViewById(R.id.btnPaymentDetails);
        TextView btnLogout = findViewById(R.id.btnLogout);
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvPlanName1 = findViewById(R.id.tvPlanName1);
        tvPlanPrice1 = findViewById(R.id.tvPlanPrice1);
        tvBillingPeriod1 = findViewById(R.id.tvBillingPeriod1);
        btnSubscribe1 = findViewById(R.id.btnSubscribe1);
        tvPlanName2 = findViewById(R.id.tvPlanName2);
        tvPlanPrice2 = findViewById(R.id.tvPlanPrice2);
        tvBillingPeriod2 = findViewById(R.id.tvBillingPeriod2);
        btnSubscribe2 = findViewById(R.id.btnSubscribe2);
        tvPlanName3 = findViewById(R.id.tvPlanName3);
        tvPlanPrice3 = findViewById(R.id.tvPlanPrice3);
        tvBillingPeriod3 = findViewById(R.id.tvBillingPeriod3);
        btnSubscribe3 = findViewById(R.id.btnSubscribe3);

        // Load user data from Firebase
        loadUserData(tvName, tvBod, tvPhone, tvPostcode);

        // Setup subscription plans
        setupSubscriptionPlans();

        // Check active subscription status
        checkActiveSubscription();

        // Set click listeners
        btnEdit.setOnClickListener(v -> {
            // Start EditProfileActivity
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnPaymentDetails.setOnClickListener(v -> {
            // Start PaymentActivity to view payment history
            Intent intent = new Intent(ProfileActivity.this, PaymentActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            // Handle logout logic
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            // Navigate back to LoginActivity
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Set up custom back button
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }

    private void setupSubscriptionPlans() {
        // Set up Monthly plan (matching landing page exactly)
        tvPlanName1.setText("Monthly");
        tvPlanPrice1.setText("$5");
        tvBillingPeriod1.setText("per month");
        
        // Set up Annual plan (matching landing page exactly)
        tvPlanName2.setText("Annual");
        tvPlanPrice2.setText("$48");
        tvBillingPeriod2.setText("per year");
        
        // Set up Semi-annual plan (matching landing page exactly)
        tvPlanName3.setText("Semi-annual");
        tvPlanPrice3.setText("$27");
        tvBillingPeriod3.setText("per 6 months");
        
        // Set click listeners for subscribe buttons
        btnSubscribe1.setOnClickListener(v -> {
            // Navigate to AddCardActivity with Monthly plan details
            Intent intent = new Intent(ProfileActivity.this, AddCardActivity.class);
            intent.putExtra("plan_id", "monthly");
            intent.putExtra("plan_name", "Monthly");
            intent.putExtra("plan_price", "$5");
            intent.putExtra("billing_period", "per month");
            startActivity(intent);
        });
        

        
        btnSubscribe2.setOnClickListener(v -> {
            // Navigate to AddCardActivity with Annual plan details
            Intent intent = new Intent(ProfileActivity.this, AddCardActivity.class);
            intent.putExtra("plan_id", "annual");
            intent.putExtra("plan_name", "Annual");
            intent.putExtra("plan_price", "$48");
            intent.putExtra("billing_period", "per year");
            startActivity(intent);
        });
        
        btnSubscribe3.setOnClickListener(v -> {
            // Navigate to AddCardActivity with Semi-annual plan details
            Intent intent = new Intent(ProfileActivity.this, AddCardActivity.class);
            intent.putExtra("plan_id", "semi_annual");
            intent.putExtra("plan_name", "Semi-annual");
            intent.putExtra("plan_price", "$27");
            intent.putExtra("billing_period", "per 6 months");
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning from EditProfileActivity
        TextView tvName = findViewById(R.id.tvName);
        TextView tvBod = findViewById(R.id.tvBod);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvPostcode = findViewById(R.id.tvPostcode);
        loadUserData(tvName, tvBod, tvPhone, tvPostcode);
        
        // Check and update subscription status
        checkActiveSubscription();
    }

    private void loadUserData(TextView tvName, TextView tvBod, TextView tvPhone, TextView tvPostcode) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get user data from Firestore
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String dateOfBirth = documentSnapshot.getString("dateOfBirth");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String postalCode = documentSnapshot.getString("postalCode");
                        String email = documentSnapshot.getString("email");

                        // Display the data
                        if (firstName != null && lastName != null) {
                            tvName.setText("Email: " + (email != null ? email : "N/A"));
                        } else {
                            tvName.setText("Email: " + (email != null ? email : "N/A"));
                        }
                        
                        tvBod.setText("Date of Birth: " + (dateOfBirth != null ? dateOfBirth : "N/A"));
                        tvPhone.setText("Phone: " + (phoneNumber != null ? phoneNumber : "N/A"));
                        tvPostcode.setText("Postcode: " + (postalCode != null ? postalCode : "N/A"));
                    } else {
                        // Document doesn't exist, show default values
                        tvName.setText("Email: N/A");
                        tvBod.setText("Date of Birth: N/A");
                        tvPhone.setText("Phone: N/A");
                        tvPostcode.setText("Postcode: N/A");
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    tvName.setText("Email: Error loading data");
                    tvBod.setText("Date of Birth: Error loading data");
                    tvPhone.setText("Phone: Error loading data");
                    tvPostcode.setText("Postcode: Error loading data");
                    Toast.makeText(this, "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            // No user is signed in
            tvName.setText("Email: No user signed in");
            tvBod.setText("Date of Birth: No user signed in");
            tvPhone.setText("Phone: No user signed in");
            tvPostcode.setText("Postcode: No user signed in");
        }
    }
    
    private void checkActiveSubscription() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();
        
        // Always read fresh data from Firebase (no caching)
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String currentPlan = documentSnapshot.getString("currentPlan");
                    
                    if (currentPlan != null && !currentPlan.isEmpty()) {
                        // Update UI based on active plan
                        updateSubscriptionButtons(currentPlan);
                    } else {
                        // No currentPlan field, show all subscribe buttons
                        updateSubscriptionButtons(null);
                    }
                } else {
                    // User document doesn't exist, show all subscribe buttons
                    updateSubscriptionButtons(null);
                }
            })
            .addOnFailureListener(e -> {
                // On error, show all subscribe buttons
                updateSubscriptionButtons(null);
            });
    }
    
    private void updateSubscriptionButtons(String activePlanName) {
        // Reset all buttons to subscribe state with consistent green color
        btnSubscribe1.setText("SUBSCRIBE");
        btnSubscribe1.setEnabled(true);
        btnSubscribe1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))); // Consistent green
        
        btnSubscribe2.setText("SUBSCRIBE");
        btnSubscribe2.setEnabled(true);
        btnSubscribe2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))); // Consistent green
        
        btnSubscribe3.setText("SUBSCRIBE");
        btnSubscribe3.setEnabled(true);
        btnSubscribe3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))); // Consistent green
        
                // If there's an active plan, update the corresponding button
        if (activePlanName != null) {
            String planNameLower = activePlanName.toLowerCase().trim();
            
            switch (planNameLower) {
                case "monthly":
                case "month":
                    btnSubscribe1.setText("SUBSCRIBED!");
                    btnSubscribe1.setEnabled(false);
                    btnSubscribe1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3"))); // Blue
                    break;
                case "annual":
                case "year":
                case "yearly":
                    btnSubscribe2.setText("SUBSCRIBED!");
                    btnSubscribe2.setEnabled(false);
                    btnSubscribe2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3"))); // Blue
                    break;
                case "semi-annual":
                case "semi_annual":
                case "semi annual":
                case "6 months":
                case "6months":
                    btnSubscribe3.setText("SUBSCRIBED!");
                    btnSubscribe3.setEnabled(false);
                    btnSubscribe3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3"))); // Blue
                    break;
                default:
                    Toast.makeText(this, "Unknown plan type: " + activePlanName, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
     }
     
           
    

}