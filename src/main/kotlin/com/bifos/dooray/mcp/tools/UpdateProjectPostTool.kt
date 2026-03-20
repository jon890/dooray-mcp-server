package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.types.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*

fun updateProjectPostTool(): Tool {
    return Tool(
        name = "dooray_project_update_post",
        description = "두레이 프로젝트의 기존 업무를 수정합니다. 제목, 내용, 담당자, 참조자, 우선순위, 마일스톤, 태그 등을 변경할 수 있습니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        projectIdProperty()
                        postIdProperty("수정할 업무 ID (dooray_project_list_posts로 조회 가능)")
                        putJsonObject("subject") {
                            put("type", "string")
                            put("description", "업무 제목 (선택사항)")
                        }
                        putJsonObject("body") {
                            put("type", "string")
                            put("description", "업무 내용 (선택사항)")
                        }
                        putJsonObject("to_member_ids") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "담당자 멤버 ID 목록 (선택사항)")
                        }
                        putJsonObject("cc_member_ids") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "참조자 멤버 ID 목록 (선택사항)")
                        }
                        putJsonObject("priority") {
                            put("type", "string")
                            put("description", "우선순위 (highest, high, normal, low, lowest, none) (선택사항)")
                        }
                        putJsonObject("milestone_id") {
                            put("type", "string")
                            put("description", "마일스톤 ID (선택사항)")
                        }
                        putJsonObject("tag_ids") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "태그 ID 목록 (선택사항)")
                        }
                        putJsonObject("due_date") {
                            put("type", "string")
                            put("description", "만기일 (ISO8601 형식, 예: 2024-12-31T18:00:00+09:00) (선택사항)")
                        }
                    },
                required = listOf("project_id", "post_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun updateProjectPostHandler(
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

            val existingPostResponse = doorayClient.getPost(projectId, postId)
            if (!existingPostResponse.header.isSuccessful) {
                throw ToolException(
                    type = ToolException.API_ERROR,
                    message = "기존 업무 정보를 조회할 수 없습니다: ${existingPostResponse.header.resultMessage}",
                    code = "DOORAY_API_${existingPostResponse.header.resultCode}"
                )
            }

            val existingPost = existingPostResponse.result

            val subject = request.optionalParam("subject") ?: existingPost.subject
            val bodyContent = request.optionalParam("body")
            val body = if (bodyContent != null) PostBody(mimeType = "text/x-markdown", content = bodyContent) else existingPost.body
            val priority = request.optionalParam("priority") ?: existingPost.priority
            val milestoneId = request.optionalParam("milestone_id") ?: existingPost.milestone?.id
            val dueDate = request.optionalParam("due_date") ?: existingPost.dueDate

            val toMemberIds = request.arguments?.get("to_member_ids")?.jsonArray?.mapNotNull { it.jsonPrimitive.content }
            val ccMemberIds = request.arguments?.get("cc_member_ids")?.jsonArray?.mapNotNull { it.jsonPrimitive.content }
            val tagIds = request.arguments?.get("tag_ids")?.jsonArray?.mapNotNull { it.jsonPrimitive.content }
                ?: existingPost.tags.map { it.id }

            val users = CreatePostUsers(
                to = if (toMemberIds != null) {
                    toMemberIds.map { CreatePostUser(type = "member", member = Member(it)) }
                } else {
                    existingPost.users.to.mapNotNull { postUser ->
                        postUser.member?.let { CreatePostUser(type = "member", member = Member(it.organizationMemberId)) }
                    }
                },
                cc = if (ccMemberIds != null) {
                    ccMemberIds.map { CreatePostUser(type = "member", member = Member(it)) }
                } else {
                    existingPost.users.cc.mapNotNull { postUser ->
                        postUser.member?.let { CreatePostUser(type = "member", member = Member(it.organizationMemberId)) }
                    }
                }
            )

            val updateRequest = UpdatePostRequest(
                users = users,
                subject = subject,
                body = body,
                priority = priority,
                milestoneId = milestoneId,
                dueDate = dueDate,
                tagIds = tagIds
            )

            val response = doorayClient.updatePost(projectId, postId, updateRequest)

            if (response.header.isSuccessful) {
                successResult(message = "업무가 성공적으로 수정되었습니다.")
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
