# GitHub Activity Insight - Structure Draft

## 1) 핵심 클래스 책임표

| No | Class | Type | Core Responsibility | Main Collaborators |
| :-- | :-- | :-- | :-- | :-- |
| 1 | User | Entity | 서비스 사용자 식별, 기본 프로필/권한 정보 보관 | GithubProfile, AnalysisRequest, AnalysisResult |
| 2 | GithubProfile | Entity | GitHub 계정 식별자와 검증 상태, 공개 프로필 메타데이터 보관 | User, AnalysisRequest |
| 3 | AnalysisRequest | Domain Model / Entity Mapping | 분석 요청 상태 관리(PENDING/RUNNING/COMPLETED/PARTIAL/CANCELLED/FAILED), 진행 단계(step/overallPct/detail), 요청 시점과 처리 이력 관리 | User, AnalysisService, AnalysisAsyncRunner, AnalysisRepository |
| 4 | ActivityData | Value Object | 저장소, 커밋, PR, Issue, 언어 통계 등 원천 활동 데이터 묶음 표현 | GithubApiClient, AnalysisService, MetricCalculator |
| 5 | Metrics | Value Object | 활동성, 다양성, 협업도, 지속성, 신뢰도 등 정량 지표 묶음 표현 | MetricCalculator, CompetencyScorer, FeedbackGenerator |
| 6 | FeedbackItem | Value Object | 강점, 약점, 개선 액션을 우선순위와 함께 표현 | FeedbackGenerator, AnalysisResult |
| 7 | AnalysisResult | Entity | totalScore, developerType, Metrics, FeedbackItem 목록을 포함한 최종 결과 보관 | User, AnalysisRequest, ReportAssembler |
| 8 | GithubApiClient | Infrastructure | GitHub REST API 호출, 사용자 검증, 저장소/커밋/언어/PR/Issue 수집, Rate Limit 처리 | AnalysisService |
| 9 | AnalysisService | Service | 분석 요청 생성, 활성 요청 재사용, 비동기 실행 트리거, 결과/이력/메트릭 조회, 취소 처리 | GithubApiClient, MetricCalculator, CompetencyScorer, FeedbackGenerator, AnalysisRepository, AnalysisAsyncRunner |
| 10 | AnalysisAsyncRunner | Service | Spring `@Async` 프록시를 통해 분석 파이프라인을 별도 스레드에서 실행 | AnalysisService |
| 11 | MetricCalculator | Service | ActivityData로부터 4개 핵심 지표 계산 및 신뢰도/설명 생성 | CompetencyScorer |
| 12 | CompetencyScorer | Service | Metrics 기반 totalScore, DeveloperType, strengths/weaknesses 계산 | MetricCalculator, FeedbackGenerator, AnalysisResult |
| 13 | FeedbackGenerator | Service | 약점 목록을 FeedbackItem 개선 권고로 변환 | CompetencyScorer, AnalysisResult |
| 14 | ReportAssembler | Service | AnalysisResult와 Metrics를 화면/PDF용 Map ViewModel로 조립 | AnalysisResult, ReportGenerator |
| 15 | ReportGenerator | Service | OpenPDF 기반 A4 PDF 바이트와 다운로드 파일명 생성 | ReportAssembler |
| 16 | AnalysisRepository | Service / Persistence Adapter | JPA Repository를 감싸 도메인 모델과 Entity 변환, 최근 10건 이력 제한, 만료 데이터 정리 수행 | AnalysisRequestRepository, AnalysisResultRepository, MetricsRepository |

---

## 2) 현재 구현 기준 주요 메서드 시그니처 (Java)

아래 시그니처는 초기 설계 초안이 아니라 현재 `backend/src/main/java/com/github/insight` 구현을 기준으로 정리한 스냅샷이다. 세부 DTO/Entity getter/setter는 생략하고, 유스케이스 흐름을 설명하는 핵심 메서드만 포함한다.

