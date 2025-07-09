# Dooray Project Post Comment API 문서

## POST /project/v1/projects/{project-id}/posts/{post-id}/logs

업무에 댓글을 생성합니다.

### Request Body

```json
{
  "body": {
    "content": "댓글 내용",
    "mimeType": "text/x-markdown"
  }
}
```

#### 지원되는 MIME 타입

- `text/x-markdown`
- `text/html`

### Response

#### 성공 응답 (200)

```json
{
  "header": {
    "isSuccessful": true,
    "resultCode": 0,
    "resultMessage": ""
  },
  "result": {
    "id": "댓글ID"
  }
}
```

### HTTP 응답 코드

| 코드 | 설명                  |
| ---- | --------------------- |
| 200  | 성공                  |
| 400  | 잘못된 요청           |
| 401  | 인증 실패             |
| 403  | 권한 없음             |
| 404  | 리소스를 찾을 수 없음 |
| 500  | 서버 내부 오류        |

---

## GET /project/v1/projects/{project-id}/posts/{post-id}/logs

업무 댓글 목록을 조회합니다.

### Request Parameters

| 파라미터 | 타입    | 설명                                                     | 기본값      |
| -------- | ------- | -------------------------------------------------------- | ----------- |
| `page`   | integer | 페이지 번호 (0부터 시작)                                 | 0           |
| `size`   | integer | 페이지 크기 (최대 100)                                   | 20          |
| `order`  | string  | 정렬 조건: `createdAt` (오래된순), `-createdAt` (최신순) | `createdAt` |

### Response

#### 성공 응답 (200)

```json
{
  "header": {
    "isSuccessful": true,
    "resultCode": 0,
    "resultMessage": ""
  },
  "totalCount": 1,
  "result": [
    {
      "id": "1",
      "post": {
        "id": "업무ID"
      },
      "type": "comment",
      "subtype": "general",
      "createdAt": "2014-10-08T19:23:32+09:00",
      "modifiedAt": "2014-10-08T19:23:32+09:00",
      "creator": {
        "type": "member",
        "member": {
          "organizationMemberId": "1"
        }
      },
      "mailUsers": {
        "from": {
          "name": "",
          "emailAddress": ""
        },
        "to": [
          {
            "name": "",
            "emailAddress": ""
          }
        ],
        "cc": [
          {
            "name": "",
            "emailAddress": ""
          }
        ]
      },
      "body": {
        "mimeType": "text/x-markdown",
        "content": "최종 기획 확인 바랍니다."
      }
    }
  ]
}
```

#### 응답 필드 설명

| 필드         | 타입   | 설명                                             |
| ------------ | ------ | ------------------------------------------------ |
| `id`         | string | 댓글 ID                                          |
| `post.id`    | string | 업무 ID                                          |
| `type`       | string | 로그 타입: `comment`, `event`                    |
| `subtype`    | string | 하위 타입: `general`, `from_email`, `sent_email` |
| `createdAt`  | string | 생성 시간 (ISO 8601)                             |
| `modifiedAt` | string | 수정 시간 (ISO 8601)                             |
| `creator`    | object | 생성자 정보                                      |
| `mailUsers`  | object | 이메일 사용자 정보                               |
| `body`       | object | 댓글 본문                                        |

#### 생성자 정보 타입

**내부 멤버인 경우:**

```json
{
  "creator": {
    "type": "member",
    "member": {
      "organizationMemberId": "1"
    }
  }
}
```

**이메일 사용자인 경우:**

```json
{
  "creator": {
    "type": "emailUser",
    "emailUser": {
      "emailAddress": "user@example.com",
      "name": "사용자명"
    }
  }
}
```

### HTTP 응답 코드

| 코드 | 설명                  |
| ---- | --------------------- |
| 200  | 성공                  |
| 400  | 잘못된 요청           |
| 401  | 인증 실패             |
| 403  | 권한 없음             |
| 404  | 리소스를 찾을 수 없음 |
| 500  | 서버 내부 오류        |

