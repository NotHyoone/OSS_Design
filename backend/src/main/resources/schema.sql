-- ========================================
-- GitHub Activity Insight 데이터베이스 스키마
-- 모든 데이터베이스(H2, PostgreSQL)에 호환되는 SQL
-- ========================================

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
  user_id VARCHAR(36) PRIMARY KEY,
  github_id VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL,
  display_name VARCHAR(255),
  avatar_url VARCHAR(500),
  session_id VARCHAR(255) UNIQUE,
  created_at TIMESTAMP NOT NULL,
  last_login_at TIMESTAMP,
  CONSTRAINT uk_users_github_id UNIQUE(github_id),
  CONSTRAINT uk_users_session_id UNIQUE(session_id)
);

-- 분석 요청 테이블
CREATE TABLE IF NOT EXISTS analysis_requests (
  request_id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36),
  github_id VARCHAR(100) NOT NULL,
  result_access_token VARCHAR(36) UNIQUE,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  requested_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP,
  error_message TEXT,
  retry_count INT NOT NULL DEFAULT 0,
  step INT DEFAULT 0,
  overall_pct DOUBLE PRECISION DEFAULT 0.0,
  detail VARCHAR(500) DEFAULT '대기 중...',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 비로그인 분석 요청을 허용하기 위해 user_id nullable 보정
ALTER TABLE analysis_requests ALTER COLUMN user_id DROP NOT NULL;

-- 분석 결과 테이블
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

-- 메트릭스 테이블
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

-- 인덱스 생성 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_analysis_requests_user_id ON analysis_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_analysis_requests_github_id ON analysis_requests(github_id);
CREATE INDEX IF NOT EXISTS idx_analysis_requests_status ON analysis_requests(status);
CREATE INDEX IF NOT EXISTS idx_analysis_requests_access_token ON analysis_requests(result_access_token);
CREATE INDEX IF NOT EXISTS idx_analysis_results_user_id ON analysis_results(user_id);
CREATE INDEX IF NOT EXISTS idx_analysis_results_github_id ON analysis_results(github_id);
CREATE INDEX IF NOT EXISTS idx_users_github_id ON users(github_id);
