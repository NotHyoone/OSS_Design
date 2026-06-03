# GitHub Activity Insight - Implementation

![GitHub Activity Insight Logo](../assets/images/logo_github_activity_insight.svg)

GitHub 기반 개발자 실력 분석 및 피드백 웹 시스템 구현 산출물

| 정보 항목 | 내용 |
| :--- | :--- |
| Student No | 22212046 |
| Name | 안효원 |
| E-Mail | [gydnjs3505@gmail.com](mailto:gydnjs3505@gmail.com) |

영남대학교 (Yeungnam University)

---

## Revision history

| Revision date | Version # | Description | Author |
| :--- | :--- | :--- | :--- |
| 2026-06-03 | 1.0 | Initial implementation document split from Design 5.2-5.6 and expanded with concrete API, persistence, runtime, and verification details | 안효원 |

---

## Contents

1. **Introduction**
2. **Implementation Scope**
3. **Security Requirements**
4. **Non-Functional Requirements**
5. **Database Implementation**
6. **Configuration and Environment Variables**
7. **Implementation Layer Mapping**
8. **API Contract**
9. **Frontend Integration**
10. **Execution and Verification**
11. **Operational Policies**
12. **Known Gaps and Follow-up Items**
13. **Glossary**
14. **References**

---

## 1. Introduction

본 문서는 GitHub Activity Insight의 구현(Implementation) 단계 산출물이다. Design 문서가 클래스 구조, 시퀀스, 상태 전이를 중심으로 시스템의 설계 근거를 설명한다면, 본 문서는 실제 Spring Boot 구현에서 적용된 보안 정책, API 계약, DB 스키마, 실행 환경, 검증 결과, 운영 보정 사항을 개발자가 직접 확인할 수 있는 수준으로 정리한다.

현재 구현은 하나의 Spring Boot 백엔드 프로세스가 REST API와 `web/` 정적 프론트엔드를 함께 제공하는 구조이다. 로컬 개발은 H2 파일 DB를 사용하고, 기본 프로필은 PostgreSQL을 대상으로 `ddl-auto=validate`를 적용한다. 분석 요청은 로그인 사용자와 비로그인 사용자 모두 생성할 수 있으며, 로그인 사용자의 요청은 `userId`와 연결되어 이력 조회와 최신 결과 조회의 소유권 검증에 사용된다.

---

## 2. Implementation Scope

### 2.1 In Scope

- GitHub OAuth 2.0 로그인과 세션 쿠키 관리
- GitHub ID 형식 검증 및 GitHub 사용자 실존 여부 확인
- 분석 요청 생성, 진행률 조회, 취소 처리
- GitHub 공개 활동 데이터 수집
- 활동성, 기술 다양성, 협업도, 지속성 지표 계산
- 종합 점수, 개발자 유형, 신뢰도, 강점/개선 피드백 생성
- requestId 기준 결과 조회 및 PDF 리포트 다운로드
- 로그인 사용자 본인 기준 최신 결과, 이력, 최신 PDF 조회
- H2/PostgreSQL 영속화 및 정기 데이터 정리
- 정적 HTML/CSS/JavaScript 프론트엔드 연동

### 2.2 Out of Scope

- Spring Security 기반 인증 필터 체인 도입
- Redis, 외부 메시지 큐, 분산 작업 큐 도입
- Docker Compose 기반 전체 개발 환경 자동화
- 비공개 저장소 활동 분석
- Recruiter/Mentor 공유 링크 기능
- 브라우저 UI 자동화 회귀 테스트 정식 파이프라인

---

## 3. Security Requirements

