package com.example.bottlenex;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class StarredPlacesActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> starredList;
    ArrayAdapter<String> adapter;
    ArrayList<String> displayList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starred_places);

        listView = findViewById(R.id.listViewStarred);
        starredList = new ArrayList<>();

        loadStarredPlaces();


        listView.setOnItemClickListener((parent, view, position, id) -> {
            String entry = starredList.get(position);
            String[] parts = entry.split("\\|");
            if (parts.length == 3) {
                String name = parts[0];
                String lat = parts[1];
                String lon = parts[2];

                Intent resultIntent = new Intent();
                resultIntent.putExtra("starred_name", name);
                resultIntent.putExtra("starred_lat", Double.parseDouble(lat));
                resultIntent.putExtra("starred_lon", Double.parseDouble(lon));
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });


        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String entryToRemove = starredList.get(position);
            showDeleteConfirmationDialog(entryToRemove, position);
            return true;  // indicate event consumed
        });
    }

    private void loadStarredPlaces() {
        SharedPreferences prefs = getSharedPreferences("starred_places", MODE_PRIVATE);
        Set<String> starredSet = prefs.getStringSet("starred_places_list", null);
        starredList.clear();
        displayList = new ArrayList<>();
        if (starredSet != null) {
            starredList.addAll(starredSet);
        }
        for (String entry : starredList) {
            String[] parts = entry.split("\\|");
            if (parts.length >= 1) displayList.add(parts[0]);
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);
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
        displayList.remove(position);
        adapter.notifyDataSetChanged();
    }
}
