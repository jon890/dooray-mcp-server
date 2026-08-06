# Phase 01 — 위키 첨부 생명주기 구현

**Execution profile**: standard
**Status**: pending

## 목표

plan008의 파일 경계를 재사용해 `dooray_wiki_attachment`의 기능을 구현한다.

**범위 외**: 공통 파일 저장소 재설계와 실제 삭제 확정은 다루지 않는다.

## 작업 항목 (4)

### 1. 목록
- 일반 파일과 inline 이미지를 하나의 정규화 목록으로 반환한다.
### 2. 업로드
- `general`과 `inline_image`를 구분하고 multipart field 순서와 307 재요청을 독립 구현한다.
### 3. 다운로드
- 단건·일괄 다운로드를 MCP 리소스 기본값으로 제공하고 부분 실패를 구조화한다.
### 4. 삭제 준비
- `prepare_delete`가 페이지·파일·본문 참조 영향을 조회하고 확인 계획을 반환하게 한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/service/wiki/WikiAttachmentService.kt` | 첨부 서비스 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayWikiAttachmentTool.kt` | compact 도구 |
| `src/test/kotlin/com/bifos/dooray/mcp/service/wiki/` | API 계약 시험 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- 위키 API의 업로드 특이점은 업무 첨부 서비스에 섞지 않는다.
