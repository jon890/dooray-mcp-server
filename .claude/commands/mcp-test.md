# MCP 연결 검사

가짜 자격 증명과 STDIO 전송을 사용해 빌드된 서버의 MCP 계약을 검증한다.

## 절차

1. 현재 버전의 실행 JAR을 빌드한다.

   ```bash
   ./gradlew shadowJar --no-daemon --console=plain
   ```

2. 전용 연결 검사를 실행한다.

   ```bash
   ./gradlew testMcpIntegration --no-daemon --console=plain
   ```

3. 다음 항목을 확인한다.

   - MCP 초기 연결과 종료
   - 선택한 도구 프로필의 정확한 도구 목록
   - 모든 도구의 설명과 입력 스키마
   - 필수 파라미터 선언
   - 구조화된 오류 응답

4. 통과한 검사 수와 실패 내용을 보고한다.

## 안전 경계

- `DOORAY_BASE_URL`과 `DOORAY_API_KEY`에는 테스트 전용 가짜 값을 사용한다.
- 서버가 실제 Dooray API를 호출하지 않는 입력만 보낸다.
- 실행 JAR이 없어서 검사가 건너뛰어지면 성공으로 간주하지 않는다.
