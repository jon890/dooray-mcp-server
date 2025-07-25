package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.*
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun updateProjectPostTool(): Tool {
    return Tool(
        name = "dooray_project_update_post",
        description = "두레이 프로젝트의 기존 업무를 수정합니다. 제목, 내용, 담당자, 참조자, 우선순위, 마일스톤, 태그 등을 변경할 수 있습니다.",
        inputSchema =
            Tool.Input(
                properties =
                    buildJsonObject {
                        putJsonObject("project_id") {
                            put("type", "string")
                            put(
                                "description",
                                "프로젝트 ID (dooray_project_list_projects로 조회 가능)"
                            )
                        }
                        putJsonObject("post_id") {
                            put("type", "string")
                            put(
                                "description",
                                "수정할 업무 ID (dooray_project_list_posts로 조회 가능)"
                            )
                        }
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
                            put(
                                "description",
                                "우선순위 (highest, high, normal, low, lowest, none) (선택사항)"
                            )
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
                            put(
                                "description",
                                "만기일 (ISO8601 형식, 예: 2024-12-31T18:00:00+09:00) (선택사항)"
                            )
                        }
                    },
                required = listOf("project_id", "post_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun updateProjectPostHandler(
    doorayClient: DoorayClient
): suspend (CallToolRequest) -> CallToolResult {
    return handler@{ request ->
        try {
            val projectId = request.arguments["project_id"]?.jsonPrimitive?.content
            val postId = request.arguments["post_id"]?.jsonPrimitive?.content

            if (projectId.isNullOrBlank()) {
                val errorResponse =
                    ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "project_id 파라미터가 필요합니다.",
                        code = "MISSING_PROJECT_ID"
                    )
                        .toErrorResponse()

                return@handler CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                )
            }

            if (postId.isNullOrBlank()) {
                val errorResponse =
                    ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "post_id 파라미터가 필요합니다.",
                        code = "MISSING_POST_ID"
                    )
                        .toErrorResponse()

                return@handler CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                )
            }

            // 기존 업무 정보 조회
            val existingPostResponse = doorayClient.getPost(projectId, postId)
            if (!existingPostResponse.header.isSuccessful) {
                val errorResponse =
                    ToolException(
                        type = ToolException.API_ERROR,
                        message =
                            "기존 업무 정보를 조회할 수 없습니다: ${existingPostResponse.header.resultMessage}",
                        code =
                            "DOORAY_API_${existingPostResponse.header.resultCode}"
                    )
                        .toErrorResponse()

                return@handler CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                )
            }

            val existingPost = existingPostResponse.result

            // 선택적 파라미터들 처리
            val subject =
                request.arguments["subject"]?.jsonPrimitive?.content ?: existingPost.subject
            val bodyContent = request.arguments["body"]?.jsonPrimitive?.content
            val body =
                if (bodyContent != null) {
                    PostBody(mimeType = "text/x-markdown", content = bodyContent)
                } else {
                    existingPost.body
                }
            val priority =
                request.arguments["priority"]?.jsonPrimitive?.content ?: existingPost.priority
            val milestoneId =
                request.arguments["milestone_id"]?.jsonPrimitive?.content
                    ?: existingPost.milestone?.id
            val dueDate =
                request.arguments["due_date"]?.jsonPrimitive?.content ?: existingPost.dueDate

            // 담당자와 참조자 처리
            val toMemberIds =
                request.arguments["to_member_ids"]?.jsonArray?.mapNotNull {
                    it.jsonPrimitive.content
                }

            val ccMemberIds =
                request.arguments["cc_member_ids"]?.jsonArray?.mapNotNull {
                    it.jsonPrimitive.content
                }

            val tagIds =
                request.arguments["tag_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content }
                    ?: existingPost.tags.map { it.id }

            // 사용자 정보 구성
            val users =
                CreatePostUsers(
                    to =
                        if (toMemberIds != null) {
                            toMemberIds.map {
                                CreatePostUser(type = "member", member = Member(it))
                            }
                        } else {
                            existingPost.users.to.mapNotNull { postUser ->
                                postUser.member?.let { member ->
                                    CreatePostUser(
                                        type = "member",
                                        member = Member(member.organizationMemberId)
                                    )
                                }
                            }
                        },
                    cc =
                        if (ccMemberIds != null) {
                            ccMemberIds.map {
                                CreatePostUser(type = "member", member = Member(it))
                            }
                        } else {
                            existingPost.users.cc.mapNotNull { postUser ->
                                postUser.member?.let { member ->
                                    CreatePostUser(
                                        type = "member",
                                        member = Member(member.organizationMemberId)
                                    )
                                }
                            }
                        }
                )

            // 업데이트 요청 객체 생성
            val updateRequest =
                UpdatePostRequest(
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
                val successResponse =
                    ToolSuccessResponse(data = null, message = "업무가 성공적으로 수정되었습니다.")

                CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                )
            } else {
                val errorResponse =
                    ToolException(
                        type = ToolException.API_ERROR,
                        message = response.header.resultMessage,
                        code = "DOORAY_API_${response.header.resultCode}"
                    )
                        .toErrorResponse()

                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
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
