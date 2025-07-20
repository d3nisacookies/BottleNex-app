package com.example.bottlenex;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class EditProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize views
        TextView tvEmail = findViewById(R.id.tvEmail);
        EditText etName = findViewById(R.id.etName);
        EditText etDob = findViewById(R.id.etDob);
        EditText etPhone = findViewById(R.id.etPhone);
        EditText etPostcode = findViewById(R.id.etPostcode);
        Button btnSave = findViewById(R.id.btnSave);

        // Set up Date Picker for Date of Birth field
        etDob.setOnClickListener(v -> showDatePickerDialog(etDob));

        // TODO: Load current user data from your data source
        String userEmail = "user@example.com"; // Replace with actual email
        tvEmail.setText(userEmail);
        etName.setText("John Doe");
        etDob.setText("01/01/1990");
        etPhone.setText("+1234567890");
        etPostcode.setText("12345");

        btnSave.setOnClickListener(v -> {
            // Get edited values
            String name = etName.getText().toString();
            String dob = etDob.getText().toString();
            String phone = etPhone.getText().toString();
            String postcode = etPostcode.getText().toString();

            // TODO: Save the updated profile
            finish(); // Return to ProfileActivity
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
                    String formattedDate = String.format("%02d/%02d/%d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    etDob.setText(formattedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }
}