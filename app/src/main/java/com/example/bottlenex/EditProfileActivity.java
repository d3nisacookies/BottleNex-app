package com.example.bottlenex;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);



        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        TextView tvEmail = findViewById(R.id.tvEmail);
        EditText etName = findViewById(R.id.etName);
        EditText etDob = findViewById(R.id.etDob);
        EditText etPhone = findViewById(R.id.etPhone);
        EditText etPostcode = findViewById(R.id.etPostcode);
        Button btnSave = findViewById(R.id.btnSave);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Set up Date Picker for Date of Birth field
        etDob.setOnClickListener(v -> showDatePickerDialog(etDob));

        // Load current user data from Firebase
        loadUserData(tvEmail, etName, etDob, etPhone, etPostcode);

        btnSave.setOnClickListener(v -> {
            // Get edited values
            String name = etName.getText().toString();
            String dob = etDob.getText().toString();
            String phone = etPhone.getText().toString();
            String postcode = etPostcode.getText().toString();

            // Save the updated profile to Firebase
            saveUserData(name, dob, phone, postcode);
        });

        // Set up custom back button
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }



    private void loadUserData(TextView tvEmail, EditText etName, EditText etDob, EditText etPhone, EditText etPostcode) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            
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

                        // Display the data in the form fields
                        tvEmail.setText(email != null ? email : "N/A");
                        
                        // Combine firstName and lastName for the name field
                        String fullName = "";
                        if (firstName != null && lastName != null) {
                            fullName = firstName + " " + lastName;
                        } else if (firstName != null) {
                            fullName = firstName;
                        } else if (lastName != null) {
                            fullName = lastName;
                        }
                        etName.setText(fullName);
                        
                        etDob.setText(dateOfBirth != null ? dateOfBirth : "");
                        etPhone.setText(phoneNumber != null ? phoneNumber : "");
                        etPostcode.setText(postalCode != null ? postalCode : "");
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
        } else {
            Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveUserData(String name, String dob, String phone, String postcode) {
        if (userId == null) {
            Toast.makeText(this, "User ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split the name into firstName and lastName
        String[] nameParts = name.trim().split("\\s+", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Create a map with the updated data
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("dateOfBirth", dob);
        updates.put("phoneNumber", phone);
        updates.put("postalCode", postcode);

        // Save to Firebase
        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Return to ProfileActivity
            })
            .addOnFailureListener(e -> {
                Toast.makeText(EditProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showDatePickerDialog(EditText etDob) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    etDob.setText(formattedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }
}