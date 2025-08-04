package com.example.bottlenex;

/**
 * SubscriptionPlan represents a subscription plan with its details.
 * Used for displaying plan options and managing subscription data.
 */
public class SubscriptionPlan {
    private String id;
    private String title;
    private String description;
    private String price;
    private String billingPeriod;
    private String[] features;
    private boolean isPopular;
    private boolean isSelected;

    public SubscriptionPlan() {
        // Default constructor
    }

    public SubscriptionPlan(String id, String title, String description, String price, 
                           String billingPeriod, String[] features, boolean isPopular) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.billingPeriod = billingPeriod;
        this.features = features;
        this.isPopular = isPopular;
        this.isSelected = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public String[] getFeatures() {
        return features;
    }

    public void setFeatures(String[] features) {
        this.features = features;
    }

    public boolean isPopular() {
        return isPopular;
    }

    public void setPopular(boolean popular) {
        isPopular = popular;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
} 