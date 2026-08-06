# Dooray MCP 계획 오버레이

공용 `planning` 스킬에 이 저장소의 제약을 추가한다.

## 필수 입력

- Dooray 공식 API 문서
- `dooray-cli` v0.16.0의 전체 도움말, 명령 구현, 테스트, 스킬, ADR, resolver
- 현재 MCP 서버의 도구 목록, 입력 스키마, 테스트, Docker와 CI
- 열린 이슈, PR, 리뷰와 최근 릴리스 상태

## 필수 산출물

- `docs/parity/dooray-cli-v0.16.0.md`
- `docs/prd.md`
- `docs/flow.md`
- `docs/code-architecture.md`
- `docs/data-schema.md`
- 필요한 ADR과 `docs/adr/INDEX.md`
- 계획 순서와 작업 트리 경계를 담은 `docs/implementation-plan.md`
- `tasks/<plan>/index.json`과 단계별 작업 파일

## 설계 제약

- Kotlin과 현재 JVM 기술 스택을 유지한다.
- CLI 코드, 패키지, 실행 파일에 의존하지 않는다.
- 목표 도구 프로필은 `compact` 12개, `legacy` 20개, `all` 32개다.
- CLI 기능을 제외하지 않는다. 도구로 직접 옮기지 않는 기능은 MCP 고유 동등 기능과 검증 근거를 남긴다.
- 삭제는 준비·확정 토큰을 사용한다.
- 인증 정보는 도구 입력이나 결과로 노출하지 않는다.
- 파일 전송은 MCP 리소스를 우선하고, 로컬 경로는 허용 목록 안에서만 사용한다.
- 실제 외부 변경을 일으키는 테스트를 계획하지 않는다.

## 계획 분할

- 계획 하나는 브랜치 하나와 PR 하나에 대응한다.
- 공통 등록 파일과 공유 문서는 선행 계획에서 안정화한다.
- 독립적인 도메인은 파일 소유권이 겹치지 않을 때만 병렬 실행한다.
- 각 계획은 구현, 테스트, 독립 검토, 문서 정합성 검증, CI 순으로 완료한다.
