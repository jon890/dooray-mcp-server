package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayHttpClient
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun runDoorayMcpServer() {
    // 서버 시작 로그를 stderr로 출력 (stdout은 MCP 통신용이므로)
    System.err.println("Dooray MCP Server starting...")

    val baseUrl =
        System.getenv("DOORAY_BASE_URL")
            ?: throw IllegalArgumentException("DOORAY_BASE_URL is required.")
    val apiKey =
        System.getenv("DOORAY_API_KEY")
            ?: throw IllegalArgumentException("DOORAY_API_KEY is required.")

    val doorayHttpClient = DoorayHttpClient(baseUrl = baseUrl, doorayApiKey = apiKey)

    System.err.println("DOORAY_API_KEY found, initializing HTTP client...")

    val server =
        Server(
            Implementation(name = "dooray-mcp", version = "0.1.0"),
            ServerOptions(
                capabilities =
                    ServerCapabilities(
                        tools = ServerCapabilities.Tools(listChanged = true)
                    )
            )
        )

    System.err.println("Adding tools...")

    server.addTool(
        name = "get_wiki_pages",
        description =
            """
            Get wiki pages for a specific wiki ID.
            Returns the list of wiki pages with optional parent page filtering.
        """.trimIndent(),
        inputSchema =
            Tool.Input(
                properties =
                    buildJsonObject {
                        putJsonObject("projectId") {
                            put("type", "string")
                            put("description", "두레이 프로젝트 ID")
                        }
                        putJsonObject("parentPageId") {
                            put("type", "string")
                            put(
                                "description",
                                "상위 페이지 ID (선택사항, null이면 최상위 페이지들 조회)"
                            )
                        }
                    },
                required = listOf("projectId")
            )
    ) { request ->
        val projectId = request.arguments["projectId"]?.jsonPrimitive?.content
        if (projectId == null) {
            return@addTool CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "The 'projectId' parameter is required."
                        )
                    )
            )
        }

        try {
            val response = doorayHttpClient.getWikiPages(projectId)

            if (response.header.isSuccessful && response.result != null) {
                val pageList =
                    response.result.joinToString("\n") { page ->
                        "- ${page.subject} (ID: ${page.id}, Version: ${page.version})"
                    }
                CallToolResult(
                    content = listOf(TextContent("위키 페이지 목록:\n$pageList"))
                )
            } else {
                CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                "API 호출 실패 (${response.header.resultCode}): ${response.header.resultMessage}"
                            )
                        )
                )
            }
        } catch (e: Exception) {
            CallToolResult(content = listOf(TextContent("오류 발생: ${e.message}")))
        }
    }

    // Create a transport using standard IO for server communication
    val transport = StdioServerTransport(System.`in`.asInput(), System.out.asSink().buffered())

    System.err.println("Starting MCP server on STDIO transport...")

    runBlocking {
        server.connect(transport)
        System.err.println("MCP server connected and ready!")

        val done = Job()
        server.onClose {
            System.err.println("MCP server closing...")
            done.complete()
        }
        done.join()
    }
}
