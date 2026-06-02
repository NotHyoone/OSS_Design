package com.github.insight.entity;

import com.github.insight.model.enums.RequestStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analysis_requests")
public class AnalysisRequestEntity {

    @Id
    private String requestId;

    @Column(nullable = true)
    private String userId;

    @Column(nullable = false)
    private String githubId;

    @Column(unique = true)
    private String resultAccessToken;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(nullable = false)
    private int retryCount = 0;

    private int step = 0;
    private double overallPct = 0.0;
    private String detail = "대기 중...";

    public AnalysisRequestEntity() {
        this.requestId = UUID.randomUUID().toString();
        this.requestedAt = LocalDateTime.now();
    }

    public AnalysisRequestEntity(String userId, String githubId) {
        this();
        this.userId = userId;
        this.githubId = githubId;
    }

    public boolean isRunning() {
        return status == RequestStatus.PENDING || status == RequestStatus.RUNNING;
    }

    public boolean isFailed() {
        return status == RequestStatus.FAILED;
    }

    public boolean canRetry() {
        return isFailed() && retryCount < 3;
    }

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGithubId() { return githubId; }
    public void setGithubId(String githubId) { this.githubId = githubId; }

    public String getResultAccessToken() { return resultAccessToken; }
    public void setResultAccessToken(String resultAccessToken) { this.resultAccessToken = resultAccessToken; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public double getOverallPct() { return overallPct; }
    public void setOverallPct(double overallPct) { this.overallPct = overallPct; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