```java
// AuthenticationService
public class AuthenticationService {
    public boolean isOAuthConfigured();
    public String initiateOAuthFlow();
    public User handleOAuthCallback(String code, String state);
    public Optional<User> getUserBySession(String sessionId);
    public void invalidateSession(String sessionId);
    public void purgeExpiredSessions();
}

// OAuthClient
public class OAuthClient {
    public boolean isConfigured();
    public String getAuthorizationUrl(String state);
    public String exchangeCodeForToken(String code);
    public User getUserProfile(String accessToken);
}

// AnalysisRequest
public class AnalysisRequest {
    public static AnalysisRequest create(String userId, String githubId);
    public static AnalysisRequest restore(...);
    public boolean transitionTo(RequestStatus newStatus);
    public void updateProgress(int step, double overallPct, String detail);
    public void markDone();
    public void markError(String message);
    public void markCancelled(String message);
    public boolean isRunning();
    public boolean isCancelled();
    public boolean canRetry();
    public void incrementRetry();
}

// GithubApiClient
public class GithubApiClient {
    public boolean validateUserExists(String githubId);
    public ActivityData collectAll(String githubId);
    public List<RepositoryData> getRepositories(String githubId);
    public List<CommitData> getCommits(String githubId, String repoName);
    public Map<String, Integer> getLanguages(String githubId, String repoName);
    public List<PullRequestData> getPullRequests(String githubId);
    public List<IssueData> getIssues(String githubId);
}

// AnalysisService + Async Runner
public class AnalysisService {
    public AnalysisRequest requestAnalysis(String userId, String githubId);
    public AnalysisRequest createRequest(String githubId);
    public void executeAnalysis(AnalysisRequest request);
    public Optional<AnalysisRequest> getRequest(String requestId);
    public Optional<AnalysisResult> getLatestResult(String githubId);
    public AnalysisResult getResult(String requestId);
    public List<AnalysisResult> getHistory(String githubId);
    public Optional<Metrics> getMetrics(String requestId);
    public void cancel(String requestId);
    public void retryFailedAnalysis(String requestId);
}

public class AnalysisAsyncRunner {
    @Async("analysisExecutor")
    public void run(AnalysisRequest request);
}

// Metric / Score / Feedback
public class MetricCalculator {
    public Metrics calculate(ActivityData data);
    public float calculateActivity(ActivityData data);
    public float calculateDiversity(ActivityData data);
    public float calculateCollaboration(ActivityData data);
    public float calculatePersistence(ActivityData data);
}

public class CompetencyScorer {
    public AnalysisResult evaluate(Metrics metrics);
    public int score(Metrics metrics);
    public DeveloperType classifyType(int score);
    public List<String> identifyStrengths(Metrics metrics);
    public List<String> identifyWeaknesses(Metrics metrics);
}

public class FeedbackGenerator {
    public List<FeedbackItem> generate(List<String> weaknesses);
}

// Report / Persistence
public class ReportAssembler {
    public Map<String, Object> toViewModel(AnalysisResult result, Metrics metrics);
    public Map<String, Object> toPdfModel(AnalysisResult result, Metrics metrics);
}

public class ReportGenerator {
    public byte[] renderPdf(AnalysisResult result, Metrics metrics);
    public String getFilename(String githubId, LocalDateTime createdAt);
}

public class AnalysisRepository {
    public void save(AnalysisRequest request);
    public AnalysisRequest findById(String requestId);
    public Optional<String> findActiveRequestId(String githubId);
    public void saveResult(AnalysisResult result);
    public void saveMetrics(String requestId, Metrics metrics);
    public AnalysisResult findResultByRequestId(String requestId);
    public Optional<AnalysisResult> findLatestResultByGithubId(String githubId);
    public List<AnalysisResult> findResultsByGithubId(String githubId);
    public List<AnalysisResult> findResultsByUserId(String userId);
    public Optional<Metrics> findMetricsByRequestId(String requestId);
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired();
}
```

---

---

## 3) 추천 기술 스택

프로젝트 요구사항(웹 기반, GitHub API 연동, 점수/피드백, 이력 저장, PDF 출력)에 맞춘 최소 실전 스택:

| Layer | Stack | 선택 이유 |
| :-- | :-- | :-- |
| Backend | Java 17, Spring Boot 3.2.x | 현재 `backend/pom.xml` 기준. 수업 요구(Java) 충족, 모듈 분리 용이 |
| API Client | Spring RestTemplate 기반 `GithubApiClient` | 현재 구현 기준. GitHub REST API 호출, 사용자 검증, 이벤트/저장소/언어 수집 처리 |
| Database | PostgreSQL(default) + H2(local) + Spring Data JPA | 운영은 PostgreSQL `ddl-auto=validate`, 로컬은 H2 file DB `ddl-auto=create` |
| Async | Spring `@Async` + `ThreadPoolTaskExecutor` | 현재 분석 실행 방식. Redis/외부 큐는 미적용 |
| Auth | GitHub OAuth 2.0 + HttpOnly SESSION_ID Cookie | 현재 구현 기준. Spring Security는 별도 적용하지 않음 |
| PDF | OpenPDF | 현재 구현 기준. 서버에서 A4 PDF 바이트 생성 |
| Frontend | 정적 HTML/CSS/Vanilla JS (`web/`) | 현재 구현 기준. Spring Boot가 `file:web/`로 직접 서빙 |
| Chart/UI | Vanilla JS, SVG/CSS 게이지, Canvas sparkline | 현재 구현 기준. 외부 차트 라이브러리 미적용 |
| Infra | Maven 실행 + PostgreSQL 초기화 스크립트 | 현재 문서화된 실행 방식. Docker Compose는 미적용 |
| Test | JUnit 5, Spring Boot Test, Mockito | 현재 Maven 테스트 기준. Testcontainers는 미적용 |

초기 MVP에서는 Redis/Queue를 제외하고 시작해도 된다.

---

## 4) 단계별 추진 방법

