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

fun setProjectPostWorkflowTool(): Tool {
    return Tool(
        name = "dooray_project_set_post_workflow",
        description = "두레이 프로젝트 업무의 상태(워크플로우)를 변경합니다. 업무 전체의 상태를 변경하며, 모든 담당자의 상태가 함께 변경됩니다.",
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
                            put("description", "업무 ID (dooray_project_list_posts로 조회 가능) (필수)")
                        }
                        putJsonObject("workflow_id") {
                            put("type", "string")
                            put("description", "변경할 워크플로우 ID (필수)")
                        }
                    },
                required = listOf("project_id", "post_id", "workflow_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun setProjectPostWorkflowHandler(
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
            val workflowId = request.requireParam(
                "workflow_id", "MISSING_WORKFLOW_ID",
                "workflow_id 파라미터가 필요합니다. 변경할 워크플로우 ID를 입력하세요."
            )

            val response = doorayClient.setPostWorkflow(projectId, postId, workflowId)

            if (response.header.isSuccessful) {
                val nextStepHint =
                    "\n\n💡 다음 가능한 작업:\n" +
                            "- dooray_project_get_post: 변경된 업무 상태 확인\n" +
                            "- dooray_project_set_post_done: 업무 완료 처리 (완료 상태로 변경)"
                successResult(
                    data = mapOf("message" to "워크플로우가 성공적으로 변경되었습니다."),
                    message = "✅ 업무 상태를 성공적으로 변경했습니다$nextStepHint"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
