# Phase 02 — 동등 결과 검증과 완료

**Execution profile**: fast
**Status**: pending

## 목표

CLI 전용 관리 기능이 MCP에서 같은 사용자 결과를 내는지 검증하고 plan014를 완료한다.

**범위 외**: 실제 GitHub 이슈 게시와 비밀 값 변경은 실행하지 않는다.

## 작업 항목 (4)

### 1. 설정·진단 시험
- 누락·마스킹·기능 비활성·버전·프로필과 비밀 부재를 시험한다.
### 2. 리소스 시험
- URI 목록, 내용 유형, 버전, 도구 계약과 설치 예시를 스냅숏으로 고정한다.
### 3. 피드백 시험
- 미리보기의 인자·비밀 정리와 제출 비활성·권한 오류를 가짜 HTTP로 시험한다.
### 4. 완료 마킹
- 기능 대응표 관리 행을 갱신하고 `tasks/plan014-server-admin/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/service/admin/` | 관리 기능 시험 |
| `src/test/kotlin/com/bifos/dooray/mcp/McpServerIntegrationTest.kt` | 리소스·도구 계약 |
| `docs/parity/dooray-cli-v0.16.0.md` | 상태 갱신 |
| `tasks/plan014-server-admin/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 외부 게시 본문은 실제 등록 전에 `content-preview` 절차를 거친다.
