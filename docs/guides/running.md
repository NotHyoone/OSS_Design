# 프로젝트 실행 가이드

GitHub Activity Insight를 로컬에서 실행하는 방법입니다.

---

## 시스템 요구사항

| 항목 | 버전 |
|------|------|
| Java | 17 이상 |
| Maven | 3.8 이상 |
| PostgreSQL | 12 이상 (로컬 개발 시 H2로 대체 가능) |
| OS | Ubuntu 22.04+ 또는 WSL2 (Ubuntu) |

---

## GitHub OAuth App 설정 (로그인 기능 사용 시 필수)

GitHub 로그인을 사용하려면 OAuth App을 먼저 등록해야 합니다.

1. [GitHub Developer Settings](https://github.com/settings/developers) → **OAuth Apps** → **New OAuth App** 클릭
2. 아래 값을 입력합니다:

   | 필드 | 값 |
   |------|----|
   | Application name | GitHub Activity Insight (자유) |
   | Homepage URL | `http://localhost:8080` |
   | Authorization callback URL | `http://localhost:8080/auth/callback` |

3. 등록 후 **Client ID**와 **Client Secret**을 메모해 둡니다.

> OAuth App 없이도 GitHub ID 분석 기능은 사용할 수 있습니다. 비로그인 요청은 응답의 `resultAccessToken`으로 해당 요청의 결과·PDF·취소 API에 접근할 수 있으며, 최신 결과/이력 조회는 로그인 사용자만 사용할 수 있습니다.

---

## 빠른 시작 — H2 임베디드 DB (별도 DB 설치 불필요)

`application-local.properties`가 이미 포함되어 있으므로 아래 명령만 실행하면 됩니다.

### 1단계: 환경 변수 설정

```bash
export GITHUB_OAUTH_CLIENT_ID=<GitHub OAuth Client ID>
export GITHUB_OAUTH_CLIENT_SECRET=<GitHub OAuth Client Secret>
# GitHub API 호출용 (선택 — 없으면 API 요청에 rate limit 적용)
export GITHUB_TOKEN=<GitHub Personal Access Token>
```

> WSL2 환경에서 환경 변수를 영구 저장하려면 `~/.bashrc` 또는 `~/.profile`에 위 export 문을 추가하세요.

### 2단계: 애플리케이션 실행

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

> **중요**: `backend/` 디렉터리에서 실행해야 합니다. Maven 빌드 설정에 의해 작업 디렉터리가 자동으로 프로젝트 루트로 설정되어 `web/` 폴더의 프론트엔드 파일이 정상 서빙됩니다.

IDE(IntelliJ IDEA, Eclipse)에서 실행할 경우 VM 옵션에 추가:

```
-Dspring.profiles.active=local
```

### 3단계: 서버 기동 확인

다음 로그가 출력되면 서버가 준비된 것입니다:

```
Started GithubInsightApplication in X.XXX seconds
```

---

## 접속 URL

서버 기동 후 아래 URL로 접근합니다.

### 웹 화면

| 페이지 | URL |
|--------|-----|
| 메인 대시보드 | `http://localhost:8080/` |
| 로그인 | `http://localhost:8080/login.html` |
| 분석 진행 | `http://localhost:8080/progress.html` |
| 분석 결과 | `http://localhost:8080/result.html` |
| 분석 이력 | `http://localhost:8080/history.html` |
| H2 DB 콘솔 (로컬 전용) | `http://localhost:8080/h2-console` |

### API 엔드포인트

| 분류 | 메서드 | 경로 | 설명 |
|------|--------|------|------|
| 정보 | GET | `/api/info` | API 버전 및 엔드포인트 목록 |
| 인증 | GET | `/auth/login` | GitHub OAuth 로그인 시작 (리다이렉트) |
| 인증 | GET | `/auth/callback` | GitHub OAuth 콜백 처리 |
| 인증 | GET | `/auth/me` | 현재 로그인 상태 조회 |
| 인증 | POST | `/auth/logout` | 로그아웃 |
| GitHub | GET | `/api/github/validate?id={githubId}` | GitHub ID 존재 여부 확인 |
| 분석 | POST | `/api/analysis/request` | 분석 요청 생성. 비로그인 요청은 `resultAccessToken` 반환 |
| 분석 | GET | `/api/analysis/status/{requestId}` | 분석 진행 상태 조회 |
| 분석 | GET | `/api/analysis/result/{githubId}` | 최신 분석 결과 조회 (로그인 필요) |
| 분석 | GET | `/api/analysis/result/request/{requestId}?token={resultAccessToken}` | 요청 ID 기준 결과 조회. 로그인 요청은 세션, 비로그인 요청은 토큰 필요 |
| 분석 | GET | `/api/analysis/history/{githubId}` | 분석 이력 목록 (로그인 필요) |
| 분석 | GET | `/api/analysis/report/{githubId}` | PDF 리포트 다운로드 (로그인 필요) |
| 분석 | GET | `/api/analysis/report/request/{requestId}?token={resultAccessToken}` | 요청 ID 기준 PDF 다운로드. 로그인 요청은 세션, 비로그인 요청은 토큰 필요 |
| 분석 | POST | `/api/analysis/cancel/{requestId}?token={resultAccessToken}` | 분석 취소. 로그인 요청은 세션, 비로그인 요청은 토큰 필요 |

### H2 콘솔 접속 정보 (로컬 개발 전용)

`http://localhost:8080/h2-console` 접속 후 아래 값 입력:

| 항목 | 값 |
|------|----|
| JDBC URL | `jdbc:h2:file:./data/insight;MODE=PostgreSQL` |
| Username | `sa` |
| Password | (공란) |

---

## PostgreSQL을 사용한 개발 (프로덕션 환경 동일)

### Docker로 PostgreSQL 실행

```bash
docker run -d \
  --name insight-postgres \
  -e POSTGRES_DB=insight \
  -e POSTGRES_USER=insight_user \
  -e POSTGRES_PASSWORD=localdev123 \
  -p 5432:5432 \
  postgres:15-alpine
```

### 환경 변수 설정

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=insight
export DB_USER=insight_user
export DB_PASSWORD=localdev123
export GITHUB_OAUTH_CLIENT_ID=<Client ID>
export GITHUB_OAUTH_CLIENT_SECRET=<Client Secret>
export GITHUB_OAUTH_REDIRECT_URI=http://localhost:8080/auth/callback
export GITHUB_TOKEN=<Personal Access Token>  # 선택
```

### 애플리케이션 실행 (PostgreSQL 프로파일 기본값)

```bash
cd backend
mvn spring-boot:run
```

---

## 빌드 및 JAR 실행

### 빌드

```bash
cd backend
mvn clean package -DskipTests
```

빌드 결과물: `backend/target/github-activity-insight-1.0.0.jar`

### JAR 실행

```bash
# 프로젝트 루트에서 실행 (web/ 폴더 서빙을 위해 루트에서 실행 필요)
java -jar backend/target/github-activity-insight-1.0.0.jar

# 로컬 H2 프로파일로 실행
java -jar backend/target/github-activity-insight-1.0.0.jar --spring.profiles.active=local

# 포트 변경
java -jar backend/target/github-activity-insight-1.0.0.jar --server.port=8081
```

> JAR 실행 시 반드시 **프로젝트 루트 디렉터리**에서 실행해야 `web/` 정적 파일이 정상적으로 서빙됩니다.

---

## 문제 해결

### 포트 충돌 (8080 이미 사용 중)

```bash
# 사용 중인 프로세스 확인
lsof -i :8080
# 또는
ss -tulnp | grep 8080

# 다른 포트로 실행
java -jar backend/target/github-activity-insight-1.0.0.jar --server.port=8081
```

### PostgreSQL 연결 실패

```
FATAL: password authentication failed for user "insight_user"
```

- `DB_PASSWORD` 환경 변수 값을 확인하세요.
- `psql -h localhost -U insight_user -d insight` 로 직접 접속 테스트
- `docker logs insight-postgres` 로 컨테이너 로그 확인

### H2 콘솔에 테이블이 없을 때

`spring.jpa.hibernate.ddl-auto=create`(application-local.properties)로 설정되어 있으므로 서버 재시작 시 스키마가 재생성됩니다. 데이터가 초기화되는 것이 정상입니다.

### 프론트엔드(web/) 파일이 404가 날 때

`mvn spring-boot:run`은 `backend/` 디렉터리에서 실행하고, `java -jar`는 프로젝트 루트에서 실행해야 합니다. `WebConfig`가 `file:web/` 경로를 참조하기 때문입니다.

### 의존성 문제

```bash
mvn clean install -U
```

---

## 주요 설정 파일

| 파일 | 설명 |
|------|------|
| `backend/src/main/resources/application.properties` | 기본 설정 (PostgreSQL) |
| `backend/src/main/resources/application-local.properties` | 로컬 개발 설정 (H2) |
| `.env.example` | 환경 변수 템플릿 |
| `backend/pom.xml` | Maven 빌드 및 의존성 |

---

## 추가 자료

- **배포 가이드**: [deployment.md](deployment.md)
- **프로젝트 개요**: [../../README.md](../../README.md)
