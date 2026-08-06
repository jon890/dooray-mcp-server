# Phase 02 — 해석·쪽매김·캐시 검증

**Execution profile**: fast
**Status**: pending

## 목표

정확·부분·모호성, 전체 페이지와 캐시 교체를 검증하고 plan005를 완료한다.

**범위 외**: 실제 조직 구성원 정보를 시험 자료에 넣지 않는다.

## 작업 항목 (4)

### 1. 일치 규칙 시험
- ID, 이메일, 이름, 사용자 코드의 exact 우선과 partial 후보를 시험한다.
### 2. 모호성 시험
- 다중 후보, ASCII 숫자 판별, fullwidth 숫자 거부와 후보 제한을 시험한다.
### 3. 페이지·캐시 시험
- 빈 페이지 종료, 전체 수집, 만료, 동시 갱신과 실패 시 기존 스냅숏 보존을 시험한다.
### 4. 완료 마킹
- 도구 스키마와 기본 검증 통과 뒤 `tasks/plan005-project-member-resolver/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/service/resolver/` | 일치·모호성 시험 |
| `src/test/kotlin/com/bifos/dooray/mcp/service/project/` | 페이지·캐시 시험 |
| `tasks/plan005-project-member-resolver/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 접근 가능 여부는 캐시가 아니라 Dooray API 응답이 최종 판정한다.
