# GitHub Activity Insight

GitHub 활동 데이터를 수집하고 분석해 개발 역량, 활동 패턴, 개선 방향을 보여주는 Spring Boot 기반 웹 서비스입니다.

## 제출 정보

| 항목 | 내용 |
| :--- | :--- |
| Student No. | 22212046 |
| Name | 안효원 |
| E-mail | [gydnjs3505@gmail.com](mailto:gydnjs3505@gmail.com) |

## 프로젝트 개요

GitHub Activity Insight는 GitHub ID를 입력하면 공개 활동 데이터를 기반으로 활동성, 기술 다양성, 협업도, 지속성을 분석합니다. 분석 결과는 종합 점수, 개발자 유형, 신뢰도, 강점, 개선 제안, PDF 리포트로 제공됩니다.

기존 GitHub 통계 도구가 커밋 수나 저장소 수 같은 단편 지표에 집중하는 한계를 보완하기 위해, 여러 활동 지표를 조합해 개인 개발자의 성장 방향을 더 구체적으로 파악할 수 있도록 설계했습니다.

## 서비스 흐름

1. 사용자가 GitHub ID를 입력합니다.
2. 서버가 ID 형식과 GitHub 사용자 존재 여부를 확인합니다.
3. 분석 요청을 생성하고 GitHub 공개 활동 데이터를 수집합니다.
4. 프론트엔드는 요청 ID로 분석 진행률을 폴링합니다.
5. 분석이 완료되면 종합 점수, 세부 지표, 개발자 유형, 피드백을 표시합니다.
6. 로그인 사용자는 본인 분석 이력 조회, 이력 비교, 최신 결과 기반 PDF 다운로드를 사용할 수 있습니다.

## 주요 기능

- GitHub ID 형식 검증 및 사용자 존재 여부 확인
- GitHub 사용자, 저장소, 커밋, Pull Request, Issue 데이터 수집
- 활동성, 기술 다양성, 협업도, 지속성 지표 계산
- 종합 점수와 개발자 유형 산출
  - `BEGINNER`
  - `JUNIOR`
  - `ADVANCED`
- 분석 신뢰도 표시
  - `HIGH`
  - `PARTIAL`
  - `LOW`
  - `LIMITED`
- 분석 진행 상태 폴링 및 취소
- 로그인 사용자 기준 분석 결과, 분석 이력, 이력 비교 제공
- PDF 분석 리포트 다운로드

## 기술 스택

| 영역 | 기술 |
| :--- | :--- |
| Backend | Spring Boot 3.2.5, Java 17 |
| API | Spring Web, Validation |
| Persistence | Spring Data JPA |
| Database | PostgreSQL 기본, H2 로컬 프로파일 |
| Report | OpenPDF |
| Frontend | HTML, CSS, JavaScript |
| Build | Maven |

## 프로젝트 구조

```text
.
├── AGENTS.md                 # AI 작업 지침
├── README.md                 # 프로젝트 소개 및 진입점
├── render.yaml               # Render Blueprint 배포 설정
├── run.sh                    # 로컬 PostgreSQL + Spring Boot 실행 스크립트
├── .env.example              # 환경 변수 예시
├── data/                     # H2 로컬 DB 파일 생성 위치
├── backend/
│   ├── Dockerfile            # Render/Docker 배포 이미지 빌드 설정
│   ├── pom.xml               # Maven 설정
│   ├── scripts/
│   │   └── init-postgres.sh  # PostgreSQL 스키마 초기화 스크립트
│   └── src/
│       ├── main/
│       │   ├── java/com/github/insight/
│       │   │   ├── GithubInsightApplication.java
│       │   │   ├── config/       # CORS, 정적 파일, 비동기 실행 설정
│       │   │   ├── controller/   # /api/**, /auth/** REST 컨트롤러
│       │   │   ├── dto/          # 요청/응답 DTO
│       │   │   ├── entity/       # JPA 엔티티
│       │   │   ├── model/        # 도메인 모델과 enum
│       │   │   ├── repository/   # Spring Data JPA 리포지토리
│       │   │   └── service/      # 분석, GitHub API, 리포트, 인증 서비스
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-local.properties
│       │       └── schema.sql
│       └── test/
│           ├── java/com/github/insight/
│           └── resources/
├── docs/
│   ├── README.md             # 문서 허브
│   ├── assets/images/        # 로고, UI 캡처, 다이어그램 이미지
│   ├── guides/               # 실행, DB, 배포/운영, 제출 가이드
│   └── project/              # 기획, 분석, 설계 산출물
└── web/
    ├── css/style.css
    ├── js/app.js
    ├── index.html            # GitHub ID 입력 및 분석 시작
    ├── login.html            # GitHub OAuth 로그인
    ├── progress.html         # 분석 진행률 표시
    ├── result.html           # 분석 결과 대시보드
    └── history.html          # 분석 이력 및 비교
```

백엔드 프로세스가 REST API와 `web/` 정적 파일을 함께 서빙합니다. `WebConfig`가 `file:web/` 경로를 사용하므로 JAR 실행 시에는 반드시 저장소 루트에서 실행해야 합니다.

## 빠른 시작

### H2 로컬 프로파일

별도 데이터베이스 설치 없이 실행할 수 있습니다.

```bash
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

접속 URL:

| 용도 | URL |
| :--- | :--- |
| 웹 서비스 | `http://localhost:8080` |
| API 정보 | `http://localhost:8080/api/info` |
| H2 콘솔 | `http://localhost:8080/h2-console` |

H2 콘솔 접속 정보:

| 항목 | 값 |
| :--- | :--- |
| JDBC URL | `jdbc:h2:file:./data/insight;MODE=PostgreSQL` |
| Username | `sa` |
| Password | 공란 |

