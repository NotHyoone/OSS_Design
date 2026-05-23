# 프로젝트 실행 가이드

GitHub Activity Insight를 로컬에서 빠르게 실행하는 방법입니다.

---

## 📋 시스템 요구사항

- **Java**: 17 이상
- **Maven**: 3.8+
- **선택 사항**: PostgreSQL 12+ (H2 임베디드 DB로도 개발 가능)

---

## 🚀 빠른 시작 (H2 임베디드 DB 사용)

로컬 개발에서는 별도의 데이터베이스 설치 없이 H2를 사용할 수 있습니다.

### 1단계: 로컬 설정 파일 생성

`backend/src/main/resources/application-local.properties` 파일을 생성하세요:

```properties
# 프로파일 활성화
spring.profiles.active=local

# H2 데이터베이스 (파일 기반, 자동 생성)
spring.datasource.url=jdbc:h2:file:./data/insight;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 콘솔 활성화 (데이터 조회/관리용)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA/Hibernate 설정
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# 로깅
logging.level.com.github.insight=DEBUG
```

### 2단계: 애플리케이션 실행

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

또는 IDE에서 실행할 때 VM 옵션 추가:
```
-Dspring.profiles.active=local
```

### 3단계: 서버 확인

서버가 시작되면 다음 URL에 접근하세요:

- **API 서버**: http://localhost:8080
- **H2 콘솔**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/insight;MODE=PostgreSQL`
  - Username: `sa`
  - Password: (공란)

---

## 🗄️ PostgreSQL을 사용한 개발

프로덕션과 같은 환경에서 테스트하려면 PostgreSQL을 사용하세요.

### Docker로 PostgreSQL 시작하기 (권장)

```bash
# PostgreSQL 컨테이너 실행
docker run -d \
  --name insight-postgres \
  -e POSTGRES_DB=insight \
  -e POSTGRES_USER=insight_user \
  -e POSTGRES_PASSWORD=localdev123 \
  -p 5432:5432 \
  postgres:15-alpine

# 데이터베이스 생성 확인
docker exec insight-postgres psql -U insight_user -d insight -c "\dt"
```

### 환경 변수 설정

```bash
# Linux/macOS
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=insight
export DB_USER=insight_user
export DB_PASSWORD=localdev123

# Windows (PowerShell)
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="insight"
$env:DB_USER="insight_user"
$env:DB_PASSWORD="localdev123"
```

### 애플리케이션 실행

```bash
cd backend
mvn spring-boot:run
```

---

## 🛠️ 빌드 및 배포

### 프로젝트 빌드

```bash
cd backend
mvn clean package -DskipTests
```

빌드 결과: `backend/target/github-activity-insight-1.0.0.jar`

### 실행 가능한 JAR 파일 실행

```bash
java -jar backend/target/github-activity-insight-1.0.0.jar
```

### Docker 이미지로 배포

```bash
# 이미지 빌드
docker build -t github-insight:latest backend/

# 컨테이너 실행
docker run -d \
  -p 8080:8080 \
  -e DB_HOST=insight-postgres \
  -e DB_USER=insight_user \
  -e DB_PASSWORD=localdev123 \
  --name github-insight \
  --link insight-postgres \
  github-insight:latest
```

### Docker Compose로 전체 스택 실행

```bash
docker-compose up -d
```

---

## 📝 주요 설정 파일

| 파일 | 설명 |
|------|------|
| `backend/pom.xml` | Maven 의존성 및 빌드 설정 |
| `backend/src/main/resources/application.properties` | 기본 설정 |
| `backend/src/main/resources/application-local.properties` | 로컬 개발 설정 (생성 필요) |

---

## ✅ 헬스 체크

### API 서버 상태 확인

```bash
curl http://localhost:8080/api/auth/me
```

### 데이터베이스 연결 확인 (H2)

```bash
curl http://localhost:8080/h2-console
```

---

## 🐛 문제 해결

### 포트 충돌 (8080 이미 사용 중)

```bash
# 포트 변경하여 실행
java -jar backend/target/github-activity-insight-1.0.0.jar --server.port=8081
```

### PostgreSQL 연결 실패

```bash
# 데이터베이스 연결 확인
psql -h localhost -U insight_user -d insight

# 컨테이너 로그 확인
docker logs insight-postgres
```

### 의존성 문제

```bash
# 의존성 업데이트 및 캐시 초기화
mvn clean install -U
```

---

## 📚 추가 정보

- **자세한 배포 가이드**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **프로젝트 개요**: [README.md](README.md)
- **Spring Boot 공식 문서**: https://spring.io/projects/spring-boot

---

## 🔄 개발 워크플로우

```bash
# 1. 로컬 환경 설정
# - application-local.properties 생성

# 2. 애플리케이션 시작
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# 3. 코드 수정 및 테스트
# (IDE에서 코드 변경 → 자동으로 핫 리로드됨)

# 4. 변경사항 커밋
git add .
git commit -m "feat: 새로운 기능 추가"

# 5. 프로덕션 배포 (준비 시)
mvn clean package -DskipTests
docker build -t github-insight:latest backend/
docker run -d -p 8080:8080 github-insight:latest
```
