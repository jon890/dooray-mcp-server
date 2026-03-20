package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.service.ProjectResolver
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

fun getProjectMembersTool(): Tool {
    return Tool(
        name = "dooray_project_list_members",
        description = "두레이 프로젝트의 멤버 목록을 조회합니다. " +
                "업무 생성(dooray_project_create_post)의 to_member_ids(담당자) 또는 cc_member_ids(참조자)에 " +
                "입력할 organizationMemberId를 찾을 때 사용하세요.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("project_id") {
                    put("type", "string")
                    put("description", "프로젝트 ID 또는 프로젝트 코드 (예: 'my-project' 또는 숫자 ID). 프로젝트 코드는 dooray_project_list_projects로 확인 가능합니다.")
                }
                putJsonObject("page") {
                    put("type", "integer")
                    put("description", "페이지 번호 (기본값: 0)")
                    put("default", 0)
                }
                putJsonObject("size") {
                    put("type", "integer")
                    put("description", "페이지 크기 (기본값: 100, 최대: 100)")
                    put("default", 100)
                }
            },
            required = listOf("project_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getProjectMembersHandler(
    doorayClient: DoorayClient,
    projectResolver: ProjectResolver
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return handler@{ _, request ->
        try {
            val projectInput = request.arguments?.get("project_id")?.jsonPrimitive?.content

            if (projectInput == null) {
                val errorResponse = ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "project_id 파라미터가 필요합니다. dooray_project_list_projects를 사용해서 프로젝트 ID를 먼저 조회하세요.",
                    code = "MISSING_PROJECT_ID"
                ).toErrorResponse()
                return@handler CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
            }

            val projectId = try {
                projectResolver.resolveProjectId(projectInput)
            } catch (e: ToolException) {
                return@handler CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(e.toErrorResponse()))))
            }

            val page = request.arguments?.get("page")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val size = request.arguments?.get("size")?.jsonPrimitive?.content?.toIntOrNull() ?: 100

            val response = doorayClient.getProjectMembers(projectId, page, size)

            if (response.header.isSuccessful) {
                val nextStepHint = "\n\n💡 다음 가능한 작업:\n" +
                        "- dooray_project_create_post: 조회한 organizationMemberId를 to_member_ids에 입력하여 업무 생성\n" +
                        "- dooray_project_update_post: 기존 업무의 담당자/참조자 변경"

                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "👥 프로젝트 멤버 목록을 성공적으로 조회했습니다 (총 ${response.result.size}명)$nextStepHint"
                )
                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(successResponse))))
            } else {
                val errorResponse = ToolException(
                    type = ToolException.API_ERROR,
                    message = response.header.resultMessage,
                    code = "DOORAY_API_${response.header.resultCode}"
                ).toErrorResponse()
                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
            }
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "내부 오류가 발생했습니다: ${e.message}",
                details = e.stackTraceToString()
            ).toErrorResponse()
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
