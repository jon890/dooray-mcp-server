package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.types.UpdateWikiPageRequest
import com.bifos.dooray.mcp.types.WikiPageBody
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun updateWikiPageTool(): Tool {
    return Tool(
            name = "dooray_wiki_update_page",
            description = "기존 두레이 위키 페이지를 수정합니다. 제목, 내용, 상위 페이지 등을 변경할 수 있습니다.",
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
                                            put("description", "새로운 위키 페이지 제목 (선택사항)")
                                        }
                                        putJsonObject("body") {
                                            put("type", "string")
                                            put(
                                                    "description",
                                                    "새로운 위키 페이지 내용 (Markdown 형식 지원, 선택사항)"
                                            )
                                        }
                                        putJsonObject("parent_page_id") {
                                            put("type", "string")
                                            put("description", "새로운 상위 페이지 ID (선택사항)")
                                        }
                                    },
                            required = listOf("wiki_id", "page_id")
                    )
    )
}

fun updateWikiPageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val wikiId = request.arguments["wiki_id"]?.jsonPrimitive?.content
            val pageId = request.arguments["page_id"]?.jsonPrimitive?.content
            val subject = request.arguments["subject"]?.jsonPrimitive?.content
            val bodyContent = request.arguments["body"]?.jsonPrimitive?.content
            val parentPageId = request.arguments["parent_page_id"]?.jsonPrimitive?.content

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
                subject == null && bodyContent == null && parentPageId == null -> {
                    val errorResponse =
                            ToolException(
                                            type = ToolException.VALIDATION_ERROR,
                                            message =
                                                    "수정할 내용이 없습니다. subject, body, parent_page_id 중 적어도 하나는 제공해야 합니다.",
                                            code = "NO_UPDATE_CONTENT"
                                    )
                                    .toErrorResponse()

                    CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val updateRequest =
                            UpdateWikiPageRequest(
                                    subject = subject,
                                    body =
                                            bodyContent?.let {
                                                WikiPageBody(
                                                        mimeType = "text/x-markdown",
                                                        content = it
                                                )
                                            },
                                    parentPageId = parentPageId
                            )

                    val response = doorayClient.updateWikiPage(wikiId, pageId, updateRequest)

                    if (response.header.isSuccessful) {
                        val updateParts = mutableListOf<String>()
                        if (subject != null) updateParts.add("제목")
                        if (bodyContent != null) updateParts.add("내용")
                        if (parentPageId != null) updateParts.add("상위 페이지")

                        val updatedFields = updateParts.joinToString(", ")

                        val successResponse =
                                ToolSuccessResponse(
                                        data = response.result,
                                        message =
                                                "✅ 위키 페이지 '${response.result.subject}'의 $updatedFields 을(를) 성공적으로 수정했습니다"
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
