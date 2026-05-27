# Documentation Center

프로젝트 문서를 목적별로 분리한 허브입니다. 루트 `README.md`는 프로젝트 소개용 엔트리로 유지하고, 상세 문서는 모두 `docs/` 아래에서 관리합니다.

## 문서 계층

```text
docs/
├── README.md                  # 문서 허브
├── assets/                    # 문서 전용 이미지/다이어그램 소스
│   ├── UseCaseDiagram.jar
│   └── images/
├── guides/                    # 실행, DB, 배포/운영 가이드
│   ├── database.md
│   ├── deployment.md
│   └── running.md
└── project/                   # 기획/분석/설계 산출물
    ├── analysis.md
    ├── conceptualization.md
    ├── design.html
    ├── design.md
    └── structure-draft.md
```

## 빠른 이동

| 카테고리 | 문서 | 용도 |
| :--- | :--- | :--- |
| Start | [README.md](../README.md) | 프로젝트 소개와 전체 진입점 |
| Guides | [guides/running.md](guides/running.md) | 로컬 실행 및 빠른 시작 |
| Guides | [guides/database.md](guides/database.md) | H2/PostgreSQL 설정 |
| Guides | [guides/deployment.md](guides/deployment.md) | 배포/운영 절차 |
| Project | [project/conceptualization.md](project/conceptualization.md) | 기획 단계 산출물 |
| Project | [project/analysis.md](project/analysis.md) | 분석 단계 산출물 |
| Project | [project/design.md](project/design.md) | 설계 단계 산출물 |
| Project | [project/design.html](project/design.html) | 설계 문서 HTML 버전 |
| Project | [project/structure-draft.md](project/structure-draft.md) | 구조 초안과 인터페이스 메모 |

## 권장 탐색 순서

1. [README.md](../README.md)
2. [guides/running.md](guides/running.md)
3. [project/conceptualization.md](project/conceptualization.md)
4. [project/analysis.md](project/analysis.md)
5. [project/design.md](project/design.md)
6. 필요 시 [guides/database.md](guides/database.md), [guides/deployment.md](guides/deployment.md)
