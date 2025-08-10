package com.example.bottlenex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class StarredPlacesAdapter extends RecyclerView.Adapter<StarredPlacesAdapter.StarredPlacesViewHolder> {

    private List<String> starredList;
    private OnStarredPlaceClickListener listener;

    public interface OnStarredPlaceClickListener {
        void onStarredPlaceClick(String entry);
        void onStarredPlaceDelete(String entry, int position);
        void onStarredPlaceNavigate(String entry);
    }

    public StarredPlacesAdapter(List<String> starredList, OnStarredPlaceClickListener listener) {
        this.starredList = starredList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StarredPlacesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_starred_place, parent, false);
        return new StarredPlacesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StarredPlacesViewHolder holder, int position) {
        String entry = starredList.get(position);
        holder.bind(entry, position);
    }

    @Override
    public int getItemCount() {
        return starredList.size();
    }

    public void updateData(List<String> newStarredList) {
        this.starredList = newStarredList;
        notifyDataSetChanged();
    }

    class StarredPlacesViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPlaceName;
        private MaterialButton btnNavigate;
        private MaterialButton btnDelete;

        public StarredPlacesViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
            btnNavigate = itemView.findViewById(R.id.btnNavigate);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // Handle navigation button click
            btnNavigate.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStarredPlaceNavigate(starredList.get(position));
                }
            });

            // Handle delete button click
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStarredPlaceDelete(starredList.get(position), position);
                }
            });
        }

        public void bind(String entry, int position) {
            String[] parts = entry.split("\\|");
            if (parts.length >= 1) {
                tvPlaceName.setText(parts[0]);
            }
        }
    }
}
