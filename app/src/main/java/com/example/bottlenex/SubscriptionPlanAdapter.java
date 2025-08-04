package com.example.bottlenex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * SubscriptionPlanAdapter handles the display of subscription plans in a RecyclerView.
 * Manages plan selection and visual styling for different plan types.
 */
public class SubscriptionPlanAdapter extends RecyclerView.Adapter<SubscriptionPlanAdapter.PlanViewHolder> {

    private List<SubscriptionPlan> plans;
    private OnPlanClickListener listener;
    private int selectedPosition = -1;

    public interface OnPlanClickListener {
        void onPlanClick(SubscriptionPlan plan, int position);
    }

    public SubscriptionPlanAdapter(List<SubscriptionPlan> plans) {
        this.plans = plans;
    }

    public void setOnPlanClickListener(OnPlanClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subscription_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        SubscriptionPlan plan = plans.get(position);
        holder.bind(plan, position);
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    public void setSelectedPosition(int position) {
        int previousSelected = selectedPosition;
        selectedPosition = position;
        
        // Update previous selection
        if (previousSelected >= 0 && previousSelected < plans.size()) {
            plans.get(previousSelected).setSelected(false);
            notifyItemChanged(previousSelected);
        }
        
        // Update new selection
        if (position >= 0 && position < plans.size()) {
            plans.get(position).setSelected(true);
            notifyItemChanged(position);
        }
    }

    public SubscriptionPlan getSelectedPlan() {
        if (selectedPosition >= 0 && selectedPosition < plans.size()) {
            return plans.get(selectedPosition);
        }
        return null;
    }

    class PlanViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout planContainer;
        private TextView tvPlanTitle;
        private TextView tvPlanDescription;
        private TextView tvPlanPrice;
        private TextView tvBillingPeriod;
        private TextView tvPopularBadge;
        private TextView tvFeature1;
        private TextView tvFeature2;
        private TextView tvFeature3;
        private TextView tvFeature4;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            
            planContainer = itemView.findViewById(R.id.planContainer);
            tvPlanTitle = itemView.findViewById(R.id.tvPlanTitle);
            tvPlanDescription = itemView.findViewById(R.id.tvPlanDescription);
            tvPlanPrice = itemView.findViewById(R.id.tvPlanPrice);
            tvBillingPeriod = itemView.findViewById(R.id.tvBillingPeriod);
            tvPopularBadge = itemView.findViewById(R.id.tvPopularBadge);
            tvFeature1 = itemView.findViewById(R.id.tvFeature1);
            tvFeature2 = itemView.findViewById(R.id.tvFeature2);
            tvFeature3 = itemView.findViewById(R.id.tvFeature3);
            tvFeature4 = itemView.findViewById(R.id.tvFeature4);

            planContainer.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    setSelectedPosition(position);
                    listener.onPlanClick(plans.get(position), position);
                }
            });
        }

        public void bind(SubscriptionPlan plan, int position) {
            tvPlanTitle.setText(plan.getTitle());
            tvPlanDescription.setText(plan.getDescription());
            tvPlanPrice.setText(plan.getPrice());
            tvBillingPeriod.setText(plan.getBillingPeriod());

            // Set features
            String[] features = plan.getFeatures();
            if (features != null && features.length > 0) {
                tvFeature1.setVisibility(View.VISIBLE);
                tvFeature1.setText(features[0]);
                
                if (features.length > 1) {
                    tvFeature2.setVisibility(View.VISIBLE);
                    tvFeature2.setText(features[1]);
                } else {
                    tvFeature2.setVisibility(View.GONE);
                }
                
                if (features.length > 2) {
                    tvFeature3.setVisibility(View.VISIBLE);
                    tvFeature3.setText(features[2]);
                } else {
                    tvFeature3.setVisibility(View.GONE);
                }
                
                if (features.length > 3) {
                    tvFeature4.setVisibility(View.VISIBLE);
                    tvFeature4.setText(features[3]);
                } else {
                    tvFeature4.setVisibility(View.GONE);
                }
            } else {
                tvFeature1.setVisibility(View.GONE);
                tvFeature2.setVisibility(View.GONE);
                tvFeature3.setVisibility(View.GONE);
                tvFeature4.setVisibility(View.GONE);
            }

            // Handle popular badge
            if (plan.isPopular()) {
                tvPopularBadge.setVisibility(View.VISIBLE);
                planContainer.setBackgroundResource(R.drawable.popular_plan_border);
            } else {
                tvPopularBadge.setVisibility(View.GONE);
                planContainer.setBackgroundResource(android.R.color.transparent);
            }

            // Handle selection state
            if (plan.isSelected()) {
                planContainer.setBackgroundResource(R.drawable.button_background);
                tvPlanTitle.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                tvPlanDescription.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                tvPlanPrice.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                tvBillingPeriod.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                tvFeature1.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                tvFeature2.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                tvFeature3.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                tvFeature4.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
            } else {
                if (!plan.isPopular()) {
                    planContainer.setBackgroundResource(android.R.color.transparent);
                }
                tvPlanTitle.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
                tvPlanDescription.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                tvPlanPrice.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
                tvBillingPeriod.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                tvFeature1.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
                tvFeature2.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
                tvFeature3.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
                tvFeature4.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
            }
        }
    }
} 