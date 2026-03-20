package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.service.ProjectResolver
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
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
                        projectIdProperty()
                        postIdProperty("상위 업무를 설정할 하위 업무 ID (dooray_project_list_posts로 조회 가능)")
                        putJsonObject("parent_post_id") {
                            put("type", "string")
                            put("description", "상위로 지정할 업무 ID (dooray_project_list_posts로 조회 가능)")
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
    return { _, request ->
        toolHandler {
            val projectId = projectResolver.resolveProjectId(
                request.requireParam("project_id", "MISSING_PROJECT_ID", "project_id 파라미터가 필요합니다. 프로젝트 ID를 입력하세요.")
            )
            val postId = request.requireParam(
                "post_id", "MISSING_POST_ID",
                "post_id 파라미터가 필요합니다. dooray_project_list_posts를 사용해서 업무 ID를 먼저 조회하세요."
            )
            val parentPostId = request.requireParam(
                "parent_post_id", "MISSING_PARENT_POST_ID",
                "parent_post_id 파라미터가 필요합니다. dooray_project_list_posts를 사용해서 상위 업무 ID를 먼저 조회하세요."
            )

            val response = doorayClient.setPostParent(projectId, postId, parentPostId)

            if (response.header.isSuccessful) {
                val nextStepHint =
                    "\n\n💡 다음 가능한 작업:\n" +
                            "- dooray_project_get_post: 업무 상세 정보 확인\n" +
                            "- dooray_project_list_posts: parent_post_id 파라미터로 하위 업무 목록 조회"
                successResult(
                    data = buildJsonObject {
                        put("postId", postId)
                        put("parentPostId", parentPostId)
                    },
                    message = "✅ 업무의 상위 업무를 성공적으로 설정했습니다$nextStepHint"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
