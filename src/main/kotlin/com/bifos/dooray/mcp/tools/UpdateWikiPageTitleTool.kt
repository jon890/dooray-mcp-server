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

fun updateWikiPageTitleTool(): Tool {
    return Tool(
            name = "dooray_wiki_update_page_title",
            description = "두레이 위키 페이지의 제목만 수정합니다. 제목만 변경하고 다른 내용은 그대로 유지됩니다.",
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
                                        putJsonObject("subject") {
                                            put("type", "string")
                                            put("description", "새로운 위키 페이지 제목")
                                        }
                                    },
                            required = listOf("wiki_id", "page_id", "subject")
                    )
    )
}

fun updateWikiPageTitleHandler(
        doorayClient: DoorayClient
): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val wikiId = request.arguments["wiki_id"]?.jsonPrimitive?.content
            val pageId = request.arguments["page_id"]?.jsonPrimitive?.content
            val subject = request.arguments["subject"]?.jsonPrimitive?.content

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
                subject == null -> {
                    val errorResponse =
                            ToolException(
                                            type = ToolException.PARAMETER_MISSING,
                                            message = "subject 파라미터가 필요합니다. 새로운 위키 페이지 제목을 입력하세요.",
                                            code = "MISSING_SUBJECT"
                                    )
                                    .toErrorResponse()

                    CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                subject.isBlank() -> {
                    val errorResponse =
                            ToolException(
                                            type = ToolException.VALIDATION_ERROR,
                                            message = "제목은 공백일 수 없습니다. 유효한 제목을 입력하세요.",
                                            code = "EMPTY_SUBJECT"
                                    )
                                    .toErrorResponse()

                    CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val response = doorayClient.updateWikiPageTitle(wikiId, pageId, subject)

                    if (response.header.isSuccessful) {
                        val successResponse =
                                ToolSuccessResponse(
                                        data =
                                                mapOf(
                                                        "wiki_id" to wikiId,
                                                        "page_id" to pageId,
                                                        "new_subject" to subject,
                                                        "updated" to true
                                                ),
                                        message = "✅ 위키 페이지의 제목을 '$subject'로 성공적으로 수정했습니다"
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
