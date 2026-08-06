# Phase 01 — SDK 업그레이드와 워크플로우 갱신

**Execution profile**: deep
**Status**: completed

---

## 목표

MCP SDK 보안 갱신을 반영하고, GitHub Actions가 Node 24 환경에서 경고 없이 지나가도록 워크플로우를 고친다.

**범위 외**: compact 도구 재설계, 메일/메신저/첨부 기능 추가, Docker 릴리스는 다루지 않는다.

---

## 작업 항목 (4)

### 1. 버전 정렬 — 빌드와 런타임 기준 상향

- `gradle/libs.versions.toml`에서 MCP SDK `0.15.0`, Kotlin `2.4.10`, Ktor `3.5.2`, 코루틴 시험 `1.11.0`으로 맞춘다.
- `build.gradle.kts`와 필요한 빌드 플러그인을 새 버전에 맞춘다.
- `settings.gradle.kts`와 Gradle wrapper `8.13`은 현재 기준을 유지한다.

### 2. 서버 전송 — STDIO 계약 갱신

- `src/main/kotlin/com/bifos/dooray/mcp/Main.kt`와 `src/main/kotlin/com/bifos/dooray/mcp/DoorayMcpServer.kt`를 새 SDK 전송 API에 맞춘다.
- 도구 등록이 정적이면 `listChanged=false` 계약을 유지한다.
- `ToolDsl.kt`가 있으면 새 SDK의 도구 래핑 방식과 맞춘다.

### 3. CI 워크플로우 — Java와 Node 호환성 확인

- `.github/workflows/main.yml`과 `.github/workflows/pr.yml`의 `actions/setup-java@v4`를 `@v5`로 옮길지 검토하고 반영한다.
- 워크플로우에서 Node 20 경고가 남는 action 버전은 Node 24 호환 공식 버전으로 바꾼다.
- `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24`가 실제로 필요한지 남기거나 정리한다.

### 4. 테스트 기반 — 계약 회귀 고정

- `src/test/kotlin/com/bifos/dooray/mcp/McpServerIntegrationTest.kt`와 관련 통합 시험을 새 SDK API에 맞춘다.
- 필요하면 `src/test/kotlin/com/bifos/dooray/mcp/client/dooray/` 아래의 공통 테스트 보조를 조금만 조정한다.
- CI 경고가 남는 액션 버전만 교체하고, 파일 수를 불필요하게 늘리지 않는다.

## Critical Files

| 파일 | 변경 |
|---|---|
| `gradle/libs.versions.toml` | SDK와 빌드 버전 정렬 |
| `build.gradle.kts` | 의존성·플러그인 정렬 |
| `src/main/kotlin/com/bifos/dooray/mcp/Main.kt` | 새 STDIO 전송 적응 |
| `src/main/kotlin/com/bifos/dooray/mcp/DoorayMcpServer.kt` | 새 SDK 서버 적응 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/ToolDsl.kt` | 도구 래핑 갱신 |
| `.github/workflows/main.yml` | `setup-java@v5`, Node 24 호환 검토 |
| `.github/workflows/pr.yml` | `setup-java@v5`, Node 24 호환 검토 |
| `src/test/kotlin/com/bifos/dooray/mcp/McpServerIntegrationTest.kt` | 통합 시험 갱신 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
rg -n "setup-java@v4|Node 20|kotlin-sdk:0\\.9\\.0" .github/workflows build.gradle.kts gradle/libs.versions.toml
./gradlew clean build --no-daemon --console=plain
```

## 의도 메모 (왜)

- SDK 갱신은 이후 모든 plan이 기대는 실행 계층을 바꾸므로, 워크플로우와 통합 시험을 함께 잡아야 한다.
- `setup-java@v5`와 Node 24 호환 action은 PR 경고를 줄이는 데 필요한 최소 수정만 남긴다.
- plan003부터는 새 SDK 기준의 서버 계약을 전제로 하므로, 여기서 `Main.kt`와 `DoorayMcpServer.kt`를 먼저 맞춘다.

## Blocked 조건

- `gradle/libs.versions.toml`의 버전 조정 근거가 없으면 `PHASE_BLOCKED: dependency decision pending`을 출력한다.
- 워크플로우가 Node 24 호환 공식 action으로 바뀌지 않으면 `completed`로 마킹하지 않는다.
