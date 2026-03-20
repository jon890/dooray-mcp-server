package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.service.ProjectResolver
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun deletePostCommentTool(): Tool {
    return Tool(
        name = "dooray_project_delete_post_comment",
        description = "두레이 프로젝트 업무의 댓글을 삭제합니다.",
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
                    },
                required = listOf("project_id", "post_id", "log_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun deletePostCommentHandler(
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

            val response = doorayClient.deletePostComment(projectId, postId, logId)

            if (response.header.isSuccessful) {
                successResult(message = "업무 댓글이 성공적으로 삭제되었습니다.")
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
