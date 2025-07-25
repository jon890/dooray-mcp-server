package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.CreateWikiPageRequest
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.types.WikiPageBody
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun createWikiPageTool(): Tool {
    return Tool(
            name = "dooray_wiki_create_page",
            description = "새로운 두레이 위키 페이지를 생성합니다. 제목과 내용을 입력하여 새 페이지를 만들 수 있습니다.",
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
                                            put(
                                                    "description",
                                                    "상위 페이지 ID (필수, dooray_wiki_list_pages로 조회 가능)"
                                            )
                                        }
                                    },
                            required = listOf("wiki_id", "subject", "body", "parent_page_id")
                    ),
            outputSchema = null,
            annotations = null
    )
}

fun createWikiPageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val wikiId = request.arguments["wiki_id"]?.jsonPrimitive?.content
            val subject = request.arguments["subject"]?.jsonPrimitive?.content
            val body = request.arguments["body"]?.jsonPrimitive?.content
            val parentPageId = request.arguments["parent_page_id"]?.jsonPrimitive?.content

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
                subject == null -> {
                    val errorResponse =
                            ToolException(
                                            type = ToolException.PARAMETER_MISSING,
                                            message = "subject 파라미터가 필요합니다. 위키 페이지의 제목을 입력하세요.",
                                            code = "MISSING_SUBJECT"
                                    )
                                    .toErrorResponse()

                    CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                body == null -> {
                    val errorResponse =
                            ToolException(
                                            type = ToolException.PARAMETER_MISSING,
                                            message = "body 파라미터가 필요합니다. 위키 페이지의 내용을 입력하세요.",
                                            code = "MISSING_BODY"
                                    )
                                    .toErrorResponse()

                    CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                parentPageId == null -> {
                    val errorResponse =
                            ToolException(
                                            type = ToolException.PARAMETER_MISSING,
                                            message =
                                                    "parent_page_id 파라미터가 필요합니다. dooray_wiki_list_pages를 사용해서 상위 페이지 ID를 먼저 조회하세요.",
                                            code = "MISSING_PARENT_PAGE_ID"
                                    )
                                    .toErrorResponse()

                    CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val createRequest =
                            CreateWikiPageRequest(
                                    subject = subject,
                                    body =
                                            WikiPageBody(
                                                    mimeType = "text/x-markdown",
                                                    content = body
                                            ),
                                    parentPageId = parentPageId
                            )

                    val response = doorayClient.createWikiPage(wikiId, createRequest)

                    if (response.header.isSuccessful) {
                        val successResponse =
                                ToolSuccessResponse(
                                        data = response.result,
                                        message =
                                                "✅ 위키 페이지를 성공적으로 생성했습니다 (페이지 ID: ${response.result.id}, 상위 페이지 ID: $parentPageId)"
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
