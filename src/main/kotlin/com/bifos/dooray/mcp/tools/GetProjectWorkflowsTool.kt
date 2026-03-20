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

fun getProjectWorkflowsTool(): Tool {
    return Tool(
        name = "dooray_project_list_workflows",
        description = "두레이 프로젝트의 워크플로우(업무 상태) 목록을 조회합니다. " +
                "dooray_project_set_post_workflow의 workflow_id에 입력할 값을 찾을 때 사용하세요. " +
                "각 워크플로우는 class(backlog/registered/working/closed)로 분류됩니다.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                projectIdProperty()
            },
            required = listOf("project_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getProjectWorkflowsHandler(
    doorayClient: DoorayClient,
    projectResolver: ProjectResolver
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val projectId = projectResolver.resolveProjectId(
                request.requireParam(
                    "project_id", "MISSING_PROJECT_ID",
                    "project_id 파라미터가 필요합니다. dooray_project_list_projects를 사용해서 프로젝트 ID를 먼저 조회하세요."
                )
            )

            val response = doorayClient.getProjectWorkflows(projectId)

            if (response.header.isSuccessful) {
                val nextStepHint = "\n\n💡 다음 가능한 작업:\n" +
                        "- dooray_project_set_post_workflow: 조회한 workflow_id를 사용하여 업무 상태 변경\n" +
                        "- dooray_project_list_posts: post_workflow_ids 파라미터로 특정 상태 업무 필터링"
                successResult(
                    data = response.result,
                    message = "🔄 프로젝트 워크플로우 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)$nextStepHint"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
