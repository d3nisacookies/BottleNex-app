package com.example.bottlenex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class FavouritesAdapter extends RecyclerView.Adapter<FavouritesAdapter.FavouritesViewHolder> {

    private List<String> favouritesList;
    private OnFavouritePlaceClickListener listener;

    public interface OnFavouritePlaceClickListener {
        void onFavouritePlaceDelete(String entry, int position);
        void onFavouritePlaceNavigate(String entry);
    }

    public FavouritesAdapter(List<String> favouritesList, OnFavouritePlaceClickListener listener) {
        this.favouritesList = favouritesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavouritesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favourite_place, parent, false);
        return new FavouritesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavouritesViewHolder holder, int position) {
        String entry = favouritesList.get(position);
        holder.bind(entry, position);
    }

    @Override
    public int getItemCount() {
        return favouritesList.size();
    }

    public void updateData(List<String> newFavouritesList) {
        this.favouritesList = newFavouritesList;
        notifyDataSetChanged();
    }

    class FavouritesViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPlaceName;
        private MaterialButton btnNavigate;
        private MaterialButton btnDelete;

        public FavouritesViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
            btnNavigate = itemView.findViewById(R.id.btnNavigate);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // Handle navigation button click
            btnNavigate.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onFavouritePlaceNavigate(favouritesList.get(position));
                }
            });

            // Handle delete button click
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onFavouritePlaceDelete(favouritesList.get(position), position);
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
