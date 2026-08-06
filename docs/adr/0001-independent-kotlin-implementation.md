# ADR 0001: 독립 Kotlin 구현

상태: 승인됨

## 결정

Dooray MCP Server는 Kotlin/JVM 프로세스 안에서 Dooray REST, IMAPS, SMTPS를 직접 호출한다.

다음 방식은 사용하지 않는다.

- `dooray` CLI 하위 프로세스 실행
- `@bifos/dooray-cli` 패키지 의존성
- CLI 소스 파일 또는 내부 모듈 가져오기
- Docker 이미지 안의 CLI 설치
- CLI 저장소가 필요한 빌드나 배포

## 근거

MCP 서버는 안정적인 서버 계약과 오류 계약을 제공해야 한다.
CLI는 터미널 상호작용, `$EDITOR`, 종료 코드, 표준 오류, 로컬 설정 파일을 중심으로 설계되어 있다.
이를 얇게 감싸면 MCP의 구조화 결과, 삭제 확인, 비밀 보호, 중복 요청 방지를 일관되게 보장하기 어렵다.

Dooray CLI는 검증된 동작의 참고 자료로만 사용한다.
외부 계약은 Dooray 공식 API와 메일 표준, MCP SDK가 된다.

## 결과

Kotlin 기술 스택을 유지한다.
메일 구현에는 같은 JVM 안의 Angus Mail을 사용한다.
기능 차이는 기능 대응표에 MCP 고유 동등 기능으로 기록한다.
