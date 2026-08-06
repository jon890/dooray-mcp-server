# Phase 01 — 공통 실행 계약과 등록 확장점

**Execution profile**: deep
**Status**: pending

## 목표

구조화 결과·오류·멱등·확인 계약과 도메인별 도구 등록 확장점을 고정한다.

**범위 외**: 개별 프로젝트·업무·위키·메일·메신저 기능은 구현하지 않는다.

## 작업 항목 (4)

### 1. 도구 프로필과 모듈
- `ToolProfile`, `ToolRegistry`, `ToolModule`을 추가하고 `legacy` 20개 등록 순서와 스키마를 스냅숏으로 고정한다.
- `compact=12`, `legacy=20`, `all=32`를 지원하되 아직 없는 compact module은 후속 plan이 독립 추가하게 한다.

### 2. 실행 경계
- 입력 검증, 시간 제한, 상관관계 ID, 구조화 성공·오류와 호환 텍스트를 `ToolExecutionBoundary`로 모은다.
- 실패는 바깥 `CallToolResult.isError=true`로 표시하고 stack trace와 비밀을 제거한다.

### 3. 변경 안전 인터페이스
- compact 쓰기에 필요한 `request_id`, 입력 fingerprint, `IdempotencyStore`, `ConfirmationStore` 인터페이스와 파일·메모리 구현을 정의한다.
- 확인 저장소는 주입 가능한 시계, 5분 TTL, 해시 토큰 기록과 단일 소비의 기반을 제공한다.
- legacy 입력은 깨지지 않게 유지하고 쓰기 자동 재시도를 금지한다.

### 4. 공통 입력·결과
- `result_mode`, `dry_run`, `BodyInput`, 안정 오류 코드와 `retryable` 계약을 JSON Schema와 Kotlin 타입으로 고정한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/tooling/` | 실행 경계와 등록 확장점 |
| `src/main/kotlin/com/bifos/dooray/mcp/types/` | 공통 입력·결과 |
| `src/main/kotlin/com/bifos/dooray/mcp/exception/` | 안정 오류 계약 |
| `src/main/kotlin/com/bifos/dooray/mcp/DoorayMcpServer.kt` | 모듈 조립만 유지 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- 중앙 파일을 먼저 분리해야 후속 도메인 PR의 파일 소유권이 겹치지 않는다.
