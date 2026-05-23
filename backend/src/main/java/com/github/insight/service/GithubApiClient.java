package com.github.insight.service;

import com.github.insight.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GithubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GithubApiClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "https://api.github.com";

    @Value("${github.token:}")
    private String token;

    private final AtomicInteger rateLimitRemaining = new AtomicInteger(5000);
    /** GitHub X-RateLimit-Reset 헤더값 (Unix epoch 초 단위) */
    private final AtomicLong rateLimitResetEpoch = new AtomicLong(0L);

    public ActivityData collectAll(String githubId) {
        checkRateLimit();
        ActivityData data = new ActivityData();

        Map<String, Object> userProfile = fetchUserProfile(githubId);
        if (userProfile != null) {
            data.setAvatarUrl((String) userProfile.getOrDefault("avatar_url", ""));
        }

        List<RepositoryData> repos = getRepositories(githubId);
        repos.forEach(data::addRepository);

        Map<String, Long> langs = new HashMap<>();
        for (RepositoryData repo : repos) {
            if (repo.getLanguage() != null && !repo.getLanguage().isBlank()) {
                langs.merge(repo.getLanguage(), 1L, Long::sum);
            }
        }
        data.setLanguages(langs);

        List<Map<String, Object>> events = fetchRawEvents(githubId);
        parseEventsIntoData(events, repos, data);

        data.setCollectedAt(LocalDateTime.now());
        data.deduplicate();
        return data;
    }

    public List<RepositoryData> getRepositories(String githubId) {
        String encodedId = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        String url = baseUrl + "/users/" + encodedId + "/repos?per_page=100&sort=updated";
        try {
            ResponseEntity<List> resp = restTemplate.exchange(
                url, HttpMethod.GET, buildHeaders(), List.class);
            List<Map<String, Object>> body = resp.getBody();
            if (body == null) return Collections.emptyList();

            List<RepositoryData> result = new ArrayList<>();
            for (Map<String, Object> r : body) {
                RepositoryData repo = new RepositoryData();
                repo.setRepoId(String.valueOf(r.getOrDefault("full_name", "")));
                repo.setName(String.valueOf(r.getOrDefault("name", "")));
                repo.setLanguage((String) r.get("language"));
                Number stars = (Number) r.getOrDefault("stargazers_count", 0);
                repo.setStarCount(stars.intValue());
                repo.setFork(Boolean.TRUE.equals(r.get("fork")));
                String pushedAt = (String) r.get("pushed_at");
                if (pushedAt != null) {
                    try {
                        repo.setLastUpdatedAt(
                            Instant.parse(pushedAt).atZone(ZoneOffset.UTC).toLocalDateTime());
                    } catch (DateTimeParseException e) {
                        log.debug("저장소 마지막 업데이트 날짜 파싱 실패 ({}): {}", githubId, e.getMessage());
                    }
                }
                result.add(repo);
            }
            updateRateLimit(resp.getHeaders());
            return result;
        } catch (Exception e) {
            extractAndThrow429(e);
            log.warn("저장소 조회 실패 ({}): {}", githubId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<CommitData> getCommits(String githubId, String repoName) {
        String encodedId = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        String encodedRepo = URLEncoder.encode(repoName, StandardCharsets.UTF_8);
        String url = baseUrl + "/repos/" + encodedId + "/" + encodedRepo
            + "/commits?per_page=100&author=" + encodedId;
        try {
            ResponseEntity<List> resp = restTemplate.exchange(
                url, HttpMethod.GET, buildHeaders(), List.class);
            List<Map<String, Object>> body = resp.getBody();
            if (body == null) return Collections.emptyList();

            List<CommitData> result = new ArrayList<>();
            for (Map<String, Object> c : body) {
                CommitData commit = parseCommit(c, githubId + "/" + repoName);
                if (commit != null) result.add(commit);
            }
            updateRateLimit(resp.getHeaders());
            return result;
        } catch (Exception e) {
            extractAndThrow429(e);
            log.warn("커밋 조회 실패 ({}/{}): {}", githubId, repoName, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Long> getLanguages(String githubId, String repoName) {
        String encodedId = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        String encodedRepo = URLEncoder.encode(repoName, StandardCharsets.UTF_8);
        String url = baseUrl + "/repos/" + encodedId + "/" + encodedRepo + "/languages";
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                url, HttpMethod.GET, buildHeaders(), Map.class);
            updateRateLimit(resp.getHeaders());
            Map<String, Object> body = resp.getBody();
            if (body == null) return Collections.emptyMap();
            Map<String, Long> result = new HashMap<>();
            body.forEach((k, v) -> result.put(k, ((Number) v).longValue()));
            return result;
        } catch (Exception e) {
            extractAndThrow429(e);
            log.warn("언어 정보 조회 실패 ({}/{}): {}", githubId, repoName, e.getMessage());
            return Collections.emptyMap();
        }
    }

    public List<PullRequestData> getPullRequests(String githubId) {
        String encodedId = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        String url = baseUrl + "/search/issues?q=author:" + encodedId
            + "+type:pr&sort=updated&order=desc&per_page=100";
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                url, HttpMethod.GET, buildHeaders(), Map.class);
            updateRateLimit(resp.getHeaders());
            Map<String, Object> body = resp.getBody();
            if (body == null) return Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null) return Collections.emptyList();

            List<PullRequestData> result = new ArrayList<>();
            for (Map<String, Object> item : items) {
                PullRequestData pr = new PullRequestData();
                pr.setPrId(String.valueOf(item.getOrDefault("id", UUID.randomUUID())));

                @SuppressWarnings("unchecked")
                Map<String, Object> repoInfo = (Map<String, Object>) item.get("repository_url");
                String repoName = repoInfo != null ? (String) repoInfo.get("name")
                    : String.valueOf(item.getOrDefault("repository_url", ""));
                pr.setRepoId(repoName);

                pr.setState((String) item.getOrDefault("state", "open"));

                String createdAtStr = (String) item.get("created_at");
                if (createdAtStr != null) {
                    try {
                        pr.setCreatedAt(Instant.parse(createdAtStr).atZone(ZoneOffset.UTC).toLocalDateTime());
                    } catch (DateTimeParseException ignored) {}
                }

                String closedAtStr = (String) item.get("closed_at");
                if (closedAtStr != null) {
                    try {
                        pr.setClosedAt(Instant.parse(closedAtStr).atZone(ZoneOffset.UTC).toLocalDateTime());
                    } catch (DateTimeParseException ignored) {}
                }

                pr.setMerged(item.get("pull_request") != null);
                result.add(pr);
            }
            return result;
        } catch (Exception e) {
            extractAndThrow429(e);
            log.warn("Pull Request 조회 실패 ({}): {}", githubId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<IssueData> getIssues(String githubId) {
        String encodedId = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        String url = baseUrl + "/search/issues?q=author:" + encodedId
            + "+type:issue&sort=updated&order=desc&per_page=100";
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                url, HttpMethod.GET, buildHeaders(), Map.class);
            updateRateLimit(resp.getHeaders());
            Map<String, Object> body = resp.getBody();
            if (body == null) return Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null) return Collections.emptyList();

            List<IssueData> result = new ArrayList<>();
            for (Map<String, Object> item : items) {
                IssueData issue = new IssueData();
                issue.setIssueId(String.valueOf(item.getOrDefault("id", UUID.randomUUID())));

                String repoUrl = (String) item.get("repository_url");
                String repoName = repoUrl != null && repoUrl.contains("/")
                    ? repoUrl.substring(repoUrl.lastIndexOf("/") + 1) : "unknown";
                issue.setRepoId(repoName);

                issue.setState((String) item.getOrDefault("state", "open"));

                String createdAtStr = (String) item.get("created_at");
                if (createdAtStr != null) {
                    try {
                        issue.setCreatedAt(Instant.parse(createdAtStr).atZone(ZoneOffset.UTC).toLocalDateTime());
                    } catch (DateTimeParseException ignored) {}
                }

                String closedAtStr = (String) item.get("closed_at");
                if (closedAtStr != null) {
                    try {
                        issue.setClosedAt(Instant.parse(closedAtStr).atZone(ZoneOffset.UTC).toLocalDateTime());
                    } catch (DateTimeParseException ignored) {}
                }

                result.add(issue);
            }
            return result;
        } catch (Exception e) {
            extractAndThrow429(e);
            log.warn("Issue 조회 실패 ({}): {}", githubId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean validateUserExists(String githubId) {
        String encodedId = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        String url = baseUrl + "/users/" + encodedId;
        try {
            restTemplate.exchange(url, HttpMethod.GET, buildHeaders(), Map.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            extractAndThrow429(e);
            log.warn("사용자 검증 실패 ({}): {}", githubId, e.getMessage());
            throw new RuntimeException("GitHub API 오류: " + e.getMessage(), e);
        }
    }

    public boolean canMakeRequest() {
        if (rateLimitRemaining.get() > 0) return true;
        // reset 시간이 지났거나 알 수 없으면 허용
        long resetAt = rateLimitResetEpoch.get();
        return resetAt == 0L || Instant.now().getEpochSecond() >= resetAt;
    }

    public int getRateLimitStatus() {
        return rateLimitRemaining.get();
    }

    /** Rate Limit 초과 시 IllegalStateException 발생 */
    private void checkRateLimit() {
        if (!canMakeRequest()) {
            long secondsUntilReset = rateLimitResetEpoch.get() - Instant.now().getEpochSecond();
            throw new IllegalStateException(String.format(
                "GitHub API Rate Limit 초과. %d초 후 재시도 가능합니다.", Math.max(secondsUntilReset, 0)));
        }
    }

    /** 예외가 429이면 rateLimitResetEpoch를 갱신하고 IllegalStateException으로 전환 */
    private void extractAndThrow429(Exception e) {
        if (e instanceof HttpClientErrorException hce && hce.getStatusCode().value() == 429) {
            rateLimitRemaining.set(0);
            org.springframework.http.HttpHeaders respHeaders = hce.getResponseHeaders();
            if (respHeaders != null) {
                String reset = respHeaders.getFirst("X-RateLimit-Reset");
                if (reset != null) {
                    try {
                        rateLimitResetEpoch.set(Long.parseLong(reset));
                    } catch (NumberFormatException ex) {
                        log.debug("Rate limit reset 시간 파싱 실패: {}", ex.getMessage());
                    }
                }
            }
            long secondsUntilReset = rateLimitResetEpoch.get() - Instant.now().getEpochSecond();
            throw new IllegalStateException(String.format(
                "GitHub API Rate Limit 초과. %d초 후 재시도 가능합니다.", Math.max(secondsUntilReset, 0)));
        }
    }

    private Map<String, Object> fetchUserProfile(String githubId) {
        String encodedId = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        String url = baseUrl + "/users/" + encodedId;
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                url, HttpMethod.GET, buildHeaders(), Map.class);
            updateRateLimit(resp.getHeaders());
            return resp.getBody();
        } catch (Exception e) {
            extractAndThrow429(e);
            log.warn("사용자 프로필 조회 실패 ({}): {}", githubId, e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> fetchRawEvents(String githubId) {
        String encodedId = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        String url = baseUrl + "/users/" + encodedId + "/events?per_page=100";
        try {
            ResponseEntity<List> resp = restTemplate.exchange(
                url, HttpMethod.GET, buildHeaders(), List.class);
            updateRateLimit(resp.getHeaders());
            List<Map<String, Object>> body = resp.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            extractAndThrow429(e);
            log.warn("이벤트 조회 실패 ({}): {}", githubId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private void parseEventsIntoData(List<Map<String, Object>> events,
                                      List<RepositoryData> repos,
                                      ActivityData data) {
        Set<String> repoIds = new HashSet<>();
        repos.forEach(r -> repoIds.add(r.getRepoId()));

        for (Map<String, Object> event : events) {
            String type = (String) event.get("type");
            String createdAtStr = (String) event.get("created_at");
            if (createdAtStr == null) continue;

            LocalDateTime createdAt;
            try {
                createdAt = Instant.parse(createdAtStr).atZone(ZoneOffset.UTC).toLocalDateTime();
            } catch (DateTimeParseException e) {
                continue;
            }

            Map<String, Object> repoInfo = (Map<String, Object>) event.get("repo");
            String repoId = repoInfo != null ? (String) repoInfo.get("name") : "unknown";

            if ("PushEvent".equals(type)) {
                Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                if (payload != null) {
                    List<Map<String, Object>> commits =
                        (List<Map<String, Object>>) payload.get("commits");
                    if (commits != null) {
                        for (Map<String, Object> c : commits) {
                            String sha = (String) c.getOrDefault("sha", UUID.randomUUID().toString());
                            String msg = (String) c.getOrDefault("message", "");
                            data.addCommit(new CommitData(sha, repoId, createdAt, msg));
                        }
                    }
                }
            } else if ("PullRequestEvent".equals(type)) {
                Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                if (payload != null) {
                    Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                    if (pr != null) {
                        String prId = String.valueOf(pr.getOrDefault("id", UUID.randomUUID()));
                        String state = (String) pr.getOrDefault("state", "open");
                        boolean merged = Boolean.TRUE.equals(pr.get("merged"));
                        data.addPullRequest(new PullRequestData(prId, repoId, state,
                            createdAt, null, merged));
                    }
                }
            } else if ("IssuesEvent".equals(type)) {
                Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                if (payload != null) {
                    Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
                    if (issue != null) {
                        String issueId = String.valueOf(issue.getOrDefault("id", UUID.randomUUID()));
                        String state = (String) issue.getOrDefault("state", "open");
                        data.addIssue(new IssueData(issueId, repoId, state, createdAt, null));
                    }
                }
            }
        }
    }

    private CommitData parseCommit(Map<String, Object> raw, String repoId) {
        try {
            String sha = (String) raw.get("sha");
            @SuppressWarnings("unchecked")
            Map<String, Object> commit = (Map<String, Object>) raw.get("commit");
            if (commit == null) return null;
            String msg = (String) commit.getOrDefault("message", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> author = (Map<String, Object>) commit.get("author");
            String dateStr = author != null ? (String) author.get("date") : null;
            LocalDateTime date = null;
            if (dateStr != null) {
                date = Instant.parse(dateStr).atZone(ZoneOffset.UTC).toLocalDateTime();
            }
            return new CommitData(sha, repoId, date, msg);
        } catch (DateTimeParseException e) {
            log.debug("커밋 날짜 파싱 실패 ({}): {}", repoId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.debug("커밋 데이터 파싱 실패 ({}): {}", repoId, e.getMessage());
            return null;
        }
    }

    private HttpEntity<?> buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        if (token != null && !token.isBlank()) {
            headers.set("Authorization", "Bearer " + token);
        }
        return new HttpEntity<>(headers);
    }

    private void updateRateLimit(org.springframework.http.HttpHeaders headers) {
        String remaining = headers.getFirst("X-RateLimit-Remaining");
        if (remaining != null) {
            try {
                rateLimitRemaining.set(Integer.parseInt(remaining));
            } catch (NumberFormatException e) {
                log.debug("Rate limit remaining 파싱 실패: {}", e.getMessage());
            }
        }
        String reset = headers.getFirst("X-RateLimit-Reset");
        if (reset != null) {
            try {
                rateLimitResetEpoch.set(Long.parseLong(reset));
            } catch (NumberFormatException e) {
                log.debug("Rate limit reset 시간 파싱 실패: {}", e.getMessage());
            }
        }
        if (rateLimitRemaining.get() == 0) {
            log.warn("GitHub API Rate Limit 소진. 재설정 시각 epoch={}", rateLimitResetEpoch.get());
        }
    }
}
