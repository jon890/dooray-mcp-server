# Dooray MCP Server 코드 구조

작성 기준일은 2026-08-06이다.

이 문서는 v0.5 계열 고도화를 위한 코드 경계를 정의한다.
현재 Kotlin/JVM 기술 스택을 유지하고, CLI와 코드·실행 환경 의존성을 만들지 않는다.

## 기술 기준

| 항목 | 목표 |
| --- | --- |
| 언어 | Kotlin `2.4.10` |
| 실행 환경 | JDK 21 |
| 빌드 | Gradle `8.13` |
| MCP SDK | `io.modelcontextprotocol:kotlin-sdk:0.15.0` |
| HTTP | Ktor `3.5.2` |
| 코루틴 시험 | `kotlinx-coroutines-test:1.11.0` |
| 메일 | `org.eclipse.angus:angus-mail:2.0.5` |

MCP SDK `0.15.0`은 표준 입력 전송의 메모리 고갈 취약점 영향 범위에서 벗어난 버전이다.
Ktor는 BOM으로 직접·전이 모듈을 한 버전으로 맞춘다.

## 계층

```text
Main
  -> RuntimeConfigLoader
  -> DoorayMcpServer
    -> ToolRegistry
    -> ToolExecutionBoundary
      -> SchemaValidator
      -> ResultMapper
      -> DomainTools
        -> ProjectService
        -> MemberService
        -> PostService
        -> PostCommentService
        -> AttachmentService
        -> WikiService
        -> MailService
        -> MessengerService
        -> LocalAdminService
      -> SharedServices
        -> ResolverService
        -> IdempotencyStore
        -> ConfirmationStore
        -> ResourceStore
        -> CacheStore
    -> DoorayRestClient
    -> MailClient
```

도구 클래스는 입력 파싱, 권한 검사, 결과 매핑을 직접 반복하지 않는다.
공통 경계는 `ToolExecutionBoundary`에서 처리한다.

## 도구 등록

도구는 시작 시 선택한 프로필에 따라 한 번 등록한다.

| 프로필 | 등록 정책 |
| --- | --- |
| `compact` | 12개 도메인 도구를 등록한다. |
| `legacy` | 기존 19개와 PR #28 위키 댓글 조회 1개를 등록한다. |
| `all` | 두 집합을 모두 등록한다. |

compact 도구는 `operation` 판별 합집합을 사용한다.
각 분기는 JSON Schema의 `oneOf`와 `unevaluatedProperties=false`로 검증한다.

표준 MCP annotation은 도구 단위로 보수적으로 선언한다.
operation별 세부 위험도는 `_meta["com.bifos.dooray/operationAnnotations"]`와 서버 계약 리소스로 제공한다.

## 공통 서비스

### `RuntimeConfig`

`RuntimeConfig`는 시작 시 한 번 만들어지는 불변 설정이다.
환경변수와 비밀 없는 상태 파일을 검증해 만든다.
비밀 값은 `SecretProvider`가 지연 읽기하고 설정 객체에 원문으로 저장하지 않는다.

### `ResolverService`

프로젝트, 업무, 멤버, 그룹, 태그, 워크플로, 위키 페이지 대상을 해석한다.
캐시는 성공한 전체 스냅숏으로만 교체한다.
오래된 항목이 권한 판단을 대신하지 않는다.

### `IdempotencyStore`

변경 요청의 `request_id`와 입력 지문을 저장한다.
성공, 부분 성공, 결과 불명확 상태를 구분한다.
이 저장소는 외부 API가 멱등성을 제공하지 않는 작업에서 자동 재시도를 허용하지 않는다.

### `ConfirmationStore`

삭제와 손실 작업의 5분 확인 토큰을 관리한다.
토큰은 대상, 입력 정규형, 대상 스냅숏, 실행 주체 지문에 결합한다.
서버 재시작 뒤 미소비 토큰은 기본 무효다.

### `ResourceStore`

다운로드와 큰 업로드 자료를 MCP 리소스로 관리한다.
자료는 TTL과 전체 저장소 크기 제한을 가진다.
경로는 상태 디렉터리 아래 임대 저장소로 제한한다.

### `CacheStore`

프로젝트, 멤버, 위키 트리, 메타데이터 조회를 도메인별 TTL로 관리한다.
개인정보가 포함될 수 있는 영속 캐시는 명시 설정이 있을 때만 사용한다.

## 외부 클라이언트

### `DoorayRestClient`

Ktor로 Dooray REST API를 직접 호출한다.
HTTP 시간 제한, 안전한 헤더 로그 마스킹, 오류 본문 단일 소비, 오류 코드 분류를 담당한다.

조회 요청만 제한 재시도한다.
쓰기 요청은 응답을 확인하지 못하면 `OUTCOME_UNKNOWN`으로 반환한다.

### `MailClient`

Angus Mail로 IMAPS와 SMTPS를 호출한다.
자격 증명은 도구 인자로 받지 않고 `MailCredentialsProvider`에서만 읽는다.

메일 구현은 다음 원칙을 따른다.

- `INBOX`를 읽기 전용으로 연다.
- 외부 식별자는 UID와 `UIDVALIDITY`를 함께 사용한다.
- `Reply-To`, 없으면 `From`으로 답장 대상을 정한다.
- `References`는 부모 계보를 유지한다.
- SMTP 일부 성공을 전체 성공으로 오해하지 않는다.

## 코드 분할 계획

후속 구현은 중앙 파일을 계속 크게 만들지 않도록 도메인별 파일 소유권을 나눈다.

| 영역 | 주 소유 |
| --- | --- |
| 도구 등록과 프로필 | `DoorayMcpServer`, `ToolRegistry` |
| 공통 실행 계약 | `tooling` 또는 `tools/common` |
| Dooray REST | `client/dooray` |
| 설정과 상태 | `config`, `state` |
| 리졸버 | `service/resolver` |
| 도메인 기능 | `service/project`, `service/post`, `service/wiki`, `service/mail`, `service/messenger` |
| 시험 고정물 | `src/test/kotlin/.../fixtures` |

`DoorayMcpServer.kt`는 도구 목록 조립만 담당하게 줄인다.
도메인별 PR이 같은 중앙 파일을 계속 충돌시키지 않도록 `ToolModule` 확장점을 먼저 만든다.
