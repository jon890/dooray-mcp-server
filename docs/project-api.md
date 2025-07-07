# Dooray Project API 문서

## GET /project/v1/projects

접근 가능한 프로젝트 목록을 조회합니다.

### Request Parameters

| 파라미터 | 타입    | 설명                                           | 기본값    |
| -------- | ------- | ---------------------------------------------- | --------- |
| `member` | string  | `me` - 내가 속한 프로젝트만 응답               | -         |
| `page`   | integer | 페이지 번호 (0부터 시작)                       | 0         |
| `size`   | integer | 페이지 크기 (최대 100)                         | 20        |
| `type`   | string  | 프로젝트 타입: `private`, `public`             | `public`  |
| `scope`  | string  | 접근 범위: `private`, `public`                 | `private` |
| `state`  | string  | 프로젝트 상태: `active`, `archived`, `deleted` | -         |

#### 파라미터 상세 설명

**`type` 파라미터:**

- `public`: 일반 프로젝트
- `private`: 개인간 프로젝트 (응답에 포함되는 경우 항상 제일 처음에 위치)
- 조건이 명시되지 않으면 `type=public`으로 동작

**`scope` 파라미터:**

- `private`: 프로젝트 멤버만 접근 가능한 프로젝트
- `public`: guest가 아닌 조직 멤버면 누구나 접근 가능한 프로젝트

**`state` 파라미터:**

- `active`: 활성화된 프로젝트
- `archived`: 보관된 프로젝트
- `deleted`: 삭제된 프로젝트

### Response

#### 성공 응답 (200)

```json
{
  "header": {
    "isSuccessful": true,
    "resultCode": 0,
    "resultMessage": "Success"
  },
  "result": [
    {
      "id": "1",
      "code": "techcenter",
      "description": "기술센터 업무용 프로젝트 입니다.",
      "state": "active",
      "scope": "public",
      "type": "project",
      "organization": {
        "id": "1"
      },
      "drive": {
        "id": "1"
      },
      "wiki": {
        "id": "1"
      }
    }
  ],
  "totalCount": 1
}
```

#### 응답 필드 설명

| 필드              | 타입   | 설명          |
| ----------------- | ------ | ------------- |
| `id`              | string | 프로젝트 ID   |
| `code`            | string | 프로젝트 코드 |
| `description`     | string | 프로젝트 설명 |
| `state`           | string | 프로젝트 상태 |
| `scope`           | string | 접근 범위     |
| `type`            | string | 프로젝트 타입 |
| `organization.id` | string | 조직 ID       |
| `drive.id`        | string | 드라이브 ID   |
| `wiki.id`         | string | 위키 ID       |

### HTTP 응답 코드

| 코드 | 설명                  |
| ---- | --------------------- |
| 200  | 성공                  |
| 400  | 잘못된 요청           |
| 401  | 인증 실패             |
| 403  | 권한 없음             |
| 404  | 리소스를 찾을 수 없음 |
| 500  | 서버 내부 오류        |

### 사용 예시

#### 기본 조회 (활성화된 프로젝트, 최대 100개)

```
GET /project/v1/projects?member=me&page=0&size=100&type=public&scope=private&state=active
```

#### 보관된 프로젝트 조회

```
GET /project/v1/projects?member=me&state=archived
```

#### 개인 프로젝트 포함 조회

```
GET /project/v1/projects?member=me&type=private,public
```
