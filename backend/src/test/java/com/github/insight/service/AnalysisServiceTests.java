package com.github.insight.service;

import com.github.insight.model.*;
import com.github.insight.model.enums.RequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("AnalysisService 테스트")
class AnalysisServiceTests {

    @MockBean
    private GithubApiClient githubApiClient;

    @MockBean
    private MetricCalculator metricCalculator;

    @MockBean
    private CompetencyScorer competencyScorer;

    @MockBean
    private FeedbackGenerator feedbackGenerator;

    @MockBean
    private ReportAssembler reportAssembler;

    @MockBean
    private ReportGenerator reportGenerator;

    @MockBean
    private AnalysisRepository analysisRepository;

    @MockBean
    private AnalysisAsyncRunner asyncRunner;

    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisService(
                githubApiClient,
                metricCalculator,
                competencyScorer,
                feedbackGenerator,
                reportAssembler,
                reportGenerator,
                analysisRepository
        );
        analysisService.setAsyncRunner(asyncRunner);
    }

    @Test
    @DisplayName("분석 요청 생성 - 활성 요청이 없을 때")
    void testRequestAnalysis_NoActiveRequest() {
        String githubId = "testuser";
        when(analysisRepository.findActiveRequestId(githubId))
                .thenReturn(Optional.empty());
        doNothing().when(analysisRepository).save(any());

        AnalysisRequest result = analysisService.requestAnalysis(null, githubId);

        assertNotNull(result);
        assertEquals(githubId, result.getGithubId());
        verify(asyncRunner).run(any());
    }

    @Test
    @DisplayName("분석 요청 생성 - 기존 활성 요청이 있을 때")
    void testRequestAnalysis_ExistingActiveRequest() {
        String githubId = "testuser";
        String requestId = "req-123";

        AnalysisRequest existing = new AnalysisRequest("user-123", githubId);
        existing.transitionTo(RequestStatus.RUNNING);

        when(analysisRepository.findActiveRequestId(githubId))
                .thenReturn(Optional.of(requestId));
        when(analysisRepository.findById(requestId))
                .thenReturn(existing);

        AnalysisRequest result = analysisService.requestAnalysis(null, githubId);

        assertEquals(existing.getRequestId(), result.getRequestId());
        verify(asyncRunner, never()).run(any());
    }

    @Test
    @DisplayName("분석 상태 조회 - 존재하는 요청")
    void testGetRequest_Success() {
        String requestId = "req-123";
        AnalysisRequest request = new AnalysisRequest("user-123", "testuser");

        when(analysisRepository.findById(requestId))
                .thenReturn(request);

        Optional<AnalysisRequest> result = analysisService.getRequest(requestId);

        assertTrue(result.isPresent());
        assertEquals(request.getRequestId(), result.get().getRequestId());
    }

    @Test
    @DisplayName("분석 상태 조회 - 존재하지 않는 요청")
    void testGetRequest_NotFound() {
        String requestId = "nonexistent";

        when(analysisRepository.findById(requestId))
                .thenThrow(new RuntimeException("Not found"));

        Optional<AnalysisRequest> result = analysisService.getRequest(requestId);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("최신 분석 결과 조회")
    void testGetLatestResult() {
        String githubId = "testuser";
        AnalysisResult result = new AnalysisResult();
        result.setGithubId(githubId);

        when(analysisRepository.findLatestResultByGithubId(githubId))
                .thenReturn(Optional.of(result));

        Optional<AnalysisResult> found = analysisService.getLatestResult(githubId);

        assertTrue(found.isPresent());
        assertEquals(githubId, found.get().getGithubId());
    }

    @Test
    @DisplayName("분석 이력 조회")
    void testGetHistory() {
        String githubId = "testuser";
        List<AnalysisResult> history = new ArrayList<>();
        AnalysisResult result1 = new AnalysisResult();
        result1.setGithubId(githubId);
        result1.setCreatedAt(LocalDateTime.now().minusDays(1));
        history.add(result1);

        when(analysisRepository.findResultsByGithubId(githubId))
                .thenReturn(history);

        List<AnalysisResult> results = analysisService.getHistory(githubId);

        assertEquals(1, results.size());
        assertEquals(githubId, results.get(0).getGithubId());
    }

    @Test
    @DisplayName("분석 요청 취소")
    void testCancel_Success() {
        String requestId = "req-123";
        AnalysisRequest request = new AnalysisRequest("user-123", "testuser");
        request.transitionTo(RequestStatus.RUNNING);

        when(analysisRepository.findById(requestId))
                .thenReturn(request);

        analysisService.cancel(requestId);

        verify(analysisRepository).save(any());
    }

    @Test
    @DisplayName("분석 요청 취소 - 존재하지 않는 요청")
    void testCancel_NotFound() {
        String requestId = "nonexistent";

        when(analysisRepository.findById(requestId))
                .thenThrow(new RuntimeException("Not found"));

        assertDoesNotThrow(() -> analysisService.cancel(requestId));
    }

    @Test
    @DisplayName("지표 조회")
    void testGetMetrics() {
        String requestId = "req-123";
        Metrics metrics = new Metrics(requestId);
        metrics.setActivityScore(85.0f);

        when(analysisRepository.findMetricsByRequestId(requestId))
                .thenReturn(Optional.of(metrics));

        Optional<Metrics> result = analysisService.getMetrics(requestId);

        assertTrue(result.isPresent());
        assertEquals(85.0f, result.get().getActivityScore());
    }

    @Test
    @DisplayName("분석 결과 조회")
    void testGetResult() {
        String requestId = "req-123";
        AnalysisResult result = new AnalysisResult();
        result.setRequestId(requestId);
        result.setGithubId("testuser");

        when(analysisRepository.findResultByRequestId(requestId))
                .thenReturn(result);

        AnalysisResult found = analysisService.getResult(requestId);

        assertNotNull(found);
        assertEquals(requestId, found.getRequestId());
    }

    @Test
    @DisplayName("재시도 가능한 분석 재실행")
    void testRetryFailedAnalysis() {
        String requestId = "req-123";
        AnalysisRequest request = new AnalysisRequest("user-123", "testuser");
        request.transitionTo(RequestStatus.FAILED);

        when(analysisRepository.findById(requestId))
                .thenReturn(request);

        analysisService.retryFailedAnalysis(requestId);

        verify(analysisRepository).save(any());
        verify(asyncRunner).run(any());
    }

    @Test
    @DisplayName("메트릭 조회 실패")
    void testGetMetrics_Empty() {
        String requestId = "nonexistent";

        when(analysisRepository.findMetricsByRequestId(requestId))
                .thenReturn(Optional.empty());

        Optional<Metrics> result = analysisService.getMetrics(requestId);

        assertTrue(result.isEmpty());
    }
}
