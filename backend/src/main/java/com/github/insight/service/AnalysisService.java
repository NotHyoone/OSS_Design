package com.github.insight.service;

import com.github.insight.model.*;
import com.github.insight.model.enums.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final GithubApiClient githubApiClient;
    private final MetricCalculator metricCalculator;
    private final CompetencyScorer competencyScorer;
    private final FeedbackGenerator feedbackGenerator;
    private final ReportAssembler reportAssembler;
    private final ReportGenerator reportGenerator;
    private final AnalysisRepository analysisRepository;

    private AnalysisAsyncRunner asyncRunner;

    public AnalysisService(GithubApiClient githubApiClient,
                           MetricCalculator metricCalculator,
                           CompetencyScorer competencyScorer,
                           FeedbackGenerator feedbackGenerator,
                           ReportAssembler reportAssembler,
                           ReportGenerator reportGenerator,
                           AnalysisRepository analysisRepository) {
        this.githubApiClient      = githubApiClient;
        this.metricCalculator     = metricCalculator;
        this.competencyScorer     = competencyScorer;
        this.feedbackGenerator    = feedbackGenerator;
        this.reportAssembler      = reportAssembler;
        this.reportGenerator      = reportGenerator;
        this.analysisRepository   = analysisRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setAsyncRunner(@Lazy AnalysisAsyncRunner asyncRunner) {
        this.asyncRunner = asyncRunner;
    }

    public AnalysisRequest requestAnalysis(String userId, String githubId) {
        Optional<String> existingId = analysisRepository.findActiveRequestId(githubId);
        if (existingId.isPresent()) {
            try {
                AnalysisRequest existing = analysisRepository.findById(existingId.get());
                if (existing.isRunning() && canReuseActiveRequest(existing, userId)) return existing;
            } catch (Exception e) {
                log.debug("기존 분석 요청 조회 실패 ({}): {}", githubId, e.getMessage());
            }
        }

        AnalysisRequest req = AnalysisRequest.create(userId, githubId);
        analysisRepository.save(req);
        asyncRunner.run(req);
        return req;
    }

    public AnalysisRequest createRequest(String githubId) {
        return requestAnalysis(null, githubId);
    }

    public void executeAnalysis(AnalysisRequest req) {
        String requestId = req.getRequestId();
        String githubId  = req.getGithubId();

        try {
            req.transitionTo(RequestStatus.RUNNING);
            analysisRepository.save(req);

            /* Step 1: 데이터 수집 */
            if (updateProgressAndStopIfCancelled(req, 1, 5.0, "사용자 프로필 조회 중...")) return;
            if (!githubApiClient.validateUserExists(githubId)) {
                throw new IllegalArgumentException("GitHub 사용자를 찾을 수 없습니다: " + githubId);
            }

            if (updateProgressAndStopIfCancelled(req, 1, 15.0, "저장소 및 활동 데이터 수집 중...")) return;
            ActivityData activityData = githubApiClient.collectAll(githubId);
            activityData.setRequestId(requestId);

            if (updateProgressAndStopIfCancelled(req, 1, 28.0, "데이터 수집 완료")) return;

            /* Step 2: 지표 계산 */
            if (updateProgressAndStopIfCancelled(req, 2, 35.0, "활동성 지표 계산 중...")) return;
            Metrics metrics = metricCalculator.calculate(activityData);
            metrics.setRequestId(requestId);

            if (updateProgressAndStopIfCancelled(req, 2, 68.0, "지표 계산 완료")) return;
            analysisRepository.saveMetrics(requestId, metrics);

            if (isCancelledInRepository(req)) return;

            /* Step 3: 점수 산출 및 피드백 생성 */
            if (updateProgressAndStopIfCancelled(req, 3, 78.0, "종합 점수 산출 중...")) return;
            AnalysisResult result = competencyScorer.evaluate(metrics);
            result.setRequestId(requestId);
            result.setUserId(req.getUserId());
            result.setGithubId(githubId);
            result.setAvatarUrl(activityData.getAvatarUrl());

            if (updateProgressAndStopIfCancelled(req, 3, 88.0, "피드백 생성 중...")) return;
            List<String> weaknesses  = competencyScorer.identifyWeaknesses(metrics);
            List<FeedbackItem> items  = feedbackGenerator.generate(weaknesses);
            result.setImprovements(items);

            List<String> strengthCategories = competencyScorer.identifyStrengths(metrics);
            List<String> strengthMsgs = buildStrengthMessages(strengthCategories, activityData, metrics);
            result.setStrengths(strengthMsgs);

            if (updateProgressAndStopIfCancelled(req, 3, 95.0, "결과 저장 중...")) return;
            analysisRepository.saveResult(result);

            if (isCancelledInRepository(req)) return;
            req.markDone();
            analysisRepository.save(req);
            log.info("분석 완료: {} → 점수={}, 유형={}",
                githubId, result.getTotalScore(), result.getDeveloperType());

        } catch (Exception e) {
            log.error("분석 실패 ({}): {}", githubId, e.getMessage());
            req.markError(e.getMessage());
            analysisRepository.save(req);
        }
    }

    public Optional<AnalysisRequest> getRequest(String requestId) {
        try {
            return Optional.of(analysisRepository.findById(requestId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<AnalysisResult> getLatestResult(String githubId) {
        return analysisRepository.findLatestResultByGithubId(githubId);
    }

    public AnalysisResult getResult(String requestId) {
        return analysisRepository.findResultByRequestId(requestId);
    }

    public List<AnalysisResult> getHistory(String githubId) {
        return analysisRepository.findResultsByGithubId(githubId);
    }

    public List<AnalysisResult> getHistory(String userId, boolean byUserId) {
        if (byUserId) return analysisRepository.findResultsByUserId(userId);
        return analysisRepository.findResultsByGithubId(userId);
    }

    public Optional<Metrics> getMetrics(String requestId) {
        return analysisRepository.findMetricsByRequestId(requestId);
    }

    public void cancel(String requestId) {
        try {
            AnalysisRequest req = analysisRepository.findById(requestId);
            if (req.isRunning()) {
                markCancelled(req);
            }
        } catch (Exception e) {
            log.debug("분석 취소 실패 ({}): {}", requestId, e.getMessage());
        }
    }

    public void retryFailedAnalysis(String requestId) {
        AnalysisRequest req = analysisRepository.findById(requestId);
        if (req.canRetry()) {
            req.incrementRetry();
            req.transitionTo(RequestStatus.RUNNING);
            analysisRepository.save(req);
            asyncRunner.run(req);
        }
    }

    public boolean validateResultAccessToken(AnalysisRequest request, String token) {
        String expected = request.getResultAccessToken();
        return expected != null && !expected.isBlank()
            && token != null && !token.isBlank()
            && expected.equals(token);
    }

    private boolean canReuseActiveRequest(AnalysisRequest existing, String userId) {
        if (existing.getUserId() == null) {
            return userId == null;
        }
        return existing.getUserId().equals(userId);
    }

    private boolean updateProgressAndStopIfCancelled(AnalysisRequest req, int step, double pct, String detail) {
        if (isCancelledInRepository(req)) return true;
        req.updateProgress(step, pct, detail);
        analysisRepository.save(req);
        return false;
    }

    private boolean isCancelledInRepository(AnalysisRequest req) {
        try {
            AnalysisRequest latest = analysisRepository.findById(req.getRequestId());
            if (latest.isCancelled()) {
                markCancelled(req);
                return true;
            }
        } catch (Exception e) {
            log.debug("취소 상태 조회 실패 ({}): {}", req.getRequestId(), e.getMessage());
        }
        return false;
    }

    private void markCancelled(AnalysisRequest req) {
        req.markError("CANCELLED");
        req.updateProgress(req.getStep(), req.getOverallPct(), "분석이 취소되었습니다.");
        analysisRepository.save(req);
    }

    private List<String> buildStrengthMessages(List<String> categories,
                                                ActivityData data,
                                                Metrics metrics) {
        java.util.stream.Stream.Builder<String> msgs = java.util.stream.Stream.builder();

        for (String cat : categories) {
            String msg = switch (cat) {
                case "activity"      -> "꾸준한 커밋 활동으로 높은 활동성 유지";
                case "diversity"     -> String.format("다양한 언어·기술 스택 경험 (%d개 언어)",
                    data.getLanguages().size());
                case "collaboration" -> "PR·Issue를 통한 활발한 협업 경험";
                case "persistence"   -> "장기적으로 꾸준한 활동 패턴 유지";
                default              -> cat;
            };
            msgs.accept(msg);
        }

        long ownStars = data.getRepositories().stream()
            .filter(r -> !r.isFork())
            .mapToLong(RepositoryData::getStarCount).sum();
        if (ownStars >= 5) {
            msgs.accept(String.format("오픈소스 저장소에서 총 %d개 스타 획득", ownStars));
        }

        List<String> result = msgs.build().collect(java.util.stream.Collectors.toList());
        if (result.isEmpty()) {
            result.add("GitHub 활동을 시작하여 역량을 쌓아가는 중");
        }
        return result;
    }
}
