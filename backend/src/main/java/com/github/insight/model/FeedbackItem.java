package com.github.insight.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class FeedbackItem {

    private String itemId;
    private String category;
    private String description;
    private String actionGuide;
    private int priority;
    private float confidence;
    private LocalDateTime createdAt;

    private static final String VERSION = "1.0";

    public FeedbackItem() {
        this.itemId = UUID.randomUUID().toString();
        this.priority = 1;
        this.confidence = 0.5f;
        this.createdAt = LocalDateTime.now();
    }

    public FeedbackItem(String category, String description, String actionGuide, int priority) {
        this();
        this.category = category;
        this.description = description;
        this.actionGuide = actionGuide;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return String.format("[%d] [%s] %s", priority, category, actionGuide);
    }

    public boolean isHighPriority() {
        return priority <= 2;
    }

    public String getId() {
        return itemId;
    }

    public String getSource() {
        return "FeedbackGenerator v" + VERSION;
    }

    public String getItemId()      { return itemId; }
    public String getCategory()    { return category; }
    public String getDescription() { return description; }
    public String getActionGuide() { return actionGuide; }
    public int getPriority()       { return priority; }
    public float getConfidence()   { return confidence; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setItemId(String itemId)         { this.itemId = itemId; }
    public void setCategory(String category)     { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setActionGuide(String actionGuide) { this.actionGuide = actionGuide; }
    public void setPriority(int priority)        { this.priority = priority; }
    public void setConfidence(float confidence)  { this.confidence = confidence; }
}
