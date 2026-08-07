#!/bin/bash

# Docker 이미지 빌드 스크립트
set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 프로젝트 루트로 이동
cd "$(dirname "$0")/.."

# 변수 설정
IMAGE_NAME="bifos/dooray-mcp"
# Gradle에서 버전 추출
VERSION=$(./gradlew properties --no-daemon --console=plain -q | grep "^version:" | awk '{print $2}')
TAG_LATEST="false"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --tag-latest)
            TAG_LATEST="true"
            shift
            ;;
        *)
            echo -e "${RED}알 수 없는 옵션: $1${NC}"
            exit 1
            ;;
    esac
done

if [[ "${TAG_LATEST}" == "true" && "${VERSION}" == *-* ]]; then
    echo -e "${RED}prerelease 버전에는 latest 태그를 만들 수 없습니다: ${VERSION}${NC}"
    exit 1
fi

echo -e "${BLUE}🐳 Dooray MCP Server Docker 빌드 시작${NC}"
echo -e "${YELLOW}📦 이미지: ${IMAGE_NAME}${NC}"
echo -e "${YELLOW}🏷️  버전: ${VERSION} (build.gradle.kts에서 추출)${NC}"

# Docker 빌드 (VERSION build arg 전달)
echo -e "\n${BLUE}🔨 Docker 이미지 빌드 중...${NC}"
docker build \
  --build-arg VERSION="${VERSION}" \
  -t "${IMAGE_NAME}:${VERSION}" \
  .

if [[ "${TAG_LATEST}" == "true" ]]; then
    docker tag "${IMAGE_NAME}:${VERSION}" "${IMAGE_NAME}:latest"
fi

if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}✅ 빌드 완료!${NC}"
    echo -e "${GREEN}📦 생성된 이미지:${NC}"
    echo -e "  - ${IMAGE_NAME}:${VERSION}"
    if [[ "${TAG_LATEST}" == "true" ]]; then
        echo -e "  - ${IMAGE_NAME}:latest"
    fi
    
    # 이미지 크기 확인
    echo -e "\n${BLUE}📊 이미지 정보:${NC}"
    docker images "${IMAGE_NAME}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    
    # 이미지 라벨 확인
    echo -e "\n${BLUE}🏷️  이미지 라벨:${NC}"
    docker inspect "${IMAGE_NAME}:${VERSION}" --format='{{range $k, $v := .Config.Labels}}{{$k}}: {{$v}}{{println}}{{end}}'
    
    echo -e "\n${GREEN}🚀 실행 방법:${NC}"
    echo -e "  docker run -e DOORAY_API_KEY=your_api_key ${IMAGE_NAME}:${VERSION}"
    
    echo -e "\n${YELLOW}💡 Docker Hub에 푸시하려면 다음 명령어를 실행하세요:${NC}"
    echo -e "  ./scripts/docker-push.sh"
else
    echo -e "\n${RED}❌ 빌드 실패!${NC}"
    exit 1
fi
