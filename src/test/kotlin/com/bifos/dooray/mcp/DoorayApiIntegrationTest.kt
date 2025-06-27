package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayHttpClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Dooray API 통합 테스트 실제 HTTP 요청을 보내므로 환경변수가 설정되어야 함:
 * - DOORAY_API_KEY: Dooray API 키
 */
class DoorayApiIntegrationTest {

    private val baseUrl = System.getenv("DOORAY_BASE_URL")
        ?: throw IllegalArgumentException("DOORAY_BASE_URL is required.")
    private val apiKey = System.getenv("DOORAY_API_KEY")
        ?: throw IllegalArgumentException("DOORAY_API_KEY is required.")
    private val projectId = System.getenv("DOORAY_PROJECT_ID")
        ?: throw IllegalArgumentException("DOORAY_PROJECT_ID is required.")

    @Test
    fun `실제 API 호출 - getWikiPages 통합 테스트`() = runTest {
        val httpClient = DoorayHttpClient(baseUrl, apiKey)

        // When - 실제 API 호출
        val response = httpClient.getWikiPages(projectId)

        // Then - 응답 구조 검증
        assertAll(
            { assertTrue { response.header.isSuccessful } },
            { assertEquals(response.header.resultCode, 0) }
        )
        response.result!!.forEach { page ->
            assertNotNull(page.id)
            assertNotNull(page.wikiId)
            assertNotNull(page.subject)
            assertNotNull(page.creator)
        }
    }
}
