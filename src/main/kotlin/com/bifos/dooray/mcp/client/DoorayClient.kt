package com.bifos.dooray.mcp.client

import com.bifos.dooray.mcp.types.WikiPagesResponse

interface DoorayClient {

    /**
     * dooray projectId를 사용하여 특정 프로젝트의
     * 위키 목록을 요청합니다.
     */
    suspend fun getWikiPages(projectId: String) : WikiPagesResponse

    suspend fun getWikiPages(projectId: String, parentPageId: String) : WikiPagesResponse
}