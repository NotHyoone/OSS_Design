package com.github.insight.service;

import com.github.insight.model.AnalysisRequest;
import com.github.insight.model.AnalysisResult;
import com.github.insight.model.Metrics;
import com.github.insight.model.enums.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AnalysisRepository {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRepository.class);

    /** 완료/실패 request 보존 시간 (시간) */
    private static final int MAX_REQUEST_TTL_HOURS = 24;
    /** result/metrics 보존 시간 (시간) */
    private static final int MAX_RESULT_TTL_HOURS  = 72;
    /** githubId·userId별 최대 이력 보존 건수 */
    private static final int MAX_HISTORY_PER_USER  = 10;

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

    /**
     * 1시간마다 만료된 데이터를 정리한다.
     * - requestStore: 완료/실패 후 24시간 경과한 항목 제거
     * - resultStore/metricsStore: 72시간 경과한 항목 제거
     * - githubIdHistory/userResultHistory: 사용자별 최대 10건 초과분 제거
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        LocalDateTime requestCutoff = LocalDateTime.now().minusHours(MAX_REQUEST_TTL_HOURS);
        LocalDateTime resultCutoff  = LocalDateTime.now().minusHours(MAX_RESULT_TTL_HOURS);

        // 1. 완료/실패된 오래된 request 제거
        List<String> staleRequestIds = requestStore.values().stream()
            .filter(r -> !r.isRunning() && r.getRequestedAt().isBefore(requestCutoff))
            .map(AnalysisRequest::getRequestId)
            .collect(Collectors.toList());
        staleRequestIds.forEach(requestStore::remove);

        // 2. 오래된 result + metrics 제거
        List<String> staleResultIds = resultStore.values().stream()
            .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isBefore(resultCutoff))
            .map(AnalysisResult::getRequestId)
            .collect(Collectors.toList());
        staleResultIds.forEach(id -> {
            resultStore.remove(id);
            metricsStore.remove(id);
        });

        // 3. 이력 목록 최대 건수 초과분 제거
        githubIdHistory.forEach((id, list) -> {
            if (list.size() > MAX_HISTORY_PER_USER) {
                list.subList(MAX_HISTORY_PER_USER, list.size()).clear();
            }
        });
        userResultHistory.forEach((id, list) -> {
            if (list.size() > MAX_HISTORY_PER_USER) {
                list.subList(MAX_HISTORY_PER_USER, list.size()).clear();
            }
        });

        log.info("정기 정리 완료: request {}건, result {}건 제거",
            staleRequestIds.size(), staleResultIds.size());
    }
}
