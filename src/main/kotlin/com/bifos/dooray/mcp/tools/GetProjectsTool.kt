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

fun getProjectsTool(): Tool {
    return Tool(
            name = "dooray_project_list_projects",
            description = "두레이에서 접근 가능한 프로젝트 목록을 조회합니다. 다양한 필터 조건으로 원하는 프로젝트를 찾을 수 있습니다.",
            inputSchema =
                    Tool.Input(
                            properties =
                                    buildJsonObject {
                                        putJsonObject("page") {
                                            put("type", "integer")
                                            put("description", "페이지 번호 (0부터 시작, 기본값: 0)")
                                            put("default", 0)
                                        }
                                        putJsonObject("size") {
                                            put("type", "integer")
                                            put("description", "페이지 크기 (최대 100, 기본값: 20, 권장: 100)")
                                            put("default", 20)
                                            put("maximum", 100)
                                        }
                                        putJsonObject("type") {
                                            put("type", "string")
                                            put(
                                                    "description",
                                                    "프로젝트 타입 (public: 일반 프로젝트, private: 개인 프로젝트, 기본값: public)"
                                            )
                                            put("default", "public")
                                        }
                                        putJsonObject("scope") {
                                            put("type", "string")
                                            put(
                                                    "description",
                                                    "접근 범위 (private: 프로젝트 멤버만 접근, public: 조직 멤버 누구나 접근, 기본값: private)"
                                            )
                                            put("default", "private")
                                        }
                                        putJsonObject("state") {
                                            put("type", "string")
                                            put(
                                                    "description",
                                                    "프로젝트 상태 (active: 활성화, archived: 보관됨, deleted: 삭제됨)"
                                            )
                                        }
                                    }
                    ),
            outputSchema = null,
            annotations = null
    )
}

fun getProjectsHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull()
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull()
            val type = request.arguments["type"]?.jsonPrimitive?.content
            val scope = request.arguments["scope"]?.jsonPrimitive?.content
            val state = request.arguments["state"]?.jsonPrimitive?.content

            val response = doorayClient.getProjects(page, size, type, scope, state)

            if (response.header.isSuccessful) {
                val successResponse =
                        ToolSuccessResponse(
                                data =
                                        buildJsonObject {
                                            put(
                                                    "projects",
                                                    JsonUtils.toJsonElement(response.result)
                                            )
                                            put("totalCount", response.totalCount)
                                            put("currentPage", page ?: 0)
                                            put("pageSize", size ?: 20)
                                            put(
                                                    "filters",
                                                    buildJsonObject {
                                                        type?.let { put("type", it) }
                                                        scope?.let { put("scope", it) }
                                                        state?.let { put("state", it) }
                                                    }
                                            )
                                        },
                                message =
                                        "프로젝트 목록을 성공적으로 조회했습니다 (총 ${response.totalCount}개 중 ${response.result.size}개 조회)"
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
