package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** MCP Tool 성공 응답 */
@Serializable
data class ToolSuccessResponse(
        val success: Boolean = true,
        val data: JsonElement? = null,
        val message: String? = null
)

/** 댓글 목록 조회 응답 데이터 */
@Serializable
data class PostCommentsResponseData(
        val comments: List<PostComment>,
        val totalCount: Int,
        val currentPage: Int,
        val pageSize: Int
)

/** MCP Tool 에러 응답 */
@Serializable
data class ToolErrorResponse(
        val isError: Boolean = true,
        val error: ToolError,
        val content: ToolErrorContent,
)

@Serializable data class ToolErrorContent(val type: String = "text", val text: String)

/** Tool 에러 정보 */
@Serializable
data class ToolError(val type: String, val code: String? = null, val details: String? = null)
