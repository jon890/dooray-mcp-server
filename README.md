# Dooray MCP Server

AI 도구를 활용하여 NHN Dooray를 컨트롤할 수 있도록 합니다.

## 🚀 빠른 시작

### 1. 환경변수 설정

`.env` 파일을 생성하고 Dooray API 키를 설정합니다:

```bash
# .env 파일 생성
cat > .env << EOF
# Dooray API 키 (필수)
DOORAY_API_KEY=your_tenant:your_api_token

# 테스트용 위키 ID (선택사항)
TEST_WIKI_ID=your_wiki_id

# 테스트용 상위 페이지 ID (선택사항)
TEST_PARENT_PAGE_ID=parent_page_id
EOF
```

### 2. 로컬 실행

```bash
# 빌드 및 실행 (한 번에)
./gradlew runLocal
```

이 명령어는 다음을 수행합니다:

1. `.env` 파일에서 환경변수 로드
2. `shadowJar`로 프로젝트 빌드
3. MCP 서버 실행

### 3. 수동 빌드 및 실행

```bash
# 빌드만 실행
./gradlew clean shadowJar

# 환경변수 설정 후 실행
export DOORAY_API_KEY="your_tenant:your_api_token"
java -jar build/libs/dooray-mcp-server-0.1.0-all.jar
```

## 🔧 개발

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests DoorayApiIntegrationTest
```

### 빌드

```bash
# Fat JAR 빌드
./gradlew shadowJar

# 일반 JAR 빌드
./gradlew jar
```

## 📋 Tools

### get_wiki_pages

- **설명**: 특정 위키의 페이지들을 조회합니다
- **파라미터**:
  - `wikiId` (필수): 위키 ID
  - `parentPageId` (선택): 상위 페이지 ID (null이면 최상위 페이지들 조회)

#### 사용 예시

```json
{
  "name": "get_wiki_pages",
  "arguments": {
    "wikiId": "3647142034893802388",
    "parentPageId": "100"
  }
}
```

## 📚 참고자료

- [두레이 API](https://helpdesk.dooray.com/share/pages/9wWo-xwiR66BO5LGshgVTg/2939987647631384419)
- [Kotlin MCP Server 예제](https://github.com/modelcontextprotocol/kotlin-sdk/blob/main/samples/weather-stdio-server/src/main/kotlin/io/modelcontextprotocol/sample/server/McpWeatherServer.kt)
- [Model Context Protocol](https://modelcontextprotocol.io/introduction)

## 🔑 API 키 설정

Dooray API 키는 다음 형식으로 설정해야 합니다:

```
tenant:token
```

예시: `ajjt1imxmtj4:CoVSbgZyR3iIpHSQTvJnmw`