---

## GET /project/v1/projects/{project-id}/posts/{post-id}/logs/{log-id}

특정 업무 댓글의 상세 내용을 조회합니다.

### Response

#### 성공 응답 (200)

```json
{
  "header": {
    "isSuccessful": true,
    "resultCode": 0,
    "resultMessage": ""
  },
  "result": {
    "id": "댓글ID",
    "post": {
      "id": "업무ID"
    },
    "type": "comment",
    "subtype": "general",
    "createdAt": "2014-10-08T19:23:32+09:00",
    "creator": {
      "type": "member",
      "member": {
        "organizationMemberId": "1"
      }
    },
    "mailUsers": {
      "from": {
        "name": "",
        "emailAddress": ""
      },
      "to": [
        {
          "name": "",
          "emailAddress": ""
        }
      ],
      "cc": [
        {
          "name": "",
          "emailAddress": ""
        }
      ]
    },
    "body": {
      "mimeType": "text/html",
      "content": "댓글 내용"
    },
    "files": [
      {
        "id": "파일ID",
        "name": "파일명",
        "size": 1024
      }
    ]
  }
}
```

### HTTP 응답 코드

| 코드 | 설명                  |
| ---- | --------------------- |
| 200  | 성공                  |
| 400  | 잘못된 요청           |
| 401  | 인증 실패             |
| 403  | 권한 없음             |
| 404  | 리소스를 찾을 수 없음 |
| 500  | 서버 내부 오류        |

---

## PUT /project/v1/projects/{project-id}/posts/{post-id}/logs/{log-id}

업무 댓글을 수정합니다.

> **참고:** 이메일로 발송된 메일은 수정이 불가능합니다.

### Request Body

```json
{
  "body": {
    "mimeType": "text/x-markdown",
    "content": "수정된 댓글 내용"
  }
}
```

### Response

#### 성공 응답 (200)

```json
{
  "header": {
    "isSuccessful": true,
    "resultCode": 0,
    "resultMessage": ""
  },
  "result": null
}
```

### HTTP 응답 코드

| 코드 | 설명                  |
| ---- | --------------------- |
| 200  | 성공                  |
| 400  | 잘못된 요청           |
| 401  | 인증 실패             |
| 403  | 권한 없음             |
| 404  | 리소스를 찾을 수 없음 |
| 500  | 서버 내부 오류        |

---

## DELETE /project/v1/projects/{project-id}/posts/{post-id}/logs/{log-id}

업무 댓글을 삭제합니다.

### Response

#### 성공 응답 (200)

```json
{
  "header": {
    "isSuccessful": true,
    "resultCode": 0,
    "resultMessage": ""
  },
  "result": null
}
```

### HTTP 응답 코드

| 코드 | 설명                  |
| ---- | --------------------- |
| 200  | 성공                  |
| 400  | 잘못된 요청           |
| 401  | 인증 실패             |
| 403  | 권한 없음             |
| 404  | 리소스를 찾을 수 없음 |
| 500  | 서버 내부 오류        |

---

### 사용 예시

#### 댓글 생성

```
POST /project/v1/projects/123/posts/456/logs
Content-Type: application/json

{
  "body": {
    "content": "검토 완료했습니다.",
    "mimeType": "text/x-markdown"
  }
}
```

#### 댓글 목록 조회 (최신순)

```
GET /project/v1/projects/123/posts/456/logs?page=0&size=20&order=-createdAt
```

#### 특정 댓글 조회

```
GET /project/v1/projects/123/posts/456/logs/789
```

#### 댓글 수정

```
PUT /project/v1/projects/123/posts/456/logs/789
Content-Type: application/json

{
  "body": {
    "content": "수정된 내용입니다.",
    "mimeType": "text/x-markdown"
  }
}
```

#### 댓글 삭제

```
DELETE /project/v1/projects/123/posts/456/logs/789
```
