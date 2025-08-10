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

public class StarredPlacesActivity extends AppCompatActivity implements StarredPlacesAdapter.OnStarredPlaceClickListener {

    private RecyclerView recyclerView;
    private ArrayList<String> starredList;
    private StarredPlacesAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starred_places);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bookmark");
        }

        recyclerView = findViewById(R.id.recyclerViewStarred);
        starredList = new ArrayList<>();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StarredPlacesAdapter(starredList, this);
        recyclerView.setAdapter(adapter);

        loadStarredPlaces();

        // Setup Delete All button
        MaterialButton btnDeleteAll = findViewById(R.id.btnDeleteAll);
        btnDeleteAll.setOnClickListener(v -> {
            if (!starredList.isEmpty()) {
                showDeleteAllConfirmationDialog();
            } else {
                Toast.makeText(this, "No starred places to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStarredPlaceClick(String entry) {
        String[] parts = entry.split("\\|");
        if (parts.length == 3) {
            String name = parts[0];
            String lat = parts[1];
            String lon = parts[2];

            Intent resultIntent = new Intent();
            resultIntent.putExtra("action", "selected");
            resultIntent.putExtra("starred_name", name);
            resultIntent.putExtra("starred_lat", Double.parseDouble(lat));
            resultIntent.putExtra("starred_lon", Double.parseDouble(lon));
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @Override
    public void onStarredPlaceNavigate(String entry) {
        String[] parts = entry.split("\\|");
        if (parts.length == 3) {
            String name = parts[0];
            String lat = parts[1];
            String lon = parts[2];

            Intent resultIntent = new Intent();
            resultIntent.putExtra("action", "navigate");
            resultIntent.putExtra("starred_name", name);
            resultIntent.putExtra("starred_lat", Double.parseDouble(lat));
            resultIntent.putExtra("starred_lon", Double.parseDouble(lon));
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @Override
    public void onStarredPlaceDelete(String entry, int position) {
        showDeleteConfirmationDialog(entry, position);
    }

    private void loadStarredPlaces() {
        SharedPreferences prefs = getSharedPreferences("starred_places", MODE_PRIVATE);
        Set<String> starredSet = prefs.getStringSet("starred_places_list", null);
        starredList.clear();
        if (starredSet != null) {
            starredList.addAll(starredSet);
        }
        adapter.updateData(starredList);
    }

    private void showDeleteConfirmationDialog(String entry, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Starred Place")
                .setMessage("Are you sure you want to remove \"" + entry.split("\\|")[0] + "\" from starred places?")
                .setPositiveButton("Yes", (dialog, which) -> removeStarredEntry(entry, position))
                .setNegativeButton("No", null)
                .show();
    }

    private void removeStarredEntry(String entry, int position) {
        SharedPreferences prefs = getSharedPreferences("starred_places", MODE_PRIVATE);
        Set<String> starredSet = prefs.getStringSet("starred_places_list", new HashSet<>());
        Set<String> newSet = new HashSet<>(starredSet);
        newSet.remove(entry);

        prefs.edit().putStringSet("starred_places_list", newSet).apply();

        starredList.remove(position);
        adapter.updateData(starredList);
        
        // Notify main map to refresh starred places
        sendBroadcast(new Intent("STARRED_PLACES_UPDATED"));
        
        // Pass back deletion info to MainActivity for immediate refresh
        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", "deleted");
        resultIntent.putExtra("deleted_entry", entry);
        setResult(RESULT_OK, resultIntent);
        
        // Show success message
        Toast.makeText(this, "Starred place removed", Toast.LENGTH_SHORT).show();
        
        // Don't finish immediately - let user continue using the activity
        // The result will be sent back when they navigate back
    }

    private void showDeleteAllConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Starred Places")
                .setMessage("Are you sure you want to delete all starred places? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> deleteAllStarredPlaces())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllStarredPlaces() {
        SharedPreferences prefs = getSharedPreferences("starred_places", MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        starredList.clear();
        adapter.updateData(starredList);
        
        // Notify main map to refresh starred places
        sendBroadcast(new Intent("STARRED_PLACES_UPDATED"));
        
        // Pass back deletion info to MainActivity for immediate refresh
        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", "deleted_all");
        setResult(RESULT_OK, resultIntent);
        
        Toast.makeText(this, "All starred places deleted", Toast.LENGTH_SHORT).show();
        
        // Don't finish immediately - let user continue using the activity
        // The result will be sent back when they navigate back
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Send any pending deletion results before navigating back
        sendPendingResults();
        onBackPressed();
        return true;
    }
    
    @Override
    public void onBackPressed() {
        // Send any pending deletion results before going back
        sendPendingResults();
        super.onBackPressed();
    }
    
    private void sendPendingResults() {
        // This method ensures that any deletion results are sent back to MainActivity
        // even if the user navigates back without explicitly finishing the activity
        // The result will be processed in MainActivity's onActivityResult
    }
}
