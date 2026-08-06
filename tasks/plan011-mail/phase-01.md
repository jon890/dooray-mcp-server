# Phase 01 — 메일 클라이언트와 도구 구현

**Execution profile**: deep
**Status**: pending

## 목표

Angus Mail로 `dooray_mail`의 목록·상세·전송·답장과 자격 증명 제거 준비를 독립 구현한다.

**범위 외**: Keychain, CLI 프로세스, 실제 자격 증명 제거 확정은 다루지 않는다.

## 작업 항목 (4)

### 1. 의존성과 설정
- `org.eclipse.angus:angus-mail:2.0.5`와 시험용 `greenmail-junit5:2.1.11`을 추가한다.
- 사용자명과 비밀번호는 환경변수 또는 secret 파일에서만 읽고 도구 입력·결과에 넣지 않는다.

### 2. IMAPS 조회
- `INBOX`를 읽기 전용으로 열어 목록과 상세를 구현하고 UID와 `UIDVALIDITY`를 함께 보존한다.

### 3. SMTPS 전송과 답장
- accepted/rejected/messageId를 구조화하고 `Reply-To`·`From`, `In-Reply-To`, `References` 규칙을 구현한다.

### 4. 오류와 자격 증명 제거 준비
- 설정·DNS·TLS·인증·시간 제한·일부 성공을 안정 오류 코드로 분류한다.
- `prepare_logout`은 저장 위치와 영향만 반환하고 비밀 원문은 읽어 내보내지 않는다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `gradle/libs.versions.toml` | Angus Mail과 GreenMail |
| `src/main/kotlin/com/bifos/dooray/mcp/mail/` | 클라이언트·서비스·오류 분류 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/compact/DoorayMailTool.kt` | compact 도구 |
| `src/test/kotlin/com/bifos/dooray/mcp/mail/` | 로컬 메일 시험 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- 메일 표준 구현체를 사용하되 CLI 패키지와 소스에는 의존하지 않는다.
