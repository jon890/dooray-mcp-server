# Phase 02 — 관리 도구 계약과 검증 마감

**Execution profile**: fast
**Status**: pending

---

## 목표

설정, 진단, 캐시 관련 관리 도구의 상태 표기와 보존 규칙을 검증하고 plan004를 닫는다.

**범위 외**: 프로젝트 해석기 외의 도메인 기능은 다루지 않는다.

---

## 작업 항목 (4)

### 1. 설정 검증 — 시작 실패와 기본값

- 잘못된 환경 변수가 조용히 기본값으로 떨어지지 않는지 확인한다.
- `config/non-secret-v1.json`의 허용 필드만 읽는지 확인한다.
- `DOORAY_MCP_STATE_MODE`가 `required|optional|memory`만 받는지 확인한다.

### 2. 진단 검증 — 도구 표기

- `dooray_server` 관리 도구가 `doctor`와 캐시 조작을 드러내는지 확인한다.
- `NOT_CONFIGURED`와 `CAPABILITY_DISABLED`가 구분되는지 확인한다.
- 민감 정보가 응답 본문에 남지 않는지 확인한다.

### 3. 캐시 검증 — 준비와 확정

- `cache_prepare_clear`와 `cache_confirm_clear`가 분리되어 있는지 확인한다.
- 캐시 비우기 전후의 항목 수와 바이트 수가 일치하는지 확인한다.
- 동일 경로를 다른 계획이 덮어쓰지 않게 한다.

### 4. 완료 마킹 — index.json 상태 정리

- `tasks/plan004-config-diagnostics-cache/index.json`의 `status`를 `completed`로 바꾼다.
- phase 상태도 `completed`로 정리한다.
- plan005는 이 설정 계약 위에서 프로젝트·멤버 해석을 시작한다.

## Critical Files

| 파일 | 변경 |
|---|---|
| `tasks/plan004-config-diagnostics-cache/index.json` | 완료 상태 마킹 |
| `tasks/plan004-config-diagnostics-cache/phase-01.md` | 검증 전제 참조 |
| `tasks/plan004-config-diagnostics-cache/phase-02.md` | 완료 절차 |
| `src/main/kotlin/com/bifos/dooray/mcp/constants/EnvVariableConst.kt` | 설정 확인 |
| `src/main/kotlin/com/bifos/dooray/mcp/utils/Env.kt` | 설정 확인 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/DoctorService.kt` | 진단 확인 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/CacheStore.kt` | 캐시 확인 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
rg -n "dooray_server|cache_prepare_clear|cache_confirm_clear|NOT_CONFIGURED|CAPABILITY_DISABLED" src/main .env.sample
./gradlew clean build --no-daemon --console=plain
```

## 의도 메모 (왜)

- plan004는 나중 계획에서 반복되는 설정 버그를 먼저 차단한다.
- 관리 도구 이름을 `dooray_server`로 고정하면 plan014가 독립적으로 확장할 수 있다.
- 캐시를 준비/확정으로 나눈 흔적이 남아야 plan013의 안전 삭제와도 구분된다.

## Blocked 조건

- `dooray_server` 표기가 없으면 완료하지 않는다.
- 캐시 비우기 준비/확정 분리가 시험에서 보이지 않으면 `completed`로 마킹하지 않는다.
