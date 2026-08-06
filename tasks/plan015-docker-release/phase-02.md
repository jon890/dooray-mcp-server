# Phase 02 — 릴리스 검증과 완료

**Execution profile**: deep
**Status**: pending

## 목표

시험 이미지를 실제 MCP 클라이언트에서 검증한 뒤 근거가 충분할 때만 안정 릴리스를 게시한다.

**범위 외**: 실패한 시험 이미지를 안정 태그나 `latest`로 승격하지 않는다.

## 작업 항목 (4)

### 1. 릴리스 전 게이트
- 전체 시험, 정적 분석, 개인 식별 정보, 독립 검토, 문서 정합성, 리뷰 스레드와 CI를 확인한다.
### 2. 시험 검증
- 시험 태그의 두 아키텍처 digest, Docker 기동, 볼륨 경계, MCP 도구·리소스 연결을 확인한다.
### 3. 안정 배포
- 게시 본문 미리보기 뒤 GitHub Release와 안정 Docker 태그를 게시하고 마지막에 `latest`를 갱신한다.
### 4. 완료 마킹
- release URL, image digest, pull 검증과 기능 대응표의 최종 상태를 확인한 뒤 `tasks/plan015-docker-release/index.json`과 phase 상태를 `completed`로 바꾼다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `docs/parity/dooray-cli-v0.16.0.md` | 최종 상태 |
| `README.md` | 안정 버전과 pull 명령 |
| `.github/workflows/docker.yml` | 승격 게이트 |
| `tasks/plan015-docker-release/index.json` | 완료 상태 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
./gradlew testMcpIntegration --no-daemon --console=plain
docker buildx imagetools inspect bifos/dooray-mcp:0.5.0-beta.1
git diff --check
```

## 의도 메모 (왜)

- 자격 증명, 외부 게시 권한, 안정 태그 승격은 실제 검증과 사용자 권한이 필요한 중단 조건이다.
