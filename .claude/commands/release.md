# Dooray MCP Server 릴리스

이 명령은 검증된 버전을 GitHub Release와 `bifos/dooray-mcp` 이미지로 배포한다.
안정 태그와 `latest`는 시험 태그 검증 전에는 갱신하지 않는다.

## 사전 조건

- 릴리스 버전과 변경 범위가 문서에 반영돼 있다.
- 기본 브랜치의 CI와 모든 리뷰 스레드가 통과했다.
- GitHub와 Docker Hub에 필요한 자격 증명이 준비돼 있다.
- 릴리스 노트와 외부 댓글은 `content-preview` 절차를 거쳤다.

## 절차

1. `gradle.properties`, `VersionConst.kt`, README, 기능 대응표, 이전 안내를 같은 버전으로 맞춘다.
2. 전체 검증을 실행한다.

   ```bash
   CI=true ./gradlew clean build --no-daemon --console=plain
   ./gradlew testMcpIntegration --no-daemon --console=plain
   ```

3. 정적 분석, 개인 식별 정보 검사, 문서 정합성 검사를 실행한다.
4. `linux/amd64`와 `linux/arm64` 이미지를 빌드한다.
5. `0.5.0-beta.1` 형식의 시험 태그를 먼저 푸시한다.
6. Docker 실행 예제와 실제 MCP 클라이언트 연결을 시험 태그로 확인한다.
7. 시험 검증 결과를 독립 검토한다.
8. 안정 배포가 승인 가능한 근거를 갖추면 버전 태그와 GitHub Release를 게시한다.
9. 안정 Docker 태그를 푸시한 뒤 마지막에 `latest`를 갱신한다.
10. GitHub Release 링크, 이미지 digest, 지원 아키텍처, pull 명령을 다시 확인한다.

## 중단 조건

- 자격 증명이 없거나 권한이 부족하다.
- 시험 이미지의 MCP 연결 검사가 실패한다.
- 공개 문서나 이미지에서 비밀 값 또는 개인 식별 정보가 발견된다.
- 미해결 리뷰 스레드나 실패한 필수 CI가 남아 있다.