| 항목 | 구현 정책 | 구현 위치 |
| :--- | :--- | :--- |
| 인증 | GitHub OAuth 2.0 Authorization Code 흐름 사용 | `AuthController`, `AuthenticationService`, `OAuthClient` |
| CSRF 방어 | OAuth `state` 단일 사용 검증, 10분 만료, pending state 500개 제한 | `AuthenticationService` |
| 세션 쿠키 | `SESSION_ID`를 HttpOnly 쿠키로 발급. Secure는 `github.oauth.cookie-secure` 설정값에 따라 적용 | `AuthController` |
| 세션 만료 | 기본 30분 세션 타임아웃, 로그아웃 시 명시적 무효화 | `AuthenticationService`, `AuthController` |
| 입력 검증 | GitHub ID는 영문, 숫자, 하이픈 1-39자만 허용하고 앞뒤 하이픈을 차단 | `GithubController`, `AnalysisController`, `UrlSafetyTests` |
| URL 인젝션 방어 | GitHub ID를 URL 조각이 아닌 검증된 path/query 값으로만 사용 | `GithubApiClient`, `UrlSafetyTests` |
| 예외 응답 | 내부 예외를 직접 노출하지 않고 공통 오류 응답으로 변환 | `GlobalExceptionHandler`, `ErrorResponse` |
| 데이터 격리 | GitHub ID 기준 최신 결과/이력/최신 PDF는 로그인 사용자 본인 GitHub ID만 허용 | `AnalysisController` |
| requestId 접근 | 요청에 `userId`가 있으면 세션 소유권 검증, `userId`가 없으면 requestId 기반 결과/PDF 조회 허용 | `AnalysisController` |
| 민감 정보 | OAuth Client Secret, DB Password는 환경 변수/설정 파일 주입만 허용 | `application.properties` |

### 3.1 Authorization Matrix

| 기능 | 비로그인 | 로그인 | 검증 기준 |
| :--- | :---: | :---: | :--- |
| GitHub ID 검증 | 허용 | 허용 | 공개 GitHub 사용자 확인 |
| 분석 요청 생성 | 허용 | 허용 | 로그인 시 userId 연결, 비로그인 시 userId=null |
| 진행 상태 조회 | 허용 | 허용 | requestId |
| requestId 결과 조회 | 조건부 허용 | 조건부 허용 | request.userId 존재 시 세션 소유권 검증 |
| requestId PDF 다운로드 | 조건부 허용 | 조건부 허용 | request.userId 존재 시 세션 소유권 검증 |
| GitHub ID 최신 결과 조회 | 차단 | 본인만 허용 | 세션 githubId와 path githubId 비교 |
| GitHub ID 최신 PDF 다운로드 | 차단 | 본인만 허용 | 세션 githubId와 path githubId 비교 |
| 분석 이력 조회 | 차단 | 본인만 허용 | 세션 githubId와 path githubId 비교 |
| 분석 취소 | 차단 | 본인 요청만 허용 | request.userId 또는 동일 GitHub ID 확인 |

---

## 4. Non-Functional Requirements

| 항목 | 목표 기준 | 구현 방식 |
| :--- | :--- | :--- |
| OAuth 로그인 응답 | 2-3초 이내 | OAuth redirect, token exchange, profile 조회 후 세션 발급 |
| 분석 요청 생성 | 2초 이내 | 요청 저장 후 `AnalysisAsyncRunner`로 비동기 실행 |
| 전체 분석 시간 | 공개 저장소 30개 기준 60초 이내 | GitHub API 수집 후 3단계 진행률 업데이트 |
| 결과 조회 응답 | 평균 2초 이내 | JPA Repository 조회 후 `ReportAssembler`로 ViewModel 변환 |
| PDF 다운로드 | 1-3초 | OpenPDF 기반 서버측 A4 PDF 생성 |
| 동시 처리 | 동일 githubId 활성 요청 중복 방지 | `findActiveRequestId(githubId)`로 PENDING/RUNNING 요청 재사용 |
| 취소 처리 | 진행 중 요청 CANCELLED 전환 | `AnalysisService.cancel()` 및 `RequestStatus.CANCELLED` |
| 데이터 보관 | 오래된 요청/결과 정리 | `AnalysisRepository.purgeExpired()` 스케줄러 |
| 로컬 실행성 | 별도 DB 없이 실행 가능 | H2 local profile |
| 운영 DB 안정성 | 스키마 검증 기반 실행 | PostgreSQL default profile + `ddl-auto=validate` |

---

## 5. Database Implementation

### 5.1 Profile별 DB 구성

| Spring Profile | DB | ddl-auto | 설명 |
| :--- | :--- | :--- | :--- |
| `local` | H2 file DB (`jdbc:h2:file:./data/insight`) | `create` | 로컬 개발 및 테스트용. 실행 시 테이블을 재생성한다. |
| `default` | PostgreSQL | `validate` | 운영/스테이징 기준. `schema.sql`과 Entity가 일치해야 실행된다. |

PostgreSQL 초기화 스크립트는 `backend/scripts/init-postgres.sh`이며, 스키마 기준 파일은 `backend/src/main/resources/schema.sql`이다.

### 5.2 주요 테이블

