# 과제 제출 및 외부 접속 URL 배포 가이드

이 문서는 GitHub Activity Insight 프로젝트를 과제 제출 형식에 맞게 준비하고, 외부에서 접속 가능한 웹 URL을 만드는 방법을 정리한다.

## 1. 제출물 구성

과제 요구사항 기준으로 제출물은 다음 두 가지로 나누어 준비한다.

1. 객체지향 언어로 구현한 소스 코드 압축본
2. 빌드가 완료된 실행 가능한 배포 파일 또는 외부 접속 가능한 웹 URL

이 프로젝트는 Java 17, Spring Boot, Maven 기반 백엔드와 `web/` 정적 프론트엔드를 함께 사용하는 웹 서비스이다. 따라서 APK 제출 대상은 아니며, 웹 서비스 URL 또는 실행 가능한 JAR 파일을 제출하는 방식이 적합하다.

## 2. 소스 코드 압축본 준비

GitHub에서 작업한 소스 코드를 그대로 제출해야 한다면, 가장 안전한 방법은 GitHub 저장소 페이지에서 `Code` -> `Download ZIP`을 사용하는 것이다.

로컬에서 직접 압축할 경우, 커밋된 파일만 포함하도록 다음 명령을 사용할 수 있다.

```bash
git status --short
git archive --format=zip --output=OSS_Design_source.zip HEAD
```

`git status --short` 결과에 변경사항이 남아 있다면, 제출할 변경인지 확인한 뒤 커밋하거나 제외한다.

### 포함할 파일과 디렉터리

```text
backend/
web/
docs/
README.md
AGENTS.md
docker-compose.yml
run.sh
.env.example
.gitignore
.dockerignore
```

### 제외할 파일과 디렉터리

```text
.git/
backend/target/
target/
data/
logs/
.env
.env.local
*.jar
*.zip
```

특히 `.env`에는 GitHub OAuth Client Secret, DB 비밀번호, GitHub Token 같은 민감정보가 들어갈 수 있으므로 제출 압축본에 포함하지 않는다.

## 3. 실행 가능한 JAR 파일 준비

이 프로젝트의 Maven 빌드 결과물은 Spring Boot 실행 JAR이다.

```bash
mvn -f backend/pom.xml clean package -DskipTests
```

빌드 결과물은 다음 경로에 생성된다.

```text
backend/target/github-activity-insight-1.0.0.jar
```

제출 파일명은 다음처럼 정리하면 된다.

```text
github-activity-insight-1.0.0.jar
```

JAR 실행 예시는 다음과 같다.

```bash
java -jar backend/target/github-activity-insight-1.0.0.jar --spring.profiles.active=local
```

실행 후 접속 주소는 다음과 같다.

```text
http://localhost:8080
```

주의할 점은 JAR 자체는 운영체제에 독립적으로 실행 가능하지만, 실행 환경에는 Java 17 이상이 필요하다는 것이다.

## 4. 외부 접속 URL 배포 전 확인사항

외부 URL을 만들려면 로컬 PC가 아니라 서버나 클라우드 플랫폼에 애플리케이션을 실행해야 한다.

배포 전 다음 항목을 확인한다.

```bash
mvn -f backend/pom.xml test
mvn -f backend/pom.xml clean package -DskipTests
```

