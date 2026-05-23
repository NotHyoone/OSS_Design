package com.github.insight.model;

/**
 * 개별 역량 지표 점수
 *
 * @param score 0~100 점수
 * @param desc  점수를 설명하는 한 줄 문구
 */
public record MetricScore(int score, String desc) {}
