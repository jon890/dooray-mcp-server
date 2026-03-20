package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getProjectPostsTool(): Tool {
    return Tool(
        name = "dooray_project_list_posts",
        description = "두레이 프로젝트의 업무 목록을 조회합니다. 다양한 필터 조건과 정렬 옵션을 지원합니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        projectIdProperty()
                        paginationProperties(defaultSize = 20, maxSize = 100)
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
                        putJsonObject("tag_ids") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "태그 ID 목록 (선택사항)")
                        }
                        putJsonObject("parent_post_id") {
                            put("type", "string")
                            put("description", "상위 업무 ID - 특정 업무의 하위 업무들만 조회 (선택사항)")
                        }
                        putJsonObject("post_workflow_classes") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "워크플로우 클래스 (backlog, registered, working, closed) (선택사항)")
                        }
                        putJsonObject("milestone_ids") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "마일스톤 ID 목록 (선택사항)")
                        }
                        putJsonObject("subjects") {
                            put("type", "string")
                            put("description", "업무 제목 검색어 (선택사항)")
                        }
                        putJsonObject("order") {
                            put("type", "string")
                            put("description", "정렬 조건 (postDueAt, postUpdatedAt, createdAt, 역순은 앞에 '-' 추가) (선택사항)")
                        }
                        putJsonObject("from_member_ids") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "업무를 등록한 멤버 ID 목록 (선택사항)")
                        }
                        putJsonObject("post_number") {
                            put("type", "string")
                            put("description", "업무 번호로 특정 업무 조회 (선택사항)")
                        }
                        putJsonObject("post_workflow_ids") {
                            put("type", "array")
                            putJsonObject("items") { put("type", "string") }
                            put("description", "특정 워크플로우 ID 목록으로 필터 - post_workflow_classes보다 정밀한 필터 (선택사항)")
                        }
                        putJsonObject("created_at") {
                            put("type", "string")
                            put("description", "생성일 기준 필터 (ISO8601 형식, 예: 2024-01-01T00:00:00+09:00) (선택사항)")
                        }
                        putJsonObject("updated_at") {
                            put("type", "string")
                            put("description", "수정일 기준 필터 (ISO8601 형식) (선택사항)")
                        }
                        putJsonObject("due_at") {
                            put("type", "string")
                            put("description", "마감일 기준 필터 (ISO8601 형식, 예: 이번 주 마감 업무 조회 시 활용) (선택사항)")
                        }
                    },
                required = listOf("project_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun getProjectPostsHandler(
    doorayClient: DoorayClient,
    projectResolver: ProjectResolver
): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val projectId = projectResolver.resolveProjectId(
                request.requireParam("project_id", "MISSING_PROJECT_ID", "project_id 파라미터가 필요합니다. 프로젝트 ID를 입력하세요.")
            )
            val page = request.intParam("page", 0)
            val size = request.intParam("size", 20)

            val toMemberIds = request.arguments?.get("to_member_ids")?.let { JsonUtils.parseStringArray(it.toString()) }
            val ccMemberIds = request.arguments?.get("cc_member_ids")?.let { JsonUtils.parseStringArray(it.toString()) }
            val tagIds = request.arguments?.get("tag_ids")?.let { JsonUtils.parseStringArray(it.toString()) }
            val postWorkflowClasses = request.arguments?.get("post_workflow_classes")?.let { JsonUtils.parseStringArray(it.toString()) }
            val milestoneIds = request.arguments?.get("milestone_ids")?.let { JsonUtils.parseStringArray(it.toString()) }
            val fromMemberIds = request.arguments?.get("from_member_ids")?.let { JsonUtils.parseStringArray(it.toString()) }
            val postWorkflowIds = request.arguments?.get("post_workflow_ids")?.let { JsonUtils.parseStringArray(it.toString()) }

            val parentPostId = request.optionalParam("parent_post_id")
            val subjects = request.optionalParam("subjects")
            val order = request.optionalParam("order")
            val postNumber = request.optionalParam("post_number")
            val createdAt = request.optionalParam("created_at")
            val updatedAt = request.optionalParam("updated_at")
            val dueAt = request.optionalParam("due_at")

            val response = doorayClient.getPosts(
                projectId = projectId,
                page = page,
                size = size,
                fromMemberIds = fromMemberIds,
                toMemberIds = toMemberIds,
                ccMemberIds = ccMemberIds,
                tagIds = tagIds,
                parentPostId = parentPostId,
                postNumber = postNumber,
                postWorkflowClasses = postWorkflowClasses,
                postWorkflowIds = postWorkflowIds,
                milestoneIds = milestoneIds,
                subjects = subjects,
                createdAt = createdAt,
                updatedAt = updatedAt,
                dueAt = dueAt,
                order = order
            )

            if (response.header.isSuccessful) {
                val pageInfo = if (page == 0) "첫 번째 페이지" else "${page + 1}번째 페이지"
                val nextStepHint =
                    if (response.result.isNotEmpty()) {
                        "\n\n💡 다음 단계: 특정 업무의 상세 정보를 보려면 dooray_project_get_post를 사용하세요."
                    } else {
                        if (page == 0) "\n\n📋 조회 결과가 없습니다. 필터 조건을 확인해주세요."
                        else "\n\n📄 더 이상 업무가 없습니다."
                    }
                successResult(
                    data = response.result,
                    message = "📋 프로젝트 업무 목록을 성공적으로 조회했습니다 ($pageInfo, 총 ${response.result.size}개)$nextStepHint"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
