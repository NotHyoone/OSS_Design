#!/bin/bash
# GitHub Activity Insight - 로컬 실행 스크립트

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.local"

# .env.local 파일 로드
if [ -f "$ENV_FILE" ]; then
    set -a
    source "$ENV_FILE"
    set +a
    echo "[INFO] .env.local 로드 완료"
else
    echo "[WARN] .env.local 파일이 없습니다. 환경변수를 직접 설정하세요."
fi

# OAuth 설정 확인
if [ -z "$GITHUB_OAUTH_CLIENT_ID" ]; then
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  GitHub OAuth App 미설정"
    echo ""
    echo "  1. https://github.com/settings/applications/new 접속"
    echo "  2. Application name: OSS Design (Local)"
    echo "  3. Homepage URL: http://localhost:8080"
    echo "  4. Authorization callback URL: http://localhost:8080/auth/callback"
    echo "  5. .env.local 파일에 GITHUB_OAUTH_CLIENT_ID, GITHUB_OAUTH_CLIENT_SECRET 입력"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "  OAuth 없이 계속 실행하려면 Enter, 종료하려면 Ctrl+C"
    read -r
fi

# PostgreSQL 컨테이너 확인 및 시작
if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^postgres-insight$"; then
    echo "[INFO] PostgreSQL 컨테이너 시작..."
    if docker ps -a --format '{{.Names}}' 2>/dev/null | grep -q "^postgres-insight$"; then
        docker start postgres-insight
    else
        docker run -d \
            --name postgres-insight \
            -e POSTGRES_DB=insight \
            -e POSTGRES_USER=postgres \
            -e POSTGRES_PASSWORD="$DB_PASSWORD" \
            -p 5432:5432 \
            postgres:15
        echo "[INFO] DB 초기화 대기 중..."
        sleep 3
        docker exec -i postgres-insight psql -U postgres -d insight \
            < "$SCRIPT_DIR/backend/src/main/resources/schema.sql"
    fi
    echo "[INFO] PostgreSQL 시작 완료"
fi

# 앱 실행
echo "[INFO] Spring Boot 앱 시작..."
cd "$SCRIPT_DIR/backend"
exec mvn spring-boot:run
