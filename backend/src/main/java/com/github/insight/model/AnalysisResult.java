package com.github.insight.model;

import com.github.insight.model.enums.DeveloperType;
import com.github.insight.model.enums.TrustLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AnalysisResult {

    private String resultId;
    private String requestId;
    private String userId;
    private String githubId;
    private String avatarUrl;
    private int totalScore;
    private DeveloperType developerType;
    private TrustLevel trustLevel;
    private List<String> strengths;
    private List<FeedbackItem> improvements;
    private LocalDateTime createdAt;
    private String ruleVersion;

    public AnalysisResult() {
        this.resultId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.ruleVersion = "1.0";
    }

    public String getSummary() {
        String typeLabel = developerType != null ? developerType.name() : "UNKNOWN";
        return String.format("%s Developer – %d/100",
            capitalize(typeLabel), totalScore);
    }

    public boolean hasLowTrust() {
        return trustLevel == TrustLevel.LOW || trustLevel == TrustLevel.LIMITED;
    }

    public boolean validate() {
        return totalScore >= 0 && totalScore <= 100
            && githubId != null && !githubId.isBlank()
            && developerType != null
            && trustLevel != null
            && strengths != null && !strengths.isEmpty()
            && improvements != null && !improvements.isEmpty();
    }

    public int compareTo(AnalysisResult other) {
        return Integer.compare(this.totalScore, other.totalScore);
    }

    public int calculateDelta(AnalysisResult previous) {
        return this.totalScore - previous.totalScore;
    }

    public FeedbackItem getTopImprovement() {
        if (improvements == null || improvements.isEmpty()) return null;
        return improvements.stream()
            .min((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
            .orElse(null);
    }

    public int getStrengthCount()     { return strengths != null ? strengths.size() : 0; }
    public int getImprovementCount()  { return improvements != null ? improvements.size() : 0; }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    public String getResultId()      { return resultId; }
    public String getRequestId()     { return requestId; }
    public String getUserId()        { return userId; }
    public String getGithubId()      { return githubId; }
    public String getAvatarUrl()     { return avatarUrl; }
    public int getTotalScore()       { return totalScore; }
    public DeveloperType getDeveloperType() { return developerType; }
    public TrustLevel getTrustLevel()      { return trustLevel; }
    public List<String> getStrengths()     { return strengths; }
    public List<FeedbackItem> getImprovements() { return improvements; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public String getRuleVersion()         { return ruleVersion; }

    public void setResultId(String resultId)       { this.resultId = resultId; }
    public void setRequestId(String requestId)     { this.requestId = requestId; }
    public void setUserId(String userId)           { this.userId = userId; }
    public void setGithubId(String githubId)       { this.githubId = githubId; }
    public void setAvatarUrl(String avatarUrl)     { this.avatarUrl = avatarUrl; }
    public void setTotalScore(int totalScore)      { this.totalScore = totalScore; }
    public void setDeveloperType(DeveloperType developerType) { this.developerType = developerType; }
    public void setTrustLevel(TrustLevel trustLevel)         { this.trustLevel = trustLevel; }
    public void setStrengths(List<String> strengths)         { this.strengths = strengths; }
    public void setImprovements(List<FeedbackItem> improvements) { this.improvements = improvements; }
    public void setCreatedAt(LocalDateTime createdAt)        { this.createdAt = createdAt; }
    public void setRuleVersion(String ruleVersion)           { this.ruleVersion = ruleVersion; }
}
