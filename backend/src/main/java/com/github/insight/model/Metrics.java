package com.github.insight.model;

import com.github.insight.model.enums.TrustLevel;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Metrics {

    private String requestId;
    private float activityScore;
    private float diversityScore;
    private float collaborationScore;
    private float persistenceScore;
    private TrustLevel trustLevel;
    private String notes;
    private LocalDateTime calculatedAt;
    private float confidence;
    private Map<String, String> descriptions;

    public Metrics() {
        this.trustLevel = TrustLevel.HIGH;
        this.notes = "";
        this.descriptions = new HashMap<>();
    }

    public Metrics(String requestId) {
        this();
        this.requestId = requestId;
    }

    public boolean isValid() {
        return validateScore(activityScore)
            && validateScore(diversityScore)
            && validateScore(collaborationScore)
            && validateScore(persistenceScore);
    }

    public boolean validateScore(float score) {
        return score >= 0.0f && score <= 100.0f;
    }

    public boolean isOutlier(float value, float threshold) {
        float avg = getOverallAverage();
        return Math.abs(value - avg) > threshold;
    }

    public TrustLevel calculateTrustLevel() {
        if (hasAnomalies()) return TrustLevel.LIMITED;
        float avg = getOverallAverage();
        if (avg >= 0 && activityScore >= 0 && diversityScore >= 0
            && collaborationScore >= 0 && persistenceScore >= 0) {
            return TrustLevel.HIGH;
        }
        return TrustLevel.PARTIAL;
    }

    public boolean hasAnomalies() {
        if (notes == null) return false;
        String lower = notes.toLowerCase();
        return lower.contains("anomaly") || lower.contains("bot") || lower.contains("outlier");
    }

    public float getConfidence() {
        return switch (trustLevel) {
            case HIGH    -> 1.0f;
            case PARTIAL -> 0.7f;
            case LOW     -> 0.4f;
            case LIMITED -> 0.2f;
        };
    }

    public float getOverallAverage() {
        return (activityScore + diversityScore + collaborationScore + persistenceScore) / 4.0f;
    }

    public int compareWith(Metrics other) {
        return Float.compare(this.getOverallAverage(), other.getOverallAverage());
    }

    public String getRequestId()           { return requestId; }
    public float getActivityScore()        { return activityScore; }
    public float getDiversityScore()       { return diversityScore; }
    public float getCollaborationScore()   { return collaborationScore; }
    public float getPersistenceScore()     { return persistenceScore; }
    public TrustLevel getTrustLevel()      { return trustLevel; }
    public String getNotes()               { return notes; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public Map<String, String> getDescriptions() { return descriptions; }

    public void setRequestId(String requestId)       { this.requestId = requestId; }
    public void setActivityScore(float score)        { this.activityScore = score; }
    public void setDiversityScore(float score)       { this.diversityScore = score; }
    public void setCollaborationScore(float score)   { this.collaborationScore = score; }
    public void setPersistenceScore(float score)     { this.persistenceScore = score; }
    public void setTrustLevel(TrustLevel trustLevel) { this.trustLevel = trustLevel; }
    public void setNotes(String notes)               { this.notes = notes; }
    public void setCalculatedAt(LocalDateTime at)    { this.calculatedAt = at; }
    public void setConfidence(float confidence)      { this.confidence = confidence; }
    public void setDescriptions(Map<String, String> descriptions) { this.descriptions = descriptions; }
    public void addDescription(String metric, String desc) { this.descriptions.put(metric, desc); }
}
