package com.bifos.dooray.mcp

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
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
        println("🚀 MCP 서버를 시작합니다...")
    } else {
        println("⚠️ .env 파일이 없습니다. 환경변수를 수동으로 설정해주세요.")
        println("💡 .env 파일 예시:")
        println("  DOORAY_API_KEY=your_api_key_here")
    }

    return env
}


fun main(): Unit = runBlocking {
    val env = parseEnv()

    val processBuilder = ProcessBuilder("java", "-jar", "build/libs/dooray-mcp-server-0.1.0-all.jar")
    processBuilder.environment().putAll(env)
    val process = processBuilder.start()

    val transport = StdioClientTransport(
        input = process.inputStream.asSource().buffered(),
        output = process.outputStream.asSink().buffered()
    )

    // Initialize the MCP client with client information
    val client = Client(
        clientInfo = Implementation(name = "Dooray MCP Server", version = "0.1.0"),
    )

    client.connect(transport)

    val toolsList = client.listTools()?.tools?.map { it.name }
    println("Available Tools = $toolsList")
}