| 테이블 | 주요 컬럼 | 설명 |
| :--- | :--- | :--- |
| `users` | `user_id`, `github_id`, `email`, `session_id`, `created_at`, `last_login_at` | OAuth 로그인 사용자와 세션 정보 저장 |
| `analysis_requests` | `request_id`, `user_id`, `github_id`, `status`, `step`, `overall_pct`, `detail` | 분석 요청 상태와 진행률 저장 |
| `analysis_results` | `result_id`, `request_id`, `user_id`, `github_id`, `total_score`, `developer_type`, `trust_level` | 최종 분석 결과 저장 |
| `metrics` | `request_id`, `activity_score`, `diversity_score`, `collaboration_score`, `persistence_score`, `trust_level` | 지표 계산 결과 저장 |

### 5.3 구현 보정 사항

- `analysis_requests.user_id`는 nullable이다. 비로그인 분석 요청을 허용하기 위해 필요하다.
- `analysis_results.user_id`도 nullable이며, 사용자 계정 삭제 시 `ON DELETE SET NULL` 정책을 사용한다.
- `analysis_results.strengths_json`, `analysis_results.improvements_json`, `metrics.descriptions_json`은 JSON 문자열로 직렬화해 저장한다.
- 이력 조회는 최신 결과 중심으로 제한되며 현재 구현 기준 최대 10건을 반환한다.
- 완료/실패/부분 완료 요청, 장시간 RUNNING 요청, 만료된 결과/지표는 정기 스케줄러로 정리한다.

### 5.4 Repository Mapping

| Repository | Entity | 주요 책임 |
| :--- | :--- | :--- |
| `UserRepository` | `UserEntity` | GitHub ID, sessionId 기준 사용자 조회 및 저장 |
| `AnalysisRequestRepository` | `AnalysisRequestEntity` | requestId/status/githubId 기준 요청 조회 |
| `AnalysisResultRepository` | `AnalysisResultEntity` | requestId/githubId/userId 기준 결과 조회 |
| `MetricsRepository` | `MetricsEntity` | requestId 기준 지표 조회 및 삭제 |

`AnalysisRepository`는 Spring Data JPA Repository를 직접 노출하지 않고 도메인 모델과 Entity 변환을 담당하는 persistence adapter 역할을 수행한다.

---

## 6. Configuration and Environment Variables

### 6.1 설정 파일

| 파일 | 설명 |
| :--- | :--- |
| `backend/src/main/resources/application.properties` | 기본 PostgreSQL 설정, OAuth 설정, JPA validate 설정 |
| `backend/src/main/resources/application-local.properties` | H2 로컬 설정, local profile 전용 설정 |
| `backend/src/main/resources/schema.sql` | PostgreSQL/H2 호환 스키마 기준 파일 |
| `backend/scripts/init-postgres.sh` | PostgreSQL 데이터베이스와 사용자 초기화 스크립트 |

### 6.2 환경 변수

| 환경 변수 | 필수 여부 | 설명 |
| :--- | :---: | :--- |
| `GITHUB_OAUTH_CLIENT_ID` | 로그인 사용 시 필수 | GitHub OAuth App Client ID |
| `GITHUB_OAUTH_CLIENT_SECRET` | 로그인 사용 시 필수 | GitHub OAuth App Client Secret |
| `GITHUB_OAUTH_REDIRECT_URI` | 선택 | OAuth callback URI. 미설정 시 기본값 사용 |
| `GITHUB_TOKEN` | 선택 | GitHub API rate limit 완화용 Personal Access Token |
| `DB_HOST` | PostgreSQL 사용 시 필수 | PostgreSQL 서버 주소 |
| `DB_PORT` | 선택 | PostgreSQL 포트. 기본값 5432 |
| `DB_NAME` | 선택 | 데이터베이스 이름. 기본값 insight |
| `DB_USER` | PostgreSQL 사용 시 필수 | DB 사용자명 |
| `DB_PASSWORD` | PostgreSQL 사용 시 필수 | DB 비밀번호 |
| `CORS_ALLOWED_ORIGINS` | 운영 배포 시 권장 | 허용할 CORS origin 목록. 콤마 구분 |

### 6.3 OAuth 미설정 동작

