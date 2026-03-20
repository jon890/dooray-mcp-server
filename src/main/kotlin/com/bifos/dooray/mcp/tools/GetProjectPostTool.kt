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

fun getProjectPostTool(): Tool {
    return Tool(
        name = "dooray_project_get_post",
        description = "두레이 프로젝트의 특정 업무의 상세 정보를 조회합니다. 업무 내용, 담당자, 첨부파일 등 모든 정보를 확인할 수 있습니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        projectIdProperty()
                        postIdProperty("업무 ID (dooray_project_list_posts로 조회 가능)")
                    },
                required = listOf("project_id", "post_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun getProjectPostHandler(
    doorayClient: DoorayClient,
    projectResolver: ProjectResolver
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val projectId = projectResolver.resolveProjectId(
                request.requireParam("project_id", "MISSING_PROJECT_ID", "project_id 파라미터가 필요합니다. 프로젝트 ID를 입력하세요.")
            )
            val postId = request.requireParam(
                "post_id", "MISSING_POST_ID",
                "post_id 파라미터가 필요합니다. dooray_project_list_posts를 사용해서 업무 ID를 먼저 조회하세요."
            )

            val response = doorayClient.getPost(projectId, postId)

            if (response.header.isSuccessful) {
                val post = response.result
                val nextStepHint =
                    "\n\n💡 다음 가능한 작업:\n" +
                            "- dooray_project_update_post: 업무 수정\n" +
                            "- dooray_project_set_post_workflow: 업무 상태 변경\n" +
                            "- dooray_project_set_post_done: 업무 완료 처리"
                successResult(
                    data = post,
                    message = "📋 업무 상세 정보를 성공적으로 조회했습니다 (업무번호: ${post.taskNumber})$nextStepHint"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
