package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** 위키 페이지의 본문 정보 */
@Serializable data class WikiPageBody(val mimeType: String, val content: String? = null)

/** 위키 페이지 상세 정보 */
@Serializable
data class WikiPageDetail(
        val id: String,
        val wikiId: String,
        val version: Int,
        val root: Boolean,
        val creator: Creator,
        val subject: String,
        val body: WikiPageBody? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
        val parentPageId: String? = null
)

/** 위키 페이지 생성 요청 */
@Serializable
data class CreateWikiPageRequest(
        val subject: String,
        val body: String,
        val parentPageId: String? = null
)

/** 위키 페이지 수정 요청 */
@Serializable
data class UpdateWikiPageRequest(
        val subject: String? = null,
        val body: String? = null,
        val parentPageId: String? = null
)

/** 위키 페이지 버전 정보 */
@Serializable
data class WikiPageVersion(
        val version: Int,
        val creator: Creator,
        val createdAt: String,
        val subject: String
)

/** 위키 검색 결과 */
@Serializable
data class WikiSearchResult(
        val id: String,
        val wikiId: String,
        val version: Int,
        val subject: String,
        val body: String? = null,
        val creator: Creator,
        val createdAt: String? = null,
        val updatedAt: String? = null
)

/** 위키 검색 응답 */
@Serializable data class WikiSearchData(val totalCount: Int, val posts: List<WikiSearchResult>)

/** 위키 페이지 상세 응답 타입 */
typealias WikiPageResponse = DoorayApiResponse<WikiPageDetail>

/** 위키 페이지 버전 목록 응답 타입 */
typealias WikiPageVersionsResponse = DoorayApiResponse<List<WikiPageVersion>>

/** 위키 검색 응답 타입 */
typealias WikiSearchResponse = DoorayApiResponse<WikiSearchData>
