# Dooray MCP Server 데이터 스키마

작성 기준일은 2026-08-06이다.

이 문서는 설정, 상태 파일, 공통 입력, 공통 결과의 논리 스키마를 정의한다.
공통 실행 계약의 실제 JSON Schema는
`src/main/resources/schema/execution-contract.schema.json`에 있다.
Kotlin 타입과 이 스키마의 결과 모드, 본문 입력, 성공·오류 필드는 계약 시험으로 함께 고정한다.

## 설정

환경변수는 시작 시 한 번 읽어 `RuntimeConfig`로 변환한다.
잘못된 값은 조용히 기본값으로 바꾸지 않고 서버 시작을 실패시킨다.

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `DOORAY_BASE_URL` | 없음 | Dooray API 기본 URL이다. 운영에서는 `https`만 허용한다. |
| `DOORAY_API_KEY` | 없음 | Dooray API 키다. `_FILE`과 함께 쓰면 실패한다. |
| `DOORAY_API_KEY_FILE` | 없음 | API 키 파일 경로다. |
| `DOORAY_MCP_TOOL_PROFILE` | 전환 중 `legacy`, 최종 `compact` | `compact`, `legacy`, `all` 중 하나다. |
| `DOORAY_MCP_STATE_DIR` | 사용자 상태 디렉터리 | 멱등 기록, 확인 토큰, 임시 리소스를 저장한다. |
| `DOORAY_MCP_STATE_MODE` | `required` | `required`, `optional`, `memory` 중 하나다. |
| `DOORAY_MCP_ADMIN_ENABLED` | `false` | 로컬 관리자 쓰기 기능을 켠다. |
| `DOORAY_MCP_FILESYSTEM_ROOTS` | 미설정 | 서버 로컬 경로 입출력 허용 루트다. |
| `DOORAY_MCP_CONFIRMATION_TTL_SECONDS` | `300` | 삭제 확인 토큰 유효 기간이다. |
| `DOORAY_MCP_IDEMPOTENCY_TTL_HOURS` | `72` | 변경 요청 기록 보존 기간이다. |
| `DOORAY_MAIL_USERNAME_FILE` | 없음 | 메일 사용자 이름 파일이다. |
| `DOORAY_MAIL_PASSWORD_FILE` | 없음 | 메일 앱 비밀번호 파일이다. |

비밀은 `VALUE`와 `VALUE_FILE` 가운데 정확히 하나만 허용한다.
비밀 파일은 일반 파일이어야 하며 64 KiB를 넘지 않는다.

## 상태 디렉터리

```text
<stateDir>/
  .server.lock
  installation-salt.bin
  config/
    non-secret-v1.json
  idempotency/v1/<scopeHash>/<keyHash>.json
  confirmations/v1/<tokenHash>.json
  leases/v1/records/<leaseId>.json
  leases/v1/blobs/<leaseId>.blob
  cache/v1/<namespace>/<keyHash>.json
  locks/<kind>/<stripe>.lck
  quarantine/
```

상태 파일은 `0600`, 상태 디렉터리는 가능한 경우 `0700`으로 만든다.
JSON 기록은 임시 파일에 쓴 뒤 같은 파일시스템에서 원자적으로 교체한다.
손상된 기록은 삭제하지 않고 `quarantine/`으로 옮긴다.
잠금 파일은 요청마다 만들지 않고 종류별 최대 4,096개 스트라이프를 재사용한다.

## 비밀 없는 설정 파일

`${stateDir}/config/non-secret-v1.json`은 비밀과 권한 경계를 넓히는 값을 담지 않는다.

```json
{
  "schemaVersion": 1,
  "toolProfile": "compact",
  "baseUrl": "https://api.dooray.com",
  "timeouts": {
    "httpConnectMs": 5000,
    "httpRequestMs": 30000,
    "httpSocketMs": 30000,
    "defaultToolMs": 60000
  },
  "cache": {
    "projectTtlMinutes": 5,
    "persist": false
  }
}
```

파일 루트, 상태 경로, 비밀 참조, 관리자 활성화 값은 이 파일에서 설정하지 않는다.

## 공통 입력 타입

### `PostTarget`

정확히 하나의 형태만 허용한다.

```json
{ "project_ref": "PROJECT", "post_number": 123 }
```

```json
{ "post_id": "1234567890123456789" }
```

```json
{ "url": "https://..." }
```

### `WikiPageTarget`

