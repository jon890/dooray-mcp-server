package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** 위키 페이지 정보 */
@Serializable
data class WikiPage(
        val id: String,
        val wikiId: String,
        val version: Int,
        val root: Boolean,
        val creator: Creator,
        val subject: String
)

/** 위키 페이지 생성자 정보 */
@Serializable data class Creator(val type: String, val member: Member)

/** 멤버 정보 */
@Serializable data class Member(val organizationMemberId: String)

/** 위키 페이지 목록 응답 타입 */
typealias WikiPagesResponse = DoorayApiResponse<List<WikiPage>>
