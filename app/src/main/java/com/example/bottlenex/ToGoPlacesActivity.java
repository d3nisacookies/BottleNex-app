package com.example.bottlenex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ToGoPlacesActivity extends AppCompatActivity {

    private SearchView searchView;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> placeNames = new ArrayList<>();
    private Set<String> placeSet = new HashSet<>();

    private static final String PREFS_NAME = "to_go_places";
    private static final String PREFS_KEY = "places_set";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_go_places);

        // Setup toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar_to_go);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bookmark");
        }

        searchView = findViewById(R.id.searchView);
        listView = findViewById(R.id.listView_places);

        loadPlaces();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, placeNames);
        listView.setAdapter(adapter);


        listView.setOnItemClickListener((parent, view, position, id) -> {
            String name = placeNames.get(position);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_place_name", name);
            setResult(RESULT_OK, resultIntent);
            finish();
        });


        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String placeToRemove = placeNames.get(position);
            new AlertDialog.Builder(ToGoPlacesActivity.this)
                    .setTitle("Remove Place")
                    .setMessage("Remove \"" + placeToRemove + "\" from To Go Places?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        placeSet.remove(placeToRemove);
                        savePlaces();

                        placeNames.remove(position);
                        adapter.notifyDataSetChanged();

                        Toast.makeText(ToGoPlacesActivity.this, "Place removed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query == null || query.trim().isEmpty()) {
                    Toast.makeText(ToGoPlacesActivity.this, "Please enter a place name", Toast.LENGTH_SHORT).show();
                    return false;
                }
                addPlace(query.trim());
                searchView.setQuery("", false);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        ImageButton btnSearchLeft = findViewById(R.id.btnSearchLeft);
        btnSearchLeft.setOnClickListener(v -> {
            String query = searchView.getQuery().toString();
            if (!query.isEmpty()) {
                addPlace(query.trim());
                searchView.setQuery("", false);
                searchView.clearFocus();
            } else {
                Toast.makeText(ToGoPlacesActivity.this, "Enter place to add", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaces() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        placeSet = prefs.getStringSet(PREFS_KEY, new HashSet<>());
        placeNames.clear();
        placeNames.addAll(placeSet);
    }

    private void savePlaces() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(PREFS_KEY, placeSet).apply();
    }

    private void addPlace(String placeName) {
        if (placeSet.contains(placeName)) {
            Toast.makeText(this, "Place already added", Toast.LENGTH_SHORT).show();
            return;
        }

        placeSet.add(placeName);
        savePlaces();

        placeNames.add(placeName);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Place added", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
