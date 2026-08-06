# Phase 01 — 프로젝트·구성원·대상 해석 구현

**Execution profile**: deep
**Status**: pending

## 목표

`dooray_project`, `dooray_member`와 공통 resolver를 구현해 후속 도메인의 대상 해석 기준을 고정한다.

**범위 외**: 업무·위키 본문·메일·메신저 전송은 다루지 않는다.

## 작업 항목 (4)

### 1. 프로젝트 메타
- 프로젝트 `list`, `members`, `workflows`, `groups`, `tags`, `templates` operation과 쪽매김을 구현한다.
### 2. 구성원
- 구성원 `get`, `list`, `search`와 이름·이메일·사용자 코드 조건을 구현한다.
### 3. resolver
- 프로젝트·구성원·그룹·태그·workflow의 ID, exact, partial 순서를 적용하고 모호 후보를 최대 10개 반환한다.
- 15자리 이상 ASCII 숫자 프로젝트 ID는 목록 조회 없이 통과시킨다.
### 4. 캐시
- 전체 페이지를 수집한 성공 스냅숏만 교체하고 실패·부분 결과가 기존 캐시를 오염시키지 않게 한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayProjectTool.kt` | 프로젝트 compact 도구 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayMemberTool.kt` | 구성원 compact 도구 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/resolver/` | 공통 대상 해석 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/project/` | 프로젝트 메타 서비스 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- 후속 업무·위키·메신저는 이 resolver를 재사용하고 자체 추측 로직을 만들지 않는다.
