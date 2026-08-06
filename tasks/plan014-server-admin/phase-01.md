# Phase 01 — 서버 관리 도구와 리소스 구현

**Execution profile**: standard
**Status**: pending

## 목표

CLI 설정 마법사와 스킬 설치를 복제하지 않고 `dooray_server`와 MCP 리소스로 같은 사용자 결과를 제공한다.

**범위 외**: 비밀 원문 쓰기, 호스트 스킬 파일 교체, `gh` 프로세스 실행은 다루지 않는다.

## 작업 항목 (4)

### 1. 설정·버전·진단
- `version`, `config_status`, `doctor`, `skill_status`를 구조화하고 비밀은 존재 여부와 마스킹 상태만 반환한다.
### 2. 캐시 관리
- plan004의 캐시 상태·비우기·지연 갱신 결과를 관리 도구에 노출한다.
### 3. MCP 리소스
- `dooray://server/version`, `install-guide`, `tool-contract`를 등록하고 버전·프로필·클라이언트 설정을 제공한다.
### 4. 피드백
- `feedback_preview`를 기본으로 구현하고 `feedback_submit`은 관리자 활성화와 외부 토큰이 있을 때 HTTP API로만 허용한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayServerTool.kt` | 관리 도구 |
| `src/main/kotlin/com/bifos/dooray/mcp/resources/ServerResources.kt` | 설치·버전·계약 리소스 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/admin/` | 진단·피드백 서비스 |
| `README.md` | 설치와 프로필 안내 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- MCP 서버가 호출자 호스트의 설정 마법사나 스킬 설치 프로그램이 되지 않게 경계를 유지한다.
