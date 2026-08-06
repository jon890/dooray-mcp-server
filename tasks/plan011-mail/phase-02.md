# Phase 02 — 메일 오류·보안 검증과 완료

**Execution profile**: fast
**Status**: pending

## 목표

로컬 메일 서버로 정상·오류·보안 계약을 검증하고 plan011을 완료한다.

**범위 외**: 외부 주소로 메일을 보내거나 운영 메일함을 읽지 않는다.

## 작업 항목 (4)

### 1. IMAP 시험
- 읽지 않음 필터, 제목 검색, MIME 본문, UIDVALIDITY 변경과 읽기 전용 동작을 시험한다.
### 2. SMTP 시험
- 전송, 부분 수신 거부, 답장 헤더와 HTML/텍스트 본문을 시험한다.
### 3. 오류·비밀 시험
- 설정·인증·TLS·시간 제한 분류와 로그·결과의 비밀 마스킹을 시험한다.
### 4. 완료 마킹
- 검증 통과 뒤 `tasks/plan011-mail/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/test/kotlin/com/bifos/dooray/mcp/mail/` | IMAP·SMTP·오류 시험 |
| `tasks/plan011-mail/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
git diff --check
```

## 의도 메모 (왜)

- 모든 시험은 로컬 가짜 서버와 가짜 주소만 사용한다.
