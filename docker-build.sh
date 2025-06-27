#!/bin/bash

# Docker 이미지 빌드 스크립트
set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 변수 설정
IMAGE_NAME="bifos/dooray-mcp"
VERSION=$(grep 'version = ' build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
LATEST_TAG="latest"

echo -e "${BLUE}🐳 Dooray MCP Server Docker 빌드 시작${NC}"
echo -e "${YELLOW}📦 이미지: ${IMAGE_NAME}${NC}"
echo -e "${YELLOW}🏷️  버전: ${VERSION}${NC}"

# .env 파일 확인 및 생성
if [ ! -f ".env" ]; then
    if [ -f ".env.sample" ]; then
        echo -e "${YELLOW}📄 .env 파일이 없습니다. .env.sample을 복사합니다...${NC}"
        cp .env.sample .env
        echo -e "${GREEN}✅ .env 파일이 생성되었습니다. 필요한 값들을 수정해주세요.${NC}"
        echo -e "${BLUE}💡 주요 설정 항목:${NC}"
        echo -e "  - DOORAY_API_KEY: Dooray API 키 (tenant:token 형식)"
        echo -e "  - DOORAY_PROJECT_ID: 프로젝트 ID"
        echo -e "  - DOORAY_BASE_URL: API 기본 URL (기본값: https://api.dooray.com)"
    else
        echo -e "${YELLOW}⚠️  .env와 .env.sample 파일이 모두 없습니다.${NC}"
        echo -e "${BLUE}💡 다음 형식으로 .env 파일을 생성하세요:${NC}"
        cat << 'EOF'
# Dooray MCP Server 환경변수 설정
DOORAY_BASE_URL=https://api.dooray.com
DOORAY_API_KEY=your_tenant:your_api_token
DOORAY_PROJECT_ID=your_project_id
JAVA_OPTS=-Xms128m -Xmx512m
LOG_LEVEL=INFO
EOF
    fi
fi

# Docker 빌드
echo -e "\n${BLUE}🔨 Docker 이미지 빌드 중...${NC}"
docker build -t "${IMAGE_NAME}:${VERSION}" -t "${IMAGE_NAME}:${LATEST_TAG}" .

if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}✅ 빌드 완료!${NC}"
    echo -e "${GREEN}📦 생성된 이미지:${NC}"
    echo -e "  - ${IMAGE_NAME}:${VERSION}"
    echo -e "  - ${IMAGE_NAME}:${LATEST_TAG}"
    
    # 이미지 크기 확인
    echo -e "\n${BLUE}📊 이미지 정보:${NC}"
    docker images "${IMAGE_NAME}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    
    echo -e "\n${GREEN}🚀 실행 방법:${NC}"
    echo -e "  docker run -e DOORAY_API_KEY=your_api_key ${IMAGE_NAME}:${VERSION}"
    
    echo -e "\n${YELLOW}💡 Docker Hub에 푸시하려면 다음 명령어를 실행하세요:${NC}"
    echo -e "  ./docker-push.sh"
else
    echo -e "\n${RED}❌ 빌드 실패!${NC}"
    exit 1
fi 