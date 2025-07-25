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

fun getProjectPostTool(): Tool {
    return Tool(
        name = "dooray_project_get_post",
        description = "두레이 프로젝트의 특정 업무의 상세 정보를 조회합니다. 업무 내용, 담당자, 첨부파일 등 모든 정보를 확인할 수 있습니다.",
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
                    },
                required = listOf("project_id", "post_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun getProjectPostHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val projectId = request.arguments["project_id"]?.jsonPrimitive?.content
            val postId = request.arguments["post_id"]?.jsonPrimitive?.content

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

                else -> {
                    val response = doorayClient.getPost(projectId, postId)

                    if (response.header.isSuccessful) {
                        val post = response.result
                        val nextStepHint =
                            "\n\n💡 다음 가능한 작업:\n" +
                                    "- dooray_project_update_post: 업무 수정\n" +
                                    "- dooray_project_set_post_workflow: 업무 상태 변경\n" +
                                    "- dooray_project_set_post_done: 업무 완료 처리"

                        val successResponse =
                            ToolSuccessResponse(
                                data = post,
                                message =
                                    "📋 업무 상세 정보를 성공적으로 조회했습니다 (업무번호: ${post.taskNumber})$nextStepHint"
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