### PostgreSQL 기본 프로파일

기본 프로파일은 PostgreSQL을 사용하며 `ddl-auto=validate`로 동작합니다.

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=insight
export DB_USER=insight_user
export DB_PASSWORD=localdev123

mvn -f backend/pom.xml spring-boot:run
```

PostgreSQL 초기화와 운영 절차는 [docs/guides/database.md](docs/guides/database.md)를 참고하세요.

### run.sh 사용

`run.sh`는 `.env.local`을 로드하고 Docker PostgreSQL 컨테이너를 준비한 뒤 Spring Boot를 실행합니다.

```bash
./run.sh
```

Docker 없이 H2로 빠르게 확인할 때는 위의 H2 로컬 프로파일 명령을 사용하세요.

## 환경 변수

| 환경 변수 | 필수 여부 | 설명 |
| :--- | :--- | :--- |
| `DB_HOST` | PostgreSQL 사용 시 | PostgreSQL 서버 주소 |
| `DB_PORT` | 선택 | PostgreSQL 포트, 기본값 `5432` |
| `DB_NAME` | 선택 | 데이터베이스 이름, 기본값 `insight` |
| `DB_USER` | PostgreSQL 사용 시 | DB 사용자명 |
| `DB_PASSWORD` | PostgreSQL 사용 시 | DB 비밀번호 |
| `GITHUB_OAUTH_CLIENT_ID` | 로그인 사용 시 | GitHub OAuth App Client ID |
| `GITHUB_OAUTH_CLIENT_SECRET` | 로그인 사용 시 | GitHub OAuth App Client Secret |
| `GITHUB_OAUTH_REDIRECT_URI` | 선택 | OAuth 콜백 URI |
| `GITHUB_TOKEN` | 선택 | GitHub API rate limit 완화용 Personal Access Token |
| `CORS_ALLOWED_ORIGINS` | 운영 배포 시 | 허용할 CORS origin 목록 |

OAuth App 없이도 GitHub ID 검증과 분석 요청은 가능합니다. 다만 로그인해야 본인 GitHub ID 기준의 최신 결과 조회, 분석 이력 조회, 이력 비교, 최신 결과 PDF 다운로드를 사용할 수 있습니다.

## 주요 API

| 분류 | 메서드 | 경로 | 인증 | 설명 |
| :--- | :--- | :--- | :--- | :--- |
| 정보 | GET | `/api/info` | 불필요 | API 정보 |
| 인증 | GET | `/auth/login` | 불필요 | GitHub OAuth 로그인 시작 |
| 인증 | GET | `/auth/callback` | 불필요 | GitHub OAuth 콜백 |
| 인증 | GET | `/auth/me` | 불필요 | 현재 로그인 상태 조회 |
| 인증 | GET | `/auth/config` | 불필요 | OAuth 설정 상태 조회 |
| 인증 | POST | `/auth/logout` | 선택 | 세션 무효화 |
| GitHub | GET | `/api/github/validate?id={githubId}` | 불필요 | GitHub ID 형식 및 실존 여부 검증 |
| 분석 | POST | `/api/analysis/request` | 선택 | 분석 요청 생성, 로그인 시 사용자와 요청 연결 |
| 분석 | GET | `/api/analysis/status/{requestId}` | 불필요 | 분석 진행 상태 조회 |
| 분석 | GET | `/api/analysis/result/{githubId}` | 필요 | 로그인 사용자 본인의 최신 분석 결과 조회 |
| 분석 | GET | `/api/analysis/result/request/{requestId}` | 조건부 | 요청 ID 기준 분석 결과 조회 |
| 분석 | GET | `/api/analysis/history/{githubId}` | 필요 | 로그인 사용자 본인의 분석 이력 조회 |
| 분석 | GET | `/api/analysis/report/{githubId}` | 필요 | 로그인 사용자 본인의 최신 분석 PDF 다운로드 |
| 분석 | GET | `/api/analysis/report/request/{requestId}` | 조건부 | 요청 ID 기준 PDF 다운로드 |
| 분석 | POST | `/api/analysis/cancel/{requestId}` | 필요 | 본인 분석 요청 취소 |

## 빌드와 테스트

```bash
# 테스트
mvn -f backend/pom.xml test

# JAR 빌드
mvn -f backend/pom.xml clean package -DskipTests

# 저장소 루트에서 JAR 실행
java -jar backend/target/github-activity-insight-1.0.0.jar
```

## 문서

| 문서 | 설명 |
| :--- | :--- |
| [docs/README.md](docs/README.md) | 문서 허브 |
| [docs/guides/running.md](docs/guides/running.md) | 로컬 실행 및 빠른 시작 |
| [docs/guides/database.md](docs/guides/database.md) | H2/PostgreSQL 설정 |
| [docs/guides/deployment.md](docs/guides/deployment.md) | 배포 및 운영 |
| [docs/guides/submission.md](docs/guides/submission.md) | 과제 제출 및 Render 배포 URL 준비 |
| [docs/project/Conceptualization_[22212046_안효원].md](docs/project/Conceptualization_%5B22212046_안효원%5D.md) | 기획 산출물 |
| [docs/project/Analysis_[22212046_안효원].md](docs/project/Analysis_%5B22212046_안효원%5D.md) | 분석 산출물 |
| [docs/project/Design_[22212046_안효원].md](docs/project/Design_%5B22212046_안효원%5D.md) | 설계 산출물 |
| [docs/project/Implementation_[22212046_안효원].md](docs/project/Implementation_%5B22212046_안효원%5D.md) | 구현 산출물 |
| [docs/project/design.html](docs/project/design.html) | 설계 문서 HTML 버전 |
| [docs/project/structure-draft.md](docs/project/structure-draft.md) | 구조 초안 |
