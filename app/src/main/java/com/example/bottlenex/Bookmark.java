package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Bookmark extends AppCompatActivity {

    private static final int REQUEST_CODE_FAVOURITES = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        Button btnStarred = findViewById(R.id.btnStarredPlaces);
        Button btnSavedTrips = findViewById(R.id.btnSavedTrips);
        Button btnToGo = findViewById(R.id.btnToGoPlaces);
        Button btnFavorites = findViewById(R.id.btnFavourites);

        btnStarred.setOnClickListener(v -> {
            Intent intent = new Intent(Bookmark.this, StarredPlacesActivity.class);
            startActivity(intent);
        });

        btnSavedTrips.setOnClickListener(v -> {
            Intent intent = new Intent(Bookmark.this, SavedTripsActivity.class);
            startActivity(intent);
        });

        btnToGo.setOnClickListener(v -> {
            Intent intent = new Intent(Bookmark.this, ToGoPlacesActivity.class);
            startActivity(intent);
        });

        btnFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(Bookmark.this, FavouritesActivity.class);
            // Launch FavouritesActivity expecting a result
            startActivityForResult(intent, REQUEST_CODE_FAVOURITES);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_FAVOURITES && resultCode == RESULT_OK && data != null) {
            String selectedLocation = data.getStringExtra("selected_location");
            if (selectedLocation != null) {
                // Pass the selected location back to MainActivity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_location", selectedLocation);
                setResult(RESULT_OK, resultIntent);
                finish(); // Close Bookmark activity and return to MainActivity
            }
        }
    }
}
