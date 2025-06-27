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

fun getWikisTool(): Tool {
    return Tool(
        name = "get_wikis",
        description = "접근 가능한 위키 목록을 조회합니다. 프로젝트 이름으로 프로젝트 ID를 찾을 때 유용합니다.",
        inputSchema =
            Tool.Input(
                properties =
                    buildJsonObject {
                        putJsonObject("page") {
                            put("type", "number")
                            put("description", "페이지 번호 (선택사항)")
                        }
                        putJsonObject("size") {
                            put("type", "number")
                            put("description", "페이지 크기 (선택사항)")
                        }
                    }
            )
    )
}

fun getWikisHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull()
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull()

            val response = doorayClient.getWikis(page, size)

            if (response.header.isSuccessful) {
                val successResponse =
                    ToolSuccessResponse(
                        data = response.result,
                        message = "접근 가능한 위키 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)"
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

                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
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
