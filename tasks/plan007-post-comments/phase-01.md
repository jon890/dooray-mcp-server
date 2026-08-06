# Phase 01 — 업무 댓글 compact 도구 구현

**Execution profile**: standard
**Status**: pending

## 목표

`dooray_post_comment`의 목록·최신·상세·추가·수정과 삭제 준비를 구현하고 legacy 댓글 도구를 같은 서비스에 연결한다.

**범위 외**: 댓글 첨부는 plan008, 실제 삭제 확정은 plan013이 담당한다.

## 작업 항목 (4)

### 1. 조회
- `list`, `latest`, `get`에 page/size/sort/reverse/latest/since/from_author의 상호배타 규칙을 적용한다.
### 2. 추가·수정
- `add`, `edit`에 BodyInput, 멘션, 그룹 멘션, 업무 링크, dry_run과 request_id를 적용한다.
### 3. 참조 손실
- 수정 전 본문을 조회해 이미지·일반 링크 참조 손실을 계산하고 명시 허용 없이는 변경하지 않는다.
### 4. 삭제 준비
- `prepare_delete`가 대상과 영향만 조회하고 외부 삭제 API는 호출하지 않게 한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayPostCommentTool.kt` | 댓글 compact 도구 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/post/PostCommentService.kt` | 댓글 서비스 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/` | legacy 댓글 도구 위임 |
| `src/test/kotlin/com/bifos/dooray/mcp/service/post/` | 댓글 계약 시험 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- 댓글 삭제는 이 plan에서 절대 실행하지 않고 plan013의 공통 확인 저장소를 기다린다.