OAuth Client ID/Secret이 없으면 `/auth/login`은 GitHub로 redirect하지 않고 OAuth 미설정 안내 응답을 반환한다. 이 경우에도 GitHub ID 직접 입력, GitHub ID 검증, 비로그인 분석 요청, requestId 기준 결과/PDF 조회는 사용할 수 있다. 단, 이력 조회와 GitHub ID 기준 최신 결과 조회는 사용할 수 없다.

---

## 7. Implementation Layer Mapping

| Layer | 구현 파일/클래스 | 구현 책임 |
| :--- | :--- | :--- |
| Application | `GithubInsightApplication` | Spring Boot 진입점, scheduling/async 활성화 |
| Config | `WebConfig`, `AsyncConfig` | `file:web/` 정적 파일 서빙, CORS, 분석 스레드풀 설정 |
| Controller | `HomeController` | 루트 redirect, `/api/info` 제공 |
| Controller | `AuthController` | OAuth login/callback, `/auth/me`, `/auth/config`, logout |
| Controller | `GithubController` | GitHub ID 형식 및 실존 여부 검증 |
| Controller | `AnalysisController` | 분석 요청, 상태, 결과, 이력, PDF, 취소 API |
| Controller Advice | `GlobalExceptionHandler` | 공통 예외 응답 처리 |
| DTO | `CreateRequestBody`, `ValidateResponse`, `AnalysisRequestResponse`, `AnalysisStatusResponse`, `ErrorResponse` | API 요청/응답 계약 |
| Domain | `AnalysisRequest`, `ActivityData`, `Metrics`, `AnalysisResult`, `FeedbackItem` | 분석 파이프라인 내부 데이터 모델 |
| Entity | `UserEntity`, `AnalysisRequestEntity`, `AnalysisResultEntity`, `MetricsEntity` | JPA 영속화 모델 |
| Service | `AuthenticationService`, `OAuthClient` | OAuth, 세션, 사용자 관리 |
| Service | `GithubApiClient` | GitHub REST API 호출과 원천 데이터 수집 |
| Service | `AnalysisService`, `AnalysisAsyncRunner` | 분석 요청 오케스트레이션과 비동기 실행 |
| Service | `MetricCalculator`, `CompetencyScorer`, `FeedbackGenerator` | 지표 계산, 점수화, 피드백 생성 |
| Service | `ReportAssembler`, `ReportGenerator` | 화면 ViewModel 조립과 OpenPDF 렌더링 |
| Persistence Adapter | `AnalysisRepository` | 도메인 모델과 JPA Entity 변환, 조회/정리 정책 |
| Frontend | `web/*.html`, `web/js/app.js`, `web/css/style.css` | 정적 화면, fetch API 호출, 진행률 폴링, 결과 렌더링 |

---

## 8. API Contract

### 8.1 Auth API

| Method | Path | 인증 | 설명 | 주요 응답 |
| :--- | :--- | :---: | :--- | :--- |
| GET | `/auth/login` | 불필요 | GitHub OAuth 로그인 시작 | 302 redirect 또는 OAuth 미설정 안내 |
| GET | `/auth/callback` | 불필요 | OAuth callback 처리 | `SESSION_ID` 쿠키 + redirect |
| GET | `/auth/me` | 선택 | 현재 로그인 상태 조회 | `{ loggedIn: true/false, ... }` |
| GET | `/auth/config` | 불필요 | OAuth 설정 여부 조회 | `{ configured: boolean }` |
| POST | `/auth/logout` | 선택 | 세션 무효화 | `{ message: "로그아웃 완료" }` |

### 8.2 GitHub API

| Method | Path | 인증 | 설명 |
| :--- | :--- | :---: | :--- |
| GET | `/api/github/validate?id={githubId}` | 불필요 | GitHub ID 형식 검증과 실존 사용자 확인 |

### 8.3 Analysis API

| Method | Path | 인증 | 설명 |
| :--- | :--- | :---: | :--- |
| POST | `/api/analysis/request` | 선택 | 분석 요청 생성. 로그인 시 userId 연결, 비로그인 시 userId=null |
| GET | `/api/analysis/status/{requestId}` | 불필요 | 분석 단계, 상태, 전체 진행률, 상세 메시지 조회 |
| GET | `/api/analysis/result/{githubId}` | 필요 | 로그인 사용자 본인의 최신 분석 결과 조회 |
| GET | `/api/analysis/result/request/{requestId}` | 조건부 | requestId 기준 특정 분석 결과 조회 |
| GET | `/api/analysis/history/{githubId}` | 필요 | 로그인 사용자 본인의 최근 분석 이력 조회 |
| GET | `/api/analysis/report/{githubId}` | 필요 | 로그인 사용자 본인의 최신 분석 결과 PDF 다운로드 |
| GET | `/api/analysis/report/request/{requestId}` | 조건부 | requestId 기준 특정 분석 결과 PDF 다운로드 |
| POST | `/api/analysis/cancel/{requestId}` | 필요 | 본인 분석 요청 취소 |

