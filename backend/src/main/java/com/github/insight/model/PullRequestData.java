package com.github.insight.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class PullRequestData {

    private String prId;
    private String repoId;
    private String state;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private boolean merged;

    public PullRequestData() {}

    public PullRequestData(String prId, String repoId, String state,
                           LocalDateTime createdAt, LocalDateTime closedAt, boolean merged) {
        this.prId = prId;
        this.repoId = repoId;
        this.state = state;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
        this.merged = merged;
    }

    public boolean isOwned() {
        return true;
    }

    public int getDaysOpen() {
        if (createdAt == null) return 0;
        LocalDateTime end = closedAt != null ? closedAt : LocalDateTime.now();
        return (int) ChronoUnit.DAYS.between(createdAt, end);
    }

    public String getPrId()            { return prId; }
    public String getRepoId()          { return repoId; }
    public String getState()           { return state; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getClosedAt()  { return closedAt; }
    public boolean isMerged()          { return merged; }

    public void setPrId(String prId)                 { this.prId = prId; }
    public void setRepoId(String repoId)             { this.repoId = repoId; }
    public void setState(String state)               { this.state = state; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setClosedAt(LocalDateTime closedAt)   { this.closedAt = closedAt; }
    public void setMerged(boolean merged)            { this.merged = merged; }
}
