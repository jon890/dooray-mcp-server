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
        name = "dooray_wiki_list_projects",
        description = "두레이에서 접근 가능한 위키 프로젝트 목록을 조회합니다. 특정 프로젝트의 이름으로 프로젝트 ID를 찾을 때 사용하세요.",
        inputSchema =
            Tool.Input(
                properties =
                    buildJsonObject {
                        putJsonObject("page") {
                            put("type", "integer")
                            put("description", "조회할 페이지 번호 (0부터 시작, 기본값: 0)")
                            put("default", 0)
                        }
                        putJsonObject("size") {
                            put("type", "integer")
                            put("description", "한 페이지당 결과 수 (기본값: 200)")
                            put("default", 200)
                        }
                    }
            ),
        outputSchema = null,
        annotations = null
    )
}

fun getWikisHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            // 기본값 처리: page는 0, size는 200
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull() ?: 200

            val response = doorayClient.getWikis(page, size)

            if (response.header.isSuccessful) {
                val pageInfo = if (page == 0) "첫 번째 페이지" else "${page + 1}번째 페이지"

                // 다음 단계 제안 메시지
                val nextStepHint =
                    if (response.result.isNotEmpty()) {
                        "\n\n💡 다음 단계: 특정 프로젝트의 위키 페이지들을 보려면 dooray_wiki_list_pages를 사용하세요."
                    } else {
                        if (page == 0) "\n\n📋 조회 결과가 없습니다. 접근 권한을 확인해주세요."
                        else "\n\n📄 더 이상 프로젝트가 없습니다."
                    }

                val successResponse =
                    ToolSuccessResponse(
                        data = response.result,
                        message =
                            "📚 두레이 위키 프로젝트 목록을 성공적으로 조회했습니다 ($pageInfo, 총 ${response.result.size}개)$nextStepHint"
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
