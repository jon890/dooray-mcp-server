# Dooray MCP Server

NHN Dooray 서비스의 MCP(Model Context Protocol) 서버입니다.

## 주요 기능

- **위키 조회**: 접근 가능한 위키 목록 및 페이지 조회
- **JSON 응답**: 규격화된 JSON 형태의 응답
- **예외 처리**: 일관된 에러 응답 제공
- **Docker 지원**: 멀티 플랫폼 Docker 이미지 제공

## 빠른 시작

### 환경변수 설정

다음 환경변수를 설정해야 합니다:

```bash
export DOORAY_API_KEY="your_api_key"
export DOORAY_BASE_URL="https://api.dooray.com"
export DOORAY_PROJECT_ID="your_project_id"
```

### 로컬 실행

```bash
# 의존성 설치 및 빌드
./gradlew clean shadowJar

# 로컬 실행 (.env 파일 사용)
./gradlew runLocal

# 또는 직접 실행
java -jar build/libs/dooray-mcp-server-0.1.4-all.jar
```

### Docker 실행

```bash
# Docker Hub에서 이미지 가져오기
docker pull bifos/dooray-mcp:latest

# 환경변수와 함께 실행
docker run -e DOORAY_API_KEY="your_api_key" \
           -e DOORAY_BASE_URL="https://api.dooray.com" \
           -e DOORAY_PROJECT_ID="your_project_id" \
           bifos/dooray-mcp:latest
```

## 사용 가능한 도구

### 1. dooray_wiki_list_projects

두레이에서 접근 가능한 위키 프로젝트 목록을 조회합니다.

**사용법:**

```json
{
  "name": "dooray_wiki_list_projects",
  "arguments": {
    "page": 1,
    "size": 20
  }
}
```

**매개변수:**

- `page` (선택): 조회할 페이지 번호 (**0부터 시작**, 기본값: 0)
- `size` (선택): 한 페이지당 결과 수 (기본값: 20, 최대: 200)

### 2. dooray_wiki_list_pages

특정 두레이 위키 프로젝트의 페이지 목록을 조회합니다.

**사용법:**

```json
{
  "name": "dooray_wiki_list_pages",
  "arguments": {
    "projectId": "3647142034893802388"
  }
}
```

**매개변수:**

- `projectId` (필수): 위키 프로젝트 ID (dooray_wiki_list_projects로 조회 가능)
- `parentPageId` (선택): 상위 페이지 ID (없으면 루트 페이지들 조회)

### 3. dooray_wiki_get_page

특정 두레이 위키 페이지의 상세 정보를 조회합니다.

**사용법:**

```json
{
  "name": "dooray_wiki_get_page",
  "arguments": {
    "projectId": "3647142034893802388",
    "pageId": "3732036680598959398"
  }
}
```

**매개변수:**

- `projectId` (필수): 위키 프로젝트 ID (dooray_wiki_list_projects로 조회 가능)
- `pageId` (필수): 위키 페이지 ID (dooray_wiki_list_pages로 조회 가능)

### 📝 사용 순서 가이드

1. **프로젝트 찾기**: `dooray_wiki_list_projects`로 원하는 프로젝트의 ID 확인
   - 💡 **팁**: 많은 프로젝트가 있다면 `size: 200`으로 한 번에 많이 조회하세요
2. **페이지 목록 조회**: `dooray_wiki_list_pages`로 해당 프로젝트의 위키 페이지들 확인
3. **페이지 상세 조회**: `dooray_wiki_get_page`로 특정 페이지의 내용 확인

### ⚠️ 주의사항

- **페이지 번호**: 모든 페이지 번호는 **0부터 시작**합니다 (1이 아님)
- **권한**: 접근 권한이 없는 프로젝트나 페이지는 조회되지 않습니다
- **API 제한**: 한 번에 최대 200개까지 조회 가능합니다

## 개발

### 테스트 실행

```bash
# 모든 테스트 실행 (환경변수 있을 때)
./gradlew test

# CI 환경에서는 통합 테스트 자동 제외
CI=true ./gradlew test
```

### 빌드

```bash
# JAR 빌드
./gradlew clean shadowJar

# Docker 이미지 빌드
docker build -t dooray-mcp:local --build-arg VERSION=0.1.2 .
```

## Docker 멀티 플랫폼 빌드

### 현재 상태

현재 Docker 이미지는 **AMD64만 지원**합니다. ARM64 빌드는 QEMU 에뮬레이션에서 Gradle 의존성 다운로드 단계에서 멈추는 문제가 있어 일시적으로 비활성화되었습니다.

### ARM64 빌드 활성화

ARM64 빌드를 다시 활성화하려면 `.github/workflows/docker-publish.yml`에서 다음 설정을 변경하세요:

```yaml
env:
  ENABLE_ARM64: true # false에서 true로 변경
```

### ARM64 빌드 문제 해결 방법

1. **네이티브 ARM64 러너 사용** (권장)
2. **QEMU 타임아웃 증가**
3. **Gradle 캐시 최적화**
4. **의존성 사전 다운로드**

현재는 안정성을 위해 AMD64만 빌드하고 있으며, ARM64 지원은 향후 업데이트에서 제공될 예정입니다.

## 환경변수

| 변수명            | 설명                | 필수 여부 |
| ----------------- | ------------------- | --------- |
| DOORAY_API_KEY    | Dooray API 키       | 필수      |
| DOORAY_BASE_URL   | Dooray API Base URL | 필수      |
| DOORAY_PROJECT_ID | 기본 프로젝트 ID    | 선택      |

## 라이선스

이 프로젝트는 오픈 소스이며, 자유롭게 사용하실 수 있습니다.

## 기여

프로젝트에 기여하고 싶으시다면 이슈를 등록하거나 풀 리퀘스트를 보내주세요.

## 📚 참고자료

- [두레이 API](https://helpdesk.dooray.com/share/pages/9wWo-xwiR66BO5LGshgVTg/2939987647631384419)
- [Kotlin MCP Server 예제](https://github.com/modelcontextprotocol/kotlin-sdk/blob/main/samples/weather-stdio-server/src/main/kotlin/io/modelcontextprotocol/sample/server/McpWeatherServer.kt)
- [Model Context Protocol](https://modelcontextprotocol.io/introduction)
