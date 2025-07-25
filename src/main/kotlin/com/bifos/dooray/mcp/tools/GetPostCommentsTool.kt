package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.PostCommentsResponseData
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

fun getPostCommentsTool(): Tool {
    return Tool(
            name = "dooray_project_get_post_comments",
            description = "두레이 프로젝트 업무의 댓글 목록을 조회합니다. 페이징과 정렬 옵션을 지원합니다.",
            inputSchema =
                    Tool.Input(
                            properties =
                                    buildJsonObject {
                                        putJsonObject("project_id") {
                                            put("type", "string")
                                            put(
                                                    "description",
                                                    "프로젝트 ID (dooray_project_list_projects로 조회 가능)"
                                            )
                                        }
                                        putJsonObject("post_id") {
                                            put("type", "string")
                                            put(
                                                    "description",
                                                    "업무 ID (dooray_project_list_posts로 조회 가능)"
                                            )
                                        }
                                        putJsonObject("page") {
                                            put("type", "number")
                                            put("description", "페이지 번호 (0부터 시작, 기본값: 0)")
                                            put("default", 0)
                                        }
                                        putJsonObject("size") {
                                            put("type", "number")
                                            put("description", "페이지 크기 (최대 100, 기본값: 20)")
                                            put("default", 20)
                                        }
                                        putJsonObject("order") {
                                            put("type", "string")
                                            put(
                                                    "description",
                                                    "정렬 조건 (createdAt: 오래된순, -createdAt: 최신순, 기본값: createdAt)"
                                            )
                                            put("default", "createdAt")
                                        }
                                    },
                            required = listOf("project_id", "post_id")
                    ),
            outputSchema = null,
            annotations = null
    )
}

fun getPostCommentsHandler(
        doorayClient: DoorayClient
): suspend (CallToolRequest) -> CallToolResult {
    return handler@{ request ->
        try {
            val projectId = request.arguments["project_id"]?.jsonPrimitive?.content
            val postId = request.arguments["post_id"]?.jsonPrimitive?.content
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull()
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull()
            val order = request.arguments["order"]?.jsonPrimitive?.content

            if (projectId.isNullOrBlank()) {
                val errorResponse =
                        ToolException(
                                        type = ToolException.PARAMETER_MISSING,
                                        message = "project_id 파라미터가 필요합니다.",
                                        code = "MISSING_PROJECT_ID"
                                )
                                .toErrorResponse()

                return@handler CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                )
            }

            if (postId.isNullOrBlank()) {
                val errorResponse =
                        ToolException(
                                        type = ToolException.PARAMETER_MISSING,
                                        message = "post_id 파라미터가 필요합니다.",
                                        code = "MISSING_POST_ID"
                                )
                                .toErrorResponse()

                return@handler CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                )
            }

            val response = doorayClient.getPostComments(projectId, postId, page, size, order)

            if (response.header.isSuccessful) {
                val successResponse =
                        ToolSuccessResponse(
                                data =
                                        PostCommentsResponseData(
                                                comments = response.result,
                                                totalCount = response.totalCount,
                                                currentPage = page ?: 0,
                                                pageSize = size ?: 20
                                        ),
                                message =
                                        "업무 댓글 목록을 성공적으로 조회했습니다. (총 ${response.totalCount}개, 현재 페이지: ${response.result.size}개)"
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
