plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
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

dependencies {
    implementation(libs.mcp.kotlin.sdk)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()

    // GitHub Actions 환경에서는 통합 테스트 제외
    if (System.getenv("CI") == "true") {
        exclude("**/*IntegrationTest*")
        println("CI 환경에서는 통합 테스트를 제외합니다.")
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.register<JavaExec>("runLocal") {
    description = "로컬에서 MCP 서버를 실행합니다 (.env 파일 사용)"
    group = "application"

    dependsOn("shadowJar")

    doFirst {
        val envFile = file(".env")
        if (envFile.exists()) {
            println(".env 파일에서 환경변수를 로드합니다...")

            envFile.readLines().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                    val parts = trimmedLine.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                        environment(key, value)
                    }
                }
            }
            println("MCP 서버를 시작합니다...")
        } else {
            println(".env 파일이 없습니다. 환경변수를 수동으로 설정해주세요.")
        }
    }

    classpath = files("build/libs/dooray-mcp-server-${version}-all.jar")
    mainClass.set("com.bifos.dooray.mcp.MainKt")

    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}
