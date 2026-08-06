# Phase 02 — 독립 검증과 완료 마킹

**Execution profile**: fast
**Status**: completed

## 목표

계획 문서와 하네스의 누락·모순·오래된 참조를 독립 검증하고 plan001을 완료한다.

**범위 외**: 검증에서 발견된 후속 제품 기능을 이 plan에 구현하지 않는다.

## 작업 항목 (4)

### 1. 대응표 검증
- CLI 66행, 공통 15행, 7열, 허용 상태 토큰과 12/20/32 도구 수를 검사한다.
### 2. 금지 경계 검증
- CLI process/package/source/Docker 의존과 비밀·개인 식별 정보가 없는지 검사한다.
### 3. 작업 명세 검증
- 15개 plan의 JSON, phase 존재, `verify-task.sh` 출력 0줄과 파일 소유권을 검사한다.
### 4. 완료 마킹
- 독립 검토와 기본 검증 통과 뒤 `tasks/plan001-project-harness/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `docs/parity/dooray-cli-v0.16.0.md` | 행·상태 검증 |
| `tasks/plan*/` | 스키마·위생 검증 |
| `tasks/plan001-project-harness/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
for d in tasks/plan*; do "${PLANNING_SKILL_DIR:?}/scripts/verify-task.sh" "${d##*/}"; done
git diff --check
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
```

## 의도 메모 (왜)

- plan001이 끝나면 plan002는 최신 `main`에서 별도 작업 트리와 PR로 시작한다.
