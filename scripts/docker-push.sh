#!/bin/bash

# Docker Hub 푸시 스크립트
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
PUSH_LATEST="false"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --push-latest)
            PUSH_LATEST="true"
            shift
            ;;
        *)
            echo -e "${RED}알 수 없는 옵션: $1${NC}"
            exit 1
            ;;
    esac
done

if [[ "${PUSH_LATEST}" == "true" && "${VERSION}" == *-* ]]; then
    echo -e "${RED}prerelease 버전에는 latest 태그를 푸시할 수 없습니다: ${VERSION}${NC}"
    exit 1
fi

echo -e "${BLUE}🚀 Docker Hub에 이미지 푸시 시작${NC}"
echo -e "${YELLOW}📦 이미지: ${IMAGE_NAME}${NC}"
echo -e "${YELLOW}🏷️  버전: ${VERSION}${NC}"

# Docker Hub 로그인 확인
echo -e "\n${BLUE}🔐 Docker Hub 로그인 상태 확인...${NC}"
if ! docker info | grep -q "Username"; then
    echo -e "${YELLOW}⚠️  Docker Hub에 로그인이 필요합니다.${NC}"
    echo -e "${BLUE}💡 다음 명령어로 로그인하세요: docker login${NC}"
    exit 1
fi

# 이미지 존재 확인
if ! docker images "${IMAGE_NAME}:${VERSION}" --format "{{.Repository}}" | grep -q "${IMAGE_NAME}"; then
    echo -e "${RED}❌ 이미지 ${IMAGE_NAME}:${VERSION}가 존재하지 않습니다.${NC}"
    echo -e "${YELLOW}💡 먼저 빌드를 실행하세요: ./scripts/docker-build.sh${NC}"
    exit 1
fi

# 버전 태그 푸시
echo -e "\n${BLUE}📤 버전 태그 푸시 중: ${VERSION}${NC}"
docker push "${IMAGE_NAME}:${VERSION}"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 버전 태그 푸시 완료: ${VERSION}${NC}"
else
    echo -e "${RED}❌ 버전 태그 푸시 실패: ${VERSION}${NC}"
    exit 1
fi

if [[ "${PUSH_LATEST}" == "true" ]]; then
    # latest 태그 푸시
    echo -e "\n${BLUE}📤 latest 태그 푸시 중...${NC}"
    docker push "${IMAGE_NAME}:latest"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ latest 태그 푸시 완료!${NC}"
    else
        echo -e "${RED}❌ latest 태그 푸시 실패!${NC}"
        exit 1
    fi
fi

echo -e "\n${GREEN}🎉 요청한 이미지 푸시 완료!${NC}"
echo -e "${GREEN}🌐 Docker Hub에서 확인: https://hub.docker.com/r/${IMAGE_NAME}${NC}"

echo -e "\n${BLUE}📋 사용 방법:${NC}"
echo -e "  docker pull ${IMAGE_NAME}:${VERSION}"
echo -e "  docker run -e DOORAY_API_KEY=your_api_key ${IMAGE_NAME}:${VERSION}"
