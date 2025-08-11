package com.example.bottlenex;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.ImageView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ToGoPlacesAdapter extends BaseAdapter {

    private Context context;
    private List<String> placesList;
    private OnToGoPlaceActionListener listener;

    public interface OnToGoPlaceActionListener {
        void onNavigate(String placeName);
        void onDelete(String placeName, int position);
    }

    public ToGoPlacesAdapter(Context context, List<String> placesList, OnToGoPlaceActionListener listener) {
        this.context = context;
        this.placesList = placesList;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return placesList.size();
    }

    @Override
    public String getItem(int position) {
        return placesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_to_go_place, parent, false);
            holder = new ViewHolder();
            holder.tvPlaceName = convertView.findViewById(R.id.tvPlaceName);
            holder.tvPlaceType = convertView.findViewById(R.id.tvPlaceType);
            holder.btnNavigate = convertView.findViewById(R.id.btnNavigate);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String placeName = getItem(position);
        holder.tvPlaceName.setText(placeName);
        holder.tvPlaceType.setText("Saved destination");

        // Set up click listeners
        holder.btnNavigate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNavigate(placeName);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(placeName, position);
            }
        });

        return convertView;
    }

    public void updateData(List<String> newPlacesList) {
        this.placesList = newPlacesList;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView tvPlaceName;
        TextView tvPlaceType;
        MaterialButton btnNavigate;
        ImageView btnDelete;
    }
}