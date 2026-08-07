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

/** 위키 댓글 목록 조회 응답 데이터 */
@Serializable
data class WikiCommentsResponseData(
        val comments: List<WikiComment>,
        val totalCount: Int,
        val currentPage: Int,
        val pageSize: Int
)
