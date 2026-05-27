# 배포 및 운영 가이드

GitHub Activity Insight를 프로덕션 환경에 배포하고 운영하는 방법입니다.

---

## 아키텍처 개요

```
브라우저
  │
  ▼
Spring Boot (포트 8080)
  ├── 정적 파일 서빙  ← web/ 디렉터리 (HTML/CSS/JS)
  ├── REST API       ← /api/**, /auth/**
  └── DB 연결
        ├── H2 (로컬 개발, file:./data/insight)
        └── PostgreSQL (스테이징 / 프로덕션)
```

백엔드가 프론트엔드 정적 파일까지 함께 서빙하는 단일 프로세스 구조입니다.

---

## 1. 사전 준비

### 1.1 시스템 요구사항

| 항목 | 버전 |
|------|------|
| Java | 17 이상 |
| Maven | 3.8 이상 (빌드 시) |
| PostgreSQL | 12 이상 (프로덕션) |
| OS | Ubuntu 22.04+ 또는 WSL2 (Ubuntu) |

### 1.2 GitHub OAuth App 등록

1. [GitHub Developer Settings](https://github.com/settings/developers) → **OAuth Apps** → **New OAuth App**
2. 아래 값 입력:

   | 필드 | 로컬 | 프로덕션 |
   |------|------|----------|
   | Homepage URL | `http://localhost:8080` | `https://your-domain.com` |
   | Authorization callback URL | `http://localhost:8080/auth/callback` | `https://your-domain.com/auth/callback` |

3. 생성 후 **Client ID**와 **Client Secret** 보관

### 1.3 환경 변수 목록

| 환경 변수 | 기본값 | 필수 여부 | 설명 |
|----------|--------|-----------|------|
| `DB_HOST` | `localhost` | 프로덕션 필수 | PostgreSQL 서버 주소 |
| `DB_PORT` | `5432` | 선택 | PostgreSQL 포트 |
| `DB_NAME` | `insight` | 선택 | 데이터베이스 이름 |
| `DB_USER` | `postgres` | 프로덕션 필수 | DB 사용자명 |
| `DB_PASSWORD` | (없음) | 프로덕션 필수 | DB 비밀번호 |
| `GITHUB_OAUTH_CLIENT_ID` | (없음) | 로그인 사용 시 필수 | OAuth App Client ID |
| `GITHUB_OAUTH_CLIENT_SECRET` | (없음) | 로그인 사용 시 필수 | OAuth App Client Secret |
| `GITHUB_OAUTH_REDIRECT_URI` | `http://localhost:8080/auth/callback` | 선택 | OAuth 콜백 URI |
| `GITHUB_TOKEN` | (없음) | 선택 | Personal Access Token (API rate limit 완화) |

---

## 2. 로컬 개발 환경 (H2 임베디드 DB)

별도 DB 설치 없이 H2를 사용해 빠르게 실행합니다.

```bash
# 1. 환경 변수 설정 (Linux/macOS)
export GITHUB_OAUTH_CLIENT_ID=<Client ID>
export GITHUB_OAUTH_CLIENT_SECRET=<Client Secret>

# 2. backend/ 디렉터리에서 실행
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

기동 후 접속:

| 목적 | URL |
|------|-----|
| 웹 서비스 | `http://localhost:8080` |
| H2 DB 콘솔 | `http://localhost:8080/h2-console` |
| API 정보 | `http://localhost:8080/api/info` |

H2 콘솔 로그인 정보:
- JDBC URL: `jdbc:h2:file:./data/insight;MODE=PostgreSQL`
- Username: `sa` / Password: (공란)

> 로컬 프로파일은 서버 재시작 시 `ddl-auto=create`로 스키마를 재생성합니다. `data/` 폴더를 삭제하면 DB가 초기화됩니다.

---

## 3. 스테이징 / 프로덕션 배포

### 3.1 PostgreSQL 설정

#### Docker로 실행 (권장)

```bash
docker run -d \
  --name insight-postgres \
  -e POSTGRES_DB=insight \
  -e POSTGRES_USER=insight_user \
  -e POSTGRES_PASSWORD=<강력한_비밀번호> \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:15-alpine

# DB 준비 확인
docker exec insight-postgres pg_isready -U insight_user
```

#### 직접 설치 (Linux)

```bash
# Ubuntu / Debian
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql

# psql로 DB 및 사용자 생성
sudo -u postgres psql <<'SQL'
CREATE DATABASE insight;
CREATE USER insight_user WITH ENCRYPTED PASSWORD '<비밀번호>';
GRANT ALL PRIVILEGES ON DATABASE insight TO insight_user;
\q
SQL
```

스키마는 애플리케이션 기동 시 `schema.sql`로 자동 생성됩니다.

---

### 3.2 JAR 직접 실행

#### 빌드

```bash
cd backend
mvn clean package -DskipTests
# 결과물: backend/target/github-activity-insight-1.0.0.jar
```

#### 환경 변수 설정 및 실행

```bash
export DB_HOST=<PostgreSQL 서버 IP>
export DB_PORT=5432
export DB_NAME=insight
export DB_USER=insight_user
export DB_PASSWORD=<비밀번호>
export GITHUB_OAUTH_CLIENT_ID=<Client ID>
export GITHUB_OAUTH_CLIENT_SECRET=<Client Secret>
export GITHUB_OAUTH_REDIRECT_URI=https://your-domain.com/auth/callback
export GITHUB_TOKEN=<Personal Access Token>  # 선택

# 프로젝트 루트에서 실행 (web/ 폴더 서빙을 위해 루트에서 실행)
java -jar backend/target/github-activity-insight-1.0.0.jar
```

> **주의**: JAR 실행은 반드시 **프로젝트 루트 디렉터리**에서 실행해야 합니다. `WebConfig`가 `file:web/` 경로를 참조하므로, `backend/` 안에서 실행하면 프론트엔드 파일이 404 오류를 반환합니다.

---

### 3.3 systemd 서비스로 등록 (Linux 상시 운영)

`/etc/systemd/system/github-insight.service` 파일 생성:

```ini
[Unit]
Description=GitHub Activity Insight
After=network.target postgresql.service

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/github-insight
EnvironmentFile=/opt/github-insight/.env
ExecStart=/usr/bin/java -jar /opt/github-insight/backend/target/github-activity-insight-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

`/opt/github-insight/.env` 파일:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=insight
DB_USER=insight_user
DB_PASSWORD=<비밀번호>
GITHUB_OAUTH_CLIENT_ID=<Client ID>
GITHUB_OAUTH_CLIENT_SECRET=<Client Secret>
GITHUB_OAUTH_REDIRECT_URI=https://your-domain.com/auth/callback
GITHUB_TOKEN=<Personal Access Token>
```

서비스 등록 및 시작:

```bash
sudo systemctl daemon-reload
sudo systemctl enable github-insight
sudo systemctl start github-insight
sudo systemctl status github-insight
```

---

### 3.4 Docker 컨테이너 배포

#### Dockerfile 생성 (`backend/Dockerfile`)

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 프론트엔드 정적 파일 복사 (web/ → /app/web/)
COPY ../web ./web

# JAR 복사
COPY target/github-activity-insight-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 이미지 빌드 및 컨테이너 실행

```bash
# 프로젝트 루트에서 빌드
cd backend && mvn clean package -DskipTests && cd ..

# 이미지 빌드
docker build -f backend/Dockerfile -t github-insight:latest .

# 컨테이너 실행
docker run -d \
  --name github-insight \
  -p 8080:8080 \
  -e DB_HOST=insight-postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=insight \
  -e DB_USER=insight_user \
  -e DB_PASSWORD=<비밀번호> \
  -e GITHUB_OAUTH_CLIENT_ID=<Client ID> \
  -e GITHUB_OAUTH_CLIENT_SECRET=<Client Secret> \
  -e GITHUB_OAUTH_REDIRECT_URI=https://your-domain.com/auth/callback \
  --link insight-postgres \
  github-insight:latest
```

#### Docker Compose로 전체 스택 실행

프로젝트 루트에 `docker-compose.yml` 생성:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: insight
      POSTGRES_USER: insight_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U insight_user"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: backend/Dockerfile
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: insight
      DB_USER: insight_user
      DB_PASSWORD: ${DB_PASSWORD}
      GITHUB_OAUTH_CLIENT_ID: ${GITHUB_OAUTH_CLIENT_ID}
      GITHUB_OAUTH_CLIENT_SECRET: ${GITHUB_OAUTH_CLIENT_SECRET}
      GITHUB_OAUTH_REDIRECT_URI: ${GITHUB_OAUTH_REDIRECT_URI}
      GITHUB_TOKEN: ${GITHUB_TOKEN}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

`.env` 파일을 프로젝트 루트에 생성 후 실행:

```bash
docker-compose up -d

# 로그 확인
docker-compose logs -f app
docker-compose logs -f postgres

# 종료
docker-compose down
```

---

## 4. 접속 URL 및 API 목록

### 웹 화면

| 페이지 | URL |
|--------|-----|
| 메인 대시보드 | `http://localhost:8080/` |
| 로그인 | `http://localhost:8080/login.html` |
| 분석 진행 | `http://localhost:8080/progress.html` |
| 분석 결과 | `http://localhost:8080/result.html` |
| 분석 이력 | `http://localhost:8080/history.html` |

### REST API

| 분류 | 메서드 | 경로 | 인증 필요 | 설명 |
|------|--------|------|-----------|------|
| 정보 | GET | `/api/info` | 불필요 | API 버전 및 엔드포인트 목록 |
| 인증 | GET | `/auth/login` | 불필요 | GitHub OAuth 로그인 시작 |
| 인증 | GET | `/auth/callback` | 불필요 | OAuth 콜백 (자동 호출) |
| 인증 | GET | `/auth/me` | 불필요 | 현재 세션 사용자 정보 |
| 인증 | POST | `/auth/logout` | 필요 | 세션 종료 |
| GitHub | GET | `/api/github/validate?id={id}` | 불필요 | GitHub ID 존재 확인 |
| 분석 | POST | `/api/analysis/request` | 불필요 | 분석 요청 (Body: `{"githubId":"..."}`) |
| 분석 | GET | `/api/analysis/status/{requestId}` | 불필요 | 분석 진행률 폴링 |
| 분석 | GET | `/api/analysis/result/{githubId}` | **필요** | 최신 분석 결과 조회 |
| 분석 | GET | `/api/analysis/history/{githubId}` | **필요** | 분석 이력 목록 |
| 분석 | GET | `/api/analysis/report/{githubId}` | **필요** | PDF 리포트 다운로드 |
| 분석 | POST | `/api/analysis/cancel/{requestId}` | 불필요 | 분석 취소 |

---

## 5. 헬스 체크

```bash
# API 기동 확인
curl http://localhost:8080/api/info

# 로그인 상태 확인 (세션 없을 때 {"loggedIn":false} 반환)
curl http://localhost:8080/auth/me

# PostgreSQL 연결 확인 (Docker)
docker exec insight-postgres pg_isready -U insight_user
```

---

## 6. 로그 확인

### 로컬 실행 시

```bash
# 콘솔 출력 확인 (mvn spring-boot:run 실행 창)
# 로그 레벨: application-local.properties에서 DEBUG 설정됨
```

### JAR 실행 시

```bash
# 백그라운드 실행 + 로그 파일 저장
java -jar backend/target/github-activity-insight-1.0.0.jar \
  > logs/app.log 2>&1 &

tail -f logs/app.log
```

### Docker 실행 시

```bash
docker logs -f github-insight
docker-compose logs -f app
```

---

## 7. 문제 해결

### PostgreSQL 연결 실패

```
FATAL: password authentication failed for user "insight_user"
```

1. `DB_PASSWORD` 환경 변수 값 확인
2. `psql -h $DB_HOST -U $DB_USER -d $DB_NAME` 으로 직접 접속 테스트
3. `docker logs insight-postgres` 컨테이너 로그 확인

### 스키마 오류 (테이블 없음)

```
ERROR: relation "analysis_requests" does not exist
```

- 프로덕션 프로파일은 `ddl-auto=validate`이므로 스키마가 미리 생성되어 있어야 합니다.
- `schema.sql`을 DB에 직접 적용: `psql -U insight_user -d insight -f backend/src/main/resources/schema.sql`

### 포트 충돌

```
Address already in use: 8080
```

```bash
# Linux/macOS: 점유 프로세스 확인 및 종료
lsof -i :8080
kill -9 <PID>

# 다른 포트로 실행
java -jar backend/target/github-activity-insight-1.0.0.jar --server.port=8081
```

### 프론트엔드 404

`WebConfig`가 `file:web/` 경로를 참조합니다.
- `mvn spring-boot:run`은 `backend/` 디렉터리에서 실행 (Maven이 작업 디렉터리를 루트로 설정)
- `java -jar`는 프로젝트 루트 디렉터리에서 실행

### OAuth 로그인 안 됨

- `GITHUB_OAUTH_CLIENT_ID`, `GITHUB_OAUTH_CLIENT_SECRET` 환경 변수 설정 여부 확인
- GitHub OAuth App의 **Authorization callback URL**이 현재 서버 주소와 일치하는지 확인
- `curl http://localhost:8080/auth/login` 응답에서 `"configured": false`가 반환되면 환경 변수 미설정 상태

---

## 8. 보안 주의사항

| 항목 | 조치 |
|------|------|
| DB 비밀번호 | 20자 이상 무작위 문자열 사용 |
| OAuth Secret | 코드/저장소에 절대 포함 금지 |
| HTTPS | 프로덕션에서 반드시 HTTPS 사용 (OAuth redirect URI 포함) |
| H2 콘솔 | 프로덕션 프로파일에서는 비활성화됨 (`enabled=false` 기본값) |
| DB 접근 | PostgreSQL 방화벽으로 내부망만 허용 |
| `.env` 파일 | `.gitignore`에 포함 여부 확인 |

---

## 참고 링크

- [빠른 실행 가이드 (running.md)](running.md)
- [Spring Boot 외부 설정](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
- [GitHub OAuth App 문서](https://docs.github.com/en/developers/apps/building-oauth-apps)
