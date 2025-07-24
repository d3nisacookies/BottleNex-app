package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bottlenex.EditProfileActivity;
//import com.example.bottlenex.PaymentActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        TextView tvName = findViewById(R.id.tvName);
        TextView tvBod = findViewById(R.id.tvBod);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvPostcode = findViewById(R.id.tvPostcode);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnPayment = findViewById(R.id.btnPayment);
        Button btnLogout = findViewById(R.id.btnLogout);

        // TODO: Load actual user data from SharedPreferences or database
        // For now, we'll use placeholder data
        tvName.setText("John Doe");
        tvBod.setText("Date of Birth: 01/01/1990");
        tvPhone.setText("Phone: +1234567890");
        tvPostcode.setText("Postcode: 12345");

        // Set click listeners
        btnEdit.setOnClickListener(v -> {
            // Start EditProfileActivity
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnPayment.setOnClickListener(v -> {
            // Show payment details
            //Intent intent = new Intent(ProfileActivity.this, PaymentActivity.class);
            //startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            // Handle logout logic
            // TODO: Implement actual logout (clear session, etc.)
            finish(); // Close this activity
        });
    }
}