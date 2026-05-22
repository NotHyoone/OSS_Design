package com.github.insight.model;

import java.time.LocalDateTime;

public class CommitData {

    private String commitId;
    private String repoId;
    private LocalDateTime committedAt;
    private String message;

    public CommitData() {}

    public CommitData(String commitId, String repoId, LocalDateTime committedAt, String message) {
        this.commitId = commitId;
        this.repoId = repoId;
        this.committedAt = committedAt;
        this.message = message != null ? message : "";
    }

    public boolean isBot() {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("[bot]")
            || lower.contains("automated")
            || lower.startsWith("merge pull request");
    }

    public boolean isMerge() {
        if (message == null) return false;
        return message.toLowerCase().startsWith("merge");
    }

    public String getCommitId()        { return commitId; }
    public String getRepoId()          { return repoId; }
    public LocalDateTime getCommittedAt() { return committedAt; }
    public String getMessage()         { return message; }

    public void setCommitId(String commitId)         { this.commitId = commitId; }
    public void setRepoId(String repoId)             { this.repoId = repoId; }
    public void setCommittedAt(LocalDateTime committedAt) { this.committedAt = committedAt; }
    public void setMessage(String message)           { this.message = message; }
}
