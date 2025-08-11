package com.example.bottlenex;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.SearchView;
import android.widget.Toast;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ToGoPlacesActivity extends AppCompatActivity implements ToGoPlacesAdapter.OnToGoPlaceActionListener {

    private ListView listView;
    private ArrayList<String> toGoPlacesList;
    private ToGoPlacesAdapter adapter;
    private SearchView searchView;
    private Geocoder geocoder;
    private TextView placesCount;
    private View emptyState;
    private MaterialButton btnDeleteAll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_go_places);

        // Initialize Geocoder
        geocoder = new Geocoder(this, Locale.getDefault());

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_to_go);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("To Go Places");
        }

        // Initialize views
        listView = findViewById(R.id.listViewToGoPlaces);
        searchView = findViewById(R.id.searchView);
        placesCount = findViewById(R.id.placesCount);
        emptyState = findViewById(R.id.emptyState);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);

        toGoPlacesList = new ArrayList<>();

        // Setup ListView with custom adapter
        adapter = new ToGoPlacesAdapter(this, toGoPlacesList, this);
        listView.setAdapter(adapter);

        loadToGoPlaces();
        updateUI();

        // Setup SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    performSearch(query.trim());
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText == null || newText.trim().isEmpty()) {
                    // Show To Go Places list when search is cleared
                    showToGoPlacesList();
                }
                return false;
            }
        });

        // Setup Delete All button
        btnDeleteAll.setOnClickListener(v -> {
            if (!toGoPlacesList.isEmpty()) {
                showDeleteAllConfirmationDialog();
            } else {
                Toast.makeText(this, "No places to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSearch(String query) {
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 5);
            if (addresses != null && !addresses.isEmpty()) {
                // Automatically save the first result to To Go Places
                Address firstAddress = addresses.get(0);
                String displayName = getReadableAddress(firstAddress);

                // Add to To Go Places automatically
                addToToGoPlaces(displayName);

                // Show success message
                Toast.makeText(this, "Added to To Go Places: " + displayName, Toast.LENGTH_SHORT).show();

                // Clear search
                searchView.setQuery("", false);
                searchView.clearFocus();

                // Show the updated list
                showToGoPlacesList();

                // Show option to navigate immediately
                showNavigateOption(displayName, firstAddress.getLatitude(), firstAddress.getLongitude());

            } else {
                Toast.makeText(this, "No locations found for: " + query, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error searching for location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showNavigateOption(String placeName, double latitude, double longitude) {
        new AlertDialog.Builder(this)
                .setTitle("Location Added!")
                .setMessage("'" + placeName + "' has been added to your To Go Places.\n\nWould you like to navigate there now?")
                .setPositiveButton("Navigate Now", (dialog, which) -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("action", "togo_navigate");
                    resultIntent.putExtra("togo_name", placeName);
                    resultIntent.putExtra("togo_lat", latitude);
                    resultIntent.putExtra("togo_lon", longitude);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void showToGoPlacesList() {
        adapter.updateData(toGoPlacesList);
        updateUI();
    }

    private String getReadableAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getLocality() != null) {
            sb.append(address.getLocality());
        }
        if (address.getAdminArea() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getAdminArea());
        }
        if (address.getCountryName() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getCountryName());
        }
        return sb.toString();
    }

    private void loadToGoPlaces() {
        SharedPreferences prefs = getSharedPreferences("to_go_places", MODE_PRIVATE);
        Set<String> toGoPlacesSet = prefs.getStringSet("places_set", new HashSet<>());
        toGoPlacesList = new ArrayList<>(toGoPlacesSet);
    }

    private void addToToGoPlaces(String placeName) {
        if (toGoPlacesList.contains(placeName)) {
            return; // Already exists, don't add duplicate
        }

        toGoPlacesList.add(placeName);
        saveToGoPlaces();
        updateUI();
    }

    private void saveToGoPlaces() {
        SharedPreferences prefs = getSharedPreferences("to_go_places", MODE_PRIVATE);
        Set<String> toGoPlacesSet = new HashSet<>(toGoPlacesList);
        prefs.edit().putStringSet("places_set", toGoPlacesSet).apply();
    }

    private void updateUI() {
        // Update places count
        int count = toGoPlacesList.size();
        placesCount.setText(count + " place" + (count != 1 ? "s" : ""));

        // Show/hide empty state
        if (count == 0) {
            emptyState.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        // Update adapter
        adapter.updateData(toGoPlacesList);
    }

    private void showDeleteAllConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Places")
                .setMessage("Are you sure you want to remove all places from your To Go Places?")
                .setPositiveButton("Clear All", (dialog, which) -> deleteAllToGoPlaces())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllToGoPlaces() {
        toGoPlacesList.clear();
        saveToGoPlaces();
        updateUI();
        Toast.makeText(this, "All places cleared", Toast.LENGTH_SHORT).show();
    }

    // ToGoPlacesAdapter.OnToGoPlaceActionListener implementation
    @Override
    public void onNavigate(String placeName) {
        // Geocode the place name to get coordinates
        try {
            List<Address> addresses = geocoder.getFromLocationName(placeName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("action", "togo_navigate");
                resultIntent.putExtra("togo_name", placeName);
                resultIntent.putExtra("togo_lat", latitude);
                resultIntent.putExtra("togo_lon", longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Could not find location for: " + placeName, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error finding location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDelete(String placeName, int position) {
        showDeleteConfirmationDialog(placeName, position);
    }

    private void showDeleteConfirmationDialog(String placeName, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Place")
                .setMessage("Are you sure you want to remove '" + placeName + "' from your To Go Places?")
                .setPositiveButton("Remove", (dialog, which) -> removeToGoPlace(placeName, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeToGoPlace(String placeName, int position) {
        toGoPlacesList.remove(position);
        saveToGoPlaces();
        updateUI();
        Toast.makeText(this, "Removed: " + placeName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
