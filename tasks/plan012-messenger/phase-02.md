# Phase 02 — 전송 결과와 오류 검증

**Execution profile**: fast
**Status**: pending

## 목표

가짜 Dooray 응답으로 메신저 대상 해석과 전송 오류를 검증하고 plan012를 완료한다.

**범위 외**: 실제 사용자나 채널에 메시지를 보내지 않는다.

## 작업 항목 (4)

### 1. 대상 해석 시험
- ID, 이메일, exact, partial, ambiguous와 후보 제한을 시험한다.
### 2. 전송 시험
- 개인·채널 요청 본문과 구조화된 성공 결과를 시험한다.
### 3. 오류·멱등 시험
- 인증·권한·대상 없음·시간 제한·결과 불명확과 `request_id` 재사용을 시험한다.
### 4. 완료 마킹
- 검증 통과 뒤 `tasks/plan012-messenger/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/service/messenger/` | 해석·전송 시험 |
| `tasks/plan012-messenger/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 외부 전송 없이 요청·응답 계약과 오류 분류만 검증한다.
