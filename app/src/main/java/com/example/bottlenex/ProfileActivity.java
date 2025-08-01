package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bottlenex.EditProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.widget.Toast;
//import com.example.bottlenex.PaymentActivity;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
        Button btnPayment = findViewById(R.id.btnPayment);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Load user data from Firebase
        loadUserData(tvName, tvBod, tvPhone, tvPostcode);

        // Set click listeners
        btnEdit.setOnClickListener(v -> {
            // Start EditProfileActivity
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnPayment.setOnClickListener(v -> {
            // Show payment details
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
}