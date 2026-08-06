# Phase 01 — 파일 경계와 업무 첨부 구현

**Execution profile**: deep
**Status**: pending

## 목표

`dooray_project_attachment`에 업무와 업무 댓글 첨부의 목록·업로드·다운로드·일괄 다운로드를 구현하고 삭제 영향 조회를 준비한다.

**범위 외**: 실제 삭제 확정은 plan013, 위키 첨부는 plan010이 담당한다.

## 작업 항목 (4)

### 1. 공통 파일 경계
- `FileSource`, `DownloadDestination`, `ResourceStore`를 `docs/data-schema.md`와 맞춘다.
- 리소스를 기본으로 하고 base64 크기 제한과 서버 경로 허용 목록을 강제한다.

### 2. 업무 첨부
- `owner_kind=post`의 `list`, `upload`, `download`, `download_all`, `prepare_delete`를 구현한다.
- 다운로드 결과에 checksum과 리소스 URI 또는 허용된 출력 경로를 구조화한다.

### 3. 댓글 첨부
- `owner_kind=comment`의 목록·업로드·다운로드·삭제 계획을 구현한다.
- 이미지 확장자는 이미지 참조, 그 외와 확장자 없음은 일반 링크로 만들고 두 형식의 제거 계획을 계산한다.

### 4. API 한계와 부분 적용
- Dooray 웹 직접 첨부가 댓글 API에 없으면 추정하지 않고 `coverageWarning`을 반환한다.
- 업로드와 댓글 본문 갱신의 부분 실패를 `PARTIAL_APPLIED`와 `steps[]`로 보존한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/file/` | 파일 입력·출력과 리소스 임대 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/attachment/` | 업무·댓글 첨부 서비스 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayProjectAttachmentTool.kt` | compact 도구 |
| `src/test/kotlin/com/bifos/dooray/mcp/file/` | 경계 시험 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- MCP 호출자와 서버가 같은 파일시스템을 공유한다고 가정하지 않는다.
- plan010은 이 파일 경계를 재사용하고, plan013은 `prepare_delete`에 `confirm_delete`를 연결한다.
