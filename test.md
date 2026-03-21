# GitHub Activity Insight

GitHub 기반 개발자 실력 분석 및 피드백 웹 시스템

## 제출 정보

| 항목 | 내용 |
| :--- | :--- |
| Student No. | 22212046 |
| Name | 안효원 |
| E-mail | gydnjs3505@gmail.com |

---

## Revision History

| Revision Date | Version # | Description | Author |
| :--- | :--- | :--- | :--- |
| 03/22/2026 | 0.01 | Initial conceptualization structure | 안효원 |

---

## Contents

1. Business Purpose
2. System Context Diagram
3. Use Case List
4. Concept of Operation
5. Problem Statement
6. Glossary
7. References

---

## 1. Business Purpose

### Project Background

현대 소프트웨어 개발에서 GitHub는 개발자의 역량을 보여주는 핵심 플랫폼이다. 그러나 많은 개발자들이 자신의 활동을 객관적으로 해석하고 강점/약점을 구조적으로 파악하는 데 어려움을 겪고 있다. 기존의 단순 지표(저장소 수, 커밋 수) 중심 평가는 실제 역량, 협업 경험, 기술 성장 방향을 충분히 설명하지 못한다.

### Motivation

개발자 개인의 커리어 성장과 취업 준비 과정에서 GitHub 포트폴리오의 중요성은 지속적으로 증가하고 있다. 하지만 사용자는 어떤 활동을 강화해야 하는지, 현재 포트폴리오가 어떤 유형의 개발자 성향을 보여주는지 판단하기 어렵다. 따라서 데이터 수집을 넘어 해석과 개선 방향까지 제시하는 피드백 시스템이 필요하다.

### Goal

GitHub 데이터를 기반으로 개발자의 활동을 종합 분석하고, 개인 맞춤형 피드백 및 성장 방향 리포트를 제공하는 웹 기반 시스템을 구현한다.

### Target Market

- 취업을 준비하는 컴퓨터공학 전공 학생
- 자신의 개발 역량을 점검하고 싶은 개발자
- GitHub 포트폴리오를 체계적으로 개선하고 싶은 사용자

---

## 2. System Context Diagram

시스템은 사용자와 GitHub 데이터 소스, 분석 모듈, 리포트 모듈 사이에서 활동 데이터를 해석하는 웹 플랫폼 역할을 한다.


![System Context Diagram](diagram.png)

| 구성 요소 | 설명 |
| :--- | :--- |
| Developer / Student | 시스템의 주 사용자 |
| Insight Web App | 사용자 입력, 결과 조회, 시각화를 제공하는 인터페이스 |
| GitHub API | 저장소, 커밋, 언어, 협업 데이터 제공 |
| Analysis Module | 활동 데이터 정제 및 지표 산출 수행 |
| Evaluation & Feedback Engine | 점수화, 개발자 유형 분류, 개선 방향 생성 |
| Report Generator | 분석 결과를 리포트(PDF 포함)로 생성 |
| User DB / Analysis History | 사용자 분석 이력 및 결과 저장 |

---

## 3. Use Case List

### 1) Connect GitHub Profile

| 항목 | 내용 |
| :--- | :--- |
| Actor | User |
| Description | 사용자가 GitHub 계정 또는 사용자명을 입력해 분석을 시작한다. |

### 2) Collect Activity Data

| 항목 | 내용 |
| :--- | :--- |
| Actor | System |
| Description | GitHub API를 통해 repository, commit, 협업 관련 데이터를 수집한다. |

### 3) Analyze Developer Activity

| 항목 | 내용 |
| :--- | :--- |
| Actor | System |
| Description | 언어 비율, 프로젝트 규모, 활동 패턴 등 핵심 지표를 분석한다. |

### 4) Evaluate Competency

| 항목 | 내용 |
| :--- | :--- |
| Actor | System |
| Description | 활동성, 협업성, 기술 스택 다양성 등을 기반으로 역량을 평가한다. |

### 5) Provide Personalized Feedback

| 항목 | 내용 |
| :--- | :--- |
| Actor | User, System |
| Description | 시스템이 개발자 유형을 분류하고 개선 방향을 사용자에게 제시한다. |

### 6) Generate Report

| 항목 | 내용 |
| :--- | :--- |
| Actor | User |
| Description | 사용자가 분석 결과를 시각화 대시보드 또는 PDF 리포트 형태로 확인한다. |

---

## 4. Concept of Operation

### 1) Connect GitHub Profile

| 항목 | 내용 |
| :--- | :--- |
| Purpose | 분석 대상 GitHub 사용자 정보를 입력받는다. |
| Approach | 사용자가 GitHub ID를 입력하면 App이 유효성을 검사하고 분석 요청을 생성한다. |
| Dynamics | 사용자가 분석 시작 버튼을 클릭하는 시점 |
| Goals | 분석 시작을 위한 입력/검증 흐름 구현 |

