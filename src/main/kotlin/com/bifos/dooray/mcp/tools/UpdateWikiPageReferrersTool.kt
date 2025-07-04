package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.Member
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.types.WikiReferrer
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun updateWikiPageReferrersTool(): Tool {
    return Tool(
            name = "dooray_wiki_update_page_referrers",
            description = "두레이 위키 페이지의 참조자를 수정합니다. 기존 참조자는 모두 지워지고 새로운 참조자로 덮어씁니다.",
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
                                        putJsonObject("referrer_member_ids") {
                                            put("type", "array")
                                            putJsonObject("items") { put("type", "string") }
                                            put(
                                                    "description",
                                                    "참조자로 설정할 조직 멤버 ID 목록 (빈 배열이면 모든 참조자 제거)"
                                            )
                                        }
                                    },
                            required = listOf("wiki_id", "page_id", "referrer_member_ids")
                    )
    )
}

fun updateWikiPageReferrersHandler(
        doorayClient: DoorayClient
): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val wikiId = request.arguments["wiki_id"]?.jsonPrimitive?.content
            val pageId = request.arguments["page_id"]?.jsonPrimitive?.content
            val referrerMemberIdsJson = request.arguments["referrer_member_ids"]?.jsonArray

            when {
                wikiId == null -> {
                    val errorResponse =
                            ToolException(
                                            type = ToolException.PARAMETER_MISSING,
                                            message =
                                                    "wiki_id 파라미터가 필요합니다. dooray_wiki_list_projects를 사용해서 위키 ID를 먼저 조회하세요.",
                                            code = "MISSING_WIKI_ID"
                                    )
                                    .toErrorResponse()

                    CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
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

                    CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                referrerMemberIdsJson == null -> {
                    val errorResponse =
                            ToolException(
                                            type = ToolException.PARAMETER_MISSING,
                                            message =
                                                    "referrer_member_ids 파라미터가 필요합니다. 참조자로 설정할 조직 멤버 ID 목록을 배열로 입력하세요.",
                                            code = "MISSING_REFERRER_MEMBER_IDS"
                                    )
                                    .toErrorResponse()

                    CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val referrerMemberIds = referrerMemberIdsJson.map { it.jsonPrimitive.content }
                    val referrers =
                            referrerMemberIds.map { memberId ->
                                WikiReferrer(
                                        type = "member",
                                        member = Member(organizationMemberId = memberId)
                                )
                            }

                    val response = doorayClient.updateWikiPageReferrers(wikiId, pageId, referrers)

                    if (response.header.isSuccessful) {
                        val message =
                                if (referrers.isEmpty()) {
                                    "🗑️ 위키 페이지의 모든 참조자를 제거했습니다"
                                } else {
                                    "👥 위키 페이지의 참조자를 성공적으로 설정했습니다 (${referrers.size}명)"
                                }

                        val successResponse =
                                ToolSuccessResponse(
                                        data =
                                                mapOf(
                                                        "wiki_id" to wikiId,
                                                        "page_id" to pageId,
                                                        "referrer_count" to referrers.size,
                                                        "referrer_member_ids" to referrerMemberIds,
                                                        "updated" to true
                                                ),
                                        message = message
                                )

                        CallToolResult(
                                content =
                                        listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse =
                                ToolException(
                                                type = ToolException.API_ERROR,
                                                message = response.header.resultMessage,
                                                code = "DOORAY_API_${response.header.resultCode}"
                                        )
                                        .toErrorResponse()

                        CallToolResult(
                                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                        )
                    }
                }
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
