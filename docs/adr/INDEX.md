# ADR 색인

작성 기준일은 2026-08-06이다.

ADR은 코드로 자명하지 않은 장기 결정과 근거만 기록한다.
단순 구현 절차, 현재 상태 요약, 코드로 바로 확인되는 사실은 일반 설계 문서에 둔다.

| ADR | 상태 | 결정 |
| --- | --- | --- |
| [0001](0001-independent-kotlin-implementation.md) | 승인됨 | Dooray CLI를 호출하거나 의존하지 않고 Kotlin 서버 안에서 직접 구현한다. |
| [0002](0002-tool-profile-strategy.md) | 승인됨 | 12개 `compact` 도구와 20개 `legacy` 도구, `all` 프로필을 유지한다. |
| [0003](0003-state-and-delete-safety.md) | 승인됨 | 상태 저장, 멱등 기록, 삭제 확인 토큰을 서버 공통 계약으로 둔다. |
| [0004](0004-file-transfer-boundary.md) | 승인됨 | 파일은 MCP 리소스 중심으로 전달하고 서버 경로는 허용 목록으로 제한한다. |
| [0005](0005-mail-implementation.md) | 승인됨 | 메일은 Angus Mail로 같은 JVM 안에 독립 구현한다. |
