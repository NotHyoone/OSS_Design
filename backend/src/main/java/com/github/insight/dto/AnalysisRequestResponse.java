package com.github.insight.dto;

/** POST /api/analysis/request 응답 */
public record AnalysisRequestResponse(String requestId, int estimatedSeconds) {}
