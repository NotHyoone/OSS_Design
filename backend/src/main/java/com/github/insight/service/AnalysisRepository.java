package com.github.insight.service;

import com.github.insight.entity.AnalysisRequestEntity;
import com.github.insight.entity.AnalysisResultEntity;
import com.github.insight.entity.MetricsEntity;
import com.github.insight.model.AnalysisRequest;
import com.github.insight.model.AnalysisResult;
import com.github.insight.model.Metrics;
import com.github.insight.model.enums.RequestStatus;
import com.github.insight.repository.AnalysisRequestRepository;
import com.github.insight.repository.AnalysisResultRepository;
import com.github.insight.repository.MetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisRepository {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRepository.class);

    private static final int MAX_REQUEST_TTL_HOURS = 24;
    private static final int MAX_RESULT_TTL_HOURS  = 72;
    private static final int MAX_HISTORY_PER_USER  = 10;
    private static final int STUCK_REQUEST_TTL_HOURS = 2;

    private final AnalysisRequestRepository requestRepo;
    private final AnalysisResultRepository resultRepo;
    private final MetricsRepository metricsRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisRepository(
            AnalysisRequestRepository requestRepo,
            AnalysisResultRepository resultRepo,
            MetricsRepository metricsRepo) {
        this.requestRepo = requestRepo;
        this.resultRepo = resultRepo;
        this.metricsRepo = metricsRepo;
    }

    public void save(AnalysisRequest request) {
        AnalysisRequestEntity entity = convertToEntity(request);
        requestRepo.save(entity);
    }

    public AnalysisRequest findById(String requestId) {
        AnalysisRequestEntity entity = requestRepo.findById(requestId)
            .orElseThrow(() -> new NoSuchElementException("요청을 찾을 수 없습니다: " + requestId));
        return convertToModel(entity);
    }

    public boolean exists(String requestId) {
        return requestRepo.existsById(requestId);
    }

    public List<AnalysisRequest> findAll() {
        return requestRepo.findAll().stream()
            .map(this::convertToModel)
            .collect(Collectors.toList());
    }

    public List<AnalysisRequest> findByStatus(RequestStatus status) {
        return requestRepo.findByStatus(status).stream()
            .map(this::convertToModel)
            .collect(Collectors.toList());
    }

    public void saveResult(AnalysisResult result) {
        AnalysisResultEntity entity = convertResultToEntity(result);
        resultRepo.save(entity);
    }

    public void saveMetrics(String requestId, Metrics metrics) {
        MetricsEntity entity = convertMetricsToEntity(requestId, metrics);
        metricsRepo.save(entity);
    }

    public AnalysisResult findResultByRequestId(String requestId) {
        AnalysisResultEntity entity = resultRepo.findByRequestId(requestId)
            .orElseThrow(() -> new NoSuchElementException("결과를 찾을 수 없습니다: " + requestId));
        return convertResultToModel(entity);
    }

    public Optional<AnalysisResult> findLatestResultByGithubId(String githubId) {
        return resultRepo.findFirstByGithubIdOrderByCreatedAtDesc(githubId)
            .map(this::convertResultToModel);
    }

    public List<AnalysisResult> findResultsByUserId(String userId) {
        return resultRepo.findLatestByUserId(userId).stream()
            .limit(MAX_HISTORY_PER_USER)
            .map(this::convertResultToModel)
            .collect(Collectors.toList());
    }

    public List<AnalysisResult> findResultsByGithubId(String githubId) {
        return resultRepo.findLatestByGithubId(githubId).stream()
            .limit(MAX_HISTORY_PER_USER)
            .map(this::convertResultToModel)
            .collect(Collectors.toList());
    }

    public List<AnalysisResult> findAllResults() {
        return resultRepo.findAll().stream()
            .map(this::convertResultToModel)
            .collect(Collectors.toList());
    }

    public Optional<Metrics> findMetricsByRequestId(String requestId) {
        return metricsRepo.findByRequestId(requestId)
            .map(this::convertMetricsToModel);
    }

    public Optional<String> findActiveRequestId(String githubId) {
        return requestRepo.findFirstByGithubIdOrderByRequestedAtDesc(githubId)
            .filter(AnalysisRequestEntity::isRunning)
            .map(AnalysisRequestEntity::getRequestId);
    }

    public int deleteExpired(int ttlMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(ttlMinutes);
        List<AnalysisResultEntity> expired = resultRepo.findExpiredResults(cutoff);
        int count = expired.size();
        expired.forEach(result -> {
            resultRepo.deleteById(result.getResultId());
            metricsRepo.deleteById(result.getRequestId());
        });
        return count;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRequests", requestRepo.count());
        stats.put("totalResults", resultRepo.count());
        double avgScore = resultRepo.findAll().stream()
            .mapToInt(AnalysisResultEntity::getTotalScore)
            .average()
            .orElse(0);
        stats.put("averageScore", Math.round(avgScore * 10) / 10.0);
        return stats;
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        LocalDateTime requestCutoff = LocalDateTime.now().minusHours(MAX_REQUEST_TTL_HOURS);
        LocalDateTime stuckCutoff = LocalDateTime.now().minusHours(STUCK_REQUEST_TTL_HOURS);
        LocalDateTime resultCutoff = LocalDateTime.now().minusHours(MAX_RESULT_TTL_HOURS);

        // 1. 완료/실패된 오래된 request 제거
        List<AnalysisRequestEntity> staleRequests = requestRepo.findExpiredByStatuses(
            Arrays.asList(RequestStatus.COMPLETED, RequestStatus.FAILED, RequestStatus.PARTIAL),
            requestCutoff
        );
        requestRepo.deleteAll(staleRequests);

        // 1-1. 멈춘 running request 제거
        List<AnalysisRequestEntity> stuckRequests = requestRepo.findStuckRequests(RequestStatus.RUNNING, stuckCutoff);
        requestRepo.deleteAll(stuckRequests);

        // 2. 오래된 result + metrics 제거
        List<AnalysisResultEntity> staleResults = resultRepo.findExpiredResults(resultCutoff);
        staleResults.forEach(result -> {
            metricsRepo.deleteById(result.getRequestId());
        });
        resultRepo.deleteAll(staleResults);

        log.info("정기 정리 완료: request {}건, stuck {}건, result {}건 제거",
            staleRequests.size(), stuckRequests.size(), staleResults.size());
    }

    private AnalysisRequestEntity convertToEntity(AnalysisRequest model) {
        AnalysisRequestEntity entity = new AnalysisRequestEntity();
        entity.setRequestId(model.getRequestId());
        entity.setUserId(model.getUserId());
        entity.setGithubId(model.getGithubId());
        entity.setResultAccessToken(model.getResultAccessToken());
        entity.setRequestedAt(model.getRequestedAt());
        entity.setCompletedAt(model.getCompletedAt());
        entity.setErrorMessage(model.getErrorMessage());
        entity.setStatus(model.getStatus());
        entity.setRetryCount(model.getRetryCount());
        entity.setStep(model.getStep());
        entity.setOverallPct(model.getOverallPct());
        entity.setDetail(model.getDetail());
        return entity;
    }

    private AnalysisRequest convertToModel(AnalysisRequestEntity entity) {
        return AnalysisRequest.restore(
            entity.getRequestId(),
            entity.getUserId(),
            entity.getGithubId(),
            entity.getResultAccessToken(),
            entity.getRequestedAt(),
            entity.getCompletedAt(),
            entity.getErrorMessage(),
            entity.getStatus(),
            entity.getRetryCount(),
            entity.getStep(),
            entity.getOverallPct(),
            entity.getDetail()
        );
    }

    private AnalysisResultEntity convertResultToEntity(AnalysisResult model) {
        AnalysisResultEntity entity = new AnalysisResultEntity();
        entity.setResultId(model.getResultId());
        entity.setRequestId(model.getRequestId());
        entity.setUserId(model.getUserId());
        entity.setGithubId(model.getGithubId());
        entity.setAvatarUrl(model.getAvatarUrl());
        entity.setTotalScore(model.getTotalScore());
        entity.setDeveloperType(model.getDeveloperType());
        entity.setTrustLevel(model.getTrustLevel());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setRuleVersion(model.getRuleVersion());

        try {
            entity.setStrengthsJson(objectMapper.writeValueAsString(model.getStrengths()));
            entity.setImprovementsJson(objectMapper.writeValueAsString(model.getImprovements()));
        } catch (Exception e) {
            log.warn("JSON 직렬화 실패: {}", e.getMessage());
        }

        return entity;
    }

    private AnalysisResult convertResultToModel(AnalysisResultEntity entity) {
        AnalysisResult model = new AnalysisResult();
        model.setResultId(entity.getResultId());
        model.setRequestId(entity.getRequestId());
        model.setUserId(entity.getUserId());
        model.setGithubId(entity.getGithubId());
        model.setAvatarUrl(entity.getAvatarUrl());
        model.setTotalScore(entity.getTotalScore());
        model.setDeveloperType(entity.getDeveloperType());
        model.setTrustLevel(entity.getTrustLevel());
        model.setCreatedAt(entity.getCreatedAt());
        model.setRuleVersion(entity.getRuleVersion());

        try {
            if (entity.getStrengthsJson() != null) {
                model.setStrengths(objectMapper.readValue(entity.getStrengthsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            }
            if (entity.getImprovementsJson() != null) {
                model.setImprovements(objectMapper.readValue(entity.getImprovementsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class,
                    objectMapper.getTypeFactory().constructType(
                        Class.forName("com.github.insight.model.FeedbackItem")))));
            }
        } catch (Exception e) {
            log.warn("JSON 역직렬화 실패: {}", e.getMessage());
        }

        return model;
    }

    private MetricsEntity convertMetricsToEntity(String requestId, Metrics model) {
        MetricsEntity entity = new MetricsEntity(requestId);
        entity.setActivityScore(model.getActivityScore());
        entity.setDiversityScore(model.getDiversityScore());
        entity.setCollaborationScore(model.getCollaborationScore());
        entity.setPersistenceScore(model.getPersistenceScore());
        entity.setTrustLevel(model.getTrustLevel());
        entity.setNotes(model.getNotes());
        entity.setCalculatedAt(model.getCalculatedAt());
        entity.setConfidence(model.getConfidence());

        try {
            entity.setDescriptionsJson(objectMapper.writeValueAsString(model.getDescriptions()));
        } catch (Exception e) {
            log.warn("JSON 직렬화 실패: {}", e.getMessage());
        }

        return entity;
    }

    private Metrics convertMetricsToModel(MetricsEntity entity) {
        Metrics model = new Metrics(entity.getRequestId());
        model.setActivityScore(entity.getActivityScore());
        model.setDiversityScore(entity.getDiversityScore());
        model.setCollaborationScore(entity.getCollaborationScore());
        model.setPersistenceScore(entity.getPersistenceScore());
        model.setTrustLevel(entity.getTrustLevel());
        model.setNotes(entity.getNotes());
        model.setCalculatedAt(entity.getCalculatedAt());
        model.setConfidence(entity.getConfidence());

        try {
            if (entity.getDescriptionsJson() != null) {
                model.setDescriptions(objectMapper.readValue(entity.getDescriptionsJson(),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class)));
            }
        } catch (Exception e) {
            log.warn("JSON 역직렬화 실패: {}", e.getMessage());
        }

        return model;
    }
}