### 8.4 Status Response Contract

`GET /api/analysis/status/{requestId}`는 `AnalysisStatusResponse`를 반환한다.

| 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| `step` | number | 현재 단계. 1=데이터 수집, 2=지표 계산, 3=점수/피드백 생성 |
| `stepStatus` | string | `running`, `done`, `cancelled`, `error` 중 하나 |
| `overallPct` | number | 0-100 전체 진행률 |
| `detail` | string | 사용자에게 표시할 상세 진행 메시지 |

---

## 9. Frontend Integration

프론트엔드는 빌드 도구 없이 `web/` 아래의 정적 HTML/CSS/JavaScript로 구성된다. Spring Boot `WebConfig`가 `file:web/`를 루트 정적 리소스 위치로 등록하므로, Maven 실행 또는 JAR 실행 시 현재 작업 디렉터리가 저장소 루트여야 한다.

| 화면 | 파일 | 주요 API |
| :--- | :--- | :--- |
| Home | `web/index.html` | `/api/github/validate`, `/api/analysis/request`, `/auth/me` |
| Login | `web/login.html` | `/auth/login`, `/auth/config` |
| Progress | `web/progress.html` | `/api/analysis/status/{requestId}`, `/api/analysis/cancel/{requestId}` |
| Result | `web/result.html` | `/api/analysis/result/request/{requestId}`, `/api/analysis/report/request/{requestId}` |
| History | `web/history.html` | `/api/analysis/history/{githubId}` |

### 9.1 구현 UX 보정 사항

- Home 화면의 GitHub ID 입력은 local regex 검증 후 서버 검증 API를 호출한다.
- Progress 화면은 requestId로 상태를 폴링하고 완료 시 Result 화면으로 이동한다.
- Result 화면은 requestId가 있으면 특정 결과를 조회하고, requestId가 없으면 GitHub ID 기준 최신 결과 조회 API를 사용한다.
- History 화면은 로그인 사용자 본인 GitHub ID 기준 이력만 조회할 수 있으므로 비로그인 401 흐름에 대한 UX 보강이 필요하다.

---

## 10. Execution and Verification

### 10.1 실행 명령

```bash
# 로컬 H2 프로필
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# 기본 PostgreSQL 프로필
mvn -f backend/pom.xml spring-boot:run

# 테스트
mvn -f backend/pom.xml test

# JAR 빌드
mvn -f backend/pom.xml clean package -DskipTests

# 저장소 루트에서 JAR 실행
java -jar backend/target/github-activity-insight-1.0.0.jar
```

### 10.2 검증 결과

2026-06-02 로컬 H2 프로필 기준으로 다음 항목을 확인하였다.

| 검증 항목 | 결과 |
| :--- | :--- |
| `mvn -f backend/pom.xml test` | 54 tests, failures 0, errors 0 |
| `GET /` | `/index.html` redirect 정상 |
| `GET /index.html` | 정적 파일 서빙 정상 |
| `GET /api/info` | API 정보 JSON 정상 |
| `GET /auth/me` | 비로그인 상태 `{ loggedIn: false }` 정상 |
| `GET /api/github/validate?id=octocat` | 유효 사용자 응답 정상 |
| 잘못된 GitHub ID 검증 | 400 응답 정상 |
| `POST /api/analysis/request` | requestId 생성 정상 |
| `GET /api/analysis/status/{requestId}` | `step=3`, `stepStatus=done`, `overallPct=100` 확인 |
| `GET /api/analysis/result/request/{requestId}` | JSON 결과 반환 정상 |
| `GET /api/analysis/report/request/{requestId}` | `application/pdf` 응답 정상 |

### 10.3 검증 중 확인된 주의점

- `GlobalExceptionHandlerTests`는 의도적으로 없는 정적 리소스를 호출하므로 테스트는 통과하지만 ERROR 로그가 출력된다.
- Browser console 자동 검증은 Mermaid 검증과 별개이며, Playwright 의존성 정리가 필요하다.
- GitHub API 응답 품질과 rate limit 상태에 따라 분석 결과 신뢰도가 달라질 수 있다.

