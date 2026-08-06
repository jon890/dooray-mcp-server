# Phase 02 — 업무 보존·부분 적용 검증

**Execution profile**: deep
**Status**: pending

## 목표

업무 입력 스키마, 필드 보존, 부분 적용과 호환성을 검증하고 plan006을 완료한다.

**범위 외**: 실제 Dooray 업무를 생성·수정·완료하지 않는다.

## 작업 항목 (4)

### 1. 입력 스키마 시험
- URL/ID/번호 혼합, clear와 목록 동시 입력, group-only 생성, 템플릿 기본값을 시험한다.
### 2. 필드 보존 시험
- to/cc만 수정할 때 제목·본문·태그 보존과 이미지·일반 파일 참조 손실 방지를 시험한다.
### 3. 변경 결과 시험
- 생성 후 workflow 실패의 `PARTIAL_APPLIED`, `request_id` 재사용과 `OUTCOME_UNKNOWN`을 시험한다.
### 4. 완료 마킹
- legacy 호환과 compact 스키마 통과 뒤 `tasks/plan006-post/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/service/post/` | 보존·부분 적용 시험 |
| `src/test/kotlin/com/bifos/dooray/mcp/McpServerIntegrationTest.kt` | 스키마·호환 시험 |
| `tasks/plan006-post/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 가장 위험한 회귀는 참여자 단독 수정에서 기존 필드가 사라지는 경우다.
