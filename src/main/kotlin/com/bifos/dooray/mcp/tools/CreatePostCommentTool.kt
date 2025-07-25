package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.CreateCommentRequest
import com.bifos.dooray.mcp.types.PostCommentBody
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

fun createPostCommentTool(): Tool {
    return Tool(
        name = "dooray_project_create_post_comment",
        description = "두레이 프로젝트 업무에 댓글을 생성합니다. 마크다운 또는 HTML 형식으로 댓글을 작성할 수 있습니다.",
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
                        putJsonObject("content") {
                            put("type", "string")
                            put("description", "댓글 내용")
                        }
                        putJsonObject("mime_type") {
                            put("type", "string")
                            put(
                                "description",
                                "MIME 타입 (text/x-markdown 또는 text/html, 기본값: text/x-markdown)"
                            )
                            put("default", "text/x-markdown")
                        }
                    },
                required = listOf("project_id", "post_id", "content")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun createPostCommentHandler(
    doorayClient: DoorayClient
): suspend (CallToolRequest) -> CallToolResult {
    return handler@{ request ->
        try {
            val projectId = request.arguments["project_id"]?.jsonPrimitive?.content
            val postId = request.arguments["post_id"]?.jsonPrimitive?.content
            val content = request.arguments["content"]?.jsonPrimitive?.content
            val mimeType =
                request.arguments["mime_type"]?.jsonPrimitive?.content ?: "text/x-markdown"

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

            if (content.isNullOrBlank()) {
                val errorResponse =
                    ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "content 파라미터가 필요합니다.",
                        code = "MISSING_CONTENT"
                    )
                        .toErrorResponse()

                return@handler CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                )
            }

            val createRequest =
                CreateCommentRequest(
                    body = PostCommentBody(mimeType = mimeType, content = content)
                )

            val response = doorayClient.createPostComment(projectId, postId, createRequest)

            if (response.header.isSuccessful) {
                val successResponse =
                    ToolSuccessResponse(
                        data = response.result,
                        message = "업무 댓글이 성공적으로 생성되었습니다. (댓글 ID: ${response.result.id})"
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
