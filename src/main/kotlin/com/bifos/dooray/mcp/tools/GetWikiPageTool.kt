package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getWikiPageTool(): Tool {
    return Tool(
        name = "dooray_wiki_get_page",
        description =
            "특정 두레이 위키 페이지의 상세 정보를 조회합니다. 페이지 제목, 내용, 작성자, 수정 이력 등 모든 정보를 확인할 수 있습니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        putJsonObject("project_id") {
                            put("type", "string")
                            put("description", "위키 프로젝트 ID (dooray_wiki_list_projects로 조회 가능)")
                        }
                        putJsonObject("page_id") {
                            put("type", "string")
                            put("description", "위키 페이지 ID (dooray_wiki_list_pages로 조회 가능)")
                        }
                    },
                required = listOf("project_id", "page_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun getWikiPageHandler(doorayClient: DoorayClient): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val projectId = request.requireParam(
                "project_id", "MISSING_PROJECT_ID",
                "project_id 파라미터가 필요합니다. dooray_wiki_list_projects를 사용해서 프로젝트 ID를 먼저 조회하세요."
            )
            val pageId = request.requireParam(
                "page_id", "MISSING_PAGE_ID",
                "page_id 파라미터가 필요합니다. dooray_wiki_list_pages를 사용해서 페이지 ID를 먼저 조회하세요."
            )

            val response = doorayClient.getWikiPage(projectId, pageId)

            if (response.header.isSuccessful) {
                successResult(
                    data = response.result,
                    message = "📖 위키 페이지 '${response.result.subject}'의 상세 정보를 성공적으로 조회했습니다"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
