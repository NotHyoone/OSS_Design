package com.github.insight.dto;

/**
 * GET /api/analysis/status/{requestId} 응답
 *
 * @param step        현재 단계 (1=수집, 2=지표계산, 3=점수생성)
 * @param stepStatus  "running" | "done" | "cancelled" | "error"
 * @param overallPct  전체 진행률 (0-100)
 * @param detail      현재 작업 설명 문구
 */
public record AnalysisStatusResponse(int step, String stepStatus, double overallPct, String detail) {}
