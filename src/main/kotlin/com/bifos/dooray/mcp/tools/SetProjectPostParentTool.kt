package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun setProjectPostParentTool(): Tool {
    return Tool(
        name = "dooray_project_set_post_parent",
        description = "두레이 프로젝트 업무의 상위 업무(부모 업무)를 설정합니다. 업무를 계층 구조로 구성할 때 사용하며, 하위 업무를 특정 상위 업무에 연결합니다.",
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
                            put("description", "상위 업무를 설정할 하위 업무 ID (dooray_project_list_posts로 조회 가능) (필수)")
                        }
                        putJsonObject("parent_post_id") {
                            put("type", "string")
                            put("description", "상위로 지정할 업무 ID (dooray_project_list_posts로 조회 가능) (필수)")
                        }
                    },
                required = listOf("project_id", "post_id", "parent_post_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun setProjectPostParentHandler(
    doorayClient: DoorayClient,
    projectResolver: ProjectResolver
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return handler@{ _, request ->
        try {
            val projectInput = request.arguments?.get("project_id")?.jsonPrimitive?.content
            val postId = request.arguments?.get("post_id")?.jsonPrimitive?.content
            val parentPostId = request.arguments?.get("parent_post_id")?.jsonPrimitive?.content

            when {
                projectInput == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message = "project_id 파라미터가 필요합니다. 프로젝트 ID를 입력하세요.",
                            code = "MISSING_PROJECT_ID"
                        ).toErrorResponse()
                    return@handler CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
                }

                postId == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message = "post_id 파라미터가 필요합니다. dooray_project_list_posts를 사용해서 업무 ID를 먼저 조회하세요.",
                            code = "MISSING_POST_ID"
                        ).toErrorResponse()
                    return@handler CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
                }

                parentPostId == null -> {
                    val errorResponse =
                        ToolException(
                            type = ToolException.PARAMETER_MISSING,
                            message = "parent_post_id 파라미터가 필요합니다. dooray_project_list_posts를 사용해서 상위 업무 ID를 먼저 조회하세요.",
                            code = "MISSING_PARENT_POST_ID"
                        ).toErrorResponse()
                    return@handler CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
                }

                else -> {
                    val projectId = try {
                        projectResolver.resolveProjectId(projectInput)
                    } catch (e: ToolException) {
                        return@handler CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(e.toErrorResponse()))))
                    }

                    val response = doorayClient.setPostParent(projectId, postId, parentPostId)

                    if (response.header.isSuccessful) {
                        val nextStepHint =
                            "\n\n💡 다음 가능한 작업:\n" +
                                    "- dooray_project_get_post: 업무 상세 정보 확인\n" +
                                    "- dooray_project_list_posts: parent_post_id 파라미터로 하위 업무 목록 조회"

                        val successResponse =
                            ToolSuccessResponse(
                                data = mapOf(
                                    "postId" to postId,
                                    "parentPostId" to parentPostId
                                ),
                                message = "✅ 업무의 상위 업무를 성공적으로 설정했습니다$nextStepHint"
                            )

                        CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(successResponse))))
                    } else {
                        val errorResponse =
                            ToolException(
                                type = ToolException.API_ERROR,
                                message = response.header.resultMessage,
                                code = "DOORAY_API_${response.header.resultCode}"
                            ).toErrorResponse()

                        CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
                    }
                }
            }
        } catch (e: Exception) {
            val errorResponse =
                ToolException(
                    type = ToolException.INTERNAL_ERROR,
                    message = "내부 오류가 발생했습니다: ${e.message}",
                    details = e.stackTraceToString()
                ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