정확히 하나의 형태만 허용한다.

```json
{ "wiki_ref": "1234567890123456789", "page_id": "1234567890123456789" }
```

```json
{ "project_ref": "PROJECT", "page_id": "1234567890123456789" }
```

```json
{ "url": "https://..." }
```

### `BodyInput`

본문 입력은 최대 하나다.
본문이 필수인 작업에서는 정확히 하나가 필요하다.

```json
{ "body": "본문" }
```

```json
{ "body_resource_uri": "dooray://resource/..." }
```

```json
{ "body_local_path": "/data/in/body.md" }
```

### `FileSource`

파일 입력은 정확히 하나다.

```json
{ "content_base64": "AAE=", "file_name": "sample.bin" }
```

```json
{ "resource_uri": "dooray://resource/...", "file_name": "sample.bin" }
```

```json
{ "local_path": "/data/in/sample.bin" }
```

### `DownloadDestination`

```json
{ "delivery": "resource" }
```

```json
{
  "delivery": "server_path",
  "output_dir": "/data/out",
  "overwrite": false
}
```

기본은 `delivery=resource`다.
서버 경로 저장은 허용된 쓰기 루트 안에서만 가능하다.

## 공통 성공 결과

```json
{
  "ok": true,
  "operation": "create",
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

`result_mode=ids`이면 `data`를 생략할 수 있다.
목록 결과는 `meta.page`에 `page`, `size`, `totalCount`, `nextPage`를 넣는다.
건조 실행은 `meta.dryRun=true`, `data.plan`, 필요한 경우 `data.confirmationToken`을 반환한다.

## 공통 오류 결과

```json
{
  "ok": false,
  "operation": "delete",
  "error": {
    "code": "CONFIRMATION_REQUIRED",
    "message": "건조 실행에서 받은 확인 토큰이 필요합니다.",
    "retryable": false,
    "retry_after_ms": null,
    "details": {}
  },
  "meta": {
    "requestId": "caller-generated-uuid",
    "correlationId": "server-generated-id"
  }
}
```

오류는 MCP 바깥 `CallToolResult.isError=true`와 함께 반환한다.
스택 추적, API 키, 메일 주소 원문, 로컬 절대경로 전체는 결과에 넣지 않는다.

## 삭제 확인 기록

확인 기록은 5분 기본 TTL을 가진다.

```json
{
  "schemaVersion": 1,
  "tokenHash": "base64url-sha256",
  "operation": "delete_comment",
  "target": {
    "kind": "post_comment",
    "id": "1234567890123456789"
  },
  "inputFingerprint": "base64url-sha256",
  "targetSnapshotFingerprint": "base64url-sha256",
  "principalFingerprint": "base64url-hmac",
  "serverInstanceId": "uuid",
  "expiresAtEpochMillis": 1785995100000,
  "consumedAtEpochMillis": null
}
```

토큰 원문은 상태 파일에 저장하지 않는다.
확정 요청이 성공하면 같은 토큰은 다시 사용할 수 없다.

## 멱등 기록

```json
{
  "schemaVersion": 1,
  "requestIdHash": "base64url-sha256",
  "inputFingerprint": "base64url-sha256",
  "principalFingerprint": "base64url-hmac",
  "status": "SUCCESS",
  "resultFingerprint": "base64url-sha256",
  "ids": [
    { "kind": "post", "id": "1234567890123456789" }
  ],
  "createdAtEpochMillis": 1785994800000,
  "expiresAtEpochMillis": 1786254000000
}
```

`status`는 `IN_PROGRESS`, `SUCCESS`, `PARTIAL_SUCCESS`, `OUTCOME_UNKNOWN`, `FAILED_BEFORE_EFFECT` 중 하나다.
쓰기 요청의 응답을 확인하지 못하면 `OUTCOME_UNKNOWN`으로 남긴다.
상태 파일의 시각은 JVM과 플랫폼 사이에서 손실 없이 비교하기 위해 UTC epoch millisecond로 저장한다.
외부 도구 결과에서 만료 시각을 보여줄 때는 ISO 8601 UTC 문자열로 변환한다.

파일 저장소를 실제 도구에 배선하는 도메인 계획은 서버 기동 시 한 번,
이후 정기적으로 `purgeExpired()`를 호출하는 수명 주기 책임도 함께 등록한다.
plan003은 저장소 계약과 구현만 제공하므로 아직 이 정리 작업을 시작하지 않는다.
