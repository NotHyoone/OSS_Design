package com.github.insight.model;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class GithubProfile {

    private static final Pattern VALID_GITHUB_ID =
        Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,37}[a-zA-Z0-9]?$");

    private String githubId;
    private String displayName;
    private String avatarUrl;
    private boolean validated;
    private LocalDateTime createdAt;

    public GithubProfile(String githubId) {
        this.githubId = githubId;
        this.displayName = "";
        this.avatarUrl = "";
        this.validated = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return validated && githubId != null && VALID_GITHUB_ID.matcher(githubId).matches();
    }

    public String getGithubId()        { return githubId; }
    public String getDisplayName()     { return displayName; }
    public String getAvatarUrl()       { return avatarUrl; }
    public boolean isValidated()       { return validated; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setGithubId(String githubId)         { this.githubId = githubId; }
    public void setDisplayName(String displayName)   { this.displayName = displayName; }
    public void setAvatarUrl(String avatarUrl)       { this.avatarUrl = avatarUrl; }
    public void setValidated(boolean validated)      { this.validated = validated; }
}
