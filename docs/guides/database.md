# 데이터베이스 설정 및 초기화 가이드

## 개요

GitHub Activity Insight는 두 가지 데이터베이스 옵션을 지원합니다:

- **H2** (로컬 개발용) - 별도 설치 없음, 자동 초기화
- **PostgreSQL** (프로덕션/개발 환경) - Docker 또는 로컬 설치

---

## 1️⃣ H2 임베디드 DB (권장: 로컬 개발)

### 특징
- ✅ 설치 불필요
- ✅ 자동 초기화 (schema.sql)
- ✅ 파일 기반 데이터 영속성 (`./data/insight.mv.db`)
- ✅ H2 콘솔 (http://localhost:8080/h2-console)

### 빠른 시작

```bash
cd backend

# Maven으로 실행 (로컬 프로필 자동 활성화)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### H2 콘솔 접속

1. **URL**: http://localhost:8080/h2-console
2. **JDBC URL**: `jdbc:h2:file:./data/insight;MODE=PostgreSQL`
3. **Username**: `sa`
4. **Password**: (공란)

### 데이터 초기화

```bash
# 데이터 디렉토리 삭제
rm -rf ./data/insight*

# 또는 Windows
rmdir /s data\insight*
```

---

## 2️⃣ PostgreSQL (프로덕션/개발)

### 옵션 A: Docker로 실행 (권장)

```bash
# PostgreSQL 컨테이너 시작
docker run -d \
  --name insight-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=postgres \
  -p 5432:5432 \
  postgres:15-alpine

# 데이터베이스 및 테이블 초기화
docker exec insight-postgres psql -U postgres << EOF
CREATE DATABASE insight;
\c insight
EOF

# insight 사용자 생성
docker exec insight-postgres psql -U postgres -d insight << EOF
CREATE USER insight_user WITH PASSWORD 'localdev123';
GRANT ALL PRIVILEGES ON DATABASE insight TO insight_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO insight_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO insight_user;
EOF
```

### 옵션 B: 로컬 PostgreSQL 설치

**macOS:**
```bash
# Homebrew 설치
brew install postgresql@15

# 서비스 시작
brew services start postgresql@15
```

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install -y postgresql-15

sudo systemctl start postgresql
```

**Windows:**
- [PostgreSQL 공식 설치 프로그램](https://www.postgresql.org/download/windows/) 다운로드
- 설치 중 기본값 유지

### 환경 변수 설정

Linux/macOS:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=insight
export DB_USER=insight_user
export DB_PASSWORD=localdev123
```

Windows (PowerShell):
```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="insight"
$env:DB_USER="insight_user"
$env:DB_PASSWORD="localdev123"
```

### 데이터베이스 초기화

**Linux/macOS:**
```bash
# 스크립트 실행
bash backend/scripts/init-postgres.sh
```

**Windows:**
```batch
# 배치 파일 실행
backend\scripts\init-postgres.bat
```

또는 수동으로:
```bash
psql -h localhost -U postgres -d insight -f backend/src/main/resources/schema.sql
```

### PostgreSQL 연결 확인

```bash
# 데이터베이스 접속
psql -h localhost -U insight_user -d insight

# 테이블 조회
\dt

# 연결 종료
\q
```

---

## 3️⃣ 애플리케이션 설정

### H2 사용 (로컬)

**자동 활성화 (권장):**
```bash
# 로컬 프로필 지정하면 H2 자동 설정
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

**IDE 실행 시:**
- VM Options: `-Dspring.profiles.active=local`

### PostgreSQL 사용

**환경 변수 설정 후:**
```bash
mvn spring-boot:run
```

또는 `.env` 파일:
```bash
# 프로젝트 루트에 .env 파일 생성
source .env.local
mvn spring-boot:run
```

---

## 🔍 문제 해결

### PostgreSQL 연결 실패

```bash
# 연결 테스트
psql -h localhost -U insight_user -d insight

# 컨테이너 상태 확인
docker ps | grep postgres

# 컨테이너 로그 확인
docker logs insight-postgres
```

### H2 파일 손상

```bash
# 데이터 디렉토리 삭제 후 재시작
rm -rf ./data/

# 다시 실행하면 자동 초기화됨
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### 포트 충돌

```bash
# 기존 프로세스 확인
lsof -i :5432  # PostgreSQL
lsof -i :8080  # 서버

# 다른 포트로 실행
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

---

## 📊 데이터베이스 스키마

### 테이블 구조

1. **users** - 사용자 정보
   - user_id (PK), github_id, email, display_name, avatar_url, session_id

2. **analysis_requests** - 분석 요청 이력
   - request_id (PK), user_id (FK), status, requested_at, completed_at

3. **analysis_results** - 분석 결과
   - result_id (PK), request_id (FK), total_score, developer_type, trust_level

4. **metrics** - 분석 메트릭
   - request_id (PK), activity_score, diversity_score, collaboration_score, persistence_score

---

## 🔒 보안 고려사항

- `DB_PASSWORD`는 `.env` 또는 환경 변수로만 관리
- Git에 커밋하지 않음 (`.gitignore` 확인)
- 프로덕션에서는 강력한 암호 사용
- PostgreSQL 접근 제한 설정 (방화벽/보안 그룹)

---

## 📚 관련 문서

- [running.md](running.md) - 빠른 시작 가이드
- [deployment.md](deployment.md) - 배포 가이드
