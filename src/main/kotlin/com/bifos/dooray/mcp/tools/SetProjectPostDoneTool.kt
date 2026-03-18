package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun setProjectPostDoneTool(): Tool {
    return Tool(
        name = "dooray_project_set_post_done",
        description =
            "두레이 프로젝트 업무를 완료 상태로 변경합니다. 완료 클래스 내의 대표 상태로 변경되며, 모든 담당자의 상태가 완료로 변경됩니다.",
        inputSchema =
            ToolSchema(
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

fun setProjectPostDoneHandler(
    doorayClient: DoorayClient
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        try {
            val projectId = request.arguments?.get("project_id")?.jsonPrimitive?.content
            val postId = request.arguments?.get("post_id")?.jsonPrimitive?.content

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
                    val response = doorayClient.setPostDone(projectId, postId)

                    if (response.header.isSuccessful) {
                        val nextStepHint =
                            "\n\n💡 다음 가능한 작업:\n" +
                                    "- dooray_project_get_post: 완료된 업무 상태 확인\n" +
                                    "- dooray_project_list_posts: 프로젝트 업무 목록 조회"

                        val successResponse =
                            ToolSuccessResponse(
                                data = mapOf("message" to "업무가 성공적으로 완료 처리되었습니다."),
                                message = "✅ 업무를 성공적으로 완료 처리했습니다$nextStepHint"
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
