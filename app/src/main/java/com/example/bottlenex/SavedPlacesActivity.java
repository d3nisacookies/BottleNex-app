package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SavedPlacesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_places);

        Button btnStarred = findViewById(R.id.btnStarredPlaces);
        Button btnSavedTrips = findViewById(R.id.btnSavedTrips);
        Button btnToGo = findViewById(R.id.btnToGoPlaces);
        Button btnFavorites = findViewById(R.id.btnFavourites);

        btnStarred.setOnClickListener(v -> {
            Intent intent = new Intent(SavedPlacesActivity.this, StarredPlacesActivity.class);
            startActivity(intent);
        });

        btnSavedTrips.setOnClickListener(v -> {
            Intent intent = new Intent(SavedPlacesActivity.this, SavedTripsActivity.class);
            startActivity(intent);
        });

        btnToGo.setOnClickListener(v -> {
            Intent intent = new Intent(SavedPlacesActivity.this, ToGoPlacesActivity.class);
            startActivity(intent);
        });

        btnFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(SavedPlacesActivity.this, FavouritesActivity.class);
            startActivity(intent);
        });
    }
}