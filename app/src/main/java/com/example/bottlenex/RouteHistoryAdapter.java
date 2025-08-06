package com.example.bottlenex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RouteHistoryAdapter extends RecyclerView.Adapter<RouteHistoryAdapter.RouteHistoryViewHolder> {

    private List<RouteHistory> routeList;
    private OnRouteClickListener listener;

    public interface OnRouteClickListener {
        void onRouteClick(RouteHistory route);
        void onRouteLongClick(RouteHistory route, View view);
    }

    public RouteHistoryAdapter(List<RouteHistory> routeList, OnRouteClickListener listener) {
        this.routeList = routeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_history, parent, false);
        return new RouteHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteHistoryViewHolder holder, int position) {
        RouteHistory route = routeList.get(position);
        holder.bind(route);
    }

    @Override
    public int getItemCount() {
        return routeList.size();
    }

    public void updateData(List<RouteHistory> newRouteList) {
        this.routeList = newRouteList;
        notifyDataSetChanged();
    }

    class RouteHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvRouteInfo;
        private TextView tvDateTime;
        private TextView tvDistance;
        private TextView tvDuration;
        private TextView tvStartTime;
        private TextView tvEndTime;

        public RouteHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRouteInfo = itemView.findViewById(R.id.tvRouteInfo);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvStartTime = itemView.findViewById(R.id.tvStartTime);
            tvEndTime = itemView.findViewById(R.id.tvEndTime);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRouteClick(routeList.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRouteLongClick(routeList.get(position), v);
                    return true;
                }
                return false;
            });
        }

        public void bind(RouteHistory route) {
            tvRouteInfo.setText(route.getFormattedRoute());
            tvDateTime.setText(route.getDate());
            tvDistance.setText(route.getFormattedDistance());
            tvDuration.setText(route.getFormattedDuration());
            tvStartTime.setText("Start: " + route.getStartTime());
            tvEndTime.setText("End: " + route.getEndTime());
        }
    }
} 