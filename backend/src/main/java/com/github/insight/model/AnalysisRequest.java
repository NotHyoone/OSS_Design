package com.github.insight.model;

import com.github.insight.model.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AnalysisRequest {

    private final String requestId;
    private final String userId;
    private final String githubId;
    private final LocalDateTime requestedAt;
    private volatile LocalDateTime completedAt;
    private volatile String errorMessage;
    private final String resultAccessToken;

    private final AtomicReference<RequestStatus> status =
            new AtomicReference<>(RequestStatus.PENDING);
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private static final int MAX_RETRIES = 3;

    private final AtomicInteger step = new AtomicInteger(0);
    private volatile double overallPct = 0.0;
    private volatile String detail = "대기 중...";

    public static AnalysisRequest create(String userId, String githubId) {
        return new AnalysisRequest(userId, githubId);
    }

    public static AnalysisRequest restore(
            String requestId,
            String userId,
            String githubId,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            String errorMessage,
            RequestStatus status,
            int retryCount,
            int step,
            double overallPct,
            String detail) {
        return restore(requestId, userId, githubId, null, requestedAt, completedAt,
            errorMessage, status, retryCount, step, overallPct, detail);
    }

    public static AnalysisRequest restore(
            String requestId,
            String userId,
            String githubId,
            String resultAccessToken,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            String errorMessage,
            RequestStatus status,
            int retryCount,
            int step,
            double overallPct,
            String detail) {
        AnalysisRequest restored = new AnalysisRequest(
                requestId,
                userId,
                githubId,
                requestedAt != null ? requestedAt : LocalDateTime.now(),
                resultAccessToken);
        restored.completedAt = completedAt;
        restored.errorMessage = errorMessage;
        restored.status.set(status != null ? status : RequestStatus.PENDING);
        restored.retryCount.set(Math.max(0, retryCount));
        restored.step.set(Math.max(0, step));
        restored.overallPct = overallPct;
        restored.detail = detail != null ? detail : "대기 중...";
        return restored;
    }

    public AnalysisRequest(String userId, String githubId) {
        this(UUID.randomUUID().toString(), userId, githubId, LocalDateTime.now());
    }

    private AnalysisRequest(String requestId, String userId, String githubId, LocalDateTime requestedAt) {
        this(requestId, userId, githubId, requestedAt, null);
    }

    private AnalysisRequest(String requestId, String userId, String githubId,
                            LocalDateTime requestedAt, String resultAccessToken) {
        this.requestId = requestId;
        this.userId = userId;
        this.githubId = githubId;
        this.requestedAt = requestedAt;
        this.resultAccessToken = resultAccessToken != null
            ? resultAccessToken
            : (userId == null ? UUID.randomUUID().toString() : null);
    }

    public void updateStatus(RequestStatus newStatus) {
        this.status.set(newStatus);
        if (newStatus == RequestStatus.COMPLETED
            || newStatus == RequestStatus.FAILED
            || newStatus == RequestStatus.PARTIAL) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public boolean transitionTo(RequestStatus newStatus) {
        RequestStatus current = this.status.get();
        boolean allowed = switch (current) {
            case PENDING  -> newStatus == RequestStatus.RUNNING || newStatus == RequestStatus.FAILED;
            case RUNNING  -> newStatus == RequestStatus.COMPLETED
                          || newStatus == RequestStatus.PARTIAL
                          || newStatus == RequestStatus.FAILED;
            case PARTIAL  -> newStatus == RequestStatus.COMPLETED || newStatus == RequestStatus.FAILED;
            default       -> false;
        };
        if (allowed) {
            updateStatus(newStatus);
        }
        return allowed;
    }

    public boolean canRetry() {
        return isFailed() && retryCount.get() < MAX_RETRIES;
    }

    public void incrementRetry() {
        retryCount.incrementAndGet();
    }

    public boolean isRunning() {
        RequestStatus s = status.get();
        return s == RequestStatus.PENDING || s == RequestStatus.RUNNING;
    }

    public boolean isFailed() {
        return status.get() == RequestStatus.FAILED;
    }

    public long getElapsedTime() {
        LocalDateTime end = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(requestedAt, end).toMillis();
    }

    public void updateProgress(int stepNum, double pct, String detailMsg) {
        this.step.set(stepNum);
        this.overallPct = pct;
        this.detail = detailMsg;
    }

    public void markDone() {
        this.overallPct = 100.0;
        this.step.set(3);
        this.detail = "완료";
        this.completedAt = LocalDateTime.now();
        this.status.set(RequestStatus.COMPLETED);
    }

    public void markError(String msg) {
        this.errorMessage = msg;
        this.completedAt = LocalDateTime.now();
        this.status.set(RequestStatus.FAILED);
    }

    public boolean isCancelled() {
        return isFailed() && "CANCELLED".equals(errorMessage);
    }

    public String getRequestId()        { return requestId; }
    public String getUserId()           { return userId; }
    public String getGithubId()         { return githubId; }
    public String getResultAccessToken() { return resultAccessToken; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getErrorMessage()     { return errorMessage; }
    public RequestStatus getStatus()    { return status.get(); }
    public int getRetryCount()          { return retryCount.get(); }
    public int getStep()                { return step.get(); }
    public double getOverallPct()       { return overallPct; }
    public String getDetail()           { return detail; }
}
