package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Bookmark extends AppCompatActivity {

    private static final int REQUEST_CODE_FAVOURITES = 100;
    private static final int REQUEST_CODE_TOGO = 101;  // Request code for ToGoPlacesActivity
    private static final int REQUEST_CODE_STARRED = 102; // Request code for StarredPlacesActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        Button btnStarred = findViewById(R.id.btnStarredPlaces);
        Button btnToGo = findViewById(R.id.btnToGoPlaces);
        Button btnFavorites = findViewById(R.id.btnFavourites);

        btnStarred.setOnClickListener(v -> {
            Intent intent = new Intent(Bookmark.this, StarredPlacesActivity.class);
            startActivityForResult(intent, REQUEST_CODE_STARRED);
        });

        btnToGo.setOnClickListener(v -> {
            Intent intent = new Intent(Bookmark.this, ToGoPlacesActivity.class);
            startActivityForResult(intent, REQUEST_CODE_TOGO);  // Start for result to get selected place name
        });

        btnFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(Bookmark.this, FavouritesActivity.class);
            startActivityForResult(intent, REQUEST_CODE_FAVOURITES);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Intent resultIntent = new Intent();

            if (requestCode == REQUEST_CODE_STARRED) {
                // Pass all extras from StarredPlacesActivity back to MainActivity
                resultIntent.putExtras(data.getExtras());
                setResult(RESULT_OK, resultIntent);
                finish();

            } else if (requestCode == REQUEST_CODE_FAVOURITES) {
                // Pass all extras from FavouritesActivity back to MainActivity
                resultIntent.putExtras(data.getExtras());
                setResult(RESULT_OK, resultIntent);
                finish();

            } else if (requestCode == REQUEST_CODE_TOGO) {
                String selectedPlaceName = data.getStringExtra("selected_place_name");
                if (selectedPlaceName != null) {
                    resultIntent.putExtra("selected_location", selectedPlaceName);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        }
    }
}
