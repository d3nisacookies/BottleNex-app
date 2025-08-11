package com.example.bottlenex;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;
import android.widget.Toast;
import android.util.Log;

public class FavouritesActivity extends AppCompatActivity implements FavouritesAdapter.OnFavouritePlaceClickListener {

    private RecyclerView recyclerView;
    private ArrayList<String> favouritesList;
    private FavouritesAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bookmark");
        }

        recyclerView = findViewById(R.id.recyclerViewFavourites);
        favouritesList = new ArrayList<>();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavouritesAdapter(favouritesList, this);
        recyclerView.setAdapter(adapter);

        loadFavourites();

        // Setup Delete All button
        MaterialButton btnDeleteAll = findViewById(R.id.btnDeleteAll);
        btnDeleteAll.setOnClickListener(v -> {
            if (!favouritesList.isEmpty()) {
                showDeleteAllConfirmationDialog();
            } else {
                Toast.makeText(this, "No favourite places to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onFavouritePlaceNavigate(String entry) {
        String[] parts = entry.split("\\|");
        if (parts.length == 3) {
            String name = parts[0];
            String lat = parts[1];
            String lon = parts[2];

            Intent resultIntent = new Intent();
            resultIntent.putExtra("action", "favourite_navigate");
            resultIntent.putExtra("favourite_name", name);
            resultIntent.putExtra("favourite_lat", Double.parseDouble(lat));
            resultIntent.putExtra("favourite_lon", Double.parseDouble(lon));
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @Override
    public void onFavouritePlaceDelete(String entry, int position) {
        showDeleteConfirmationDialog(entry, position);
    }

    private void loadFavourites() {
        SharedPreferences prefs = getSharedPreferences("favourites", MODE_PRIVATE);
        Set<String> favouritesSet = prefs.getStringSet("favourites_list", new HashSet<>());
        favouritesList = new ArrayList<>(favouritesSet);
        adapter.updateData(favouritesList);
    }

    private void showDeleteConfirmationDialog(String entry, int position) {
        String[] parts = entry.split("\\|");
        String placeName = parts.length > 0 ? parts[0] : "this place";

        new AlertDialog.Builder(this)
                .setTitle("Remove Favourite")
                .setMessage("Are you sure you want to remove '" + placeName + "' from your favourites?")
                .setPositiveButton("Remove", (dialog, which) -> removeFavouriteEntry(entry, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeFavouriteEntry(String entry, int position) {
        favouritesList.remove(position);
        adapter.notifyItemRemoved(position);

        // Update SharedPreferences
        SharedPreferences prefs = getSharedPreferences("favourites", MODE_PRIVATE);
        Set<String> newSet = new HashSet<>(favouritesList);
        prefs.edit().putStringSet("favourites_list", newSet).apply();

        String[] parts = entry.split("\\|");
        String placeName = parts.length > 0 ? parts[0] : "this place";
        Toast.makeText(this, "Removed '" + placeName + "' from favourites", Toast.LENGTH_SHORT).show();
        
        // Send broadcast to update map markers
        Intent intent = new Intent("FAVOURITES_UPDATED");
        sendBroadcast(intent);
        
        // Pass back deletion info to MainActivity for immediate refresh
        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", "deleted_favourite");
        resultIntent.putExtra("deleted_entry", entry);
        setResult(RESULT_OK, resultIntent);
        
        Log.d("Favourites", "Sent deletion result back to MainActivity for: " + placeName);
    }

    private void showDeleteAllConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Favourites")
                .setMessage("Are you sure you want to delete all your favourite places? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> deleteAllFavourites())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllFavourites() {
        favouritesList.clear();
        adapter.notifyDataSetChanged();

        // Update SharedPreferences
        SharedPreferences prefs = getSharedPreferences("favourites", MODE_PRIVATE);
        prefs.edit().putStringSet("favourites_list", new HashSet<>()).apply();

        Toast.makeText(this, "All favourite places deleted", Toast.LENGTH_SHORT).show();
        
        // Send broadcast to update map markers
        Intent intent = new Intent("FAVOURITES_UPDATED");
        sendBroadcast(intent);
        
        // Pass back deletion info to MainActivity for immediate refresh
        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", "deleted_all_favourites");
        setResult(RESULT_OK, resultIntent);
        
        Log.d("Favourites", "Sent bulk deletion result back to MainActivity");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Return to previous activity without any result
        finish();
    }
}
