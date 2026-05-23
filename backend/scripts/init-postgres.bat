@echo off
REM ========================================
REM PostgreSQL 데이터베이스 초기화 스크립트 (Windows)
REM ========================================

setlocal enabledelayedexpansion

set DB_HOST=%DB_HOST:localhost=localhost%
set DB_PORT=%DB_PORT:5432=5432%
set DB_NAME=insight
set DB_USER=%DB_USER:insight_user=insight_user%
set DB_PASSWORD=%DB_PASSWORD:localdev123=localdev123%

echo PostgreSQL 데이터베이스 초기화 시작...
echo Host: %DB_HOST%, Port: %DB_PORT%, Database: %DB_NAME%, User: %DB_USER%

REM PostgreSQL이 설치되어 있는지 확인
where psql >nul 2>nul
if errorlevel 1 (
    echo 오류: PostgreSQL이 설치되지 않았거나 PATH에 추가되지 않았습니다.
    exit /b 1
)

REM 데이터베이스 생성
echo 데이터베이스 생성 중...
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = '%DB_NAME%'" | find "1" >nul
if errorlevel 1 (
    psql -h %DB_HOST% -U postgres -c "CREATE DATABASE %DB_NAME%"
)

REM 테이블 생성
echo 테이블 생성 중...
psql -h %DB_HOST% -U %DB_USER% -d %DB_NAME% -f schema.sql

echo 데이터베이스 초기화 완료!
echo 다음 명령으로 데이터베이스 상태를 확인할 수 있습니다:
echo   psql -h %DB_HOST% -U %DB_USER% -d %DB_NAME% -c "\dt"

endlocal
