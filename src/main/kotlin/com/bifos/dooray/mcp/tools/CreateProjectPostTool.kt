package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.types.*
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun createProjectPostTool(): Tool {
    return Tool(
        name = "dooray_project_create_post",
        description = "두레이 프로젝트에 새로운 업무를 생성합니다. 담당자, 참조자, 우선순위 등을 설정할 수 있습니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        projectIdProperty()
                        putJsonObject("subject") {
                            put("type", "string")
                            put("description", "업무 제목 (필수)")
                        }
                        putJsonObject("body") {
                            put("type", "string")
                            put("description", "업무 내용 (필수)")
                        }
                        putJsonObject("to_member_ids") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "담당자 멤버 ID 목록 (필수)")
                        }
                        putJsonObject("cc_member_ids") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "참조자 멤버 ID 목록 (선택사항)")
                        }
                        putJsonObject("parent_post_id") {
                            put("type", "string")
                            put("description", "상위 업무 ID - 하위 업무로 생성할 경우 (선택사항)")
                        }
                        putJsonObject("due_date") {
                            put("type", "string")
                            put("description", "만기일 (ISO8601 형식, 예: 2024-12-31T18:00:00+09:00) (선택사항)")
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
                        putJsonObject("priority") {
                            put("type", "string")
                            put("description", "우선순위 (highest, high, normal, low, lowest, none) (기본값: none)")
                            put("default", "none")
                        }
                    },
                required = listOf("project_id", "subject", "body", "to_member_ids")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun createProjectPostHandler(
    doorayClient: DoorayClient,
    projectResolver: ProjectResolver
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val projectId = projectResolver.resolveProjectId(
                request.requireParam("project_id", "MISSING_PROJECT_ID", "project_id 파라미터가 필요합니다. 프로젝트 ID를 입력하세요.")
            )
            val subject = request.requireParam("subject", "MISSING_SUBJECT", "subject 파라미터가 필요합니다. 업무 제목을 입력하세요.")
            val body = request.requireParam("body", "MISSING_BODY", "body 파라미터가 필요합니다. 업무 내용을 입력하세요.")

            val toMemberIds = request.arguments?.get("to_member_ids")?.let { JsonUtils.parseStringArray(it.toString()) }
            if (toMemberIds.isNullOrEmpty()) {
                throw com.bifos.dooray.mcp.exception.ToolException(
                    type = com.bifos.dooray.mcp.exception.ToolException.PARAMETER_MISSING,
                    message = "to_member_ids 파라미터가 필요합니다. 담당자 멤버 ID 목록을 입력하세요.",
                    code = "MISSING_TO_MEMBER_IDS"
                )
            }

            val ccMemberIds = request.arguments?.get("cc_member_ids")?.let { JsonUtils.parseStringArray(it.toString()) } ?: emptyList()
            val parentPostId = request.optionalParam("parent_post_id")
            val dueDate = request.optionalParam("due_date")
            val milestoneId = request.optionalParam("milestone_id")
            val tagIds = request.arguments?.get("tag_ids")?.let { JsonUtils.parseStringArray(it.toString()) } ?: emptyList()
            val priority = request.arguments?.get("priority")?.jsonPrimitive?.content ?: "none"

            val toUsers = toMemberIds.map { CreatePostUser(type = "member", member = Member(organizationMemberId = it)) }
            val ccUsers = ccMemberIds.map { CreatePostUser(type = "member", member = Member(organizationMemberId = it)) }

            val createRequest = CreatePostRequest(
                parentPostId = parentPostId,
                users = CreatePostUsers(to = toUsers, cc = ccUsers),
                subject = subject,
                body = PostBody(mimeType = "text/x-markdown", content = body),
                dueDate = dueDate,
                milestoneId = milestoneId,
                tagIds = tagIds,
                priority = priority
            )

            val response = doorayClient.createPost(projectId, createRequest)

            if (response.header.isSuccessful) {
                val nextStepHint =
                    "\n\n💡 다음 가능한 작업:\n" +
                            "- dooray_project_get_post: 생성된 업무 상세 조회\n" +
                            "- dooray_project_list_posts: 프로젝트 업무 목록 조회"
                successResult(
                    data = response.result,
                    message = "✅ 업무를 성공적으로 생성했습니다 (업무 ID: ${response.result.id})$nextStepHint"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
