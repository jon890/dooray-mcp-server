package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.types.PostCommentsResponseData
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getPostCommentsTool(): Tool {
    return Tool(
        name = "dooray_project_get_post_comments",
        description = "두레이 프로젝트 업무의 댓글 목록을 조회합니다. 페이징과 정렬 옵션을 지원합니다.",
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
                        putJsonObject("page") {
                            put("type", "number")
                            put("description", "페이지 번호 (0부터 시작, 기본값: 0)")
                            put("default", 0)
                        }
                        putJsonObject("size") {
                            put("type", "number")
                            put("description", "페이지 크기 (최대 100, 기본값: 20)")
                            put("default", 20)
                        }
                        putJsonObject("order") {
                            put("type", "string")
                            put("description", "정렬 조건 (createdAt: 오래된순, -createdAt: 최신순, 기본값: createdAt)")
                            put("default", "createdAt")
                        }
                    },
                required = listOf("project_id", "post_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun getPostCommentsHandler(
    doorayClient: DoorayClient,
    projectResolver: ProjectResolver
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val projectId = projectResolver.resolveProjectId(
                request.requireParam("project_id", "MISSING_PROJECT_ID", "project_id 파라미터가 필요합니다.")
            )
            val postId = request.requireParam("post_id", "MISSING_POST_ID", "post_id 파라미터가 필요합니다.")
            val page = request.arguments?.get("page")?.jsonPrimitive?.content?.toIntOrNull()
            val size = request.arguments?.get("size")?.jsonPrimitive?.content?.toIntOrNull()
            val order = request.optionalParam("order")

            val response = doorayClient.getPostComments(projectId, postId, page, size, order)

            if (response.header.isSuccessful) {
                successResult(
                    data = PostCommentsResponseData(
                        comments = response.result,
                        totalCount = response.totalCount,
                        currentPage = page ?: 0,
                        pageSize = size ?: 20
                    ),
                    message = "업무 댓글 목록을 성공적으로 조회했습니다. (총 ${response.totalCount}개, 현재 페이지: ${response.result.size}개)"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
