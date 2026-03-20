package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.types.PostCommentBody
import com.bifos.dooray.mcp.types.UpdateCommentRequest
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun updatePostCommentTool(): Tool {
    return Tool(
        name = "dooray_project_update_post_comment",
        description = "두레이 프로젝트 업무의 댓글을 수정합니다. 이메일로 발송된 댓글은 수정할 수 없습니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        putJsonObject("project_id") {
                            put("type", "string")
                            put("description", "프로젝트 ID 또는 프로젝트 코드 (예: 'my-project' 또는 숫자 ID). 프로젝트 코드는 dooray_project_list_projects로 확인 가능합니다.")
                        }
                        putJsonObject("post_id") {
                            put("type", "string")
                            put("description", "업무 ID (dooray_project_list_posts로 조회 가능)")
                        }
                        putJsonObject("log_id") {
                            put("type", "string")
                            put("description", "댓글 ID (dooray_project_get_post_comments로 조회 가능)")
                        }
                        putJsonObject("content") {
                            put("type", "string")
                            put("description", "수정할 댓글 내용")
                        }
                        putJsonObject("mime_type") {
                            put("type", "string")
                            put("description", "MIME 타입 (text/x-markdown 또는 text/html, 기본값: text/x-markdown)")
                            put("default", "text/x-markdown")
                        }
                    },
                required = listOf("project_id", "post_id", "log_id", "content")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun updatePostCommentHandler(
    doorayClient: DoorayClient,
    projectResolver: ProjectResolver
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val projectId = projectResolver.resolveProjectId(
                request.requireParam("project_id", "MISSING_PROJECT_ID", "project_id 파라미터가 필요합니다.")
            )
            val postId = request.requireParam("post_id", "MISSING_POST_ID", "post_id 파라미터가 필요합니다.")
            val logId = request.requireParam("log_id", "MISSING_LOG_ID", "log_id 파라미터가 필요합니다.")
            val content = request.requireParam("content", "MISSING_CONTENT", "content 파라미터가 필요합니다.")
            val mimeType = request.arguments?.get("mime_type")?.jsonPrimitive?.content ?: "text/x-markdown"

            val response = doorayClient.updatePostComment(
                projectId, postId, logId,
                UpdateCommentRequest(body = PostCommentBody(mimeType = mimeType, content = content))
            )

            if (response.header.isSuccessful) {
                successResult(message = "업무 댓글이 성공적으로 수정되었습니다.")
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
