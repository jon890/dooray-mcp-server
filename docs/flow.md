# Dooray MCP Server 호출 흐름

작성 기준일은 2026-08-06이다.

이 문서는 MCP 호출이 서버 내부에서 어떻게 검증, 실행, 응답으로 변환되는지 정의한다.

## 전체 흐름

```text
MCP STDIO
  -> ToolRegistry
  -> ToolExecutionBoundary
  -> 입력 스키마 검증
  -> RequestContext
  -> 도구 프로필과 기능 플래그 검사
  -> IdempotencyStore 또는 ConfirmationStore
  -> 도메인 서비스
  -> DoorayRestClient, MailClient, MessengerService, FileService
  -> ResultMapper
  -> structuredContent, content, resource link
```

서버는 표준 입출력으로 MCP 요청을 받는다.
도구 처리기는 Dooray REST, IMAPS, SMTPS를 직접 호출한다.
CLI 하위 프로세스나 Node.js 런타임은 사용하지 않는다.

## 공통 요청 흐름

1. `ToolRegistry`가 시작 시 선택한 프로필의 도구만 등록한다.
2. `ToolExecutionBoundary`가 입력 스키마를 검증한다.
3. `RequestContext`가 `request_id`, 상관관계 ID, 시간 제한, 실행 주체 지문을 만든다.
4. 도구 프로필, 기능 플래그, 관리자 권한을 확인한다.
5. 대상 해석이 필요하면 `ResolverService`를 호출한다.
6. 변경 요청이면 중복 요청 기록을 확인한다.
7. 삭제 또는 손실 작업이면 확인 토큰을 검증한다.
8. 도메인 서비스가 외부 API를 호출한다.
9. 결과를 `structuredContent`와 호환용 `content`로 변환한다.
10. 실패하면 `isError=true`와 구조화 오류를 반환한다.

## 대상 해석

대상 해석은 추측하지 않는다.

우선순위는 다음과 같다.

1. 명시적 URL
2. 명시적 ID
3. 프로젝트 코드와 업무 번호
4. 이름 또는 이메일의 정확 일치
5. 부분 일치 후보

후보가 둘 이상이면 임의로 고르지 않는다.
`AMBIGUOUS_TARGET` 또는 `AMBIGUOUS_REFERENCE`를 반환하고 최대 10개 후보를 함께 제공한다.

프로젝트 ID가 15자리 이상 ASCII 숫자이면 프로젝트 목록 소속 여부를 확인하지 않고 Dooray API 대상으로 통과시킨다.
최종 접근 가능 여부는 Dooray API 응답으로 판정한다.

## 변경 요청과 중복 방지

생성, 수정, 업로드, 발송처럼 외부 상태를 바꾸는 도구는 `request_id`를 받는다.

compact 변경 도구는 호출자가 만든 UUID 형식의 `request_id`를 필수로 검증한다.
조회와 legacy 호환 호출에 값이 없으면 서버가 결과 추적용 요청 ID를 생성한다.

- 같은 `request_id`와 같은 정규화 입력은 진행 중 요청에 합류하거나 저장된 결과를 재사용한다.
- 같은 `request_id`와 다른 입력은 `IDEMPOTENCY_KEY_REUSED`로 거부한다.
- 응답 유실로 외부 효과가 불명확하면 `OUTCOME_UNKNOWN`을 반환한다.
- 불명확한 변경은 자동 재시도하지 않고 조회로 상태를 대조하게 한다.

조회 요청은 제한된 횟수로만 자동 재시도한다.
발송, 생성, 수정, 삭제, 업로드는 자동 재시도하지 않는다.

## 삭제 준비와 확정

MCP 서버는 TTY 확인을 사용할 수 없다.
삭제와 되돌릴 수 없는 작업은 다음 흐름만 허용한다.

1. 준비 요청이 대상과 영향을 조회한다.
2. 서버가 삭제 계획과 `confirmation_token`을 반환한다.
3. 토큰은 대상, 입력 정규형, 실행 주체 지문, 대상 스냅숏, 만료 시각에 묶인다.
4. 확정 요청이 같은 입력과 토큰을 보낸다.
5. 토큰이 없거나 만료되었거나 대상이 바뀌면 실행하지 않는다.
6. API가 조건부 삭제를 지원하지 않는 대상은 남는 경쟁 조건을 결과와 문서에 명시한다.

기본 토큰 유효 기간은 5분이다.
시험은 삭제 API를 실제 호출하지 않는다.

## 파일 전송

파일 입력은 다음 중 하나만 허용한다.

- MCP 리소스 URI
- 작은 파일의 `content_base64`
- 허용된 서버 로컬 경로

다운로드 기본값은 MCP 리소스 반환이다.
서버 경로 저장은 `DOORAY_MCP_FILESYSTEM_ROOTS`에 쓰기 루트가 있을 때만 허용한다.

Docker 실행 시 경계는 다음과 같다.

| 마운트 | 권한 | 용도 |
| --- | --- | --- |
| `/data/in` | 읽기 전용 | 서버 경로 업로드 입력 |
| `/data/out` | 쓰기 가능 | 서버 경로 다운로드 출력 |
| `/data/state` | 쓰기 가능 | 상태, 임시 리소스, 중복 기록 |
| `/run/secrets` | 읽기 전용 | API 키와 메일 자격 증명 파일 |

이미지 확장자는 댓글과 페이지 본문에서 이미지 참조로 처리한다.
그 외 파일과 확장자가 없는 파일은 일반 링크로 처리한다.
삭제 로직은 이미지 참조와 일반 링크 형식을 모두 안전하게 제거해야 한다.

## 결과 계약

성공 결과는 `structuredContent`를 기본으로 제공한다.
기존 호스트를 위해 짧은 한국어 텍스트도 `content`에 넣는다.
JSON 문자열을 `content` 안에 이중 인코딩하지 않는다.

```json
{
  "ok": true,
  "operation": "get",
  "ids": [
    { "kind": "post", "id": "1234567890123456789" }
  ],
  "data": {},
  "meta": {
    "resultMode": "compact",
    "dryRun": false,
    "requestId": "caller-generated-uuid",
    "warnings": []
  }
}
```

오류 결과는 `isError=true`를 반드시 사용한다.

```json
{
  "ok": false,
  "operation": "delete",
  "error": {
    "code": "CONFIRMATION_REQUIRED",
    "message": "건조 실행에서 받은 확인 토큰이 필요합니다.",
    "retryable": false,
    "details": {}
  },
  "meta": {
    "requestId": "caller-generated-uuid"
  }
}
```

## MCP 방식으로 재설계한 CLI 기능

| CLI 기능 | MCP 흐름 |
| --- | --- |
| `setup` | `dooray_server.doctor`, `dooray_server.config_status`, `resources/install-guide`로 대체한다. |
| `config set` | 비밀 없는 설정만 관리자 기능으로 다룬다. |
| `config get` | 비밀 원문을 반환하지 않고 마스킹 상태를 반환한다. |
| `skill install` | 도구가 로컬 스킬을 설치하지 않고 `resources/install-guide`로 안내한다. |
| `feedback` | 기본은 게시 미리보기이며 외부 게시에는 별도 관리자 설정이 필요하다. |
| `$EDITOR` | 본문 입력 필드나 본문 리소스로 대체한다. |
| `--yes` | 준비·확정 토큰으로 대체한다. |
