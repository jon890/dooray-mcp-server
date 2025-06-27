package com.bifos.dooray.mcp

import java.io.File

fun parseEnv(): Map<String, String>{
    val envFile = File(".env")
    val env = mutableMapOf<String, String>()

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
                    println("  ✅ $key = $value")
                }
            }
        }
    } else {
        println("⚠️ .env 파일이 없습니다. 환경변수를 수동으로 설정해주세요.")
        println("💡 .env 파일 예시:")
        println("  DOORAY_API_KEY=your_api_key_here")
    }

    return env
}