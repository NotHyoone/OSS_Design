package com.github.insight.model;

import java.time.LocalDateTime;

public class RepositoryData {

    private String repoId;
    private String name;
    private String language;
    private int starCount;
    private boolean fork;
    private LocalDateTime lastUpdatedAt;

    public RepositoryData() {}

    public RepositoryData(String repoId, String name, String language,
                          int starCount, boolean fork, LocalDateTime lastUpdatedAt) {
        this.repoId = repoId;
        this.name = name;
        this.language = language;
        this.starCount = starCount;
        this.fork = fork;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public boolean isOwned() {
        return !fork;
    }

    public float getActivityScore() {
        float starFactor = Math.min(1.0f, starCount / 50.0f);
        float recencyFactor = 0.0f;
        if (lastUpdatedAt != null) {
            long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(lastUpdatedAt, LocalDateTime.now());
            recencyFactor = Math.max(0.0f, 1.0f - daysAgo / 365.0f);
        }
        return (starFactor * 0.3f + recencyFactor * 0.7f);
    }

    public String getRepoId()          { return repoId; }
    public String getName()            { return name; }
    public String getLanguage()        { return language; }
    public int getStarCount()          { return starCount; }
    public boolean isFork()            { return fork; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }

    public void setRepoId(String repoId)             { this.repoId = repoId; }
    public void setName(String name)                 { this.name = name; }
    public void setLanguage(String language)         { this.language = language; }
    public void setStarCount(int starCount)          { this.starCount = starCount; }
    public void setFork(boolean fork)                { this.fork = fork; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
