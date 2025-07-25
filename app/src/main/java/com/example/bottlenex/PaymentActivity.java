package com.example.bottlenex;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast; // This import is used for displaying short messages
import androidx.appcompat.app.AppCompatActivity;

/**
 * PaymentActivity handles the display of payment information and related actions.
 * It allows users to download monthly statements and navigate through the app.
 */
public class PaymentActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Set up download icon click listeners for each month
        int[] downloadIds = {
                R.id.downloadJan, R.id.downloadFeb, R.id.downloadMar,
                R.id.downloadApr, R.id.downloadMay, R.id.downloadJun
        };
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        for (int i = 0; i < downloadIds.length; i++) {
            final String month = months[i];
            ImageView downloadIcon = findViewById(downloadIds[i]);
            if (downloadIcon != null) {
                downloadIcon.setOnClickListener(v ->
                        Toast.makeText(this, "Download for " + month, Toast.LENGTH_SHORT).show()
                );
            }
        }

        // Set up bottom navigation button click listeners
        int[] navIds = {R.id.btnNavigation, R.id.btnBookmark, R.id.btnCar, R.id.btnMenu};
        for (int navId : navIds) {
            ImageButton btn = findViewById(navId);
            if (btn != null) {
                btn.setOnClickListener(v ->
                        Toast.makeText(this, "Navigation action", Toast.LENGTH_SHORT).show()
                );
            }
        }
    }
}