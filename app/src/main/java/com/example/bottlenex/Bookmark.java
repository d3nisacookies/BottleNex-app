package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Bookmark extends AppCompatActivity {

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
            startActivity(intent);
        });
    }
}