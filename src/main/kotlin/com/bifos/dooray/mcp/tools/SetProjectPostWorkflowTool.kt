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

fun setProjectPostWorkflowTool(): Tool {
    return Tool(
        name = "dooray_project_set_post_workflow",
        description = "두레이 프로젝트 업무의 상태(워크플로우)를 변경합니다. 업무 전체의 상태를 변경하며, 모든 담당자의 상태가 함께 변경됩니다.",
        inputSchema =
            Tool.Input(
                properties =
                    buildJsonObject {
                        putJsonObject("project_id") {
                            put("type", "string")
                            put("description", "프로젝트 ID (필수)")
                        }
                        putJsonObject("post_id") {
                            put("type", "string")
                            put(
                                "description",
                                "업무 ID (dooray_project_list_posts로 조회 가능) (필수)"
                            )
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
    doorayClient: DoorayClient
): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val projectId = request.arguments["project_id"]?.jsonPrimitive?.content
            val postId = request.arguments["post_id"]?.jsonPrimitive?.content
            val workflowId = request.arguments["workflow_id"]?.jsonPrimitive?.content

            when {
                projectId == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message = "project_id 파라미터가 필요합니다. 프로젝트 ID를 입력하세요.",
                            code = "MISSING_PROJECT_ID"
                        )
                            .toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }

                postId == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message =
                                "post_id 파라미터가 필요합니다. dooray_project_list_posts를 사용해서 업무 ID를 먼저 조회하세요.",
                            code = "MISSING_POST_ID"
                        )
                            .toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }

                workflowId == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message =
                                "workflow_id 파라미터가 필요합니다. 변경할 워크플로우 ID를 입력하세요.",
                            code = "MISSING_WORKFLOW_ID"
                        )
                            .toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }

                else -> {
                    val response = doorayClient.setPostWorkflow(projectId, postId, workflowId)

                    if (response.header.isSuccessful) {
                        val nextStepHint =
                            "\n\n💡 다음 가능한 작업:\n" +
                                    "- dooray_project_get_post: 변경된 업무 상태 확인\n" +
                                    "- dooray_project_set_post_done: 업무 완료 처리 (완료 상태로 변경)"

                        val successResponse =
                            ToolSuccessResponse(
                                data = mapOf("message" to "워크플로우가 성공적으로 변경되었습니다."),
                                message = "✅ 업무 상태를 성공적으로 변경했습니다$nextStepHint"
                            )

                        CallToolResult(
                            content =
                                listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse =
                            ToolException(
                                type = ToolException.API_ERROR,
                                message = response.header.resultMessage,
                                code = "DOORAY_API_${response.header.resultCode}"
                            )
                                .toErrorResponse()

                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                        )
                    }
                }
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
