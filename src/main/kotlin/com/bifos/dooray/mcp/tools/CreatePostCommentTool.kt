package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.types.CreateCommentRequest
import com.bifos.dooray.mcp.types.PostCommentBody
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun createPostCommentTool(): Tool {
    return Tool(
        name = "dooray_project_create_post_comment",
        description = "두레이 프로젝트 업무에 댓글을 생성합니다. 마크다운 또는 HTML 형식으로 댓글을 작성할 수 있습니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        projectIdProperty()
                        postIdProperty()
                        putJsonObject("content") {
                            put("type", "string")
                            put("description", "댓글 내용")
                        }
                        putJsonObject("mime_type") {
                            put("type", "string")
                            put("description", "MIME 타입 (text/x-markdown 또는 text/html, 기본값: text/x-markdown)")
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
    doorayClient: DoorayClient,
    projectResolver: ProjectResolver
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val projectId = projectResolver.resolveProjectId(
                request.requireParam("project_id", "MISSING_PROJECT_ID", "project_id 파라미터가 필요합니다.")
            )
            val postId = request.requireParam("post_id", "MISSING_POST_ID", "post_id 파라미터가 필요합니다.")
            val content = request.requireParam("content", "MISSING_CONTENT", "content 파라미터가 필요합니다.")
            val mimeType = request.arguments?.get("mime_type")?.jsonPrimitive?.content ?: "text/x-markdown"

            val response = doorayClient.createPostComment(
                projectId, postId,
                CreateCommentRequest(body = PostCommentBody(mimeType = mimeType, content = content))
            )

            if (response.header.isSuccessful) {
                successResult(
                    data = response.result,
                    message = "업무 댓글이 성공적으로 생성되었습니다. (댓글 ID: ${response.result.id})"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
