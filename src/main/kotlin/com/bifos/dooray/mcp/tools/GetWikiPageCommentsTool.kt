package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.types.WikiCommentsResponseData
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getWikiPageCommentsTool(): Tool {
    return Tool(
        name = "dooray_wiki_get_page_comments",
        description =
            "특정 두레이 위키 페이지의 댓글 목록을 조회합니다. 최신순으로 응답하며(0 페이지의 첫 번째 원소가 가장 최신 댓글), 페이징을 지원합니다.",
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
                        paginationProperties(defaultSize = 20, maxSize = 100)
                    },
                required = listOf("project_id", "page_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun getWikiPageCommentsHandler(doorayClient: DoorayClient): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
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
            val page = request.intParamOrNull("page")
            val size = request.intParamOrNull("size")

            val response = doorayClient.getWikiPageComments(projectId, pageId, page, size)

            if (response.header.isSuccessful) {
                successResult(
                    data = WikiCommentsResponseData(
                        comments = response.result,
                        totalCount = response.totalCount,
                        currentPage = page ?: 0,
                        pageSize = size ?: 20
                    ),
                    message = "💬 위키 페이지 댓글 목록을 성공적으로 조회했습니다. (총 ${response.totalCount}개, 현재 페이지: ${response.result.size}개)"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
