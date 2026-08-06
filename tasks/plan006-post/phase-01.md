# Phase 01 — 업무 compact 도구와 보존 규칙 구현

**Execution profile**: deep
**Status**: pending

## 목표

`dooray_post`의 조회·검색·생성·수정·완료·흐름·상위 업무 기능을 구현하고 legacy 7개 업무 도구를 같은 서비스에 연결한다.

**범위 외**: 댓글, 첨부와 삭제 확정은 후속 계획이 담당한다.

## 작업 항목 (4)

### 1. 조회와 대상
- `list`, `search`, `get`에 프로젝트 코드+업무 번호, 업무 ID와 URL의 상호배타 `PostTarget`을 적용한다.
### 2. 생성
- 담당자·참조자·그룹·태그·멘션·업무 링크·템플릿·부모·workflow와 `dry_run`, `request_id`를 지원한다.
### 3. 수정과 보존
- `edit`의 to/cc 추가·그룹·clear, 태그 추가·제거·배열 대체를 구분한다.
- 참여자만 수정하면 기존 제목·본문·태그와 첨부 참조를 보존한다.
### 4. 상태와 부모
- `set_done`, `set_workflow`, `set_parent`에 optimistic snapshot과 결과 불명확 처리를 적용한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayPostTool.kt` | 업무 compact 도구 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/post/` | 업무 서비스와 보존 규칙 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/` | legacy 업무 도구 위임 |
| `src/test/kotlin/com/bifos/dooray/mcp/service/post/` | 업무 계약 시험 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- 도구 수를 늘리지 않고 `operation` 판별 스키마로 7개 업무 기능과 세부 옵션을 묶는다.
