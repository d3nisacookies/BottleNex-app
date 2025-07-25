package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DirectionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);

        // Update time
        TextView timeTextView = findViewById(R.id.tvTime);
        timeTextView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));

        // Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.rvLocations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final LocationAdapter adapter = new LocationAdapter(getSampleLocations());
        recyclerView.setAdapter(adapter);

        // Set item click listener
        adapter.setOnItemClickListener(new LocationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Location location) {
                Intent intent = new Intent(DirectionActivity.this, LocationDetailsActivity.class);
                intent.putExtra("location_name", location.getName());
                startActivity(intent);
            }
        });
    }

    private List<Location> getSampleLocations() {
        List<Location> locations = new ArrayList<>();
        locations.add(new Location("CLEMENTI", "CIF", ""));
        locations.add(new Location("Commenti'Ra", "Urban Care Employ", "8 mins - 2.6km"));
        locations.add(new Location("Sinopec", "10 min", "8 mins - 2.6km"));
        return locations;
    }

    public static class Location {
        private String name;
        private String description;
        private String distance;

        public Location(String name, String description, String distance) {
            this.name = name;
            this.description = description;
            this.distance = distance;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getDistance() { return distance; }
    }

    public static class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {
        private List<Location> locations;
        private OnItemClickListener onItemClickListener;

        public interface OnItemClickListener {
            void onItemClick(Location location);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.onItemClickListener = listener;
        }

        public LocationAdapter(List<Location> locations) {
            this.locations = locations;
        }

        @NonNull
        @Override
        public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new LocationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
            final Location location = locations.get(position);
            holder.bind(location);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(location);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return locations.size();
        }

        static class LocationViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;

            LocationViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }

            void bind(Location location) {
                text1.setText(location.getName());
                String desc = (location.getDescription() + " " + location.getDistance()).trim();
                text2.setText(desc);
            }
        }
    }
} 