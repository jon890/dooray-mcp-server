# Phase 02 — 첨부 계약 검증과 완료

**Execution profile**: fast
**Status**: pending

## 목표

위키 첨부 요청·응답과 파일 경계를 검증하고 plan010을 완료한다.

**범위 외**: 실제 Dooray 파일 삭제 API를 호출하지 않는다.

## 작업 항목 (4)

### 1. 목록·업로드 시험
- files/images 합침, 일반·inline 구분, field 순서와 307를 시험한다.
### 2. 다운로드 시험
- 리소스, 허용 경로, 일괄 부분 실패와 checksum을 시험한다.
### 3. 삭제 계획 시험
- 대상 불일치와 본문 참조 영향을 외부 삭제 없이 시험한다.
### 4. 완료 마킹
- 검증 통과 뒤 `tasks/plan010-wiki-attachments/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/service/wiki/WikiAttachmentServiceTest.kt` | 첨부 계약 |
| `tasks/plan010-wiki-attachments/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 삭제 실행은 plan013이 공통 확인 저장소와 함께 검증한다.