---

## 11. Operational Policies

### 11.1 Retention Cleanup

`AnalysisRepository.purgeExpired()`는 1시간 간격으로 실행되며 다음 데이터를 정리한다.

| 대상 | 기준 | 처리 |
| :--- | :--- | :--- |
| 완료/실패/부분 완료 요청 | 24시간 초과 | `analysis_requests` 삭제 |
| 장시간 RUNNING 요청 | 2시간 초과 | stuck request 삭제 |
| 분석 결과/지표 | 72시간 초과 | `analysis_results`, `metrics` 삭제 |

### 11.2 Rate Limit Handling

GitHub API 호출 중 rate limit이 발생하면 `GithubApiClient`는 관련 헤더를 확인하고 안전한 오류 응답으로 전파한다. 문서상 목표는 `X-RateLimit-Reset` 기반 재시도 가능 시각 계산이며, 사용자 UI에는 아직 재시도 가능 시각이 명확히 표시되지 않는다.

### 11.3 Static Resource Serving

정적 파일은 classpath가 아니라 repository root의 `web/` 디렉터리에서 제공된다. 따라서 JAR 실행 시 다음 조건을 지켜야 한다.

- 실행 위치는 repository root여야 한다.
- `web/index.html`, `web/js/app.js`, `web/css/style.css`가 실행 위치 기준으로 존재해야 한다.
- Maven `spring-boot:run`은 plugin 설정에 따라 repository root 작업 디렉터리를 사용한다.

---

## 12. Known Gaps and Follow-up Items

| 항목 | 현재 상태 | 후속 조치 |
| :--- | :--- | :--- |
| History 비로그인 UX | API는 401을 반환하지만 화면 안내가 일반 오류에 가깝다 | 로그인 필요 메시지와 Login 이동 버튼 강화 |
| Queue position | 문서 초안에는 대기열 위치가 있으나 구현은 `@Async`와 활성 요청 재사용 중심 | 실제 큐 도입 시 queuePosition 필드 추가 |
| Browser 자동화 검증 | HTTP/API 검증 중심 | Playwright 의존성 정리 후 화면 렌더링 테스트 추가 |
| Rate limit UI | 서버에서 오류 처리, UI 상세 안내 부족 | reset 시간 기반 재시도 안내 표시 |
| 공유 조회 | Recruiter/Mentor 공유 조회 기능 미구현 | 공유 토큰 또는 공개 리포트 링크 설계 필요 |
| 비공개 활동 분석 | GitHub 공개 데이터 중심 | OAuth scope와 사용자 동의 범위 확대 검토 |

---

## 13. Glossary

| 용어 | 정의 |
| :--- | :--- |
| Implementation Document | 실제 구현의 API, 보안, DB, 환경 변수, 검증 결과를 정리한 산출물 |
| SESSION_ID | 로그인 사용자를 식별하는 HttpOnly 세션 쿠키 |
| Anonymous Analysis Request | userId 없이 생성된 분석 요청. requestId 기준 조회를 통해 결과 확인 가능 |
| Owner Check | 세션 GitHub ID 또는 userId가 요청/결과 소유자와 일치하는지 확인하는 권한 검증 |
| Retention Cleanup | 오래된 요청, 결과, 지표를 주기적으로 삭제하는 운영 정책 |
| ViewModel | `ReportAssembler`가 프론트엔드와 PDF 렌더링에 맞게 조립한 Map 기반 응답 모델 |

---

## 14. References

1. GitHub, "REST API Documentation," GitHub Docs. Available: <https://docs.github.com/en/rest> (accessed: 2026-06-03).
2. GitHub, "Authorizing OAuth apps," GitHub Docs. Available: <https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps> (accessed: 2026-06-03).
3. Spring, "Spring Boot Reference Documentation." Available: <https://docs.spring.io/spring-boot/> (accessed: 2026-06-03).
4. H2 Database Engine, "Features." Available: <https://www.h2database.com/html/features.html> (accessed: 2026-06-03).
5. PostgreSQL Global Development Group, "PostgreSQL Documentation." Available: <https://www.postgresql.org/docs/> (accessed: 2026-06-03).
6. OpenPDF, "OpenPDF GitHub Repository." Available: <https://github.com/LibrePDF/OpenPDF> (accessed: 2026-06-03).
