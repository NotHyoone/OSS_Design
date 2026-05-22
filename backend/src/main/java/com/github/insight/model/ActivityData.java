package com.github.insight.model;

import java.time.LocalDateTime;
import java.util.*;

public class ActivityData {

    private String requestId;
    private String avatarUrl;
    private List<RepositoryData> repositories;
    private List<CommitData> commits;
    private Map<String, Long> languages;
    private List<PullRequestData> pullRequests;
    private List<IssueData> issues;
    private LocalDateTime collectedAt;

    public ActivityData() {
        this.repositories = new ArrayList<>();
        this.commits = new ArrayList<>();
        this.languages = new HashMap<>();
        this.pullRequests = new ArrayList<>();
        this.issues = new ArrayList<>();
    }

    public ActivityData(String requestId) {
        this();
        this.requestId = requestId;
    }

    public boolean validate() {
        return repositories.size() >= 1 && commits.size() >= 1;
    }

    public boolean isEmpty() {
        return repositories.isEmpty() && commits.isEmpty()
            && pullRequests.isEmpty() && issues.isEmpty();
    }

    public boolean hasEnoughData() {
        return repositories.size() >= 1 && commits.size() >= 1 && languages.size() >= 1;
    }

    public void deduplicate() {
        Set<String> seenCommitIds = new HashSet<>();
        commits.removeIf(c -> !seenCommitIds.add(c.getCommitId()));

        Set<String> seenRepoIds = new HashSet<>();
        repositories.removeIf(r -> !seenRepoIds.add(r.getRepoId()));
    }

    public int getCommitCount() {
        return commits.size();
    }

    public int getLanguageCount() {
        return languages.size();
    }

    public int getTimeRange() {
        if (commits.isEmpty()) return 0;
        LocalDateTime earliest = commits.stream()
            .map(CommitData::getCommittedAt)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        if (earliest == null) return 0;
        return (int) java.time.temporal.ChronoUnit.MONTHS.between(earliest, LocalDateTime.now());
    }

    public int getUniqueRepositoryCount() {
        return (int) repositories.stream().filter(r -> !r.isFork()).count();
    }

    public ActivityData filterByDateRange(LocalDateTime from, LocalDateTime to) {
        ActivityData filtered = new ActivityData(this.requestId);
        filtered.setAvatarUrl(this.avatarUrl);
        this.commits.stream()
            .filter(c -> c.getCommittedAt() != null
                && !c.getCommittedAt().isBefore(from)
                && !c.getCommittedAt().isAfter(to))
            .forEach(filtered::addCommit);
        filtered.getRepositories().addAll(this.repositories);
        filtered.getLanguages().putAll(this.languages);
        return filtered;
    }

    public void addRepository(RepositoryData repo) { repositories.add(repo); }
    public void addCommit(CommitData commit)        { commits.add(commit); }
    public void addPullRequest(PullRequestData pr)  { pullRequests.add(pr); }
    public void addIssue(IssueData issue)           { issues.add(issue); }

    public String getRequestId()         { return requestId; }
    public String getAvatarUrl()         { return avatarUrl; }
    public List<RepositoryData> getRepositories() { return repositories; }
    public List<CommitData> getCommits()          { return commits; }
    public Map<String, Long> getLanguages()       { return languages; }
    public List<PullRequestData> getPullRequests() { return pullRequests; }
    public List<IssueData> getIssues()            { return issues; }
    public LocalDateTime getCollectedAt()         { return collectedAt; }

    public void setRequestId(String requestId)     { this.requestId = requestId; }
    public void setAvatarUrl(String avatarUrl)     { this.avatarUrl = avatarUrl; }
    public void setRepositories(List<RepositoryData> repositories) { this.repositories = repositories; }
    public void setCommits(List<CommitData> commits)               { this.commits = commits; }
    public void setLanguages(Map<String, Long> languages)          { this.languages = languages; }
    public void setPullRequests(List<PullRequestData> pullRequests) { this.pullRequests = pullRequests; }
    public void setIssues(List<IssueData> issues)                  { this.issues = issues; }
    public void setCollectedAt(LocalDateTime collectedAt)          { this.collectedAt = collectedAt; }
}