운영 배포에서는 기본 프로필이 PostgreSQL을 사용한다. 다음 환경변수가 필요하다.

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
GITHUB_OAUTH_CLIENT_ID
GITHUB_OAUTH_CLIENT_SECRET
GITHUB_OAUTH_REDIRECT_URI
GITHUB_TOKEN
CORS_ALLOWED_ORIGINS
SERVER_PORT
```

GitHub 로그인 기능을 사용하려면 GitHub OAuth App의 callback URL도 실제 배포 주소로 바꿔야 한다.

예를 들어 배포 주소가 다음과 같다면:

```text
https://github-activity-insight.example.com
```

GitHub OAuth callback URL은 다음처럼 설정한다.

```text
https://github-activity-insight.example.com/auth/callback
```

## 5. 배포 방법 선택지

### 방법 A. Render

Render는 GitHub 저장소를 연결해 Web Service로 배포할 수 있고, 서비스마다 `onrender.com` 하위 도메인을 제공한다. 공식 문서에서도 GitHub/GitLab/Bitbucket 저장소 또는 Docker 이미지 기반 배포를 지원한다고 안내한다.

추천 상황:

- 가장 단순하게 웹 URL을 만들고 싶을 때
- GitHub 저장소에서 자동 빌드와 자동 배포를 하고 싶을 때
- PostgreSQL 서비스를 같은 플랫폼에서 같이 만들고 싶을 때

기본 흐름:

1. 이 저장소의 루트에 있는 `render.yaml`을 GitHub에 push한다.
2. Render Dashboard에서 `New` -> `Blueprint`를 선택한다.
3. GitHub 저장소를 연결한다.
4. Render가 `render.yaml`을 읽어 Web Service와 PostgreSQL을 생성하는지 확인한다.
5. 초기 생성 화면에서 `sync: false`로 표시된 비밀값을 입력한다.
6. 배포 완료 후 발급된 `onrender.com` 주소로 접속한다.

`render.yaml`은 Docker 기반 Web Service와 Render PostgreSQL을 함께 만든다. Docker 빌드 과정에서 Maven으로 JAR를 생성하고, 런타임 컨테이너에서 `java -jar app.jar`로 실행한다.

Render 환경변수 입력 예시:

```text
CORS_ALLOWED_ORIGINS=https://github-activity-insight.onrender.com
GITHUB_OAUTH_CLIENT_ID=<GitHub OAuth Client ID>
GITHUB_OAUTH_CLIENT_SECRET=<GitHub OAuth Client Secret>
GITHUB_OAUTH_REDIRECT_URI=https://github-activity-insight.onrender.com/auth/callback
GITHUB_TOKEN=<GitHub Personal Access Token, optional>
```

GitHub OAuth를 사용하지 않을 경우 OAuth 관련 값은 비워둘 수 있다. 단, 로그인, 분석 이력, PDF 다운로드 기능을 검증하려면 OAuth 값을 설정해야 한다.

참고:

- Render Web Services: https://render.com/docs/web-services
- Render Blueprint YAML Reference: https://render.com/docs/blueprint-spec

### 방법 B. Railway

Railway는 Spring Boot 프로젝트 배포 가이드를 제공하며, GitHub 저장소 연결과 PostgreSQL 플러그인 구성이 비교적 간단하다.

추천 상황:

- 과제 제출용으로 빠르게 URL을 만들고 싶을 때
- PostgreSQL까지 한 화면에서 붙이고 싶을 때
- 빌드 명령과 시작 명령을 직접 지정하고 싶을 때

기본 흐름:

1. Railway에서 새 프로젝트를 만든다.
2. GitHub 저장소를 연결한다.
3. PostgreSQL 서비스를 추가한다.
4. 애플리케이션 서비스에 DB 환경변수를 연결한다.
5. Build Command를 설정한다.

```bash
mvn -f backend/pom.xml clean package -DskipTests
```

6. Start Command를 설정한다.

```bash
java -jar backend/target/github-activity-insight-1.0.0.jar
```

7. 발급된 Railway 도메인을 기준으로 OAuth callback과 CORS를 설정한다.

참고:

- Railway Spring Boot Guide: https://docs.railway.com/guides/spring-boot
- Railway Deploying with CLI: https://docs.railway.com/cli/deploying

### 방법 C. Fly.io

Fly.io는 Docker 기반 배포에 적합하다. `fly launch`로 앱을 만들고 `fly deploy`로 새 릴리스를 배포하는 흐름을 제공한다.

추천 상황:

- Dockerfile을 추가해서 컨테이너 기반으로 배포하고 싶을 때
- 단순 과제 제출을 넘어 서버 배포 구조를 경험하고 싶을 때
- 배포 지역과 런타임 설정을 직접 관리하고 싶을 때

기본 흐름:

1. 프로젝트에 Dockerfile을 추가한다.
2. `fly launch`로 앱 설정을 생성한다.
3. PostgreSQL은 Fly Postgres 또는 외부 PostgreSQL 서비스를 연결한다.
4. 환경변수를 secret으로 등록한다.
5. `fly deploy`로 배포한다.

예시 Dockerfile:

```Dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -f backend/pom.xml clean package -DskipTests || mvn -f backend/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/backend/target/github-activity-insight-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

