# Phase 02 — 파일 계약 검증과 완료

**Execution profile**: fast
**Status**: pending

## 목표

파일 전송과 댓글 참조 계약을 가짜 전송으로 검증하고 plan008을 완료한다.

**범위 외**: 실제 Dooray 삭제 API와 허용 목록 밖의 로컬 파일은 사용하지 않는다.

## 작업 항목 (4)

### 1. 입력 경계 시험
- base64 상한, 리소스 만료, 경로 이탈, symlink, 출력 덮어쓰기를 시험한다.
### 2. API 계약 시험
- multipart field 순서, 파일 메타데이터, 다운로드 스트림, 일괄 부분 실패를 시험한다.
### 3. 본문 참조 시험
- 이미지와 일반 링크의 추가·제거 계획, 확장자 없는 파일, API 누락 경고를 시험한다.
### 4. 완료 마킹
- 모든 시험 통과 뒤 `tasks/plan008-file-resources-post-attachments/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/file/` | 경계와 리소스 시험 |
| `src/test/kotlin/com/bifos/dooray/mcp/service/attachment/` | 첨부·참조 시험 |
| `tasks/plan008-file-resources-post-attachments/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 삭제는 영향 계산까지만 검증하고 외부 삭제 호출은 plan013의 가짜 서버 시험으로 남긴다.
