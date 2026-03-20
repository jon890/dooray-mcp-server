package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*

fun updateWikiPageTool(): Tool {
    return Tool(
        name = "dooray_wiki_update_page",
        description =
            "기존 두레이 위키 페이지를 수정합니다. 제목, 내용, 참조자 등을 변경할 수 있습니다. 변경되지 않은 필드는 기존 값을 유지합니다.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        putJsonObject("wiki_id") {
                            put("type", "string")
                            put("description", "위키 ID (dooray_wiki_list_projects로 조회 가능)")
                        }
                        putJsonObject("page_id") {
                            put("type", "string")
                            put("description", "수정할 위키 페이지 ID (dooray_wiki_list_pages로 조회 가능)")
                        }
                        putJsonObject("subject") {
                            put("type", "string")
                            put("description", "새로운 위키 페이지 제목 (선택사항)")
                        }
                        putJsonObject("body") {
                            put("type", "string")
                            put("description", "새로운 위키 페이지 내용 (Markdown 형식 지원, 선택사항)")
                        }
                        putJsonObject("referrer_member_ids") {
                            put("type", "array")
                            put("description", "참조자로 설정할 조직 멤버 ID 목록 (선택사항, 빈 배열이면 모든 참조자 제거)")
                            putJsonObject("items") { put("type", "string") }
                        }
                    },
                required = listOf("wiki_id", "page_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

fun updateWikiPageHandler(doorayClient: DoorayClient): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val wikiId = request.requireParam(
                "wiki_id", "MISSING_WIKI_ID",
                "wiki_id 파라미터가 필요합니다. dooray_wiki_list_projects를 사용해서 위키 ID를 먼저 조회하세요."
            )
            val pageId = request.requireParam(
                "page_id", "MISSING_PAGE_ID",
                "page_id 파라미터가 필요합니다. dooray_wiki_list_pages를 사용해서 페이지 ID를 먼저 조회하세요."
            )

            val newSubject = request.optionalParam("subject")
            val newBodyContent = request.optionalParam("body")
            val referrerMemberIds =
                request.arguments?.get("referrer_member_ids")?.jsonArray?.map { it.jsonPrimitive.content }

            if (newSubject == null && newBodyContent == null && referrerMemberIds == null) {
                throw ToolException(
                    type = ToolException.VALIDATION_ERROR,
                    message = "수정할 내용이 없습니다. subject, body, referrer_member_ids 중 적어도 하나는 제공해야 합니다.",
                    code = "NO_UPDATE_CONTENT"
                )
            }

            val currentPageResponse = doorayClient.getWikiPage(wikiId, pageId)
            if (!currentPageResponse.header.isSuccessful) {
                throw ToolException(
                    type = ToolException.API_ERROR,
                    message = "기존 위키 페이지를 조회할 수 없습니다: ${currentPageResponse.header.resultMessage}",
                    code = "DOORAY_API_${currentPageResponse.header.resultCode}"
                )
            }

            val currentPage = currentPageResponse.result
            val finalSubject = newSubject ?: currentPage.subject
            val finalBody =
                if (newBodyContent != null) WikiPageBody(mimeType = "text/x-markdown", content = newBodyContent)
                else currentPage.body
            val finalReferrers = referrerMemberIds?.map { memberId ->
                WikiReferrer(type = "member", member = Member(organizationMemberId = memberId))
            }

            val updateRequest = UpdateWikiPageRequest(
                subject = finalSubject,
                body = finalBody,
                referrers = finalReferrers
            )
            val response = doorayClient.updateWikiPage(wikiId, pageId, updateRequest)

            if (response.header.isSuccessful) {
                val updatedFields = listOfNotNull(
                    "제목".takeIf { newSubject != null },
                    "내용".takeIf { newBodyContent != null },
                    "참조자".takeIf { referrerMemberIds != null }
                ).joinToString(", ")

                successResult(
                    data = buildJsonObject {
                        put("wiki_id", wikiId)
                        put("page_id", pageId)
                        put("subject", finalSubject)
                        put("updated_fields", updatedFields)
                        if (referrerMemberIds != null) put("referrer_count", referrerMemberIds.size)
                    },
                    message = "✅ 위키 페이지 '${finalSubject}'의 $updatedFields 을(를) 성공적으로 수정했습니다"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
