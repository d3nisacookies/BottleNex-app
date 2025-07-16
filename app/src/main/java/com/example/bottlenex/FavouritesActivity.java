package com.example.bottlenex;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FavouritesActivity extends AppCompatActivity {

    private ListView listViewFavourites;
    private ArrayList<String> favouritesList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        listViewFavourites = findViewById(R.id.listViewFavourites);

        loadFavourites();

        if (favouritesList.isEmpty()) {
            Toast.makeText(this, "No favourites saved yet.", Toast.LENGTH_SHORT).show();
        }

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, favouritesList);
        listViewFavourites.setAdapter(adapter);

        // When user taps a favorite location, return it
        listViewFavourites.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLocation = favouritesList.get(position);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_location", selectedLocation);

            setResult(Activity.RESULT_OK, resultIntent);
            finish();  // Close activity and return to caller
        });

        // Long press to remove favorite
        listViewFavourites.setOnItemLongClickListener((parent, view, position, id) -> {
            String itemToRemove = favouritesList.get(position);

            // Remove from list
            favouritesList.remove(position);

            // Update SharedPreferences
            SharedPreferences prefs = getSharedPreferences("favourites", MODE_PRIVATE);
            Set<String> newSet = new HashSet<>(favouritesList);
            prefs.edit().putStringSet("favourites_list", newSet).apply();

            // Notify adapter about data change
            adapter.notifyDataSetChanged();

            Toast.makeText(this, "Removed from favourites: " + itemToRemove, Toast.LENGTH_SHORT).show();
            return true; // consume the event (no further click)
        });
    }

    private void loadFavourites() {
        SharedPreferences prefs = getSharedPreferences("favourites", MODE_PRIVATE);
        Set<String> favouritesSet = prefs.getStringSet("favourites_list", new HashSet<>());
        favouritesList = new ArrayList<>(favouritesSet);
    }
}
