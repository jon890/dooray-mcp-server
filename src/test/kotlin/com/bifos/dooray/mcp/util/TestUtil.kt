package com.bifos.dooray.mcp.util

import java.io.File

fun parseEnv(): Map<String, String> {
    val env = mutableMapOf<String, String>()

    val envFile = File(".env")
    if (envFile.exists()) {
        println("📄 .env 파일에서 환경변수를 로드합니다...")

        envFile.readLines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                val parts = trimmedLine.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                    env[key] = value
                    println("  ✅ $key = ${value.take(10)}...")
                }
            }
        }
    } else {
        println("⚠️ 환경변수와 .env 파일이 모두 없습니다.")
        println("💡 GitHub Actions에서는 secrets를 설정하고, 로컬에서는 .env 파일을 생성해주세요.")
        println("  .env 파일 예시:")
        println("  DOORAY_API_KEY=your_api_key_here")
        println("  DOORAY_BASE_URL=your_base_url_here")
        println("  DOORAY_PROJECT_ID=your_project_id_here")
    }

    return env
}
