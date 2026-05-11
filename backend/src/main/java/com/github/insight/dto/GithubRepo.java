package com.github.insight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub REST API /users/{username}/repos 배열 항목 매핑
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubRepo(
        String name,
        boolean fork,
        String language,
        @JsonProperty("forks_count")       int forksCount,
        @JsonProperty("open_issues_count") int openIssuesCount,
        @JsonProperty("stargazers_count")  int stargazersCount,
        @JsonProperty("updated_at")        String updatedAt,
        @JsonProperty("pushed_at")         String pushedAt,
        @JsonProperty("created_at")        String createdAt
) {}
