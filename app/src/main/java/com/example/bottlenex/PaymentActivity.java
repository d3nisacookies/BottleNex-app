package com.example.bottlenex;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * PaymentActivity displays the user's subscription payment history from Firebase.
 * Shows real payment data including dates, amounts, plan details, and status.
 */
public class PaymentActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private TextView tvNoPayments;
    private TextView tvPaymentTitle;
    private PaymentAdapter paymentAdapter;
    private List<PaymentRecord> paymentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewPayments);
        tvNoPayments = findViewById(R.id.tvNoPayments);
        tvPaymentTitle = findViewById(R.id.tvPaymentTitle);

        // Set up RecyclerView
        paymentList = new ArrayList<>();
        paymentAdapter = new PaymentAdapter(paymentList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(paymentAdapter);

        // Load payment data
        loadPaymentData();
    }

    private void loadPaymentData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showNoPaymentsMessage("Please log in to view payment history");
            return;
        }

        String userId = currentUser.getUid();
        
        db.collection("users").document(userId).collection("payments")
            .orderBy("paymentDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                paymentList.clear();
                
                if (queryDocumentSnapshots.isEmpty()) {
                    showNoPaymentsMessage("No payment history found");
                    return;
                }

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    PaymentRecord payment = new PaymentRecord();
                    payment.setId(document.getId());
                    payment.setCardNumber(document.getString("cardNumber"));
                    payment.setCardholderName(document.getString("cardholderName"));
                    payment.setPaymentDate(document.getTimestamp("paymentDate"));
                    payment.setPlanBilling(document.getString("planBilling"));
                    payment.setPlanPrice(document.getString("planPrice"));
                    payment.setPlanTitle(document.getString("planTitle"));
                    payment.setStatus(document.getString("status"));
                    payment.setUserEmail(document.getString("userEmail"));
                    
                    paymentList.add(payment);
                }
                
                paymentAdapter.notifyDataSetChanged();
                showPaymentList();
                
            })
            .addOnFailureListener(e -> {
                showNoPaymentsMessage("Failed to load payment history: " + e.getMessage());
                Toast.makeText(this, "Error loading payments", Toast.LENGTH_SHORT).show();
            });
    }

    private void showNoPaymentsMessage(String message) {
        recyclerView.setVisibility(View.GONE);
        tvNoPayments.setVisibility(View.VISIBLE);
        tvNoPayments.setText(message);
    }

    private void showPaymentList() {
        recyclerView.setVisibility(View.VISIBLE);
        tvNoPayments.setVisibility(View.GONE);
    }

    // PaymentRecord data class
    public static class PaymentRecord {
        private String id;
        private String cardNumber;
        private String cardholderName;
        private com.google.firebase.Timestamp paymentDate;
        private String planBilling;
        private String planPrice;
        private String planTitle;
        private String status;
        private String userEmail;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public String getCardholderName() { return cardholderName; }
        public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
        
        public com.google.firebase.Timestamp getPaymentDate() { return paymentDate; }
        public void setPaymentDate(com.google.firebase.Timestamp paymentDate) { this.paymentDate = paymentDate; }
        
        public String getPlanBilling() { return planBilling; }
        public void setPlanBilling(String planBilling) { this.planBilling = planBilling; }
        
        public String getPlanPrice() { return planPrice; }
        public void setPlanPrice(String planPrice) { this.planPrice = planPrice; }
        
        public String getPlanTitle() { return planTitle; }
        public void setPlanTitle(String planTitle) { this.planTitle = planTitle; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    }
}