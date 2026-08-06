# Phase 02 — 댓글 필터·참조·삭제 준비 검증

**Execution profile**: fast
**Status**: pending

## 목표

댓글 입력·필터·참조 보존과 삭제 준비의 비변경성을 검증하고 plan007을 완료한다.

**범위 외**: 실제 Dooray 댓글 생성·수정·삭제 API를 호출하지 않는다.

## 작업 항목 (4)

### 1. 조회 시험
- 정렬, latest N, since, author, 빈 결과와 단건 상세를 시험한다.
### 2. 본문 시험
- 멘션·링크 생성과 이미지·일반 파일 참조 보존·손실 허용을 시험한다.
### 3. 삭제 준비 시험
- 대상 없음·불일치와 정상 계획에서 외부 DELETE 호출이 0회인지 시험한다.
### 4. 완료 마킹
- legacy 호환과 compact 스키마 통과 뒤 `tasks/plan007-post-comments/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/service/post/PostCommentServiceTest.kt` | 필터·참조·삭제 준비 |
| `src/test/kotlin/com/bifos/dooray/mcp/McpServerIntegrationTest.kt` | 도구·스키마 호환 |
| `tasks/plan007-post-comments/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 삭제 준비가 실제 삭제 호출을 만들지 않는다는 검증이 plan013의 선행 조건이다.
