package com.example.bottlenex;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class LocationDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_details);

        // Set up CIF keyboard
        setupKeyboard();

        // Get data from intent
        String locationName = getIntent().getStringExtra("location_name");
        if (locationName == null) locationName = "CLEMENTI";
        ((TextView) findViewById(R.id.tvLocationTitle)).setText(locationName);
    }

    private void setupKeyboard() {
        String[][] rows = {
                {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
                {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
                {"⭕", "z", "x", "c", "v", "b", "n", "m", "💶"}
        };
        LinearLayout keyboardContainer = findViewById(R.id.keyboardContainer);
        for (String[] rowKeys : rows) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            rowLayout.setGravity(Gravity.CENTER);
            for (final String key : rowKeys) {
                Button button = new Button(this);
                button.setText(key);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                int margin = dpToPx(4);
                params.setMargins(margin, margin, margin, margin);
                button.setLayoutParams(params);
                button.setBackground(ContextCompat.getDrawable(this, R.drawable.keyboard_key_bg));
                button.setOnClickListener(v -> onKeyPressed(key));
                rowLayout.addView(button);
            }
            keyboardContainer.addView(rowLayout);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void onKeyPressed(String key) {
        Toast.makeText(this, "Pressed: " + key, Toast.LENGTH_SHORT).show();
    }
} 