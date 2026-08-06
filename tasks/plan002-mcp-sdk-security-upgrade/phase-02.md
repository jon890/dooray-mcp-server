# Phase 02 — 빌드·CI 검증과 완료 마킹

**Execution profile**: fast
**Status**: pending

---

## 목표

SDK 업그레이드 뒤의 빌드, 테스트, GitHub Actions 경고를 확인하고 plan002를 닫는다.

**범위 외**: 새 도구 추가, compact 프로필 전환, 릴리스 배포는 다루지 않는다.

---

## 작업 항목 (4)

### 1. 빌드 확인 — 컴파일과 테스트

- `./gradlew clean build`가 새 SDK 기준으로 통과하는지 확인한다.
- 통합 시험이 새 SDK 전송과 충돌하지 않는지 확인한다.
- 실패 시 plan002 범위 밖 파일은 건드리지 않는다.

### 2. 워크플로우 확인 — 경고 제거

- `.github/workflows/main.yml`과 `.github/workflows/pr.yml`에서 `actions/setup-java@v4`가 남아 있지 않은지 확인한다.
- Node 20 경고를 남기는 action 버전이 남아 있지 않은지 확인한다.
- `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24`가 계속 필요하면 이유를 문장으로 남긴다.

### 3. SDK 계약 확인 — 실행 표면 정합성

- `Main.kt`, `DoorayMcpServer.kt`, `ToolDsl.kt`가 새 SDK API와 맞는지 확인한다.
- `listChanged=false`와 도구 정적 등록 계약을 유지한다.
- 경고만 지우고 동작을 바꾸지 않는지 확인한다.

### 4. 완료 마킹 — index.json 상태 정리

- `tasks/plan002-mcp-sdk-security-upgrade/index.json`의 `status`를 `completed`로 바꾼다.
- phase 상태도 `completed`로 정리한다.
- plan003은 이 plan의 결과를 기준으로 execution contract를 시작한다.

## Critical Files

| 파일 | 변경 |
|---|---|
| `tasks/plan002-mcp-sdk-security-upgrade/index.json` | 완료 상태 마킹 |
| `tasks/plan002-mcp-sdk-security-upgrade/phase-01.md` | 검증 전제 참조 |
| `tasks/plan002-mcp-sdk-security-upgrade/phase-02.md` | 완료 절차 |
| `.github/workflows/main.yml` | 경고 제거 확인 |
| `.github/workflows/pr.yml` | 경고 제거 확인 |
| `src/main/kotlin/com/bifos/dooray/mcp/Main.kt` | SDK 정합성 확인 |
| `src/main/kotlin/com/bifos/dooray/mcp/DoorayMcpServer.kt` | SDK 정합성 확인 |
| `src/test/kotlin/com/bifos/dooray/mcp/McpServerIntegrationTest.kt` | 회귀 확인 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
rg -n "setup-java@v4|Node 20|kotlin-sdk:0\\.9\\.0" .github/workflows build.gradle.kts gradle/libs.versions.toml
./gradlew clean build --no-daemon --console=plain
```

## 의도 메모 (왜)

- plan002는 실행 표면의 기준 버전을 바꾸는 작업이므로, 빌드와 CI 경고가 같이 사라지는지 확인해야 한다.
- 계획 파일은 경고 제거 여부를 숫자로 표현하지 말고, 남은 참조 문자열로 확인한다.
- 이 단계가 끝나야 plan003이 공통 실행 계약을 새 SDK 위에서 정의할 수 있다.

## Blocked 조건

- `setup-java@v4` 또는 Node 20 경고가 남아 있으면 완료 마킹을 하지 않는다.
- SDK 업그레이드로 테스트가 깨지면 원인만 기록하고 plan003로 넘기지 않는다.
