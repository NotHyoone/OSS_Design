# 배포 및 운영 가이드

## 개요

GitHub Activity Insight 백엔드는 Spring Boot 기반 애플리케이션으로, PostgreSQL 데이터베이스를 사용하여 데이터를 영속성있게 관리합니다.

이 문서는 로컬 개발 환경과 프로덕션 배포 환경에서 애플리케이션을 설정하고 실행하는 방법을 설명합니다.

---

## 1. 로컬 개발 환경 설정

### 1.1 필수 요구사항

- Java 17 이상
- Maven 3.8+
- PostgreSQL 12 이상 (또는 H2 임베디드 DB 사용)

### 1.2 H2 임베디드 DB를 이용한 로컬 개발

H2를 사용하면 별도의 PostgreSQL 서버 설정 없이 로컬에서 빠르게 개발할 수 있습니다.

#### application-local.properties 생성

`backend/src/main/resources/application-local.properties` 파일을 생성하세요:

```properties
# 프로파일 활성화
spring.profiles.active=local

# H2 데이터베이스 설정 (파일 기반)
spring.datasource.url=jdbc:h2:file:./data/insight;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 콘솔 활성화 (http://localhost:8080/h2-console)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA/Hibernate 설정
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# 로깅
logging.level.com.github.insight=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

#### 로컬 실행

```bash
cd backend

# application-local.properties로 실행
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# 또는 IDE에서 실행할 때 VM 옵션에 추가:
# -Dspring.profiles.active=local
```

#### H2 콘솔 접근

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/insight;MODE=PostgreSQL`
- Username: `sa`
- Password: (공란)

---

## 2. 프로덕션 배포 환경 설정

### 2.1 PostgreSQL 설치 및 설정

#### Docker를 이용한 설치 (권장)

```bash
docker run -d \
  --name insight-postgres \
  -e POSTGRES_DB=insight \
  -e POSTGRES_USER=insight_user \
  -e POSTGRES_PASSWORD=your_secure_password \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:15-alpine
```

#### 직접 설치 (Linux/macOS)

```bash
# macOS (Homebrew)
brew install postgresql@15
brew services start postgresql@15

# Ubuntu/Debian
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
```

#### 데이터베이스 생성

```bash
# PostgreSQL 클라이언트 접속
psql -U postgres

# 데이터베이스 생성
CREATE DATABASE insight;

# 사용자 생성
CREATE USER insight_user WITH ENCRYPTED PASSWORD 'your_secure_password';

# 권한 부여
GRANT ALL PRIVILEGES ON DATABASE insight TO insight_user;

# 접속 종료
\q
```

### 2.2 환경 변수 설정

#### 필수 환경 변수

애플리케이션 실행 전에 다음 환경 변수를 설정하세요:

| 환경 변수 | 기본값 | 설명 | 예시 |
|----------|--------|------|------|
| `DB_HOST` | `localhost` | PostgreSQL 서버 주소 | `db.example.com` |
| `DB_PORT` | `5432` | PostgreSQL 포트 | `5432` |
| `DB_NAME` | `insight` | 데이터베이스 이름 | `insight` |
| `DB_USER` | `postgres` | DB 사용자명 | `insight_user` |
| `DB_PASSWORD` | (필수) | DB 비밀번호 | `secure_password123` |
| `GITHUB_OAUTH_CLIENT_ID` | (필수) | GitHub OAuth 클라이언트 ID | (GitHub App에서 취득) |
| `GITHUB_OAUTH_CLIENT_SECRET` | (필수) | GitHub OAuth 클라이언트 시크릿 | (GitHub App에서 취득) |
| `GITHUB_OAUTH_REDIRECT_URI` | `http://localhost:8080/auth/callback` | OAuth 리다이렉트 URI | `https://your-domain.com/auth/callback` |

#### Linux/macOS에서 환경 변수 설정

**방법 1: 명령어로 직접 설정**

```bash
export DB_HOST=your-postgres-server
export DB_PORT=5432
export DB_NAME=insight
export DB_USER=insight_user
export DB_PASSWORD=your_secure_password
export GITHUB_OAUTH_CLIENT_ID=your_client_id
export GITHUB_OAUTH_CLIENT_SECRET=your_client_secret
export GITHUB_OAUTH_REDIRECT_URI=https://your-domain.com/auth/callback
```

**방법 2: .env 파일 생성 및 로드**

`.env` 파일을 프로젝트 루트에 생성:

```bash
DB_HOST=your-postgres-server
DB_PORT=5432
DB_NAME=insight
DB_USER=insight_user
DB_PASSWORD=your_secure_password
GITHUB_OAUTH_CLIENT_ID=your_client_id
GITHUB_OAUTH_CLIENT_SECRET=your_client_secret
GITHUB_OAUTH_REDIRECT_URI=https://your-domain.com/auth/callback
```

셸에 로드:

```bash
set -a
source .env
set +a
```

**방법 3: systemd 서비스로 관리**

`/etc/systemd/system/github-insight.service` 파일 생성:

