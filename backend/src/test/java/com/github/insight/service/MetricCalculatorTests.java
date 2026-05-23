package com.github.insight.service;

import com.github.insight.model.*;
import com.github.insight.model.enums.TrustLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("MetricCalculator 테스트")
class MetricCalculatorTests {

    private MetricCalculator metricCalculator;

    @BeforeEach
    void setUp() {
        metricCalculator = new MetricCalculator();
    }

    private ActivityData createTestActivityData() {
        ActivityData data = new ActivityData("test-request-123");

        RepositoryData repo = new RepositoryData();
        repo.setRepoId("testuser/test-repo");
        repo.setName("test-repo");
        repo.setLanguage("Java");
        repo.setStarCount(10);
        repo.setFork(false);
        data.addRepository(repo);

        CommitData commit = new CommitData(
                "abc123",
                "testuser/test-repo",
                LocalDateTime.now().minusDays(5),
                "Fix: test commit"
        );
        data.addCommit(commit);

        data.getLanguages().put("Java", 1000L);
        data.getLanguages().put("Python", 500L);

        PullRequestData pr = new PullRequestData();
        pr.setPrId("pr-1");
        pr.setRepoId("testuser/test-repo");
        pr.setState("closed");
        pr.setCreatedAt(LocalDateTime.now().minusDays(10));
        pr.setMerged(true);
        data.addPullRequest(pr);

        return data;
    }

    @Test
    @DisplayName("지표 계산 - 정상 데이터")
    void testCalculate_Success() {
        ActivityData data = createTestActivityData();

        Metrics metrics = metricCalculator.calculate(data);

        assertNotNull(metrics);
        assertNotNull(metrics.getActivityScore());
        assertNotNull(metrics.getDiversityScore());
        assertNotNull(metrics.getCollaborationScore());
        assertNotNull(metrics.getPersistenceScore());
        assertNotNull(metrics.getTrustLevel());
        assertTrue(metrics.getActivityScore() >= 0 && metrics.getActivityScore() <= 100);
    }

    @Test
    @DisplayName("활동성 점수 계산")
    void testCalculateActivity() {
        ActivityData data = createTestActivityData();

        float score = metricCalculator.calculateActivity(data);

        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    @DisplayName("다양성 점수 계산")
    void testCalculateDiversity() {
        ActivityData data = createTestActivityData();

        float score = metricCalculator.calculateDiversity(data);

        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    @DisplayName("협업 점수 계산")
    void testCalculateCollaboration() {
        ActivityData data = createTestActivityData();

        float score = metricCalculator.calculateCollaboration(data);

        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    @DisplayName("지속성 점수 계산")
    void testCalculatePersistence() {
        ActivityData data = createTestActivityData();

        float score = metricCalculator.calculatePersistence(data);

        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    @DisplayName("빈 데이터 처리")
    void testCalculate_EmptyData() {
        ActivityData data = new ActivityData("empty-request");

        Metrics metrics = metricCalculator.calculate(data);

        assertNotNull(metrics);
        assertTrue(metrics.getActivityScore() >= 0);
    }

    @Test
    @DisplayName("이상 탐지 - 정상 데이터")
    void testDetectAnomalies_NoAnomalies() {
        ActivityData data = createTestActivityData();
        Metrics metrics = new Metrics("test-request");

        boolean hasAnomalies = metricCalculator.detectAnomalies(data, metrics);

        assertFalse(hasAnomalies);
    }
}
