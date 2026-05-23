package com.github.insight.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {

    private String userId;
    private String githubId;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public User() {
        this.userId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public User(String githubId, String email, String displayName, String avatarUrl) {
        this();
        this.githubId = githubId;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }

    public boolean validate() {
        return userId != null && !userId.isBlank()
            && githubId != null && !githubId.isBlank()
            && email != null && !email.isBlank();
    }

    public boolean isSessionValid() {
        return sessionId != null && !sessionId.isBlank();
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public String getUserId()          { return userId; }
    public String getGithubId()        { return githubId; }
    public String getEmail()           { return email; }
    public String getDisplayName()     { return displayName; }
    public String getAvatarUrl()       { return avatarUrl; }
    public String getSessionId()       { return sessionId; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getLastLoginAt()  { return lastLoginAt; }

    public void setUserId(String userId)             { this.userId = userId; }
    public void setGithubId(String githubId)         { this.githubId = githubId; }
    public void setEmail(String email)               { this.email = email; }
    public void setDisplayName(String displayName)   { this.displayName = displayName; }
    public void setAvatarUrl(String avatarUrl)       { this.avatarUrl = avatarUrl; }
    public void setSessionId(String sessionId)       { this.sessionId = sessionId; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
