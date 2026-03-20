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

fun getWikiPagesTool(): Tool {
    return Tool(
        name = "dooray_wiki_list_pages",
        description = "특정 두레이 위키 프로젝트의 페이지 목록을 조회합니다. 전체 목록 또는 특정 부모 페이지의 하위 페이지들을 조회할 수 있습니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        putJsonObject("project_id") {
                            put("type", "string")
                            put("description", "위키 프로젝트 ID (dooray_wiki_list_projects로 조회 가능)")
                        }
                        putJsonObject("parent_page_id") {
                            put("type", "string")
                            put("description", "상위 페이지 ID (선택사항, 없으면 루트 페이지들 조회)")
                        }
                    },
                required = listOf("project_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun getWikiPagesHandler(doorayClient: DoorayClient): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val projectId = request.requireParam(
                "project_id", "MISSING_PROJECT_ID",
                "project_id 파라미터가 필요합니다. dooray_wiki_list_projects를 사용해서 프로젝트 ID를 먼저 조회하세요."
            )
            val parentPageId = request.optionalParam("parent_page_id")

            val response =
                if (parentPageId != null) doorayClient.getWikiPages(projectId, parentPageId)
                else doorayClient.getWikiPages(projectId)

            if (response.header.isSuccessful) {
                val messagePrefix = if (parentPageId != null) "📄 하위 위키 페이지" else "📚 루트 위키 페이지"
                successResult(
                    data = response.result,
                    message = "$messagePrefix 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
