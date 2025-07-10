package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun updateWikiPageContentTool(): Tool {
    return Tool(
        name = "dooray_wiki_update_page_content",
        description = "두레이 위키 페이지의 내용만 수정합니다. 내용만 변경하고 제목은 그대로 유지됩니다.",
        inputSchema =
            Tool.Input(
                properties =
                    buildJsonObject {
                        putJsonObject("wiki_id") {
                            put("type", "string")
                            put(
                                "description",
                                "위키 ID (dooray_wiki_list_projects로 조회 가능)"
                            )
                        }
                        putJsonObject("page_id") {
                            put("type", "string")
                            put(
                                "description",
                                "수정할 위키 페이지 ID (dooray_wiki_list_pages로 조회 가능)"
                            )
                        }
                        putJsonObject("content") {
                            put("type", "string")
                            put("description", "새로운 위키 페이지 내용 (Markdown 형식 지원)")
                        }
                    },
                required = listOf("wiki_id", "page_id", "content")
            )
    )
}

fun updateWikiPageContentHandler(
    doorayClient: DoorayClient
): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val wikiId = request.arguments["wiki_id"]?.jsonPrimitive?.content
            val pageId = request.arguments["page_id"]?.jsonPrimitive?.content
            val content = request.arguments["content"]?.jsonPrimitive?.content

            when {
                wikiId == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message =
                                "wiki_id 파라미터가 필요합니다. dooray_wiki_list_projects를 사용해서 위키 ID를 먼저 조회하세요.",
                            code = "MISSING_WIKI_ID"
                        )
                            .toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }

                pageId == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message =
                                "page_id 파라미터가 필요합니다. dooray_wiki_list_pages를 사용해서 페이지 ID를 먼저 조회하세요.",
                            code = "MISSING_PAGE_ID"
                        )
                            .toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }

                content == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message = "content 파라미터가 필요합니다. 새로운 위키 페이지 내용을 입력하세요.",
                            code = "MISSING_CONTENT"
                        )
                            .toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }

                else -> {
                    val response = doorayClient.updateWikiPageContent(wikiId, pageId, content)

                    if (response.header.isSuccessful) {
                        val successResponse =
                            ToolSuccessResponse(
                                data =
                                    mapOf(
                                        "wiki_id" to wikiId,
                                        "page_id" to pageId,
                                        "content_length" to content.length,
                                        "updated" to true
                                    ),
                                message = "✅ 위키 페이지의 내용을 성공적으로 수정했습니다 (${content.length}자)"
                            )

                        CallToolResult(
                            content =
                                listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse =
                            ToolException(
                                type = ToolException.API_ERROR,
                                message = response.header.resultMessage,
                                code = "DOORAY_API_${response.header.resultCode}"
                            )
                                .toErrorResponse()

                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                        )
                    }
                }
            }
        } catch (e: Exception) {
            val errorResponse =
                ToolException(
                    type = ToolException.INTERNAL_ERROR,
                    message = "내부 오류가 발생했습니다: ${e.message}",
                    details = e.stackTraceToString()
                )
                    .toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
