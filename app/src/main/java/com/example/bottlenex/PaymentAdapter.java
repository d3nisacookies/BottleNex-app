package com.example.bottlenex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying payment records in a RecyclerView
 * Works with PaymentHistoryActivity.PaymentRecord
 */
public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {

    private List<PaymentHistoryActivity.PaymentRecord> paymentList;
    private SimpleDateFormat dateFormat;

    public PaymentAdapter(List<PaymentHistoryActivity.PaymentRecord> paymentList) {
        this.paymentList = paymentList;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        PaymentHistoryActivity.PaymentRecord payment = paymentList.get(position);
        
        // Set payment details
        holder.tvPlanTitle.setText(payment.getPlanTitle());
        holder.tvPlanPrice.setText(payment.getPlanPrice());
        holder.tvPlanBilling.setText(payment.getPlanBilling());
        holder.tvStatus.setText(payment.getStatus());
        holder.tvCardholderName.setText(payment.getCardholderName());
        holder.tvCardNumber.setText("**** **** **** " + payment.getCardNumber());
        
        // Format and set payment date
        if (payment.getPaymentDate() != null) {
            String formattedDate = dateFormat.format(payment.getPaymentDate().toDate());
            holder.tvPaymentDate.setText(formattedDate);
        } else {
            holder.tvPaymentDate.setText("Date not available");
        }
        
        setStatusColor(holder, payment.getStatus());
    }

    private void setStatusColor(PaymentViewHolder holder, String status) {
        if ("completed".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else if ("pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    public int getItemCount() {
        return paymentList.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlanTitle, tvPlanPrice, tvPlanBilling, tvStatus, tvCardholderName, tvCardNumber, tvPaymentDate;

        PaymentViewHolder(View itemView) {
            super(itemView);
            tvPlanTitle = itemView.findViewById(R.id.tvPlanTitle);
            tvPlanPrice = itemView.findViewById(R.id.tvPlanPrice);
            tvPlanBilling = itemView.findViewById(R.id.tvPlanBilling);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCardholderName = itemView.findViewById(R.id.tvCardholderName);
            tvCardNumber = itemView.findViewById(R.id.tvCardNumber);
            tvPaymentDate = itemView.findViewById(R.id.tvPaymentDate);
        }
    }
} 