### 2.2 분석 단계 (What 정의)

목표: "무엇을 만들지"를 명확히 고정한다.

1. 유스케이스 상세화
   - UC-01 GitHub ID 입력
   - UC-02 분석 요청 생성
   - UC-03 GitHub 데이터 수집
   - UC-04 지표 계산
   - UC-05 점수/피드백 생성
   - UC-06 결과 조회
   - UC-07 PDF 리포트 다운로드
   - UC-08 분석 이력 비교

2. 도메인 모델 고정
   - 엔티티: User, GithubProfile, AnalysisRequest, AnalysisResult
   - 값 객체/DTO: ActivityData, Metrics, FeedbackItem, DashboardView, PdfReportView

3. 규칙 정의
   - 점수 계산식(가중치): 예) 활동성 25, 다양성 25, 협업도 25, 지속성 25
   - 피드백 규칙: 임계값별 액션 템플릿

4. 비기능 요구사항을 수치화
   - 평균 조회 2초 이내
   - 분석 완료 60초 이내(저장소 30개 기준)
   - 실패 재시도 3회

5. 산출물
   - 유스케이스 명세서
   - 도메인 클래스 다이어그램(분석 수준)
   - KPI/점수 규칙 문서

### 2.3 설계 단계 (How 구조화)

목표: "어떻게 만들지"를 아키텍처와 인터페이스로 확정한다.

1. 아키텍처 설계
   - Layered 구조: presentation -> application -> domain -> infrastructure
   - 외부 연동(GitHub API) 어댑터 분리

2. 클래스 설계
   - 위 핵심 클래스들을 기준으로 인터페이스/구현체 분리
   - 에러 모델 정의: RateLimitException, ExternalApiException, DataIntegrityException

3. 시퀀스 다이어그램 작성
   - 분석 요청부터 리포트 저장까지
   - 실패 시 재시도/부분 실패 플로우

4. 데이터베이스 설계
   - tables: users, github_profiles, analysis_requests, analysis_results, feedback_items
   - 인덱스: (user_id, requested_at), (github_username, requested_at)

5. API 설계
   - POST /api/analyses
   - GET /api/analyses/{id}
   - GET /api/users/{userId}/analyses
   - GET /api/analyses/{id}/pdf

6. 산출물
   - 설계 클래스 다이어그램
   - API 명세(OpenAPI)
   - DB ERD
   - 컴포넌트 다이어그램

### 2.4 구현 단계 (Build & Verify)

목표: MVP 동작 + 검증 가능한 품질 확보.

1. 스켈레톤 구현
   - Spring Boot 프로젝트 생성
   - 패키지 구조/10개 클래스 기본 구현

2. 핵심 기능 구현
   - GitHub API 연동 + 페이징 + 재시도
   - 데이터 정규화 및 지표 계산
   - 점수/피드백 생성
   - 분석 결과 저장 및 조회

3. UI/PDF 구현
   - 결과 대시보드(점수 + 카테고리 + 개선 액션)
   - PDF 다운로드

4. 테스트
   - 단위 테스트: MetricCalculator, CompetencyScorer, FeedbackGenerator
   - 통합 테스트: 분석 요청 end-to-end

5. 운영 준비
   - Docker Compose로 DB 포함 실행
   - 환경변수로 GitHub 토큰/DB 설정 분리

6. 데모 준비
   - 20분 발표 시나리오
   - 샘플 GitHub ID 2~3개 결과 비교

---

## 5) 지금 당장 시작 순서 (실행용)

1. 2.2 문서에서 점수 가중치와 피드백 규칙 표를 먼저 확정한다.
2. 2.3에서 10개 클래스의 클래스 다이어그램과 1개 핵심 시퀀스를 그린다.
3. 2.4에서 API 2개(분석 요청/결과 조회)만 먼저 구현해 MVP를 만든다.
4. 이후 PDF 출력과 고도화(캐시, 재시도 최적화)를 추가한다.

---

## 5) 현재 구현 반영 메모

- `ActivityCollector`와 `DataNormalizer`는 별도 클래스로 구현되지 않았고, 현재는 `GithubApiClient.collectAll()`과 `MetricCalculator.calculate()`가 해당 책임을 나누어 수행한다.
- 분석 요청 생성은 로그인 없이도 가능하다. 로그인 요청은 `userId`가 저장되고, 비로그인 요청은 `userId=null`로 저장된다.
- 결과/리포트 조회는 두 경로로 나뉜다. GitHub ID 기준 최신 조회는 로그인 본인만 가능하고, requestId 기준 조회는 요청 소유자가 없으면 허용된다.
- 이력 조회는 로그인 사용자 본인 GitHub ID 기준이며 최근 10건으로 제한된다.
- `AnalysisRepository.purgeExpired()`는 1시간 주기로 완료/실패/부분 완료 요청, 2시간 이상 멈춘 RUNNING 요청, 72시간 초과 결과/지표를 정리한다.
