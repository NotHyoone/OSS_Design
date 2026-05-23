package com.github.insight.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class IssueData {

    private String issueId;
    private String repoId;
    private String state;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    public IssueData() {}

    public IssueData(String issueId, String repoId, String state,
                     LocalDateTime createdAt, LocalDateTime closedAt) {
        this.issueId = issueId;
        this.repoId = repoId;
        this.state = state;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
    }

    public boolean isOwned() {
        return true;
    }

    public int getDaysOpen() {
        if (createdAt == null) return 0;
        LocalDateTime end = closedAt != null ? closedAt : LocalDateTime.now();
        return (int) ChronoUnit.DAYS.between(createdAt, end);
    }

    public String getIssueId()         { return issueId; }
    public String getRepoId()          { return repoId; }
    public String getState()           { return state; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getClosedAt()  { return closedAt; }

    public void setIssueId(String issueId)           { this.issueId = issueId; }
    public void setRepoId(String repoId)             { this.repoId = repoId; }
    public void setState(String state)               { this.state = state; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setClosedAt(LocalDateTime closedAt)   { this.closedAt = closedAt; }
}
