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

fun getWikiPagesTool(): Tool {
    return Tool(
        name = "get_wiki_pages",
        description =
            """
            특정 프로젝트의 위키 페이지 목록을 조회합니다.
            상위 페이지 ID를 지정하면 해당 페이지의 하위 페이지들을 조회할 수 있습니다.
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
        try {
            val projectId = request.arguments["projectId"]?.jsonPrimitive?.content
            if (projectId == null) {
                val errorResponse =
                    ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "projectId 파라미터가 필요합니다.",
                        code = "MISSING_PROJECT_ID"
                    )
                        .toErrorResponse()

                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
            } else {
                val parentPageId = request.arguments["parentPageId"]?.jsonPrimitive?.content

                val response =
                    if (parentPageId != null) {
                        doorayClient.getWikiPages(projectId, parentPageId)
                    } else {
                        doorayClient.getWikiPages(projectId)
                    }

                if (response.header.isSuccessful) {
                    val successResponse =
                        ToolSuccessResponse(
                            data = response.result,
                            message =
                                if (parentPageId != null) {
                                    "하위 위키 페이지 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)"
                                } else {
                                    "위키 페이지 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)"
                                }
                        )

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
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
