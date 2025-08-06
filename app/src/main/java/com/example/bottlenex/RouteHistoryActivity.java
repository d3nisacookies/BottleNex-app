package com.example.bottlenex;

//To commit

import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.osmdroid.util.GeoPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RouteHistoryActivity extends AppCompatActivity implements RouteHistoryAdapter.OnRouteClickListener {

    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private RouteHistoryAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<RouteHistory> routeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_history);

        // ini db helper
        databaseHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Personal Tools");
            getSupportActionBar().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF2196F3));
        }

        recyclerView = findViewById(R.id.recyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        routeList = new ArrayList<>();
        adapter = new RouteHistoryAdapter(routeList, this);
        recyclerView.setAdapter(adapter);

        loadRouteHistory();
    }

    private void loadRouteHistory() {
        routeList.clear();
        routeList.addAll(databaseHelper.getAllRouteHistory());
        adapter.updateData(routeList);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (routeList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRouteClick(RouteHistory route) {
        // Show route details dialog
        showRouteDetailsDialog(route);
    }

    @Override
    public void onRouteLongClick(RouteHistory route, View view) {
        // Show options dialog (delete, etc.)
        showRouteOptionsDialog(route);
    }

    private void showRouteDetailsDialog(RouteHistory route) {
        String details = String.format(
            "Route Details\n\n" +
            "From: %s\n" +
            "To: %s\n\n" +
            "Distance: %s\n" +
            "Duration: %s\n\n" +
            "Date: %s\n" +
            "Start Time: %s\n" +
            "End Time: %s",
            route.getStartAddress() != null ? route.getStartAddress() : 
                String.format("%.4f, %.4f", route.getStartLat(), route.getStartLon()),
            route.getEndAddress() != null ? route.getEndAddress() : 
                String.format("%.4f, %.4f", route.getEndLat(), route.getEndLon()),
            route.getFormattedDistance(),
            route.getFormattedDuration(),
            route.getDate(),
            route.getStartTime(),
            route.getEndTime()
        );

        new AlertDialog.Builder(this)
            .setTitle("Route Information")
            .setMessage(details)
            .setPositiveButton("Close", null)
            .show();
    }

    private void showRouteOptionsDialog(RouteHistory route) {
        String[] options = {"Delete Route", "Cancel"};
        
        new AlertDialog.Builder(this)
            .setTitle("Route Options")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Delete route
                    showDeleteConfirmationDialog(route);
                }
            })
            .show();
    }

    private void showDeleteConfirmationDialog(RouteHistory route) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Route")
            .setMessage("Are you sure you want to delete this route from your history?")
            .setPositiveButton("Delete", (dialog, which) -> {
                databaseHelper.deleteRouteHistory(route.getId());
                loadRouteHistory();
                Toast.makeText(this, "Route deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.route_history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_clear_all) {
            showClearAllConfirmationDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void showClearAllConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Clear All Routes")
            .setMessage("Are you sure you want to delete all route history? This action cannot be undone.")
            .setPositiveButton("Clear All", (dialog, which) -> {
                databaseHelper.clearAllRouteHistory();
                loadRouteHistory();
                Toast.makeText(this, "All routes cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRouteHistory();
    }
}