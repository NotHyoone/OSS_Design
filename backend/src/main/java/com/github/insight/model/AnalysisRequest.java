package com.github.insight.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 분석 요청 상태를 추적하는 모델 (in-memory)
 */
public class AnalysisRequest {

    public enum Status {
        PENDING, RUNNING, DONE, ERROR
    }

    private final String requestId;
    private final String githubId;
    private final Instant createdAt;

    private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
    private final AtomicInteger step = new AtomicInteger(0);
    private volatile double overallPct = 0.0;
    private volatile String detail = "대기 중...";
    private volatile String errorMsg;

    public AnalysisRequest(String requestId, String githubId) {
        this.requestId = requestId;
        this.githubId  = githubId;
        this.createdAt = Instant.now();
    }

    /* ── Getters ── */

    public String getRequestId()  { return requestId; }
    public String getGithubId()   { return githubId; }
    public Instant getCreatedAt() { return createdAt; }
    public Status getStatus()     { return status.get(); }
    public int getStep()          { return step.get(); }
    public double getOverallPct() { return overallPct; }
    public String getDetail()     { return detail; }
    public String getErrorMsg()   { return errorMsg; }

    /* ── Setters (thread-safe) ── */

    public void setStatus(Status s) { status.set(s); }

    public void updateProgress(int stepNum, double pct, String detailMsg) {
        this.step.set(stepNum);
        this.overallPct = pct;
        this.detail     = detailMsg;
    }

    public void markDone() {
        this.overallPct = 100.0;
        this.step.set(3);
        this.detail     = "완료";
        this.status.set(Status.DONE);
    }

    public void markError(String msg) {
        this.errorMsg = msg;
        this.status.set(Status.ERROR);
    }

    public boolean isCancelled() {
        return status.get() == Status.ERROR && "CANCELLED".equals(errorMsg);
    }
}
