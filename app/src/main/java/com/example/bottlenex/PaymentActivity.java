package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * PaymentActivity displays the user's current subscription details and payment information.
 * Shows active subscription plan, payment details, and payment method information.
 */
public class PaymentActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvSubscriptionStatus, tvPlanName, tvPlanPrice, tvBillingPeriod, tvPlanFeatures;
    private TextView tvPaymentDate, tvNextPaymentDate, tvPaymentStatus;
    private TextView tvCardholderName, tvCardNumber;
    private Button btnChangePaymentMethod, btnViewPaymentHistory, btnCancelSubscription;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());

        // Initialize views
        initializeViews();

        // Load subscription data
        loadSubscriptionData();

        // Set click listeners
        setupClickListeners();
        
        // Set up back button
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
        });
    }

    private void initializeViews() {
        // Subscription details views
        tvSubscriptionStatus = findViewById(R.id.tvSubscriptionStatus);
        tvPlanName = findViewById(R.id.tvPlanName);
        tvPlanPrice = findViewById(R.id.tvPlanPrice);
        tvBillingPeriod = findViewById(R.id.tvBillingPeriod);
        tvPlanFeatures = findViewById(R.id.tvPlanFeatures);

        // Payment information views
        tvPaymentDate = findViewById(R.id.tvPaymentDate);
        tvNextPaymentDate = findViewById(R.id.tvNextPaymentDate);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);

        // Payment method views
        tvCardholderName = findViewById(R.id.tvCardholderName);
        tvCardNumber = findViewById(R.id.tvCardNumber);

        // Buttons
        btnChangePaymentMethod = findViewById(R.id.btnChangePaymentMethod);
        btnViewPaymentHistory = findViewById(R.id.btnViewPaymentHistory);
        btnCancelSubscription = findViewById(R.id.btnCancelSubscription);
    }

    private void loadSubscriptionData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showNoSubscriptionMessage("Please log in to view subscription details");
            return;
        }

        String userId = currentUser.getUid();
        
        // Load user document to get currentPlan and subscriptionStatus
        db.collection("users").document(userId).get()
            .addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    String currentPlan = userDoc.getString("currentPlan");
                    String subscriptionStatus = userDoc.getString("subscriptionStatus");
                    
                    if (currentPlan != null && !currentPlan.isEmpty()) {
                        // User has a current plan, now try to get payment details
                        loadPaymentDetails(userId, currentPlan, subscriptionStatus);
                    } else {
                        showNoSubscriptionMessage("No active subscription found");
                    }
                } else {
                    showNoSubscriptionMessage("User document not found");
                }
            })
            .addOnFailureListener(e -> {
                showNoSubscriptionMessage("Failed to load user data: " + e.getMessage());
            });
    }
    
    private void loadPaymentDetails(String userId, String currentPlan, String subscriptionStatus) {
        // Get payment details from payments sub-collection (standardized approach)
        db.collection("users").document(userId).collection("payments")
            .orderBy("paymentDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener(paymentSnapshots -> {
                if (!paymentSnapshots.isEmpty()) {
                    // Found payment details, display subscription with payment info
                    DocumentSnapshot paymentDoc = paymentSnapshots.getDocuments().get(0);
                    displaySubscriptionDetails(currentPlan, paymentDoc, subscriptionStatus);
                } else {
                    // No payment details found, display basic subscription info
                    displayBasicSubscriptionDetails(currentPlan, subscriptionStatus);
                }
            })
            .addOnFailureListener(e -> {
                // On error, display basic subscription info
                displayBasicSubscriptionDetails(currentPlan, subscriptionStatus);
            });
    }

    private void displaySubscriptionDetails(String currentPlan, DocumentSnapshot paymentDoc, String subscriptionStatus) {
        // Get payment data from payment document (standardized approach)
        String planTitle = paymentDoc.getString("planTitle");
        String planPrice = paymentDoc.getString("planPrice");
        String planBilling = paymentDoc.getString("planBilling");
        String cardholderName = paymentDoc.getString("cardholderName");
        String cardNumber = paymentDoc.getString("cardNumber");
        Date paymentDate = paymentDoc.getDate("paymentDate");
        
        // Set subscription details
        tvPlanName.setText(planTitle != null ? planTitle : currentPlan);
        tvPlanPrice.setText(planPrice != null ? planPrice : getDefaultPrice(currentPlan));
        tvBillingPeriod.setText(planBilling != null ? "/" + planBilling : "/" + getDefaultBilling(currentPlan));
        
        // Set subscription status based on subscriptionStatus field
        if ("cancelled".equals(subscriptionStatus)) {
            tvSubscriptionStatus.setText("Cancelled");
            tvSubscriptionStatus.setBackgroundResource(R.drawable.status_background);
            tvSubscriptionStatus.setTextColor(getResources().getColor(android.R.color.white));
            
            // Disable and grey out the cancel subscription button
            btnCancelSubscription.setEnabled(false);
            btnCancelSubscription.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
            btnCancelSubscription.setText("Subscription Cancelled");
        } else {
            tvSubscriptionStatus.setText("Active");
            tvSubscriptionStatus.setBackgroundResource(R.drawable.status_background);
            tvSubscriptionStatus.setTextColor(getResources().getColor(android.R.color.white));
            
            // Enable the cancel subscription button
            btnCancelSubscription.setEnabled(true);
            btnCancelSubscription.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
            btnCancelSubscription.setText("Cancel Subscription");
        }
        
        tvPaymentStatus.setText("Completed");
        
        // Set plan features based on plan type
        setPlanFeatures(currentPlan);
        
        // Set payment dates
        if (paymentDate != null) {
            tvPaymentDate.setText(dateFormat.format(paymentDate));
        } else {
            tvPaymentDate.setText("Date not available");
        }
        
        // Calculate next payment date based on plan
        Date nextPaymentDate = calculateNextPaymentDate(paymentDate, currentPlan);
        if (nextPaymentDate != null) {
            if ("cancelled".equals(subscriptionStatus)) {
                tvNextPaymentDate.setText("Expires: " + dateFormat.format(nextPaymentDate));
            } else {
                tvNextPaymentDate.setText(dateFormat.format(nextPaymentDate));
            }
        } else {
            tvNextPaymentDate.setText("Date not available");
        }
        
        // Set payment method details
        tvCardholderName.setText(cardholderName != null ? cardholderName : "User");
        if (cardNumber != null && cardNumber.length() >= 4) {
            tvCardNumber.setText("**** **** **** " + cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
        } else {
            tvCardNumber.setText("**** **** **** ****");
        }
    }

    private void showNoSubscriptionMessage(String message) {
        // Hide all subscription-related card containers
        findViewById(R.id.cardSubscription).setVisibility(View.GONE);
        findViewById(R.id.cardPaymentInfo).setVisibility(View.GONE);
        findViewById(R.id.cardPaymentMethod).setVisibility(View.GONE);
        findViewById(R.id.layoutActionButtons).setVisibility(View.GONE);

        // Show no subscription message
        TextView tvNoPayments = findViewById(R.id.tvNoPayments);
        if (tvNoPayments != null) {
            tvNoPayments.setVisibility(View.VISIBLE);
            tvNoPayments.setText(message);
        }
    }

    private void setupClickListeners() {
        btnChangePaymentMethod.setOnClickListener(v -> {
            // Navigate to AddCardActivity to change payment method
            Intent intent = new Intent(PaymentActivity.this, AddCardActivity.class);
            // Pass current subscription details so user can update payment method
            intent.putExtra("plan_id", "change_payment");
            intent.putExtra("plan_name", "Change Payment Method");
            intent.putExtra("plan_price", "");
            intent.putExtra("billing_period", "");
            startActivity(intent);
        });

        btnViewPaymentHistory.setOnClickListener(v -> {
            // Navigate to payment history activity
            Intent intent = new Intent(PaymentActivity.this, PaymentHistoryActivity.class);
            startActivity(intent);
        });

        btnCancelSubscription.setOnClickListener(v -> {
            // Show confirmation dialog for subscription cancellation
            showCancelSubscriptionDialog();
        });
    }

    private void showCancelSubscriptionDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cancel Subscription")
            .setMessage("Are you sure you want to cancel your subscription? You will keep access to premium features until the end of your current billing period, but it won't renew automatically.")
            .setPositiveButton("Cancel Subscription", (dialog, which) -> {
                // Handle subscription cancellation
                cancelSubscription();
            })
            .setNegativeButton("Keep Subscription", null)
            .show();
    }

    private void cancelSubscription() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to cancel subscription", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        
        // Get current subscription details first
        db.collection("users").document(userId).get()
            .addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    String currentPlan = userDoc.getString("currentPlan");
                    if (currentPlan != null && !currentPlan.isEmpty()) {
                        // Calculate subscription end date based on payment date
                        db.collection("users").document(userId).collection("payments")
                            .orderBy("paymentDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(paymentSnapshots -> {
                                if (!paymentSnapshots.isEmpty()) {
                                    Date paymentDate = paymentSnapshots.getDocuments().get(0).getDate("paymentDate");
                                    Date subscriptionEndDate = calculateNextPaymentDate(paymentDate, currentPlan);
                                    
                                    // Update user document with cancellation info
                                    db.collection("users").document(userId)
                                        .update("subscriptionStatus", "cancelled")
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Subscription cancelled. You'll have access until " + 
                                                dateFormat.format(subscriptionEndDate), Toast.LENGTH_LONG).show();
                                            loadSubscriptionData(); // Reload data
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to cancel subscription", Toast.LENGTH_SHORT).show();
                                        });
                                } else {
                                    Toast.makeText(this, "Failed to find payment record", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to find payment record", Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        Toast.makeText(this, "No active subscription found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            });
    }
    

    
    private void displayBasicSubscriptionDetails(String currentPlan, String subscriptionStatus) {
        // Set basic subscription details without payment info
        tvPlanName.setText(currentPlan);
        tvPlanPrice.setText(getDefaultPrice(currentPlan));
        tvBillingPeriod.setText("/" + getDefaultBilling(currentPlan));
        
        // Set subscription status based on subscriptionStatus field
        if ("cancelled".equals(subscriptionStatus)) {
            tvSubscriptionStatus.setText("Cancelled");
            
            // Disable and grey out the cancel subscription button
            btnCancelSubscription.setEnabled(false);
            btnCancelSubscription.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
            btnCancelSubscription.setText("Subscription Cancelled");
        } else {
            tvSubscriptionStatus.setText("Active");
            
            // Enable the cancel subscription button
            btnCancelSubscription.setEnabled(true);
            btnCancelSubscription.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
            btnCancelSubscription.setText("Cancel Subscription");
        }
        
        tvPaymentStatus.setText("Completed");
        
        // Set plan features based on plan type
        setPlanFeatures(currentPlan);
        
        // Set payment dates (use current date as payment date)
        Date currentDate = new Date();
        tvPaymentDate.setText(dateFormat.format(currentDate));
        
        // Calculate next payment date
        Date nextPaymentDate = calculateNextPaymentDate(currentDate, currentPlan);
        if (nextPaymentDate != null) {
            if ("cancelled".equals(subscriptionStatus)) {
                tvNextPaymentDate.setText("Expires: " + dateFormat.format(nextPaymentDate));
            } else {
                tvNextPaymentDate.setText(dateFormat.format(nextPaymentDate));
            }
        } else {
            tvNextPaymentDate.setText("Date not available");
        }
        
        // Set payment method details (placeholder)
        tvCardholderName.setText("Payment method not available");
        tvCardNumber.setText("**** **** **** ****");
        
        // Set status colors
        tvSubscriptionStatus.setBackgroundResource(R.drawable.status_background);
        tvSubscriptionStatus.setTextColor(getResources().getColor(android.R.color.white));
    }
    
    private String getDefaultPrice(String planName) {
        switch (planName.toLowerCase()) {
            case "monthly":
                return "$5";
            case "annual":
                return "$48";
            case "semi-annual":
                return "$27";
            default:
                return "$48";
        }
    }
    
    private String getDefaultBilling(String planName) {
        switch (planName.toLowerCase()) {
            case "monthly":
                return "per month";
            case "annual":
                return "per year";
            case "semi-annual":
                return "per 6 months";
            default:
                return "per year";
        }
    }
    
    private void setPlanFeatures(String planName) {
        switch (planName.toLowerCase()) {
            case "monthly":
                tvPlanFeatures.setText("• Basic navigation features\n• Limited route previews\n• Standard support");
                break;
            case "annual":
                tvPlanFeatures.setText("• Real time traffic data\n• Route optimization\n• Priority support\n• Save 20% compared to monthly");
                break;
            case "semi-annual":
                tvPlanFeatures.setText("• All monthly features\n• Discounted rate\n• Early access to new tools\n• Save 10% compared to monthly");
                break;
            default:
                tvPlanFeatures.setText("• Real time traffic data\n• Route optimization\n• Priority support\n• Save 20% compared to monthly");
        }
    }
    
    private Date calculateNextPaymentDate(Date paymentDate, String planName) {
        if (paymentDate == null) {
            return null;
        }
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(paymentDate);
        
        switch (planName.toLowerCase()) {
            case "monthly":
                cal.add(Calendar.MONTH, 1);
                break;
            case "annual":
                cal.add(Calendar.YEAR, 1);
                break;
            case "semi-annual":
                cal.add(Calendar.MONTH, 6);
                break;
            default:
                cal.add(Calendar.YEAR, 1);
        }
        
        return cal.getTime();
    }
}