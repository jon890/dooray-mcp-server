package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** 위키 댓글이 속한 페이지 정보 */
@Serializable data class WikiCommentPage(val id: String)

/** 위키 댓글 본문 (mimeType 고정: text/x-markdown) */
@Serializable data class WikiCommentBody(val mimeType: String, val content: String? = null)

/** 위키 댓글 정보 */
@Serializable
data class WikiComment(
    val id: String,
    val page: WikiCommentPage,
    val createdAt: String? = null,
    val modifiedAt: String? = null,
    val creator: Creator,
    val body: WikiCommentBody
)

/** 위키 댓글 목록 API 응답 */
@Serializable
data class WikiCommentListApiResponse(
    val header: DoorayApiHeader,
    val result: List<WikiComment>,
    val totalCount: Int
)

/** 위키 댓글 목록 응답 타입 */
typealias WikiCommentListResponse = WikiCommentListApiResponse
