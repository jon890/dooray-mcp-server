package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getWikiPagesTool(): Tool {
    return Tool(
        name = "get_wiki_pages",
        description = """
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
    )
}

fun getWikiPagesHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        val projectId = request.arguments["projectId"]?.jsonPrimitive?.content
        if (projectId == null) {
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "The 'projectId' parameter is required."
                        )
                    )
            )
        }

        try {
            val response = doorayClient.getWikiPages(projectId!!)

            if (response.header.isSuccessful) {
                val wikiPages = response.result
                CallToolResult(
                    content = wikiPages.map { it -> TextContent(it.toString()) }
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
}