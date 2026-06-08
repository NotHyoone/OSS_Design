# 운영 가이드

GitHub Activity Insight를 Docker Compose 기반으로 실행하고 점검하는 운영 절차입니다.

## 1. 운영 구성

- 애플리케이션: Spring Boot 3.2, Java 17, 내장 Tomcat, 포트 8080
- 데이터베이스: PostgreSQL 15 Alpine, 포트 5432
- 프론트엔드: 빌드 시 JAR 내부 `classpath:/static/`에 포함
- 배포 단위: `docker-compose.yml`의 `app`, `postgres` 서비스

현재 Docker Compose 구성은 PostgreSQL 볼륨 최초 생성 시 `backend/src/main/resources/schema.sql`을 자동 적용합니다. 기본 프로필의 `ddl-auto=validate` 설정을 유지하므로, 스키마가 없거나 엔티티와 맞지 않으면 앱 컨테이너가 기동에 실패합니다.

## 2. 필수 파일

| 파일 | 역할 |
| --- | --- |
| `backend/target/github-activity-insight-1.0.0.jar` | 실행 JAR |
| `backend/Dockerfile` | 앱 컨테이너 이미지 정의 |
| `docker-compose.yml` | 앱과 PostgreSQL 스택 정의 |
| `.env` | 운영 환경 변수. 저장소에 커밋하지 않음 |
| `backend/src/main/resources/schema.sql` | PostgreSQL 초기 스키마 |

## 3. 빌드

```bash
mvn -f backend/pom.xml clean package
```

빌드 결과물:

```text
backend/target/github-activity-insight-1.0.0.jar
```

## 4. 환경 변수

`.env.example`을 복사해 `.env`를 만들고 실제 값을 채웁니다.

```bash
cp .env.example .env
```

운영 필수 값:

| 변수 | 설명 |
| --- | --- |
| `DB_NAME` | PostgreSQL DB 이름 |
| `DB_USER` | PostgreSQL 사용자 |
| `DB_PASSWORD` | PostgreSQL 비밀번호 |
| `APP_PORT` | 호스트에 공개할 앱 포트. 기본 8080 |
| `GITHUB_OAUTH_CLIENT_ID` | GitHub OAuth App Client ID |
| `GITHUB_OAUTH_CLIENT_SECRET` | GitHub OAuth App Client Secret |
| `GITHUB_OAUTH_REDIRECT_URI` | OAuth 콜백 URI |
| `CORS_ALLOWED_ORIGINS` | 허용할 웹 Origin |

운영에서는 `TEST_LOGIN_ENABLED=false`를 유지합니다.

## 5. 배포

```bash
sudo docker compose up -d --build
```

상태 확인:

```bash
sudo docker compose ps
```

정상 상태 예:

```text
oss_design-app-1        Up        0.0.0.0:8080->8080/tcp
oss_design-postgres-1   Up (healthy)
```

## 6. 헬스 체크

웹 루트:

```bash
curl -I http://localhost:8080/
```

API:

```bash
curl -s http://localhost:8080/api/info
```

PostgreSQL:

```bash
sudo docker compose exec postgres pg_isready -U insight_user -d insight
```

로그:

```bash
sudo docker compose logs -f app
sudo docker compose logs -f postgres
```

## 7. 시험용 로그인

시험 환경에서는 GitHub OAuth App을 만들지 않아도 로그인 후 기능을 검증할 수 있습니다.

`.env`에서 다음 값을 설정합니다.

```env
TEST_LOGIN_ENABLED=true
TEST_LOGIN_GITHUB_ID=test-user
TEST_LOGIN_EMAIL=test-user@example.local
TEST_LOGIN_DISPLAY_NAME=Test User
TEST_LOGIN_AVATAR_URL=
```

이 상태에서 `GITHUB_OAUTH_CLIENT_ID`, `GITHUB_OAUTH_CLIENT_SECRET`이 비어 있으면 `/auth/login`은 GitHub로 리다이렉트하지 않고 시험용 사용자 세션을 생성한 뒤 `/?login=test`로 이동합니다. 브라우저에는 `SESSION_ID` HttpOnly 쿠키가 내려가며, `/auth/me`는 로그인 상태를 반환합니다.

시험용 로그인은 OAuth 장애를 숨길 수 있으므로 운영에서는 반드시 비활성화합니다.

```env
TEST_LOGIN_ENABLED=false
```

## 8. 재배포

코드나 프론트엔드 변경 후:

```bash
mvn -f backend/pom.xml clean package
sudo docker compose up -d --build
```

설정만 변경한 경우:

```bash
sudo docker compose up -d
```

## 9. 중지와 정리

컨테이너 중지:

```bash
sudo docker compose down
```

데이터까지 초기화:

```bash
sudo docker compose down -v
```

`down -v`는 PostgreSQL 볼륨을 삭제합니다. 운영 데이터가 모두 사라지므로 백업 없이 실행하지 않습니다.

## 10. 백업

```bash
sudo docker compose exec postgres pg_dump -U insight_user insight > backup.sql
```

복구:

```bash
sudo docker compose exec -T postgres psql -U insight_user insight < backup.sql
```

## 11. 장애 대응

앱이 바로 종료될 때:

```bash
sudo docker compose logs app
```

자주 보는 원인:

- DB 비밀번호 불일치
- PostgreSQL 볼륨은 남아 있는데 `.env`의 DB 사용자나 DB 이름을 변경함
- `schema.sql`과 JPA 엔티티 불일치
- `APP_PORT=8080` 포트 충돌

PostgreSQL 초기 스키마를 다시 적용해야 하는 시험 환경이면 볼륨을 삭제하고 재기동합니다.

```bash
sudo docker compose down -v
sudo docker compose up -d --build
```
