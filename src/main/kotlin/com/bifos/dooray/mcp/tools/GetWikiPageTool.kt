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

fun getWikiPageTool(): Tool {
    return Tool(
        name = "get_wiki_page",
        description = "특정 위키 페이지의 상세 정보를 조회합니다. 페이지 내용, 작성자, 생성/수정 날짜 등을 확인할 수 있습니다.",
        inputSchema =
            Tool.Input(
                properties =
                    buildJsonObject {
                        putJsonObject("projectId") {
                            put("type", "string")
                            put("description", "프로젝트 ID")
                        }
                        putJsonObject("pageId") {
                            put("type", "string")
                            put("description", "조회할 위키 페이지 ID")
                        }
                    },
                required = listOf("projectId", "pageId")
            )
    )
}

fun getWikiPageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val projectId = request.arguments["projectId"]?.jsonPrimitive?.content
            val pageId = request.arguments["pageId"]?.jsonPrimitive?.content

            when {
                projectId == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message = "projectId 파라미터가 필요합니다.",
                            code = "MISSING_PROJECT_ID"
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
                            message = "pageId 파라미터가 필요합니다.",
                            code = "MISSING_PAGE_ID"
                        )
                            .toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }

                else -> {
                    val response = doorayClient.getWikiPage(projectId, pageId)

                    if (response.header.isSuccessful) {
                        val successResponse =
                            ToolSuccessResponse(
                                data = response.result,
                                message =
                                    "위키 페이지 '${response.result.subject}'를 성공적으로 조회했습니다"
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
