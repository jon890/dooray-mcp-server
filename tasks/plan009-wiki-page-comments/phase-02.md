# Phase 02 — 이슈 2 성능과 계약 검증

**Execution profile**: fast
**Status**: pending

## 목표

위키 API 계약과 트리 성능을 결정적 가짜 응답으로 검증하고 plan009를 완료한다.

**범위 외**: 실제 위키 페이지나 댓글을 생성·수정·삭제하지 않는다.

## 작업 항목 (4)

### 1. 트리 시험
- 섞인 parent 목록, 고아 노드, 순환, root/depth 제한, 전체 페이지 처리를 시험한다.
### 2. 성능·캐시 시험
- API 요청 횟수, 캐시 적중·만료, 상한 초과와 시간 제한을 시험한다.
### 3. 페이지·댓글 계약 시험
- URL/ID/코드, latest 빈 결과, 목록 쪽매김, legacy 위임과 입력 스키마를 시험한다.
### 4. 완료 마킹
- 이슈 #2 회귀 시험과 기본 검증 통과 뒤 `tasks/plan009-wiki-page-comments/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/service/wiki/` | 트리·캐시·댓글 시험 |
| `src/test/kotlin/com/bifos/dooray/mcp/McpServerIntegrationTest.kt` | 도구·스키마 스냅숏 |
| `tasks/plan009-wiki-page-comments/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 실제 시간 측정 대신 요청 횟수와 처리 상한을 결정적으로 검증한다.
