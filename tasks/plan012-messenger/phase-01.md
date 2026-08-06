# Phase 01 — 메신저 대상 해석과 전송 구현

**Execution profile**: standard
**Status**: pending

## 목표

`dooray_messenger`에 사용자·채널 전송과 대상 해석을 독립 구현한다.

**범위 외**: 대화방 관리, 메시지 조회, CLI 프로세스 호출은 다루지 않는다.

## 작업 항목 (4)

### 1. 사용자 대상
- member ID와 이메일을 정확 일치 우선으로 해석하고 모호 후보를 반환한다.
### 2. 채널 대상
- 15자리 이상 ASCII 숫자 ID는 통과시키고 이름은 exact·partial·ambiguous 순서로 해석한다.
### 3. 전송
- `send_user`, `send_channel`에 `BodyInput`, `dry_run`, `request_id`를 적용하고 변경 자동 재시도를 금지한다.
### 4. 결과와 오류
- 대상, 생성된 메시지 ID, 수락 상태와 인증·권한·대상·시간 제한 오류를 구조화한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/service/messenger/` | 대상 해석과 전송 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayMessengerTool.kt` | compact 도구 |
| `src/test/kotlin/com/bifos/dooray/mcp/service/messenger/` | 계약 시험 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- 발송 결과가 불명확하면 `OUTCOME_UNKNOWN`을 반환하고 자동 재시도하지 않는다.
