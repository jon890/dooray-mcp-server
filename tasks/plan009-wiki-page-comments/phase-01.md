# Phase 01 — 위키 트리와 페이지·댓글 구현

**Execution profile**: deep
**Status**: pending

## 목표

`dooray_wiki`, `dooray_wiki_page`, `dooray_wiki_comment`로 위키 탐색과 페이지·댓글 기능을 완성한다.

**범위 외**: 위키 첨부는 plan010, 삭제 확정은 plan013이 담당한다.

## 작업 항목 (4)

### 1. 위키 탐색
- `dooray_wiki`의 `list`, `pages`, `tree`를 구현한다.
- `tree`는 전체 페이지를 순회해 parent 관계로 구성하고 `root_page_id`, `max_depth`, 상한과 시간 제한을 적용한다.

### 2. 페이지 생명주기
- URL·ID·프로젝트 코드 대상 해석과 `get`, `create`, `edit`, `prepare_delete`를 구현한다.
- 삭제 계획에는 하위 페이지와 관측 가능한 재부착 정책을 포함한다.

### 3. 위키 댓글
- `list`, `latest`, `get`, `add`, `edit`, `prepare_delete`를 구현한다.
- PR #28의 legacy 댓글 목록 도구는 같은 서비스에 위임해 호환성을 유지한다.

### 4. 캐시와 전체 페이지 처리
- 완전한 페이지 스냅숏만 캐시에 넣고 만료·새로고침·잘림 정보를 결과에 담는다.
- 여러 호출의 DFS 대신 한 호출로 같은 트리를 반환한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/service/wiki/` | 위키·트리·페이지·댓글 서비스 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayWikiTool.kt` | 위키 탐색 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayWikiPageTool.kt` | 페이지 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayWikiCommentTool.kt` | 댓글 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- `dooray_wiki(operation=tree)`는 이슈 #2의 다중 호출 병목을 해결하는 종료 조건이다.
