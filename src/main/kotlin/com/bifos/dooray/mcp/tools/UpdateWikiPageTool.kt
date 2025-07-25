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

fun updateWikiPageTool(): Tool {
    return Tool(
        name = "dooray_wiki_update_page",
        description =
            "기존 두레이 위키 페이지를 수정합니다. 제목, 내용, 참조자 등을 변경할 수 있습니다. 변경되지 않은 필드는 기존 값을 유지합니다.",
        inputSchema =
            Tool.Input(
                properties =
                    buildJsonObject {
                        putJsonObject("wiki_id") {
                            put("type", "string")
                            put(
                                "description",
                                "위키 ID (dooray_wiki_list_projects로 조회 가능)"
                            )
                        }
                        putJsonObject("page_id") {
                            put("type", "string")
                            put(
                                "description",
                                "수정할 위키 페이지 ID (dooray_wiki_list_pages로 조회 가능)"
                            )
                        }
                        putJsonObject("subject") {
                            put("type", "string")
                            put("description", "새로운 위키 페이지 제목 (선택사항)")
                        }
                        putJsonObject("body") {
                            put("type", "string")
                            put(
                                "description",
                                "새로운 위키 페이지 내용 (Markdown 형식 지원, 선택사항)"
                            )
                        }
                        putJsonObject("referrer_member_ids") {
                            put("type", "array")
                            put(
                                "description",
                                "참조자로 설정할 조직 멤버 ID 목록 (선택사항, 빈 배열이면 모든 참조자 제거)"
                            )
                            putJsonObject("items") { put("type", "string") }
                        }
                    },
                required = listOf("wiki_id", "page_id")
            ),
        outputSchema = null,
        annotations = null
    )
}

data class UpdateWikiPageParams(
    val wikiId: String,
    val pageId: String,
    val newSubject: String?,
    val newBodyContent: String?,
    val referrerMemberIds: List<String>?
)

fun updateWikiPageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val validationResult = validateUpdateWikiPageParams(request)
            if (validationResult != null) {
                validationResult
            } else {
                val params = extractUpdateWikiPageParams(request)
                performUpdateWikiPage(doorayClient, params)
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

/** 파라미터 검증 */
private fun validateUpdateWikiPageParams(request: CallToolRequest): CallToolResult? {
    val wikiId = request.arguments["wiki_id"]?.jsonPrimitive?.content
    val pageId = request.arguments["page_id"]?.jsonPrimitive?.content
    val newSubject = request.arguments["subject"]?.jsonPrimitive?.content
    val newBodyContent = request.arguments["body"]?.jsonPrimitive?.content
    val referrerMemberIds =
        request.arguments["referrer_member_ids"]?.jsonArray?.map { it.jsonPrimitive.content }

    return when {
        wikiId == null -> {
            val errorResponse =
                ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message =
                        "wiki_id 파라미터가 필요합니다. dooray_wiki_list_projects를 사용해서 위키 ID를 먼저 조회하세요.",
                    code = "MISSING_WIKI_ID"
                )
                    .toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }

        pageId == null -> {
            val errorResponse =
                ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message =
                        "page_id 파라미터가 필요합니다. dooray_wiki_list_pages를 사용해서 페이지 ID를 먼저 조회하세요.",
                    code = "MISSING_PAGE_ID"
                )
                    .toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }

        newSubject == null && newBodyContent == null && referrerMemberIds == null -> {
            val errorResponse =
                ToolException(
                    type = ToolException.VALIDATION_ERROR,
                    message =
                        "수정할 내용이 없습니다. subject, body, referrer_member_ids 중 적어도 하나는 제공해야 합니다.",
                    code = "NO_UPDATE_CONTENT"
                )
                    .toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }

        else -> null // 검증 통과
    }
}

/** 파라미터 추출 */
private fun extractUpdateWikiPageParams(request: CallToolRequest): UpdateWikiPageParams {
    val wikiId = request.arguments["wiki_id"]?.jsonPrimitive?.content!!
    val pageId = request.arguments["page_id"]?.jsonPrimitive?.content!!
    val newSubject = request.arguments["subject"]?.jsonPrimitive?.content
    val newBodyContent = request.arguments["body"]?.jsonPrimitive?.content
    val referrerMemberIds =
        request.arguments["referrer_member_ids"]?.jsonArray?.map { it.jsonPrimitive.content }

    return UpdateWikiPageParams(
        wikiId = wikiId,
        pageId = pageId,
        newSubject = newSubject,
        newBodyContent = newBodyContent,
        referrerMemberIds = referrerMemberIds
    )
}

/** 실제 위키 페이지 수정 로직 */
private suspend fun performUpdateWikiPage(
    doorayClient: DoorayClient,
    params: UpdateWikiPageParams
): CallToolResult {
    // 1. 기존 위키 페이지 조회
    val currentPageResponse = doorayClient.getWikiPage(params.wikiId, params.pageId)

    if (!currentPageResponse.header.isSuccessful) {
        val errorResponse =
            ToolException(
                type = ToolException.API_ERROR,
                message =
                    "기존 위키 페이지를 조회할 수 없습니다: ${currentPageResponse.header.resultMessage}",
                code = "DOORAY_API_${currentPageResponse.header.resultCode}"
            )
                .toErrorResponse()

        return CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
    }

    val currentPage = currentPageResponse.result

    // 2. 변경되지 않은 필드는 기존 값 사용
    val finalSubject = params.newSubject ?: currentPage.subject
    val finalBody =
        when {
            params.newBodyContent != null ->
                WikiPageBody(mimeType = "text/x-markdown", content = params.newBodyContent)

            else -> currentPage.body
        }

    // 3. 참조자 처리
    val finalReferrers =
        params.referrerMemberIds?.map { memberId ->
            WikiReferrer(type = "member", member = Member(organizationMemberId = memberId))
        }

    // 4. 업데이트 요청 생성
    val updateRequest =
        UpdateWikiPageRequest(
            subject = finalSubject,
            body = finalBody,
            referrers = finalReferrers
        )

    // 5. 업데이트 요청 전송
    val response = doorayClient.updateWikiPage(params.wikiId, params.pageId, updateRequest)

    return if (response.header.isSuccessful) {
        val updateParts = mutableListOf<String>()
        if (params.newSubject != null) updateParts.add("제목")
        if (params.newBodyContent != null) updateParts.add("내용")
        if (params.referrerMemberIds != null) updateParts.add("참조자")

        val updatedFields = updateParts.joinToString(", ")

        val successResponse =
            ToolSuccessResponse(
                data =
                    buildJsonObject {
                        put("wiki_id", params.wikiId)
                        put("page_id", params.pageId)
                        put("subject", finalSubject)
                        put("updated_fields", updatedFields)
                        if (params.referrerMemberIds != null) {
                            put("referrer_count", params.referrerMemberIds.size)
                        }
                    },
                message = "✅ 위키 페이지 '${finalSubject}'의 $updatedFields 을(를) 성공적으로 수정했습니다"
            )

        CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(successResponse))))
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
}
