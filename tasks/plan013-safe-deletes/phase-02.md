# Phase 02 — 삭제 안전 적대 검증

**Execution profile**: deep
**Status**: pending

## 목표

토큰 위조·만료·재사용·대상 변경을 검증하고 외부 삭제 없이 plan013을 완료한다.

**범위 외**: 실제 Dooray 삭제 API와 운영 자격 증명 제거는 실행하지 않는다.

## 작업 항목 (4)

### 1. 토큰 수명 시험
- 누락, 만료, 재사용, 다른 서버 인스턴스, 다른 실행 주체와 변조를 시험한다.
### 2. 대상 결합 시험
- ID, operation, 입력, 스냅숏이 하나라도 다르면 삭제하지 않는지 시험한다.
### 3. 도메인·legacy 시험
- 적용 대상 전부와 기존 삭제 도구가 가짜 전송에서만 확정 호출을 만드는지 시험한다.
### 4. 완료 마킹
- 삭제 API 호출 횟수가 실패 분기에서 0임을 검증한 뒤 `tasks/plan013-safe-deletes/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/service/delete/` | 적대 토큰 시험 |
| `src/test/kotlin/com/bifos/dooray/mcp/McpServerIntegrationTest.kt` | legacy 이전 계약 |
| `tasks/plan013-safe-deletes/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 실패 분기에서 외부 삭제 전송이 0회라는 사실을 필수 근거로 남긴다.
