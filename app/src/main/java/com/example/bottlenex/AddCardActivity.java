package com.example.bottlenex;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * AddCardActivity allows users to add a new payment method and subscribe to a plan.
 * Handles card validation and processes subscription payments like the landing page.
 */
public class AddCardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private EditText etCardNumber, etCardholderName, etExpiryMonth, etExpiryYear, etCvv;
    private Button btnAddCard;
    private TextView tvCardNumberError, tvCardholderError, tvExpiryError, tvCvvError;
    
    // Subscription plan details
    private String planId, planName, planPrice, billingPeriod;
    private TextView tvPlanTitle, tvPlanPrice, tvPlanBilling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get plan details from intent
        getPlanDetailsFromIntent();

        // Initialize views
        initializeViews();

        // Set up text watchers for validation
        setupTextWatchers();

        // Set click listener for add card button
        btnAddCard.setOnClickListener(v -> {
            validateAndAddCard();
        });

        // Set up back button
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
        });
    }

    private void getPlanDetailsFromIntent() {
        Intent intent = getIntent();
        planId = intent.getStringExtra("plan_id");
        planName = intent.getStringExtra("plan_name");
        planPrice = intent.getStringExtra("plan_price");
        billingPeriod = intent.getStringExtra("billing_period");
        
        // If no plan details, this is just adding a card (not subscribing)
        if (planId == null) {
            planId = "none";
            planName = "Add Payment Method";
            planPrice = "";
            billingPeriod = "";
        }
    }

    private void initializeViews() {
        etCardNumber = findViewById(R.id.etCardNumber);
        etCardholderName = findViewById(R.id.etCardholderName);
        etExpiryMonth = findViewById(R.id.etExpiryMonth);
        etExpiryYear = findViewById(R.id.etExpiryYear);
        etCvv = findViewById(R.id.etCvv);
        btnAddCard = findViewById(R.id.btnAddCard);

        tvCardNumberError = findViewById(R.id.tvCardNumberError);
        tvCardholderError = findViewById(R.id.tvCardholderError);
        tvExpiryError = findViewById(R.id.tvExpiryError);
        tvCvvError = findViewById(R.id.tvCvvError);
        
        // Initialize plan display views
        tvPlanTitle = findViewById(R.id.tvPlanTitle);
        tvPlanPrice = findViewById(R.id.tvPlanPrice);
        tvPlanBilling = findViewById(R.id.tvPlanBilling);
        
        // Show/hide plan summary based on whether this is a subscription
        View planSummaryContainer = findViewById(R.id.planSummaryContainer);
        if (planSummaryContainer != null) {
            if ("change_payment".equals(planId)) {
                // This is changing payment method - hide plan summary
                planSummaryContainer.setVisibility(View.GONE);
                btnAddCard.setText("Update Payment Method");
            } else if (!"none".equals(planId)) {
                // This is a subscription - show plan summary
                planSummaryContainer.setVisibility(View.VISIBLE);
                
                // Update plan display
                if (tvPlanTitle != null) {
                    tvPlanTitle.setText(planName);
                }
                if (tvPlanPrice != null) {
                    tvPlanPrice.setText(planPrice);
                }
                if (tvPlanBilling != null) {
                    tvPlanBilling.setText(billingPeriod);
                }
                
                // Update button text
                btnAddCard.setText("Subscribe & Pay " + planPrice);
            } else {
                // This is just adding a card - hide plan summary
                planSummaryContainer.setVisibility(View.GONE);
            }
        }
    }

    private void setupTextWatchers() {
        // Card number formatting
        etCardNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String cardNumber = s.toString().replaceAll("\\s", "");
                if (cardNumber.length() > 16) {
                    cardNumber = cardNumber.substring(0, 16);
                }
                
                // Format card number with spaces
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < cardNumber.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(cardNumber.charAt(i));
                }
                
                if (!s.toString().equals(formatted.toString())) {
                    etCardNumber.setText(formatted.toString());
                    etCardNumber.setSelection(formatted.length());
                }
                
                validateCardNumber();
            }
        });

        // Expiry month validation
        etExpiryMonth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String month = s.toString();
                if (month.length() == 1 && Integer.parseInt(month) > 1) {
                    etExpiryMonth.setText("0" + month);
                    etExpiryMonth.setSelection(2);
                } else if (month.length() == 2) {
                    int monthInt = Integer.parseInt(month);
                    if (monthInt < 1 || monthInt > 12) {
                        etExpiryMonth.setText(month.substring(0, 1));
                        etExpiryMonth.setSelection(1);
                    }
                }
                validateExpiry();
            }
        });

        // Expiry year validation
        etExpiryYear.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateExpiry();
            }
        });

        // CVV validation
        etCvv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateCvv();
            }
        });
    }

    private void validateAndAddCard() {
        // Clear previous errors
        clearErrors();

        boolean isValid = true;
        String errorMessage = "";

        // Validate card number
        if (!validateCardNumber()) {
            isValid = false;
            errorMessage += "Card number error. ";
        }

        // Validate cardholder name
        if (!validateCardholderName()) {
            isValid = false;
            errorMessage += "Cardholder name error. ";
        }

        // Validate expiry date
        if (!validateExpiry()) {
            isValid = false;
            errorMessage += "Expiry date error. ";
        }

        // Validate CVV
        if (!validateCvv()) {
            isValid = false;
            errorMessage += "CVV error. ";
        }

        if (isValid) {
            Toast.makeText(this, "Verifying Payment Credentials...", Toast.LENGTH_SHORT).show();
            addCardToFirebase();
        } else {
            Toast.makeText(this, "Validation failed: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private boolean validateCardNumber() {
        String cardNumber = etCardNumber.getText().toString().replaceAll("\\s", "");
        
        if (cardNumber.isEmpty()) {
            tvCardNumberError.setText("Card number is required");
            tvCardNumberError.setVisibility(View.VISIBLE);
            return false;
        }
        
        // Match landing page validation: exactly 16 digits
        if (!cardNumber.matches("^\\d{16}$")) {
            tvCardNumberError.setText("Please enter a valid 16-digit card number");
            tvCardNumberError.setVisibility(View.VISIBLE);
            return false;
        }
        
        tvCardNumberError.setVisibility(View.GONE);
        return true;
    }

    private boolean validateCardholderName() {
        String name = etCardholderName.getText().toString().trim();
        
        if (name.isEmpty()) {
            tvCardholderError.setText("Please enter the cardholder name");
            tvCardholderError.setVisibility(View.VISIBLE);
            return false;
        }
        
        tvCardholderError.setVisibility(View.GONE);
        return true;
    }

    private boolean validateExpiry() {
        String month = etExpiryMonth.getText().toString();
        String year = etExpiryYear.getText().toString();
        
        if (month.isEmpty() || year.isEmpty()) {
            tvExpiryError.setText("Expiry date is required");
            tvExpiryError.setVisibility(View.VISIBLE);
            return false;
        }
        
        // Match landing page validation: MM/YY format
        String expiryString = month + "/" + year;
        if (!expiryString.matches("^(0[1-9]|1[0-2])/([0-9]{2})$")) {
            tvExpiryError.setText("Please enter expiry date in MM/YY format");
            tvExpiryError.setVisibility(View.VISIBLE);
            return false;
        }
        
        try {
            int monthInt = Integer.parseInt(month);
            int yearInt = Integer.parseInt(year);
            
            // Check if card is expired
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentYear = cal.get(java.util.Calendar.YEAR) % 100;
            int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;
            
            if (yearInt < currentYear || (yearInt == currentYear && monthInt < currentMonth)) {
                tvExpiryError.setText("Card has expired");
                tvExpiryError.setVisibility(View.VISIBLE);
                return false;
            }
        } catch (NumberFormatException e) {
            tvExpiryError.setText("Invalid expiry date numbers");
            tvExpiryError.setVisibility(View.VISIBLE);
            return false;
        }
        
        tvExpiryError.setVisibility(View.GONE);
        return true;
    }

    private boolean validateCvv() {
        String cvv = etCvv.getText().toString();
        
        if (cvv.isEmpty()) {
            tvCvvError.setText("CVV is required");
            tvCvvError.setVisibility(View.VISIBLE);
            return false;
        }
        
        // Match landing page validation: 3-4 digits
        if (!cvv.matches("^\\d{3,4}$")) {
            tvCvvError.setText("Please enter a valid CVV (3-4 digits)");
            tvCvvError.setVisibility(View.VISIBLE);
            return false;
        }
        
        tvCvvError.setVisibility(View.GONE);
        return true;
    }

    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
    }

    private void clearErrors() {
        tvCardNumberError.setVisibility(View.GONE);
        tvCardholderError.setVisibility(View.GONE);
        tvExpiryError.setVisibility(View.GONE);
        tvCvvError.setVisibility(View.GONE);
    }

    private void addCardToFirebase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add a card", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddCard.setEnabled(false);
        String originalButtonText = btnAddCard.getText().toString();
        btnAddCard.setText("Processing Payment...");

        String cardNumber = etCardNumber.getText().toString().replaceAll("\\s", "");
        String cardholderName = etCardholderName.getText().toString().trim();
        String expiryMonth = etExpiryMonth.getText().toString();
        String expiryYear = etExpiryYear.getText().toString();
        String cvv = etCvv.getText().toString();

        // Simulate payment processing delay (like in PaymentModal.jsx)
        new android.os.Handler().postDelayed(() -> {
            processSubscription(currentUser, cardNumber, cardholderName, originalButtonText);
        }, 2000); // 2 second delay
    }
    
    private void processSubscription(FirebaseUser currentUser, String cardNumber, String cardholderName, String originalButtonText) {
        String userId = currentUser.getUid();
        
        try {
            // First, check if user document exists, if not create it
            db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Create user document first
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", currentUser.getEmail());
                        userData.put("userType", "Free");
                        userData.put("createdAt", new java.util.Date());
                        
                        db.collection("users").document(userId).set(userData)
                            .addOnSuccessListener(aVoid -> {
                                processPaymentData(userId, currentUser, cardNumber, cardholderName, originalButtonText);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to create user document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnAddCard.setEnabled(true);
                                btnAddCard.setText(originalButtonText);
                            });
                    } else {
                        // User document exists, proceed with payment
                        processPaymentData(userId, currentUser, cardNumber, cardholderName, originalButtonText);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check user document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnAddCard.setEnabled(true);
                    btnAddCard.setText(originalButtonText);
                });
                
        } catch (Exception e) {
            Toast.makeText(this, "Payment processing failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnAddCard.setEnabled(true);
            btnAddCard.setText(originalButtonText);
        }
    }
    
    private void processPaymentData(String userId, FirebaseUser currentUser, String cardNumber, String cardholderName, String originalButtonText) {
        if ("change_payment".equals(planId)) {
            // Handle payment method change
            updatePaymentMethod(userId, currentUser, cardNumber, cardholderName, originalButtonText);
        } else {
            // Handle new subscription
            processNewSubscription(userId, currentUser, cardNumber, cardholderName, originalButtonText);
        }
    }
    
    private void updatePaymentMethod(String userId, FirebaseUser currentUser, String cardNumber, String cardholderName, String originalButtonText) {
        // Update the most recent payment record with new payment method (standardized approach)
        db.collection("users").document(userId).collection("payments")
            .orderBy("paymentDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    String paymentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("cardholderName", cardholderName);
                    updateData.put("cardNumber", "**** **** **** " + cardNumber.substring(cardNumber.length() - 4));
                    updateData.put("lastUpdated", new java.util.Date());
                    
                    // Also update user document to set subscription status back to active
                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("subscriptionStatus", "active");
                    
                    db.collection("users").document(userId).collection("payments")
                        .document(paymentId)
                        .update(updateData)
                        .addOnSuccessListener(aVoid -> {
                            // Also update user document to set subscription status back to active
                            db.collection("users").document(userId)
                                .update(userUpdate)
                                .addOnSuccessListener(userUpdateVoid -> {
                                    Toast.makeText(this, "Payment Method Updated Successfully!", Toast.LENGTH_LONG).show();
                                    
                                    // Navigate back to PaymentActivity
                                    Intent intent = new Intent(this, PaymentActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Payment method updated but failed to update subscription status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    btnAddCard.setEnabled(true);
                                    btnAddCard.setText(originalButtonText);
                                });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to update payment method: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            btnAddCard.setEnabled(true);
                            btnAddCard.setText(originalButtonText);
                        });
                } else {
                    Toast.makeText(this, "No payment record found", Toast.LENGTH_SHORT).show();
                    btnAddCard.setEnabled(true);
                    btnAddCard.setText(originalButtonText);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to find payment record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnAddCard.setEnabled(true);
                btnAddCard.setText(originalButtonText);
            });
    }
    
    private void processNewSubscription(String userId, FirebaseUser currentUser, String cardNumber, String cardholderName, String originalButtonText) {
        // 1. Store payment info in Firebase (exactly like PaymentModal.jsx)
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("userId", userId);
        paymentData.put("userEmail", currentUser.getEmail());
        paymentData.put("planTitle", planName);
        paymentData.put("planPrice", planPrice);
        paymentData.put("planBilling", billingPeriod);
        paymentData.put("cardNumber", "**** **** **** " + cardNumber.substring(cardNumber.length() - 4));
        paymentData.put("cardholderName", cardholderName);
        paymentData.put("paymentDate", new java.util.Date());
        paymentData.put("status", "completed");

        // Save payment info to users/{userId}/payments collection (like PaymentModal.jsx)
        db.collection("users").document(userId).collection("payments")
            .add(paymentData)
            .addOnSuccessListener(documentReference -> {
                // 2. Update user status to Premium (exactly like PaymentModal.jsx)
                Map<String, Object> userUpdate = new HashMap<>();
                userUpdate.put("userType", "Premium");
                userUpdate.put("lastPayment", new java.util.Date());
                userUpdate.put("currentPlan", planName);
                userUpdate.put("subscriptionStatus", "active");
                
                db.collection("users").document(userId)
                    .update(userUpdate)
                    .addOnSuccessListener(aVoid -> {
                        // 3. Log successful subscription (exactly like PaymentModal.jsx)
                        Map<String, Object> subscriptionLog = new HashMap<>();
                        subscriptionLog.put("userId", userId);
                        subscriptionLog.put("userEmail", currentUser.getEmail());
                        subscriptionLog.put("userName", currentUser.getDisplayName() != null ? 
                            currentUser.getDisplayName() : currentUser.getEmail());
                        subscriptionLog.put("planTitle", planName);
                        subscriptionLog.put("planPrice", planPrice);
                        subscriptionLog.put("planBilling", billingPeriod);
                        subscriptionLog.put("timestamp", new java.util.Date());
                        subscriptionLog.put("status", "completed");
                        
                        // Use the same collection as landing page: subscription_attempts
                        db.collection("subscription_attempts")
                            .add(subscriptionLog)
                            .addOnSuccessListener(logRef -> {
                                // Subscription successful! (No need to create separate subscription record)
                                Toast.makeText(this, "Subscription Successful!", Toast.LENGTH_LONG).show();
                                
                                // Navigate back to ProfileActivity to show updated subscription status
                                Intent intent = new Intent(this, ProfileActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Subscription logged but failed to save details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnAddCard.setEnabled(true);
                                btnAddCard.setText(originalButtonText);
                            });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Payment processed but failed to update user status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnAddCard.setEnabled(true);
                        btnAddCard.setText(originalButtonText);
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to process payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnAddCard.setEnabled(true);
                btnAddCard.setText(originalButtonText);
            });
    }
    

    
    private java.util.Date calculateNextPaymentDate() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        
        switch (planId) {
            case "monthly":
                cal.add(java.util.Calendar.MONTH, 1);
                break;
            case "annual":
                cal.add(java.util.Calendar.YEAR, 1);
                break;
            case "semi_annual":
                cal.add(java.util.Calendar.MONTH, 6);
                break;
            default:
                cal.add(java.util.Calendar.YEAR, 1);
        }
        
        return cal.getTime();
    }
} 