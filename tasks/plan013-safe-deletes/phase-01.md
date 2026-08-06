# Phase 01 — 확인 저장소와 삭제 연결

**Execution profile**: deep
**Status**: pending

## 목표

plan003의 확인 저장소를 강화하고 대상과 영향에 묶인 일회용 확인 토큰을 모든 손실 operation에 연결한다.

**범위 외**: 새 조회·생성 기능과 실제 외부 삭제 시험은 다루지 않는다.

## 작업 항목 (4)

### 1. 확인 저장소 강화
- plan003 구현에 대상·입력·스냅숏·실행 주체 결합과 재시작 무효 정책을 완성한다.
- 기본 TTL 300초, 단일 소비, 원자 기록과 손상 기록 격리를 검증한다.

### 2. 도메인 삭제 연결
- 업무 댓글, 업무 첨부, 댓글 첨부, 위키 페이지, 위키 첨부, 위키 댓글에 `prepare_delete`와 `confirm_delete`를 연결한다.
- 메일 자격 증명은 `prepare_logout`과 `confirm_logout`으로 연결한다.

### 3. 대상 변화와 경쟁 조건
- 확정 직전 대상을 다시 조회하고 fingerprint가 달라지면 `CONFIRMATION_TARGET_CHANGED`로 거부한다.
- 조건부 삭제가 없는 API의 남는 경쟁 조건을 결과와 문서에 표시한다.

### 4. legacy 이전
- 기존 삭제 도구 이름은 유지하되 무토큰 즉시 삭제를 중단하고 안전한 이전 오류와 준비 계획을 반환한다.
- 하위 호환 변경과 이전 방법을 README와 기능 대응표에 기록한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `src/main/kotlin/com/bifos/dooray/mcp/state/ConfirmationStore.kt` | 확인 기록 |
| `src/main/kotlin/com/bifos/dooray/mcp/service/delete/` | 공통 삭제 흐름 |
| `src/main/kotlin/com/bifos/dooray/mcp/tools/` | compact·legacy 안전 연결 |
| `src/test/kotlin/com/bifos/dooray/mcp/service/delete/` | 확인 토큰 시험 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- MCP에서는 TTY와 CLI `--yes`를 사용할 수 없으므로 operation 자체를 두 단계로 분리한다.
