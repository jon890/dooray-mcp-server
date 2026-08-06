# ADR 0005: 메일 구현 방식

상태: 승인됨

## 결정

메일 기능은 `org.eclipse.angus:angus-mail:2.0.5`로 같은 JVM 안에 독립 구현한다.

IMAPS와 SMTPS를 사용한다.
기본 호스트는 Dooray 메일 서버이며, 인증 정보는 도구 인자로 받지 않는다.

## 근거

메일 기능을 구현하려면 IMAP 상태, SMTP 응답, TLS, MIME, 주소 검증, 답장 헤더, 오류 분류를 다뤄야 한다.
이를 직접 구현하면 유지보수와 보안 위험이 크다.

Angus Mail은 Jakarta Mail 구현체이며 JDK 21과 Kotlin에서 직접 사용할 수 있다.
별도 프로세스나 CLI 의존성을 추가하지 않고도 IMAP, SMTP, MIME 동작을 검증할 수 있다.

## 결과

`MailService`, `MailClient`, `MailCredentialsProvider`, `MailErrorClassifier` 경계를 둔다.
`INBOX`는 읽기 전용으로 열고 UID와 `UIDVALIDITY`를 외부 식별자로 사용한다.
답장은 `Reply-To`, 없으면 `From`을 사용하고 `References` 계보를 보존한다.

메일 자격 증명 제거는 삭제 안전 정책의 준비·확정 흐름을 따른다.
