package com.github.insight.service;

import com.github.insight.model.AnalysisRequest;
import com.github.insight.model.AnalysisResult;
import com.github.insight.model.Metrics;
import com.github.insight.model.enums.RequestStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AnalysisRepository {

    private final Map<String, AnalysisRequest> requestStore = new ConcurrentHashMap<>();
    private final Map<String, AnalysisResult> resultStore = new ConcurrentHashMap<>();
    private final Map<String, Metrics> metricsStore = new ConcurrentHashMap<>();

    /** githubId → requestId (최신) */
    private final Map<String, String> activeRequests = new ConcurrentHashMap<>();

    /** userId → result list (최신 순) */
    private final Map<String, List<AnalysisResult>> userResultHistory = new ConcurrentHashMap<>();

    /** githubId → result list (최신 순, 비인증 fallback) */
    private final Map<String, List<AnalysisResult>> githubIdHistory = new ConcurrentHashMap<>();

    public void save(AnalysisRequest request) {
        requestStore.put(request.getRequestId(), request);
        if (request.isRunning()) {
            activeRequests.put(request.getGithubId(), request.getRequestId());
        } else {
            activeRequests.remove(request.getGithubId(), request.getRequestId());
        }
    }

    public AnalysisRequest findById(String requestId) {
        AnalysisRequest req = requestStore.get(requestId);
        if (req == null) throw new NoSuchElementException("요청을 찾을 수 없습니다: " + requestId);
        return req;
    }

    public boolean exists(String requestId) {
        return requestStore.containsKey(requestId);
    }

    public List<AnalysisRequest> findAll() {
        return new ArrayList<>(requestStore.values());
    }

    public List<AnalysisRequest> findByStatus(RequestStatus status) {
        return requestStore.values().stream()
            .filter(r -> r.getStatus() == status)
            .collect(Collectors.toList());
    }

    public void saveResult(AnalysisResult result) {
        resultStore.put(result.getRequestId(), result);

        if (result.getUserId() != null) {
            userResultHistory
                .computeIfAbsent(result.getUserId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(0, result);
        }
        if (result.getGithubId() != null) {
            githubIdHistory
                .computeIfAbsent(result.getGithubId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(0, result);
        }
    }

    public void saveMetrics(String requestId, Metrics metrics) {
        metricsStore.put(requestId, metrics);
    }

    public AnalysisResult findResultByRequestId(String requestId) {
        AnalysisResult r = resultStore.get(requestId);
        if (r == null) throw new NoSuchElementException("결과를 찾을 수 없습니다: " + requestId);
        return r;
    }

    public Optional<AnalysisResult> findLatestResultByGithubId(String githubId) {
        List<AnalysisResult> list = githubIdHistory.get(githubId);
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }

    public List<AnalysisResult> findResultsByUserId(String userId) {
        List<AnalysisResult> list = userResultHistory.get(userId);
        if (list == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    public List<AnalysisResult> findResultsByGithubId(String githubId) {
        List<AnalysisResult> list = githubIdHistory.get(githubId);
        if (list == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    public List<AnalysisResult> findAllResults() {
        return new ArrayList<>(resultStore.values());
    }

    public Optional<Metrics> findMetricsByRequestId(String requestId) {
        return Optional.ofNullable(metricsStore.get(requestId));
    }

    public Optional<String> findActiveRequestId(String githubId) {
        return Optional.ofNullable(activeRequests.get(githubId));
    }

    public int deleteExpired(int ttlMinutes) {
        java.time.LocalDateTime cutoff =
            java.time.LocalDateTime.now().minusMinutes(ttlMinutes);
        List<String> toDelete = resultStore.values().stream()
            .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isBefore(cutoff))
            .map(AnalysisResult::getRequestId)
            .collect(Collectors.toList());
        toDelete.forEach(id -> {
            resultStore.remove(id);
            metricsStore.remove(id);
        });
        return toDelete.size();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRequests", requestStore.size());
        stats.put("totalResults", resultStore.size());
        double avgScore = resultStore.values().stream()
            .mapToInt(AnalysisResult::getTotalScore).average().orElse(0);
        stats.put("averageScore", Math.round(avgScore * 10) / 10.0);
        return stats;
    }
}
