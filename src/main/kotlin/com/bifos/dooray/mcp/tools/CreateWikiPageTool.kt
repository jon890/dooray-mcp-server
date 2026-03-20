package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.types.CreateWikiPageRequest
import com.bifos.dooray.mcp.types.WikiPageBody
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun createWikiPageTool(): Tool {
    return Tool(
        name = "dooray_wiki_create_page",
        description = "새로운 두레이 위키 페이지를 생성합니다. 제목과 내용을 입력하여 새 페이지를 만들 수 있습니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        putJsonObject("wiki_id") {
                            put("type", "string")
                            put("description", "위키 ID (dooray_wiki_list_projects로 조회 가능)")
                        }
                        putJsonObject("subject") {
                            put("type", "string")
                            put("description", "위키 페이지 제목")
                        }
                        putJsonObject("body") {
                            put("type", "string")
                            put("description", "위키 페이지 내용 (Markdown 형식 지원)")
                        }
                        putJsonObject("parent_page_id") {
                            put("type", "string")
                            put("description", "상위 페이지 ID (필수, dooray_wiki_list_pages로 조회 가능)")
                        }
                    },
                required = listOf("wiki_id", "subject", "body", "parent_page_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun createWikiPageHandler(doorayClient: DoorayClient): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val wikiId = request.requireParam(
                "wiki_id", "MISSING_WIKI_ID",
                "wiki_id 파라미터가 필요합니다. dooray_wiki_list_projects를 사용해서 위키 ID를 먼저 조회하세요."
            )
            val subject = request.requireParam(
                "subject", "MISSING_SUBJECT",
                "subject 파라미터가 필요합니다. 위키 페이지의 제목을 입력하세요."
            )
            val body = request.requireParam(
                "body", "MISSING_BODY",
                "body 파라미터가 필요합니다. 위키 페이지의 내용을 입력하세요."
            )
            val parentPageId = request.requireParam(
                "parent_page_id", "MISSING_PARENT_PAGE_ID",
                "parent_page_id 파라미터가 필요합니다. dooray_wiki_list_pages를 사용해서 상위 페이지 ID를 먼저 조회하세요."
            )

            val createRequest = CreateWikiPageRequest(
                subject = subject,
                body = WikiPageBody(mimeType = "text/x-markdown", content = body),
                parentPageId = parentPageId
            )

            val response = doorayClient.createWikiPage(wikiId, createRequest)

            if (response.header.isSuccessful) {
                successResult(
                    data = response.result,
                    message = "✅ 위키 페이지를 성공적으로 생성했습니다 (페이지 ID: ${response.result.id}, 상위 페이지 ID: $parentPageId)"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
