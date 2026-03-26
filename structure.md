# GitHub Activity Insight - Structure Draft

## 1) 클래스 10개 책임표

| No | Class | Type | Core Responsibility | Main Collaborators |
| :-- | :-- | :-- | :-- | :-- |
| 1 | User | Entity | 서비스 사용자 식별, 기본 프로필/권한 정보 보관 | GithubProfile, AnalysisRequest |
| 2 | GithubProfile | Entity | GitHub 계정 식별자(username), 공개 프로필 메타데이터 보관 | User, AnalysisRequest |
| 3 | AnalysisRequest | Entity | 분석 요청 상태 관리(PENDING/RUNNING/SUCCESS/FAILED), 요청 범위 관리 | User, GithubApiClient, ReportAssembler |
| 4 | GithubApiClient | Infrastructure | GitHub REST/GraphQL 호출, 페이징/Rate Limit/재시도 처리 | ActivityCollector |
| 5 | ActivityCollector | Service | API 응답을 모아 Raw Activity Dataset 생성 | GithubApiClient, DataNormalizer |
| 6 | DataNormalizer | Service | Raw 데이터를 분석 가능한 정규화 구조로 변환 | ActivityCollector, MetricCalculator |
| 7 | MetricCalculator | Service | 활동성/협업도/기술다양성/지속성 지표 산출 | DataNormalizer, CompetencyScorer |
| 8 | CompetencyScorer | Service | 지표 기반 역량 점수 계산 및 카테고리별 점수 분해 | MetricCalculator, FeedbackGenerator |
| 9 | FeedbackGenerator | Service | 점수/지표를 해석해 실행 가능한 개선 피드백 생성 | CompetencyScorer, ReportAssembler |
| 10 | ReportAssembler | Service | 최종 결과(JSON/PDF용 ViewModel) 조립 및 저장 포맷 구성 | FeedbackGenerator, AnalysisRequest |

---

## 2) 메서드 시그니처 초안 (Java)

아래 시그니처는 2.1 개념화 문서를 2.2/2.3으로 연결하기 위한 1차 인터페이스 초안이다.

```java
// 1) User
public class User {
    private Long id;
    private String email;
    private String displayName;

    public void updateDisplayName(String displayName);
    public boolean isSameUser(Long userId);
}

// 2) GithubProfile
public class GithubProfile {
    private Long id;
    private Long userId;
    private String githubUsername;
    private String profileUrl;

    public void changeGithubUsername(String githubUsername);
    public boolean matches(String githubUsername);
}

// 3) AnalysisRequest
public class AnalysisRequest {
    private Long id;
    private Long userId;
    private String githubUsername;
    private AnalysisStatus status;
    private java.time.Instant requestedAt;
    private java.time.Instant finishedAt;

    public void markRunning();
    public void markSuccess();
    public void markFailed(String reason);
    public boolean isTerminal();
}

// 4) GithubApiClient
public interface GithubApiClient {
    RepoActivityResponse fetchRepositories(String githubUsername, int page, int perPage);
    CommitActivityResponse fetchCommits(String githubUsername, String repoName, int page, int perPage);
    ContributionResponse fetchContributionSummary(String githubUsername);
}

// 5) ActivityCollector
public interface ActivityCollector {
    RawActivityDataset collect(String githubUsername);
    RawActivityDataset collectWithinPeriod(String githubUsername, java.time.LocalDate from, java.time.LocalDate to);
}

// 6) DataNormalizer
public interface DataNormalizer {
    NormalizedActivityData normalize(RawActivityDataset raw);
    NormalizedActivityData removeOutliers(NormalizedActivityData normalized);
}

// 7) MetricCalculator
public interface MetricCalculator {
    ActivityMetrics calculate(NormalizedActivityData data);
    java.util.Map<String, Double> calculateSubMetrics(NormalizedActivityData data);
}

// 8) CompetencyScorer
public interface CompetencyScorer {
    CompetencyScore score(ActivityMetrics metrics);
    java.util.Map<String, Integer> scoreByCategory(ActivityMetrics metrics);
}

// 9) FeedbackGenerator
public interface FeedbackGenerator {
    FeedbackReport generate(CompetencyScore score, ActivityMetrics metrics);
    java.util.List<String> suggestActionItems(CompetencyScore score, ActivityMetrics metrics);
}

// 10) ReportAssembler
public interface ReportAssembler {
    AnalysisReport assemble(AnalysisRequest request, ActivityMetrics metrics, CompetencyScore score, FeedbackReport feedback);
    PdfReportView toPdfView(AnalysisReport report);
}
```

---

## 3) 추천 기술 스택

프로젝트 요구사항(웹 기반, GitHub API 연동, 점수/피드백, 이력 저장, PDF 출력)에 맞춘 최소 실전 스택:

| Layer | Stack | 선택 이유 |
| :-- | :-- | :-- |
| Backend | Java 21, Spring Boot 3.x | 수업 요구(Java) 충족, 모듈 분리 용이 |
| API Client | Spring WebClient or OpenFeign | GitHub API 호출/재시도/타임아웃 정책 구현 용이 |
| Database | PostgreSQL + Spring Data JPA | 분석 이력/결과 저장 및 조회 안정성 |
| Cache/Queue(옵션) | Redis + Spring Cache | 반복 조회 성능 개선, 호출량 완화 |
| Auth(초기 단순) | Spring Security (email login or GitHub OAuth) | 사용자별 이력 접근 제어 |
| PDF | OpenHTMLtoPDF or iText | 분석 결과 PDF 내보내기 |
| Frontend | React + TypeScript + Vite | 대시보드/차트 UI 빠른 구현 |
| Chart | Recharts or ECharts | 지표 시각화 |
| Infra | Docker Compose | 로컬 개발 환경 재현성 확보 |
| Test | JUnit 5, Mockito, Testcontainers | 계산/평가 로직 신뢰성 확보 |

초기 MVP에서는 Redis/Queue를 제외하고 시작해도 된다.

---

## 4) 단계별 추진 방법

### 2.2 분석 단계 (What 정의)

목표: "무엇을 만들지"를 명확히 고정한다.

1. 유스케이스 상세화
   - UC-01 분석 요청 생성
   - UC-02 GitHub 데이터 수집
   - UC-03 지표 계산
   - UC-04 점수/피드백 생성
   - UC-05 결과 저장/조회/PDF 다운로드

2. 도메인 모델 고정
   - 엔티티: User, GithubProfile, AnalysisRequest
   - 값 객체/DTO: RawActivityDataset, NormalizedActivityData, ActivityMetrics, CompetencyScore, FeedbackReport, AnalysisReport

3. 규칙 정의
   - 점수 계산식(가중치): 예) 활동성 30, 지속성 25, 협업도 25, 기술다양성 20
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
   - 위 10개 클래스를 기준으로 인터페이스/구현체 분리
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
