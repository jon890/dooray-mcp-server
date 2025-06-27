# Dooray MCP Server

AI 도구를 활용하여 NHN Dooray를 컨트롤할 수 있도록 합니다.

## 🚀 빠른 시작

### 🐳 Docker 사용 (권장)

```bash
# Docker Hub에서 이미지 다운로드 및 실행
docker run \
  -e DOORAY_API_KEY="your_tenant:your_api_token" \
  -e DOORAY_PROJECT_ID="your_project_id" \
  bifos/dooray-mcp:latest

# 모든 환경변수 지정
docker run \
  -e DOORAY_BASE_URL="https://api.dooray.com" \
  -e DOORAY_API_KEY="your_tenant:your_api_token" \
  -e DOORAY_PROJECT_ID="your_project_id" \
  -e JAVA_OPTS="-Xms256m -Xmx1g" \
  bifos/dooray-mcp:latest
```

### 📦 로컬 개발

#### 1. 환경변수 설정

`.env.sample` 파일을 복사하여 `.env` 파일을 생성하고 설정을 수정합니다:

````bash
# .env.sample을 복사하여 .env 파일 생성
cp .env.sample .env

**필수 환경변수:**

- `DOORAY_BASE_URL`: Dooray Base URL (https://api.dooray.com)
- `DOORAY_API_KEY`: Dooray API 키 (tenant:token 형식)
- `DOORAY_PROJECT_ID`: 프로젝트 ID

#### 2. 로컬 실행

```bash
# 빌드 및 실행 (한 번에)
./gradlew runLocal
````

이 명령어는 다음을 수행합니다:

1. `.env` 파일에서 환경변수 로드
2. `shadowJar`로 프로젝트 빌드
3. MCP 서버 실행

## 🐳 Docker 사용법

### Docker Hub에서 사용

```bash
# 최신 버전 실행
docker run -e DOORAY_API_KEY="your_tenant:your_api_token" bifos/dooray-mcp:latest

# 특정 버전 실행
docker run -e DOORAY_API_KEY="your_tenant:your_api_token" bifos/dooray-mcp:0.1.1
```

**필요한 .env 파일 설정:**

```bash
DOORAY_BASE_URL=https://api.dooray.com
DOORAY_API_KEY=your_tenant:your_api_token
DOORAY_PROJECT_ID=your_project_id
```

### 로컬에서 Docker 이미지 빌드

```bash
# 빌드
./scripts/docker-build.sh

# Docker Hub에 푸시
./scripts/docker-push.sh
```

## 📋 Tools

### get_wiki_pages

- **설명**: 특정 프로젝트의 위키 페이지들을 조회합니다
- **파라미터**:
  - `projectId` (필수): 프로젝트 ID
  - `parentPageId` (선택): 상위 페이지 ID (null이면 최상위 페이지들 조회)

#### 사용 예시

```json
{
  "name": "get_wiki_pages",
  "arguments": {
    "projectId": "1234567890",
    "parentPageId": "1234567890"
  }
}
```

## 📚 참고자료

- [두레이 API](https://helpdesk.dooray.com/share/pages/9wWo-xwiR66BO5LGshgVTg/2939987647631384419)
- [Kotlin MCP Server 예제](https://github.com/modelcontextprotocol/kotlin-sdk/blob/main/samples/weather-stdio-server/src/main/kotlin/io/modelcontextprotocol/sample/server/McpWeatherServer.kt)
- [Model Context Protocol](https://modelcontextprotocol.io/introduction)
