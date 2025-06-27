package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** 위키 홈 페이지 정보 */
@Serializable data class WikiHome(val pageId: String)

/** 위키의 프로젝트 정보 */
@Serializable data class WikiProject(val id: String)

/** 위키 정보 */
@Serializable
data class Wiki(
        val id: String,
        val project: WikiProject,
        val name: String,
        val type: String,
        val scope: String,
        val home: WikiHome
)

/** 위키 목록 응답 타입 (API는 배열을 직접 반환) */
typealias WikiListResponse = DoorayApiResponse<List<Wiki>>
