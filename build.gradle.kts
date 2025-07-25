plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

application {
    mainClass.set("com.bifos.dooray.mcp.MainKt")
}

group = "com.bifos.dooray.mcp"
version = project.findProperty("project.version") as String? ?: "0.1.5"

repositories {
    mavenCentral()
}

val mcpVersion = project.findProperty("mcp.version") as String? ?: "0.6.0"
val ktorVersion = project.findProperty("ktor.version") as String? ?: "3.1.1"
val logbackVersion = project.findProperty("logback.version") as String? ?: "1.5.18"

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:${mcpVersion}")

    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("io.ktor:ktor-client-logging:${ktorVersion}")

    implementation("ch.qos.logback:logback-classic:${logbackVersion}")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("io.ktor:ktor-client-mock:${ktorVersion}")
    testImplementation("io.mockk:mockk:1.13.10")
}

tasks.test {
    useJUnitPlatform()

    // GitHub Actions 환경에서는 통합 테스트 제외
    if (System.getenv("CI") == "true") {
        exclude("**/*IntegrationTest*")
        println("🚫 CI 환경에서는 통합 테스트를 제외합니다.")
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.register<JavaExec>("runLocal") {
    description = "로컬에서 MCP 서버를 실행합니다 (.env 파일 사용)"
    group = "application"

    // shadowJar 태스크에 의존
    dependsOn("shadowJar")

    // .env 파일에서 환경변수 로드
    doFirst {
        val envFile = file(".env")
        if (envFile.exists()) {
            println("📄 .env 파일에서 환경변수를 로드합니다...")

            envFile.readLines().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                    val parts = trimmedLine.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                        environment(key, value)
                        println("  ✅ $key = $value")
                    }
                }
            }
            println("🚀 MCP 서버를 시작합니다...")
        } else {
            println("⚠️ .env 파일이 없습니다. 환경변수를 수동으로 설정해주세요.")
            println("💡 .env 파일 예시:")
            println("  DOORAY_API_KEY=your_api_key_here")
        }
    }

    // 빌드된 JAR 파일 실행 (동적 버전 사용)
    classpath = files("build/libs/dooray-mcp-server-${version}-all.jar")
    mainClass.set("com.bifos.dooray.mcp.MainKt")

    // 표준 입출력 연결 (MCP 통신용)
    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}
