# Dooray Wiki Update API 문서

## PUT /wiki/v1/wikis/{wiki-id}/pages/{page-id}

위키 페이지 제목과 본문을 수정합니다.

### Request Parameters

| 파라미터  | 타입   | 설명                  |
| --------- | ------ | --------------------- |
| `wiki-id` | string | 위키 ID (경로 변수)   |
| `page-id` | string | 페이지 ID (경로 변수) |

### Request Body

```json
{
  "subject": "두레이 사용법",
  "body": {
    "mimeType": "text/x-markdown",
    "content": "위키 본문 내용 블라블라..."
  }
}
```

#### 요청 필드 설명

| 필드            | 타입   | 필수 여부 | 설명                        |
| --------------- | ------ | --------- | --------------------------- |
| `subject`       | string | 선택      | 위키 페이지 제목            |
| `body`          | object | 선택      | 위키 페이지 본문 정보       |
| `body.mimeType` | string | 필수      | MIME 타입 (text/x-markdown) |
| `body.content`  | string | 필수      | 위키 페이지 본문 내용       |

### Response

#### 성공 응답 (200)

```json
{
  "header": {
    "isSuccessful": true,
    "resultCode": 0,
    "resultMessage": "Success"
  },
  "result": null
}
```

#### 응답 필드 설명

| 필드                   | 타입    | 설명                   |
| ---------------------- | ------- | ---------------------- |
| `header.isSuccessful`  | boolean | 성공 여부              |
| `header.resultCode`    | number  | 결과 코드 (0: 성공)    |
| `header.resultMessage` | string  | 결과 메시지            |
| `result`               | null    | 수정 완료 시 null 반환 |

### HTTP 응답 코드

| 코드 | 설명                  |
| ---- | --------------------- |
| 200  | 성공                  |
| 401  | 인증 실패             |
| 403  | 권한 없음             |
| 404  | 리소스를 찾을 수 없음 |
| 500  | 서버 내부 오류        |

### 사용 예시

#### 제목과 본문 모두 수정

```bash
PUT /wiki/v1/wikis/1/pages/123
Content-Type: application/json

{
  "subject": "업데이트된 위키 제목",
  "body": {
    "mimeType": "text/x-markdown",
    "content": "# 업데이트된 본문\n\n새로운 내용입니다."
  }
}
```

#### 제목만 수정

```bash
PUT /wiki/v1/wikis/1/pages/123
Content-Type: application/json

{
  "subject": "업데이트된 위키 제목"
}
```

#### 본문만 수정

```bash
PUT /wiki/v1/wikis/1/pages/123
Content-Type: application/json

{
  "body": {
    "mimeType": "text/x-markdown",
    "content": "# 업데이트된 본문\n\n새로운 내용입니다."
  }
}
```
