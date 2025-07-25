# Dooray MCP Server

NHN Dooray 서비스의 MCP(Model Context Protocol) 서버입니다.

## 주요 기능

- **위키 관리**: 위키 조회, 생성, 수정, 참조자 관리
- **업무 관리**: 업무 조회, 생성, 수정, 상태 변경
- **댓글 관리**: 업무 댓글 생성, 조회, 수정, 삭제
- **JSON 응답**: 규격화된 JSON 형태의 응답
- **예외 처리**: 일관된 에러 응답 제공
- **Docker 지원**: 멀티 플랫폼 Docker 이미지 제공

## 빠른 시작

### 환경변수 설정

다음 환경변수를 설정해야 합니다:

```bash
export DOORAY_API_KEY="your_api_key"
export DOORAY_BASE_URL="https://api.dooray.com"

# 선택사항: 로깅 레벨 제어
export DOORAY_LOG_LEVEL="WARN"         # DEBUG, INFO, WARN, ERROR (기본값: WARN)
export DOORAY_HTTP_LOG_LEVEL="WARN"    # HTTP 클라이언트 로깅 (기본값: WARN)
```

#### 로깅 설정

**일반 로깅 (`DOORAY_LOG_LEVEL`)**

- `WARN` (기본값): 경고 및 에러만 로깅 - **MCP 통신 안정성을 위해 권장**
- `INFO`: 일반 정보 포함 로깅
- `DEBUG`: 상세한 디버그 정보 포함

**HTTP 로깅 (`DOORAY_HTTP_LOG_LEVEL`)**

- `WARN` (기본값): HTTP 에러만 로깅 - **MCP 통신 안정성을 위해 권장**
- `INFO`: 기본 요청/응답 정보만 로깅
- `DEBUG`: 상세한 HTTP 정보 로깅

> ⚠️ **중요**: MCP 서버는 stdin/stdout을 통해 통신하므로, 모든 로그는 **stderr**로 출력됩니다. 로깅 레벨을 높이면 프로토콜 통신에는 영향을 주지 않지만, 성능에 영향을 줄 수 있습니다.

### 로컬 실행

```bash
# 의존성 설치 및 빌드
./gradlew clean shadowJar

# 로컬 실행 (.env 파일 사용)
./gradlew runLocal

# 또는 직접 실행
java -jar build/libs/dooray-mcp-server-0.2.1-all.jar
```

### Docker 실행

```bash
# Docker Hub에서 이미지 가져오기
docker pull bifos/dooray-mcp:latest

# 환경변수와 함께 실행
docker run -e DOORAY_API_KEY="your_api_key" \
           -e DOORAY_BASE_URL="https://api.dooray.com" \
           bifos/dooray-mcp:latest
```

## 사용 가능한 도구 (총 19개)

### 위키 관련 도구 (8개)

#### 1. dooray_wiki_list_projects

두레이에서 접근 가능한 위키 프로젝트 목록을 조회합니다.

#### 2. dooray_wiki_list_pages

특정 두레이 위키 프로젝트의 페이지 목록을 조회합니다.

#### 3. dooray_wiki_get_page

특정 두레이 위키 페이지의 상세 정보를 조회합니다.

#### 4. dooray_wiki_create_page

새로운 위키 페이지를 생성합니다.

#### 5. dooray_wiki_update_page

기존 위키 페이지를 수정합니다.

#### 6. dooray_wiki_update_page_title

위키 페이지의 제목만 수정합니다.

#### 7. dooray_wiki_update_page_content

위키 페이지의 내용만 수정합니다.

#### 8. dooray_wiki_update_page_referrers

위키 페이지의 참조자를 수정합니다.

### 프로젝트 관련 도구 (1개)

#### 9. dooray_project_list_projects

접근 가능한 프로젝트 목록을 조회합니다.

### 업무 관련 도구 (6개)

#### 10. dooray_project_list_posts

프로젝트의 업무 목록을 조회합니다.

#### 11. dooray_project_get_post

특정 업무의 상세 정보를 조회합니다.

#### 12. dooray_project_create_post

새로운 업무를 생성합니다.

#### 13. dooray_project_update_post

기존 업무를 수정합니다.

#### 14. dooray_project_set_post_workflow

업무의 상태(워크플로우)를 변경합니다.

#### 15. dooray_project_set_post_done

업무를 완료 상태로 변경합니다.

### 업무 댓글 관련 도구 (4개)

#### 16. dooray_project_create_post_comment

업무에 댓글을 생성합니다.

#### 17. dooray_project_get_post_comments

업무의 댓글 목록을 조회합니다.

#### 18. dooray_project_update_post_comment

업무 댓글을 수정합니다.

#### 19. dooray_project_delete_post_comment

업무 댓글을 삭제합니다.

## 사용 예시

### 위키 페이지 조회

```json
{
  "name": "dooray_wiki_list_projects",
  "arguments": {
    "page": 0,
    "size": 20
  }
}
```

### 업무 생성

```json
{
  "name": "dooray_project_create_post",
  "arguments": {
    "project_id": "your_project_id",
    "subject": "새로운 업무",
    "body": "업무 내용",
    "to_member_ids": ["member_id_1", "member_id_2"],
    "priority": "high"
  }
}
```

### 댓글 생성

```json
{
  "name": "dooray_project_create_post_comment",
  "arguments": {
    "project_id": "your_project_id",
    "post_id": "your_post_id",
    "content": "댓글 내용",
    "mime_type": "text/x-markdown"
  }
}
```

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
docker build -t dooray-mcp:local --build-arg VERSION=0.2.1 .
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

| 변수명          | 설명                | 필수 여부 |
| --------------- | ------------------- | --------- |
| DOORAY_API_KEY  | Dooray API 키       | 필수      |
| DOORAY_BASE_URL | Dooray API Base URL | 필수      |

## 라이선스

이 프로젝트는 오픈 소스이며, 자유롭게 사용하실 수 있습니다.

## 기여

프로젝트에 기여하고 싶으시다면 이슈를 등록하거나 풀 리퀘스트를 보내주세요.

## 📚 참고자료

- [두레이 API](https://helpdesk.dooray.com/share/pages/9wWo-xwiR66BO5LGshgVTg/2939987647631384419)
- [Kotlin MCP Server 예제](https://github.com/modelcontextprotocol/kotlin-sdk/blob/main/samples/weather-stdio-server/src/main/kotlin/io/modelcontextprotocol/sample/server/McpWeatherServer.kt)
- [Model Context Protocol](https://modelcontextprotocol.io/introduction)