```ini
[Unit]
Description=GitHub Activity Insight Backend
After=network.target postgresql.service

[Service]
Type=simple
User=app_user
WorkingDirectory=/opt/github-insight
Environment="DB_HOST=your-postgres-server"
Environment="DB_PORT=5432"
Environment="DB_NAME=insight"
Environment="DB_USER=insight_user"
Environment="DB_PASSWORD=your_secure_password"
Environment="GITHUB_OAUTH_CLIENT_ID=your_client_id"
Environment="GITHUB_OAUTH_CLIENT_SECRET=your_client_secret"
Environment="GITHUB_OAUTH_REDIRECT_URI=https://your-domain.com/auth/callback"
ExecStart=/opt/github-insight/backend/target/github-activity-insight-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

#### Windows에서 환경 변수 설정

**방법 1: 시스템 속성에서 설정**

1. `Win + X` → "시스템" 클릭
2. "고급 시스템 설정" → "환경 변수" 클릭
3. "시스템 환경 변수" → "새로 만들기" 클릭
4. 변수명과 값 입력
5. 시스템 재부팅

**방법 2: PowerShell에서 설정 (임시)**

```powershell
$env:DB_HOST = "your-postgres-server"
$env:DB_PORT = "5432"
$env:DB_NAME = "insight"
$env:DB_USER = "insight_user"
$env:DB_PASSWORD = "your_secure_password"
$env:GITHUB_OAUTH_CLIENT_ID = "your_client_id"
$env:GITHUB_OAUTH_CLIENT_SECRET = "your_client_secret"
$env:GITHUB_OAUTH_REDIRECT_URI = "https://your-domain.com/auth/callback"
```

### 2.3 애플리케이션 빌드 및 실행

#### 빌드

```bash
cd backend
mvn clean package -DskipTests
```

#### JAR 파일 실행

```bash
java -jar target/github-activity-insight-1.0.0.jar
```

#### Docker를 이용한 배포

**Dockerfile 생성** (`backend/Dockerfile`):

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/github-activity-insight-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Docker 이미지 빌드 및 실행:**

```bash
# 이미지 빌드
docker build -t github-insight:latest backend/

# 컨테이너 실행
docker run -d \
  --name github-insight \
  -p 8080:8080 \
  -e DB_HOST=insight-postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=insight \
  -e DB_USER=insight_user \
  -e DB_PASSWORD=your_secure_password \
  -e GITHUB_OAUTH_CLIENT_ID=your_client_id \
  -e GITHUB_OAUTH_CLIENT_SECRET=your_client_secret \
  --link insight-postgres \
  github-insight:latest
```

#### Docker Compose를 이용한 전체 스택 배포

**docker-compose.yml** 생성:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: insight
      POSTGRES_USER: insight_user
      POSTGRES_PASSWORD: your_secure_password
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
    build: ./backend
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: insight
      DB_USER: insight_user
      DB_PASSWORD: your_secure_password
      GITHUB_OAUTH_CLIENT_ID: ${GITHUB_OAUTH_CLIENT_ID}
      GITHUB_OAUTH_CLIENT_SECRET: ${GITHUB_OAUTH_CLIENT_SECRET}
      GITHUB_OAUTH_REDIRECT_URI: ${GITHUB_OAUTH_REDIRECT_URI}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

**실행:**

```bash
docker-compose up -d
```

---

## 3. 환경별 설정 요약

### 로컬 개발 (H2)

```bash
# 1. application-local.properties 생성
# 2. Maven 실행
mvn spring-boot:run -Dspring.profiles.active=local

# 3. H2 콘솔 접근
# http://localhost:8080/h2-console
```

### 스테이징 (PostgreSQL)

```bash
# 1. PostgreSQL 설정
# 2. 환경 변수 설정
export DB_HOST=staging-postgres.internal
export DB_NAME=insight_staging
export DB_USER=insight_user
export DB_PASSWORD=staging_password
# ... (나머지 변수)

# 3. 애플리케이션 실행
java -jar target/github-activity-insight-1.0.0.jar
```

### 프로덕션 (PostgreSQL + Docker)

```bash
# docker-compose.yml 실행
docker-compose -f docker-compose.yml up -d

# 상태 확인
docker-compose ps
docker-compose logs -f app
```

---

## 4. 헬스 체크 및 모니터링

### 헬스 체크 엔드포인트

```bash
# 서버 상태 확인
curl http://localhost:8080/api/auth/me

# 데이터베이스 연결 확인
# Spring Boot Actuator 활성화 시:
curl http://localhost:8080/actuator/health
```

### 로그 모니터링

**로컬:**

```bash
tail -f logs/application.log
```

**Docker:**

```bash
docker-compose logs -f app
docker-compose logs -f postgres
```

---

## 5. 문제 해결

### PostgreSQL 연결 실패

```
ERROR: FATAL: password authentication failed for user "insight_user"
```

**해결:**
1. DB_PASSWORD 환경 변수 확인
2. PostgreSQL 사용자 비밀번호 확인
3. PostgreSQL 서버 상태 확인: `pg_isready -h your-host -U insight_user`

### 데이터베이스 마이그레이션 오류

```
ERROR: relation "analysis_requests" does not exist
```

**해결:**
1. `spring.jpa.hibernate.ddl-auto=update` 확인 (application.properties)
2. PostgreSQL 권한 확인
3. 데이터베이스 로그 확인: `docker-compose logs postgres`

### 포트 충돌

```
Address already in use: 8080
```

**해결:**
1. 포트 변경: `java -jar app.jar --server.port=8081`
2. 기존 프로세스 종료: `kill -9 $(lsof -t -i:8080)`

---

## 6. 보안 주의사항

⚠️ **프로덕션 환경에서:**

1. ✅ 환경 변수를 사용하여 민감한 정보 분리
2. ✅ DB 비밀번호는 강력한 암호 사용 (20자 이상, 특수문자 포함)
3. ✅ GitHub OAuth 시크릿은 절대 코드에 포함하지 않기
4. ✅ HTTPS 사용 (OAuth 리다이렉트 URI)
5. ✅ 데이터베이스 백업 정기 수행
6. ✅ PostgreSQL 접근 제한 (방화벽 설정)
7. ✅ 로그에서 민감 정보 마스킹 확인

---

## 참고 링크

- [Spring Boot 환경 변수 설정](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
- [GitHub OAuth Documentation](https://docs.github.com/en/developers/apps/building-oauth-apps)
