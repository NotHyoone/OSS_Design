package com.github.insight.entity;

import com.github.insight.model.enums.DeveloperType;
import com.github.insight.model.enums.TrustLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analysis_results")
public class AnalysisResultEntity {

    @Id
    private String resultId;

    @Column(nullable = false)
    private String requestId;

    private String userId;

    @Column(nullable = false)
    private String githubId;

    private String avatarUrl;

    @Column(nullable = false)
    private int totalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeveloperType developerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrustLevel trustLevel;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String strengthsJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String improvementsJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String ruleVersion = "1.0";

    public AnalysisResultEntity() {
        this.resultId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGithubId() { return githubId; }
    public void setGithubId(String githubId) { this.githubId = githubId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public DeveloperType getDeveloperType() { return developerType; }
    public void setDeveloperType(DeveloperType developerType) { this.developerType = developerType; }

    public TrustLevel getTrustLevel() { return trustLevel; }
    public void setTrustLevel(TrustLevel trustLevel) { this.trustLevel = trustLevel; }

    public String getStrengthsJson() { return strengthsJson; }
    public void setStrengthsJson(String strengthsJson) { this.strengthsJson = strengthsJson; }

    public String getImprovementsJson() { return improvementsJson; }
    public void setImprovementsJson(String improvementsJson) { this.improvementsJson = improvementsJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
}
