package com.github.insight.model;

import java.util.List;
import java.util.Map;

/**
 * 최종 분석 결과 (직렬화 대상 → JSON으로 프론트엔드에 반환)
 *
 * @param githubId      분석 대상 GitHub ID
 * @param avatarUrl     GitHub 프로필 이미지 URL
 * @param analysisDate  분석 완료 일시 (ISO-8601)
 * @param developerType 개발자 유형 문자열
 * @param totalScore    종합 점수 (0-100)
 * @param trustLevel    신뢰도 ("HIGH" | "MEDIUM" | "LOW")
 * @param summaryText   종합 평가 문구
 * @param metrics       key → MetricScore (activity, diversity, collab, persist)
 * @param strengths     강점 목록
 * @param improvements  개선 사항 목록
 */
public record AnalysisResult(
        String githubId,
        String avatarUrl,
        String analysisDate,
        String developerType,
        int totalScore,
        String trustLevel,
        String summaryText,
        Map<String, MetricScore> metrics,
        List<String> strengths,
        List<String> improvements
) {}
