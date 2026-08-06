# Phase 01 — 설정 로더와 진단·캐시 경계

**Execution profile**: standard
**Status**: pending

---

## 목표

환경 변수, 비밀 파일, 상태 디렉터리, 진단 응답, 캐시 조작을 하나의 시작 계약으로 묶는다.

**범위 외**: 프로젝트 조회, 업무, 위키, 메일, 메신저는 다루지 않는다.

---

## 작업 항목 (4)

### 1. 설정 해석 — 불변 런타임 설정

- `EnvVariableConst.kt`와 `utils/Env.kt`에서 불변 `RuntimeConfig`를 만든다.
- `DOORAY_MCP_STATE_DIR`, `DOORAY_MCP_STATE_MODE`, `DOORAY_MCP_CACHE_PERSIST`를 명시적으로 검증한다.
- `dooray_server` 관리 도구의 활성화 조건을 설정 계약에 넣는다.

### 2. 비밀과 상태 — 경계 분리

- `DOORAY_API_KEY_FILE`, 메일 비밀 파일, 상태 파일의 경계를 분리한다.
- 상태 파일은 `config/non-secret-v1.json`만 다룬다.
- 비밀은 도구 결과와 로그에 남기지 않는다.

### 3. 진단과 캐시 — 읽기와 비우기 분리

- `VersionConst.kt`와 진단 보고를 정리한다.
- 캐시 조회와 캐시 비우기 준비를 `cache_prepare_clear`와 `cache_confirm_clear`로 나눈다.
- `doctor`가 `NOT_CONFIGURED`와 `CAPABILITY_DISABLED`를 구분해 보여주게 한다.

### 4. 상태 저장 — 원자 기록과 잠금

- `ProjectResolver.kt`의 캐시 만료와 상태 저장 규칙을 단순한 JSON 기록으로 맞춘다.
- 상태 디렉터리 잠금, 원자 이동, 권한 확인 규칙을 정리한다.
- 캐시 경로와 설정 파일 경로가 서로 겹치지 않게 막는다.

## Critical Files

| 파일 | 변경 |
|---|---|
| `src/main/kotlin/com/bifos/dooray/mcp/constants/EnvVariableConst.kt` | 설정 상수 정리 |
| `src/main/kotlin/com/bifos/dooray/mcp/utils/Env.kt` | 설정 로더 |
| `src/main/kotlin/com/bifos/dooray/mcp/constants/VersionConst.kt` | 버전 진단 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/ProjectResolver.kt` | 캐시와 잠금 정리 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/DoctorService.kt` | 진단 보고 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/CacheStore.kt` | 캐시 저장 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/DoorayServerTool.kt` | `dooray_server` 관리 진입점 |
| `.env.sample` | 설정 예시 정렬 |
| `src/test/kotlin/com/bifos/dooray/mcp/service/ProjectResolverTest.kt` | 캐시 회귀 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
rg -n "dooray_server|cache_prepare_clear|cache_confirm_clear|NOT_CONFIGURED|CAPABILITY_DISABLED" src/main .env.sample
./gradlew clean build --no-daemon --console=plain
```

## 의도 메모 (왜)

- plan004는 이후 계획이 읽는 런타임 기준선을 정한다.
- `dooray_server` 이름을 여기서 고정해야 plan014와 plan015에서 같은 용어를 다시 쓰지 않는다.
- 캐시 비우기는 준비와 확정으로 나누어 plan013의 파괴적 작업 규칙과 모양을 맞춘다.

## Blocked 조건

- 상태 디렉터리와 비밀 파일 경계가 분리되지 않으면 `PHASE_BLOCKED: config boundary unresolved`를 출력한다.
- `dooray_server` 이름이 남지 않으면 완료하지 않는다.
