# 멀티스테이지 빌드: 빌드 스테이지
FROM gradle:8.10-jdk21 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 래퍼와 설정 파일 복사
COPY gradle/ gradle/
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts ./

# 빌드 스크립트 복사
COPY build.gradle.kts ./

# 의존성 다운로드 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src/ src/

# shadowJar 빌드
RUN ./gradlew clean shadowJar --no-daemon

# 런타임 스테이지
FROM eclipse-temurin:21-jre-alpine

# 버전을 빌드 인수로 받기
ARG VERSION=0.1.0
ENV APP_VERSION=${VERSION}

# 메타데이터 라벨
LABEL maintainer="bifos"
LABEL description="Dooray MCP Server - Model Context Protocol server for NHN Dooray"
LABEL version="${APP_VERSION}"

# 비루트 사용자 생성
RUN addgroup -g 1000 dooray && \
    adduser -D -s /bin/sh -u 1000 -G dooray dooray

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사 (버전 변수 사용)
COPY --from=builder /app/build/libs/dooray-mcp-server-${VERSION}-all.jar app.jar

# 파일 소유권 변경
RUN chown -R dooray:dooray /app

# 비루트 사용자로 전환
USER dooray

# 헬스체크 (MCP 서버는 STDIO 통신이므로 프로세스 존재 여부만 확인)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD pgrep -f "java.*app.jar" > /dev/null || exit 1

# 환경변수 기본값 설정
ENV JAVA_OPTS="-Xms128m -Xmx512m"

# 포트 노출 (MCP는 STDIO 통신이지만 문서화 목적)
EXPOSE 8080

# 엔트리포인트 설정
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# 기본 명령어
CMD [] 