package com.bifos.dooray.mcp.client

import com.bifos.dooray.mcp.types.*

interface DoorayClient {

    /** 접근 가능한 위키 목록을 조회합니다. */
    suspend fun getWikis(page: Int? = null, size: Int? = null): WikiListResponse

    /** 특정 프로젝트의 위키 목록을 조회합니다. */
    suspend fun getWikiPages(projectId: String): WikiPagesResponse

    /** 특정 상위 페이지의 자식 위키 페이지들을 조회합니다. */
    suspend fun getWikiPages(projectId: String, parentPageId: String): WikiPagesResponse

    /** 특정 위키 페이지의 상세 정보를 조회합니다. */
    suspend fun getWikiPage(projectId: String, pageId: String): WikiPageResponse

    /** 새로운 위키 페이지를 생성합니다. */
    suspend fun createWikiPage(projectId: String, request: CreateWikiPageRequest): WikiPageResponse

    /** 위키 페이지를 수정합니다. */
    suspend fun updateWikiPage(
        projectId: String,
        pageId: String,
        request: UpdateWikiPageRequest
    ): WikiPageResponse

    /** 위키 페이지를 삭제합니다. */
    suspend fun deleteWikiPage(projectId: String, pageId: String): DoorayApiResponse<Unit>

    /** 위키 페이지의 버전 히스토리를 조회합니다. */
    suspend fun getWikiPageVersions(projectId: String, pageId: String): WikiPageVersionsResponse

    /** 특정 버전의 위키 페이지를 조회합니다. */
    suspend fun getWikiPageVersion(
        projectId: String,
        pageId: String,
        version: Int
    ): WikiPageResponse

    /** 위키 페이지를 검색합니다. */
    suspend fun searchWikiPages(
        projectId: String,
        query: String,
        size: Int? = null,
        page: Int? = null
    ): WikiSearchResponse
}
