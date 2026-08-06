# Phase 01 — 한국어 하네스와 계획 문서 확정

**Execution profile**: standard
**Status**: completed

## 목표

저장소 지침, 설계 문서, 기능 대응표와 plan001~plan015 실행 명세를 이후 구현의 기준선으로 확정한다.

**범위 외**: 제품 코드, 의존성, README, 기존 API 참고 문서, Docker와 릴리스 태그는 바꾸지 않는다.

## 작업 항목 (4)

### 1. 저장소 지침
- `AGENTS.md`, `CLAUDE.md`, `.claude/commands/*.md`, 두 overlay와 문서 검증자를 한국어로 정리한다.
### 2. 제품·설계 문서
- PRD, 흐름, 코드 구조, 데이터 스키마, 구현 계획과 ADR 색인을 확정한다.
### 3. 기능 대응표
- CLI 말단 명령 66개와 공통 실행 계약 15개를 7열 표로 기록하고 현재 상태를 과장하지 않는다.
### 4. 실행 명세
- `tasks/plan001-project-harness/`부터 `tasks/plan015-docker-release/`까지 index와 phase 파일을 만든다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `AGENTS.md`, `CLAUDE.md`, `.claude/` | 한국어 지침과 하네스 |
| `docs/prd.md`, `docs/flow.md`, `docs/code-architecture.md`, `docs/data-schema.md` | 기준 설계 |
| `docs/implementation-plan.md`, `docs/adr/`, `docs/retrospectives/README.md` | 실행·결정 기록 |
| `docs/parity/dooray-cli-v0.16.0.md` | 81행 대응표 |
| `tasks/plan*/` | 15개 실행 명세 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
git diff --check
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- 공유 계약과 파일 소유권을 먼저 고정해야 후속 PR을 독립적으로 검토하고 병렬화할 수 있다.