참고:

- Fly Deploy Docs: https://fly.io/docs/launch/deploy/
- Fly Launch Overview: https://fly.io/docs/reference/fly-launch/

### 방법 D. Azure App Service

Azure App Service는 Java SE 기반 Spring Boot JAR 배포를 지원한다. Microsoft Learn의 Java App Service 문서에서는 Maven 플러그인이나 포털 기반 배포 흐름을 안내한다.

추천 상황:

- 학교나 개인 Azure 계정이 있을 때
- 관리형 플랫폼에서 Java 웹앱을 운영하고 싶을 때
- PostgreSQL, 로그, 모니터링을 Azure 안에서 함께 관리하고 싶을 때

기본 흐름:

1. Azure App Service에서 Java 17 런타임 웹앱을 만든다.
2. Azure Database for PostgreSQL 또는 외부 PostgreSQL을 준비한다.
3. 애플리케이션 설정에 환경변수를 등록한다.
4. Maven 또는 포털, GitHub Actions로 JAR를 배포한다.
5. App Service URL 기준으로 OAuth callback과 CORS를 설정한다.

참고:

- Azure App Service Java Quickstart: https://learn.microsoft.com/en-us/azure/app-service/quickstart-java
- Java on Azure App Service: https://azure.github.io/AppService/java/

### 방법 E. 개인 VPS 또는 학교 서버

VPS, 학교 서버, 개인 Linux 서버가 있다면 JAR 파일을 직접 업로드해서 실행할 수 있다.

추천 상황:

- 이미 서버가 있을 때
- 클라우드 PaaS 비용을 줄이고 싶을 때
- Nginx, systemd, PostgreSQL을 직접 설정해보고 싶을 때

기본 흐름:

1. 서버에 Java 17을 설치한다.
2. PostgreSQL을 설치하거나 외부 DB를 연결한다.
3. JAR 파일을 서버로 업로드한다.
4. 환경변수를 설정한다.
5. 다음 명령으로 실행한다.

```bash
java -jar github-activity-insight-1.0.0.jar
```

6. `systemd` 서비스로 등록해 서버 재부팅 후에도 자동 실행되게 한다.
7. Nginx를 리버스 프록시로 설정하고 도메인 또는 서버 IP로 접속한다.

이 방식은 자유도가 높지만, 방화벽, HTTPS 인증서, 프로세스 관리, DB 백업을 직접 관리해야 한다.

## 6. 과제 제출용 추천 순서

단순히 제출 가능한 외부 URL이 목표라면 다음 순서를 추천한다.

1. Railway 또는 Render
2. Azure App Service
3. Fly.io
4. 개인 VPS 또는 학교 서버

이 프로젝트는 서버와 DB가 함께 필요하므로, PostgreSQL을 같은 플랫폼에서 쉽게 붙일 수 있는 Railway 또는 Render가 과제 제출용으로 가장 단순하다.

## 7. 공개 URL 제출 전 체크리스트

- `https://배포주소/` 접속 시 메인 화면이 열린다.
- `/api/info`가 정상 응답한다.
- GitHub ID 분석 요청이 정상 동작한다.
- GitHub OAuth 로그인을 사용하는 경우 callback URL이 실제 배포 주소로 등록되어 있다.
- `.env`나 secret 값이 GitHub 저장소에 올라가지 않았다.
- `CORS_ALLOWED_ORIGINS`가 실제 배포 주소로 설정되어 있다.
- DB가 H2가 아니라 PostgreSQL이면 서버 재시작 후에도 데이터가 유지된다.
- 과제 제출 창에는 웹 접속 주소를 정확히 입력한다.

## 8. 제출 예시

소스 코드 첨부:

```text
OSS_Design_source.zip
```

실행 파일 첨부:

```text
github-activity-insight-1.0.0.jar
```

웹 URL 입력:

```text
https://your-service-name.onrender.com
```

또는:

```text
https://your-service-name.up.railway.app
```

실제 URL은 배포 플랫폼에서 발급된 주소를 사용한다.
