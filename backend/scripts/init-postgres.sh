#!/bin/bash
# ========================================
# PostgreSQL 데이터베이스 초기화 스크립트
# ========================================

set -e

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=insight
DB_USER=${DB_USER:-insight_user}
DB_PASSWORD=${DB_PASSWORD:-localdev123}

echo "PostgreSQL 데이터베이스 초기화 시작..."

# PostgreSQL 접속 정보 확인
echo "Host: $DB_HOST, Port: $DB_PORT, Database: $DB_NAME, User: $DB_USER"

# 데이터베이스 생성
echo "✓ 데이터베이스 생성 중..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" | grep -q 1 || \
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U postgres -c "CREATE DATABASE $DB_NAME"

# 테이블 생성
echo "✓ 테이블 생성 중..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d $DB_NAME << EOF
CREATE TABLE IF NOT EXISTS users (
  user_id VARCHAR(36) PRIMARY KEY,
  github_id VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL,
  display_name VARCHAR(255),
  avatar_url VARCHAR(500),
  session_id VARCHAR(255) UNIQUE,
  created_at TIMESTAMP NOT NULL,
  last_login_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analysis_requests (
  request_id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  github_id VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  requested_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP,
  error_message TEXT,
  retry_count INT NOT NULL DEFAULT 0,
  step INT DEFAULT 0,
  overall_pct DOUBLE PRECISION DEFAULT 0.0,
  detail VARCHAR(500) DEFAULT '대기 중...',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS analysis_results (
  result_id VARCHAR(36) PRIMARY KEY,
  request_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36),
  github_id VARCHAR(100) NOT NULL,
  avatar_url VARCHAR(500),
  total_score INT NOT NULL,
  developer_type VARCHAR(50) NOT NULL,
  trust_level VARCHAR(20) NOT NULL,
  strengths_json TEXT,
  improvements_json TEXT,
  created_at TIMESTAMP NOT NULL,
  rule_version VARCHAR(10) NOT NULL DEFAULT '1.0',
  FOREIGN KEY (request_id) REFERENCES analysis_requests(request_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS metrics (
  request_id VARCHAR(36) PRIMARY KEY,
  activity_score FLOAT NOT NULL,
  diversity_score FLOAT NOT NULL,
  collaboration_score FLOAT NOT NULL,
  persistence_score FLOAT NOT NULL,
  trust_level VARCHAR(20) NOT NULL DEFAULT 'HIGH',
  notes TEXT DEFAULT '',
  confidence FLOAT,
  descriptions_json TEXT,
  calculated_at TIMESTAMP NOT NULL,
  FOREIGN KEY (request_id) REFERENCES analysis_requests(request_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_analysis_requests_user_id ON analysis_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_analysis_requests_github_id ON analysis_requests(github_id);
CREATE INDEX IF NOT EXISTS idx_analysis_requests_status ON analysis_requests(status);
CREATE INDEX IF NOT EXISTS idx_analysis_results_user_id ON analysis_results(user_id);
CREATE INDEX IF NOT EXISTS idx_analysis_results_github_id ON analysis_results(github_id);
CREATE INDEX IF NOT EXISTS idx_users_github_id ON users(github_id);
EOF

echo "✓ 데이터베이스 초기화 완료!"
echo ""
echo "다음 명령으로 데이터베이스 상태를 확인할 수 있습니다:"
echo "  psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \"\\dt\""
