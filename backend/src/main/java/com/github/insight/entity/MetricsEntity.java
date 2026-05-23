package com.github.insight.entity;

import com.github.insight.model.enums.TrustLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "metrics")
public class MetricsEntity {

    @Id
    private String requestId;

    @Column(nullable = false)
    private float activityScore;

    @Column(nullable = false)
    private float diversityScore;

    @Column(nullable = false)
    private float collaborationScore;

    @Column(nullable = false)
    private float persistenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrustLevel trustLevel = TrustLevel.HIGH;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String notes = "";

    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    private float confidence;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String descriptionsJson;

    public MetricsEntity() {
        this.calculatedAt = LocalDateTime.now();
    }

    public MetricsEntity(String requestId) {
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

    public float getOverallAverage() {
        return (activityScore + diversityScore + collaborationScore + persistenceScore) / 4.0f;
    }

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public float getActivityScore() { return activityScore; }
    public void setActivityScore(float activityScore) { this.activityScore = activityScore; }

    public float getDiversityScore() { return diversityScore; }
    public void setDiversityScore(float diversityScore) { this.diversityScore = diversityScore; }

    public float getCollaborationScore() { return collaborationScore; }
    public void setCollaborationScore(float collaborationScore) { this.collaborationScore = collaborationScore; }

    public float getPersistenceScore() { return persistenceScore; }
    public void setPersistenceScore(float persistenceScore) { this.persistenceScore = persistenceScore; }

    public TrustLevel getTrustLevel() { return trustLevel; }
    public void setTrustLevel(TrustLevel trustLevel) { this.trustLevel = trustLevel; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }

    public String getDescriptionsJson() { return descriptionsJson; }
    public void setDescriptionsJson(String descriptionsJson) { this.descriptionsJson = descriptionsJson; }
}