### 2) Collect Activity Data

| 항목 | 내용 |
| :--- | :--- |
| Purpose | 분석에 필요한 원천 데이터를 확보한다. |
| Approach | 시스템이 GitHub API를 호출해 repository, commit, language, collaboration 데이터를 수집한다. |
| Dynamics | 입력 검증 완료 직후 |
| Goals | 안정적인 데이터 수집 파이프라인 구현 |

### 3) Analyze Developer Activity

| 항목 | 내용 |
| :--- | :--- |
| Purpose | 단순 원시 데이터를 해석 가능한 지표로 변환한다. |
| Approach | Analysis Module이 언어 사용 비율, 프로젝트 수/규모, 커밋 주기 등을 계산한다. |
| Dynamics | 데이터 수집 완료 후 자동 수행 |
| Goals | 핵심 분석 지표 산출 |

### 4) Evaluate Competency

| 항목 | 내용 |
| :--- | :--- |
| Purpose | 개발자의 활동 역량을 정량/정성으로 평가한다. |
| Approach | Evaluation Engine이 활동성 점수, 협업 지표, 기술 스택 분류를 생성한다. |
| Dynamics | 지표 산출 직후 |
| Goals | 평가 모델 및 점수 체계 구현 |

### 5) Provide Personalized Feedback

| 항목 | 내용 |
| :--- | :--- |
| Purpose | 사용자가 개선해야 할 포인트를 제시한다. |
| Approach | 평가 결과를 바탕으로 개발자 유형(예: Backend 중심)과 성장 가이드를 생성한다. |
| Dynamics | 역량 평가 완료 후 |
| Goals | 실질적 행동 지침 중심 피드백 제공 |

### 6) Generate Report

| 항목 | 내용 |
| :--- | :--- |
| Purpose | 분석 결과를 재사용 가능한 형태로 제공한다. |
| Approach | 시각화 대시보드와 PDF 리포트를 생성하여 사용자에게 제공한다. |
| Dynamics | 사용자가 리포트 생성을 요청하는 시점 |
| Goals | 결과 공유 및 이력 관리 가능한 리포트 제공 |

---

## 5. Problem Statement

### 시스템 구조

본 시스템은 웹 프론트엔드 인터페이스, 백엔드 데이터 수집/분석 서비스, 외부 GitHub API, 평가/리포트 모듈로 구성된 계층형 구조를 채택한다. 데이터 수집과 해석 로직을 분리하여 확장성과 유지보수성을 확보한다.

### 기술적 과제 및 제약 사항

1. 단순 지표의 한계: commit 수나 repository 수만으로는 실질적 역량을 충분히 설명하기 어렵다.
2. 데이터 편향 문제: 개인의 활동 성격(학업/실무/오픈소스)에 따라 수치 해석 기준이 달라질 수 있다.
3. API 의존성: GitHub API rate limit 및 장애 상황에 대응하는 캐싱/재시도 전략이 필요하다.
4. 평가 신뢰성: 활동 데이터와 실제 개발 능력 사이의 간극을 줄이기 위한 평가 기준 고도화가 필요하다.
5. 피드백 품질: 사용자가 실행 가능한 수준의 구체적 개선 방향을 제공해야 한다.
6. 개인정보 보호: 사용자 분석 결과와 계정 정보를 안전하게 저장/관리해야 한다.
7. 확장성: 향후 코드 품질 분석, 팀 단위 분석 등 고급 기능을 수용할 수 있는 모듈형 구조가 요구된다.

---

## 6. Glossary

| Term | Description |
| :--- | :--- |
| GitHub API | GitHub의 repository, commit, contributor 등 데이터를 조회하기 위한 API |
| Activity Metric | 개발 활동을 정량화한 지표(커밋 빈도, 언어 사용 비율 등) |
| Collaboration Index | PR, issue, contributor 정보 등을 통해 협업 수준을 나타내는 지표 |
| Technical Stack Profile | 사용 언어 및 프로젝트 성격 기반 기술 스택 분류 결과 |
| Personalized Feedback | 사용자 활동 결과에 맞춘 개선 방향 및 추천 액션 |
| Insight Report | 분석 결과를 시각화/문서화한 출력물(PDF 포함) |
| MVP | Minimum Viable Product. 핵심 기능 중심의 최소 실행 제품 |
| Rate Limit | API 요청 횟수 제한 정책 |

---

## 7. References

1. GitHub, GitHub REST API Documentation, https://docs.github.com/en/rest
2. OpenSSF, Open Source Project Security Baseline, https://baseline.openssf.org
3. Bird, C., Rigby, P. C., Barr, E. T., et al., The Promises and Perils of Mining GitHub, MSR Proceedings
4. Spinellis, D., GitHub Mining and Software Engineering Analytics 관련 연구 자료
