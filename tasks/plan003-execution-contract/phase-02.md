# Phase 02 — 실행 계약 적대 검증과 완료

**Execution profile**: fast
**Status**: completed

## 목표

도구 목록·스키마·결과·오류·STDIO 오염 방지 계약을 검증하고 plan003을 완료한다.

**범위 외**: 도메인 API 정상 동작과 실제 외부 변경은 시험하지 않는다.

## 작업 항목 (4)

### 1. 등록·스키마 시험
- legacy 20개 정확 목록과 모든 입력 스키마, 중복 등록 거부를 시험한다.
### 2. 결과·오류 시험
- structuredContent, 호환 text, `isError`, 오류 코드, `retryable`, ANSI 부재를 시험한다.
### 3. 안전 경계 시험
- JSON 문자열 이중 인코딩, stack trace·비밀 노출, stdout 로그·진행 표시와 변경 자동 재시도를 차단한다.
### 4. 완료 마킹
- 검증 통과 뒤 `tasks/plan003-execution-contract/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/McpServerIntegrationTest.kt` | 목록·스키마·STDIO 시험 |
| `src/test/kotlin/com/bifos/dooray/mcp/tooling/` | 실행 경계 시험 |
| `tasks/plan003-execution-contract/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 이 plan 이후 compact 도메인 모듈은 공통 실행 계약을 재정의하지 않는다.
