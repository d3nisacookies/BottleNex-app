package com.example.bottlenex;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bottlenex.services.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class BugReportActivity extends AppCompatActivity {
    
    @Inject
    FirebaseService firebaseService;
    
    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private Uri selectedImageUri;
    
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Toast.makeText(this, "Screenshot selected", Toast.LENGTH_SHORT).show();
                }
            }
    );
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bug_report);

        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);

        MaterialButton btnSubmit = findViewById(R.id.btnSubmitBug);
        btnSubmit.setOnClickListener(v -> submitBugReport());

        MaterialButton btnUpload = findViewById(R.id.btnUploadScreenshot);
        btnUpload.setOnClickListener(v -> selectScreenshot());

        // Optional: Handle toolbar back button
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }
    
    private void submitBugReport() {
        String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
        String description = descriptionInput.getText() != null ? descriptionInput.getText().toString().trim() : "";
        
        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in the title and description", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedImageUri != null) {
            firebaseService.uploadScreenshot(selectedImageUri, screenshotUrl -> {
                firebaseService.saveBugReport(title, description, aVoid -> {
                    Toast.makeText(this, "Bug report submitted with screenshot!", Toast.LENGTH_SHORT).show();
                    finish();
                }, e -> {
                    Toast.makeText(this, "Failed to submit bug report", Toast.LENGTH_SHORT).show();
                }, screenshotUrl);
            }, e -> {
                Toast.makeText(this, "Failed to upload screenshot", Toast.LENGTH_SHORT).show();
            });
        } else {
            firebaseService.saveBugReport(title, description, aVoid -> {
                Toast.makeText(this, "Bug report submitted!", Toast.LENGTH_SHORT).show();
                finish();
            }, e -> {
                Toast.makeText(this, "Failed to submit bug report", Toast.LENGTH_SHORT).show();
            }, null);
        }
    }
    
    private void selectScreenshot() {
        imagePickerLauncher.launch("image/*");
    }
} 