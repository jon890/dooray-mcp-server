# Phase 01 — 배포 하네스와 시험 이미지

**Execution profile**: standard
**Status**: pending

## 목표

독립 Docker 이미지와 안전한 시험 배포 절차를 완성한다.

**범위 외**: 시험 검증 전 안정 태그와 `latest` 갱신은 하지 않는다.

## 작업 항목 (4)

### 1. 이미지와 볼륨
- CLI를 설치하지 않는 JDK 21 이미지와 `/data/in`, `/data/out`, `/data/state`, `/run/secrets` 경계를 검증한다.
### 2. 다중 아키텍처
- `linux/amd64`, `linux/arm64`를 로컬에서 빌드하고 Docker 기동·STDIO 연결 검사를 자동화한다.
### 3. 문서와 스킬
- README, 이전 안내, 기능 대응표, 릴리스 노트를 갱신한다.
- `skill-creator`로 `.claude/skills/release/`를 만들고 `quick_validate.py`로 검증한다.
### 4. 시험 태그
- 자격 증명이 있으면 `bifos/dooray-mcp:0.5.0-beta.1` 형식으로 먼저 게시하고 digest를 기록한다.

## Critical Files

| 파일 | 변경 |
| --- | --- |
| `Dockerfile` | 독립 이미지와 볼륨 경계 |
| `.github/workflows/docker.yml` | 다중 아키텍처와 시험·안정 게이트 |
| `scripts/` | 기동·MCP 연결 검사 |
| `.claude/skills/release/` | 저장소 릴리스 스킬 |
| `README.md` | 실행·이전 안내 |

## 검증

```bash
# cwd: 현재 plan 작업 트리의 저장소 루트
CI=true ./gradlew clean build --no-daemon --console=plain
docker buildx build --platform linux/amd64,linux/arm64 --output=type=oci,dest=/tmp/dooray-mcp.oci .
```

## 의도 메모 (왜)

- 시험 태그 게시에는 Docker Hub 자격 증명이 필요하므로 없으면 로컬 다중 아키텍처 검증까지 완료하고 차단 근거를 남긴다.
