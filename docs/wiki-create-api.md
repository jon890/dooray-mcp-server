# Dooray Wiki Create API 문서

## POST /wiki/v1/wikis/{wiki-id}/pages

위키 페이지를 생성합니다.

### Request Parameters

| 파라미터  | 타입   | 설명                |
| --------- | ------ | ------------------- |
| `wiki-id` | string | 위키 ID (경로 변수) |

### Request Headers

| 헤더         | 값               | 설명      |
| ------------ | ---------------- | --------- |
| Content-Type | application/json | JSON 형식 |

### Request Body

```json
{
  "parentPageId": "{parentPageId}",
  "subject": "두레이 사용법",
  "body": {
    "mimeType": "text/x-markdown",
    "content": "위키 본문 내용"
  },
  "attachFileIds": ["{attachFileId}"],
  "referrers": [
    {
      "type": "member",
      "member": {
        "organizationMemberId": ""
      }
    }
  ]
}
```

#### 요청 필드 설명

| 필드                                      | 타입     | 필수 여부 | 설명                        |
| ----------------------------------------- | -------- | --------- | --------------------------- |
| `parentPageId`                            | string   | 필수      | 위키 부모 페이지 ID         |
| `subject`                                 | string   | 필수      | 위키 페이지 제목            |
| `body`                                    | object   | 필수      | 위키 페이지 본문 정보       |
| `body.mimeType`                           | string   | 필수      | MIME 타입 (text/x-markdown) |
| `body.content`                            | string   | 필수      | 위키 페이지 본문 내용       |
| `attachFileIds`                           | string[] | 선택      | 첨부파일 ID 목록            |
| `referrers`                               | object[] | 선택      | 참조자 설정 목록            |
| `referrers[].type`                        | string   | 필수      | 참조자 타입 (member 고정값) |
| `referrers[].member`                      | object   | 필수      | 참조자 멤버 정보            |
| `referrers[].member.organizationMemberId` | string   | 필수      | 조직 멤버 ID                |

### Response

#### 성공 응답 (201)

```json
{
  "header": {
    "isSuccessful": true,
    "resultCode": 0,
    "resultMessage": "Success"
  },
  "result": {
    "id": "123456",
    "wikiId": "wiki-id",
    "parentPageId": "parent-page-id",
    "version": 1
  }
}
```

#### 응답 필드 설명

| 필드                   | 타입    | 설명                  |
| ---------------------- | ------- | --------------------- |
| `header.isSuccessful`  | boolean | 성공 여부             |
| `header.resultCode`    | number  | 결과 코드 (0: 성공)   |
| `header.resultMessage` | string  | 결과 메시지           |
| `result.id`            | string  | 생성된 위키 페이지 ID |
| `result.wikiId`        | string  | 위키 ID               |
| `result.parentPageId`  | string  | 부모 페이지 ID        |
| `result.version`       | number  | 위키 페이지 버전      |

### HTTP 응답 코드

| 코드 | 설명                  |
| ---- | --------------------- |
| 201  | 생성 성공             |
| 400  | 잘못된 요청           |
| 401  | 인증 실패             |
| 403  | 권한 없음             |
| 404  | 리소스를 찾을 수 없음 |
| 500  | 서버 내부 오류        |

### 중요 사항

- **본문 형식**: 본문은 Markdown 형식으로 처리됩니다.
- **참조자 설정**: `referrers` 필드는 위키의 참조자를 설정합니다.
- **참조자 타입**: `type` 필드는 `member` 고정값입니다.
- **부모 페이지**: `parentPageId`는 필수 필드로, 위키 페이지의 계층 구조를 형성합니다.

### 사용 예시

#### 기본 위키 페이지 생성

```bash
POST /wiki/v1/wikis/wiki-123/pages
Content-Type: application/json

{
  "parentPageId": "parent-page-123",
  "subject": "새로운 위키 페이지",
  "body": {
    "mimeType": "text/x-markdown",
    "content": "# 새로운 위키 페이지\n\n이것은 새로운 위키 페이지입니다."
  }
}
```

#### 참조자가 포함된 위키 페이지 생성

```bash
POST /wiki/v1/wikis/wiki-123/pages
Content-Type: application/json

{
  "parentPageId": "parent-page-123",
  "subject": "팀 회의 위키",
  "body": {
    "mimeType": "text/x-markdown",
    "content": "# 팀 회의 위키\n\n## 참석자\n- 홍길동\n- 김철수"
  },
  "referrers": [
    {
      "type": "member",
      "member": {
        "organizationMemberId": "member-123"
      }
    }
  ]
}
```
