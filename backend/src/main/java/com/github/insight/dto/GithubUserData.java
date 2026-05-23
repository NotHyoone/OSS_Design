package com.github.insight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub REST API /users/{username} 응답 매핑
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubUserData(
        String login,
        long id,
        @JsonProperty("avatar_url")   String avatarUrl,
        @JsonProperty("public_repos") int publicRepos,
        int followers,
        int following,
        @JsonProperty("created_at")   String createdAt
) {}
