package com.github.insight.service;

import com.github.insight.dto.GithubRepo;
import com.github.insight.dto.GithubUserData;
import com.github.insight.model.AnalysisRequest;
import com.github.insight.model.AnalysisRequest.Status;
import com.github.insight.model.AnalysisResult;
import com.github.insight.model.MetricScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 분석 요청 생성·진행·결과 조회·이력 관리를 담당하는 핵심 서비스.
 * <p>
 * 저장소는 인메모리(ConcurrentHashMap)를 사용합니다.
 * 서버 재시작 시 데이터가 초기화됩니다.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final GithubApiService githubApiService;
    private AnalysisAsyncRunner asyncRunner; // setter injection으로 순환 의존 방지

    /** requestId → AnalysisRequest */
    private final Map<String, AnalysisRequest> requests = new ConcurrentHashMap<>();

    /** githubId → 분석 결과 이력 (최신 순으로 앞에 추가) */
    private final Map<String, List<AnalysisResult>> history = new ConcurrentHashMap<>();

    /** githubId → 진행 중인 requestId (중복 실행 방지용) */
    private final Map<String, String> activeRequest = new ConcurrentHashMap<>();

    public AnalysisService(GithubApiService githubApiService) {
        this.githubApiService = githubApiService;
    }

    /** @Lazy로 순환 의존 해결 */
    @org.springframework.beans.factory.annotation.Autowired
    public void setAsyncRunner(@Lazy AnalysisAsyncRunner asyncRunner) {
        this.asyncRunner = asyncRunner;
    }

    /* ── 요청 생성 ── */

    /**
     * 분석 요청을 생성하고 비동기로 분석을 시작합니다.
     *
     * @param githubId 분석 대상 GitHub ID
     * @return 생성된 AnalysisRequest
     */
    public AnalysisRequest createRequest(String githubId) {
        // 동일 사용자 중복 RUNNING 요청 차단
        String existingReqId = activeRequest.get(githubId);
        if (existingReqId != null) {
            AnalysisRequest existing = requests.get(existingReqId);
            if (existing != null && existing.getStatus() == Status.RUNNING) {
                return existing;
            }
        }

        String requestId = UUID.randomUUID().toString();
        AnalysisRequest req = new AnalysisRequest(requestId, githubId);
        requests.put(requestId, req);
        activeRequest.put(githubId, requestId);

        asyncRunner.run(req);

        return req;
    }

    /* ── 분석 실행 (AnalysisAsyncRunner에서 @Async로 호출됨) ── */

    public void executeAnalysis(AnalysisRequest req) {
        String requestId = req.getRequestId();
        String githubId  = req.getGithubId();

        try {
            req.setStatus(Status.RUNNING);

            /* ── Step 1: GitHub 데이터 수집 ── */
            req.updateProgress(1, 5.0, "사용자 프로필 조회 중...");
            GithubUserData user = githubApiService.getUser(githubId)
                    .orElseThrow(() -> new IllegalArgumentException("GitHub 사용자를 찾을 수 없습니다: " + githubId));

            req.updateProgress(1, 15.0, "저장소 목록 조회 중...");
            List<GithubRepo> repos = githubApiService.getRepos(githubId);

            req.updateProgress(1, 28.0, "활동 이벤트 조회 중...");
            List<Map<String, Object>> events = githubApiService.getEvents(githubId);

            if (req.isCancelled()) return;

            /* ── Step 2: 지표 계산 ── */
            req.updateProgress(2, 35.0, "활동성 지표 계산 중...");
            MetricScore activity  = calcActivity(events);

            req.updateProgress(2, 48.0, "기술 다양성 지표 계산 중...");
            MetricScore diversity = calcDiversity(repos);

            req.updateProgress(2, 58.0, "협업도 지표 계산 중...");
            MetricScore collab    = calcCollab(events, repos);

            req.updateProgress(2, 68.0, "지속성 지표 계산 중...");
            MetricScore persist   = calcPersistence(events, repos);

            if (req.isCancelled()) return;

            /* ── Step 3: 점수·피드백 생성 ── */
            req.updateProgress(3, 78.0, "종합 점수 계산 중...");
            int totalScore = (int) Math.round(
                    activity.score()  * 0.30 +
                    diversity.score() * 0.25 +
                    collab.score()    * 0.25 +
                    persist.score()   * 0.20
            );

            req.updateProgress(3, 88.0, "개발자 유형 판별 중...");
            String developerType = classifyDeveloper(totalScore);

            req.updateProgress(3, 93.0, "강점 및 개선 피드백 생성 중...");
            String trustLevel = deriveTrustLevel(repos, events);
            String summaryText = buildSummary(githubId, totalScore, developerType,
                    activity.score(), diversity.score(), collab.score(), persist.score());

            List<String> strengths    = buildStrengths(activity.score(), diversity.score(),
                                                        collab.score(), persist.score(), repos);
            List<String> improvements = buildImprovements(activity.score(), diversity.score(),
                                                           collab.score(), persist.score());

            Map<String, MetricScore> metrics = Map.of(
                    "activity",  activity,
                    "diversity", diversity,
                    "collab",    collab,
                    "persist",   persist
            );

            AnalysisResult result = new AnalysisResult(
                    githubId,
                    user.avatarUrl(),
                    Instant.now().toString(),
                    developerType,
                    totalScore,
                    trustLevel,
                    summaryText,
                    metrics,
                    strengths,
                    improvements
            );

            // 이력 저장 (최신이 앞)
            history.computeIfAbsent(githubId, k -> Collections.synchronizedList(new ArrayList<>()))
                   .add(0, result);

            req.markDone();
            log.info("분석 완료: {} → 점수={}, 유형={}", githubId, totalScore, developerType);

        } catch (Exception e) {
            log.error("분석 실패 ({}): {}", githubId, e.getMessage());
            req.markError(e.getMessage());
        } finally {
            activeRequest.remove(githubId, requestId);
        }
    }

    /* ── 상태 조회 ── */

    public Optional<AnalysisRequest> getRequest(String requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }

    /* ── 결과 조회 ── */

    public Optional<AnalysisResult> getLatestResult(String githubId) {
        List<AnalysisResult> list = history.get(githubId);
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }

    /* ── 이력 조회 ── */

    public List<AnalysisResult> getHistory(String githubId) {
        List<AnalysisResult> list = history.get(githubId);
        if (list == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    /* ── 취소 ── */

    public void cancel(String requestId) {
        AnalysisRequest req = requests.get(requestId);
        if (req != null && req.getStatus() == Status.RUNNING) {
            req.markError("CANCELLED");
            activeRequest.remove(req.getGithubId(), requestId);
        }
    }

    /* ================================================================
       지표 계산 로직
       ================================================================ */

    /** 활동성: 최근 90일 커밋 수 + 활동 일수 기반 */
    private MetricScore calcActivity(List<Map<String, Object>> events) {
        long totalCommits = 0;
        Set<String> activeDays = new HashSet<>();
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);

        for (Map<String, Object> event : events) {
            String createdAt = (String) event.get("created_at");
            if (createdAt == null) continue;
            try {
                Instant ts = Instant.parse(createdAt);
                if (ts.isBefore(cutoff)) continue;
                String day = createdAt.substring(0, 10);
                activeDays.add(day);

                if ("PushEvent".equals(event.get("type"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                    if (payload != null) {
                        Object size = payload.get("size");
                        if (size instanceof Number) totalCommits += ((Number) size).longValue();
                    }
                }
            } catch (DateTimeParseException ignored) {}
        }

        // 점수화: 커밋 70% + 활동일 30%
        int commitScore = (int) Math.min(100, totalCommits * 100 / 120L);
        int dayScore    = (int) Math.min(100, activeDays.size() * 100 / 60);
        int score       = (int) (commitScore * 0.7 + dayScore * 0.3);

        String desc;
        if (totalCommits >= 100)       desc = String.format("90일간 커밋 %d회 – 매우 활발", totalCommits);
        else if (totalCommits >= 50)   desc = String.format("90일간 커밋 %d회 – 활발", totalCommits);
        else if (totalCommits >= 20)   desc = String.format("90일간 커밋 %d회 – 보통", totalCommits);
        else if (totalCommits > 0)     desc = String.format("90일간 커밋 %d회 – 낮음", totalCommits);
        else                           desc = "최근 90일 공개 커밋 없음";

        return new MetricScore(score, desc);
    }

    /** 기술 다양성: 고유 언어 수 기반 */
    private MetricScore calcDiversity(List<GithubRepo> repos) {
        Set<String> langs = repos.stream()
                .filter(r -> !r.fork() && r.language() != null && !r.language().isBlank())
                .map(GithubRepo::language)
                .collect(Collectors.toSet());
        int count = langs.size();

        int score = switch (count) {
            case 0 -> 8;
            case 1 -> 25;
            case 2 -> 42;
            case 3 -> 58;
            case 4 -> 70;
            case 5 -> 80;
            case 6 -> 87;
            case 7 -> 92;
            default -> 96;
        };

        String langList = langs.isEmpty() ? "없음" : String.join(", ", langs);
        String desc = count == 0
                ? "공개 저장소에서 언어 정보 없음"
                : String.format("%d개 언어 사용 (%s)", count, langList);

        return new MetricScore(score, desc);
    }

    /** 협업도: PR·Issue 이벤트 + 포크 저장소 비율 기반 */
    private MetricScore calcCollab(List<Map<String, Object>> events, List<GithubRepo> repos) {
        Set<String> collabTypes = Set.of(
                "PullRequestEvent", "IssuesEvent", "IssueCommentEvent",
                "PullRequestReviewEvent", "PullRequestReviewCommentEvent"
        );
        long collabCount = events.stream()
                .filter(e -> collabTypes.contains(e.get("type")))
                .count();

        long forkRepoCount = repos.stream().filter(GithubRepo::fork).count();
        long totalRepos    = repos.size();
        double forkRatio   = totalRepos > 0 ? (double) forkRepoCount / totalRepos : 0.0;

        // collabEvents → 0~70점, forkRatio → 0~30점
        int eventScore = (int) Math.min(70, collabCount * 70 / 25.0);
        int forkScore  = (int) (forkRatio * 30);
        int score      = eventScore + forkScore;

        String desc;
        if (collabCount >= 20)       desc = String.format("PR·Issue 참여 %d건 – 활발한 협업", collabCount);
        else if (collabCount >= 8)   desc = String.format("PR·Issue 참여 %d건 – 보통", collabCount);
        else if (collabCount >= 2)   desc = String.format("PR·Issue 참여 %d건 – 초기 단계", collabCount);
        else                         desc = "PR·Issue 이벤트 거의 없음";

        return new MetricScore(score, desc);
    }

    /** 지속성: 최근 1년 활동 월 수 기반 */
    private MetricScore calcPersistence(List<Map<String, Object>> events, List<GithubRepo> repos) {
        Set<String> activeMonths = new HashSet<>();
        Instant cutoff = Instant.now().minus(365, ChronoUnit.DAYS);

        // 이벤트에서 활동 월 추출
        for (Map<String, Object> event : events) {
            String createdAt = (String) event.get("created_at");
            if (createdAt != null && createdAt.length() >= 7) {
                try {
                    Instant ts = Instant.parse(createdAt);
                    if (!ts.isBefore(cutoff)) activeMonths.add(createdAt.substring(0, 7));
                } catch (DateTimeParseException ignored) {}
            }
        }

        // 저장소 pushedAt에서 활동 월 보완
        for (GithubRepo repo : repos) {
            String pushedAt = repo.pushedAt();
            if (pushedAt != null && pushedAt.length() >= 7) {
                try {
                    Instant ts = Instant.parse(pushedAt);
                    if (!ts.isBefore(cutoff)) activeMonths.add(pushedAt.substring(0, 7));
                } catch (DateTimeParseException ignored) {}
            }
        }

        int months = activeMonths.size(); // 최대 12
        int score = months >= 12 ? 100
                  : months >= 10 ? 90
                  : months >= 8  ? 78
                  : months >= 6  ? 64
                  : months >= 4  ? 50
                  : months >= 2  ? 32
                  : months == 1  ? 18
                  : 5;

        String desc = months == 0
                ? "최근 1년 공개 활동 없음"
                : String.format("최근 1년 중 %d개월 활동", months);

        return new MetricScore(score, desc);
    }

    /* ================================================================
       분류·텍스트 생성 유틸
       ================================================================ */

    private String classifyDeveloper(int score) {
        if (score >= 86) return "Expert";
        if (score >= 71) return "Senior Developer";
        if (score >= 56) return "Mid-level Developer";
        if (score >= 41) return "Junior Developer";
        if (score >= 21) return "Learner";
        return "Beginner";
    }

    private String deriveTrustLevel(List<GithubRepo> repos, List<Map<String, Object>> events) {
        if (repos.size() >= 3 && events.size() >= 5) return "HIGH";
        if (repos.size() >= 1 || events.size() >= 2) return "MEDIUM";
        return "LOW";
    }

    private String buildSummary(String githubId, int totalScore, String devType,
                                 int activity, int diversity, int collab, int persist) {
        String best = List.of(
                Map.entry("활동성", activity),
                Map.entry("기술 다양성", diversity),
                Map.entry("협업도", collab),
                Map.entry("지속성", persist)
        ).stream()
         .max(Comparator.comparingInt(Map.Entry::getValue))
         .map(Map.Entry::getKey)
         .orElse("전반적인 역량");

        return String.format(
                "%s님은 종합 %d점으로 '%s' 수준으로 평가됩니다. " +
                "특히 %s 항목이 두드러지며, 꾸준한 활동을 유지하면 다음 단계로 성장할 수 있습니다.",
                githubId, totalScore, devType, best);
    }

    private List<String> buildStrengths(int activity, int diversity, int collab, int persist,
                                         List<GithubRepo> repos) {
        List<String> list = new ArrayList<>();
        if (activity >= 65)  list.add("꾸준한 커밋 활동으로 높은 활동성 유지");
        if (diversity >= 60) list.add(String.format("다양한 언어·기술 스택 경험 (%d개 언어)",
                repos.stream().filter(r -> !r.fork() && r.language() != null)
                     .map(GithubRepo::language).collect(Collectors.toSet()).size()));
        if (collab >= 55)    list.add("PR·Issue를 통한 활발한 협업 경험");
        if (persist >= 65)   list.add("장기적으로 꾸준한 활동 패턴 유지");

        long ownStars = repos.stream().filter(r -> !r.fork()).mapToLong(GithubRepo::stargazersCount).sum();
        if (ownStars >= 5)   list.add(String.format("오픈소스 저장소에서 총 %d개 스타 획득", ownStars));

        if (list.isEmpty()) list.add("GitHub 활동을 시작하여 역량을 쌓아가는 중");
        return list;
    }

    private List<String> buildImprovements(int activity, int diversity, int collab, int persist) {
        List<String> list = new ArrayList<>();
        if (activity < 50)   list.add("더 자주 커밋하는 습관 기르기 (주 3회 이상 목표)");
        if (diversity < 50)  list.add("새로운 언어 또는 프레임워크 학습 시도");
        if (collab < 50)     list.add("오픈소스 프로젝트에 PR·Issue로 기여 시작");
        if (persist < 55)    list.add("꾸준한 활동 패턴 유지 (매월 1개 이상 커밋)");
        if (collab < 70)     list.add("타인의 저장소 포크 후 개선 사항 반영하여 PR 도전");

        // 중복 제거 후 최대 4개
        return list.stream().distinct().limit(4).collect(Collectors.toList());
    }
}
