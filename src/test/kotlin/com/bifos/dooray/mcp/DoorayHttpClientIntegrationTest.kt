package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.client.DoorayHttpClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Dooray Http Client 통합 테스트 실제 HTTP 요청을 보내므로 환경변수가 설정되어야 함 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoorayHttpClientIntegrationTest {

    lateinit var env: Map<String, String>
    lateinit var doorayClient: DoorayClient
    private var hasRequiredEnv = false

    @BeforeAll
    fun setup() {
        env = parseEnv()

        val baseUrl = env["DOORAY_BASE_URL"]
        val apiKey = env["DOORAY_API_KEY"]
        val projectId = env["DOORAY_PROJECT_ID"]

        hasRequiredEnv = baseUrl != null && apiKey != null && projectId != null

        if (hasRequiredEnv) {
            doorayClient = DoorayHttpClient(baseUrl!!, apiKey!!)
            println("✅ 통합 테스트 환경 준비 완료")
        } else {
            println("⚠️ 통합 테스트에 필요한 환경변수가 없습니다. 테스트를 건너뜁니다.")
        }
    }

    @Test
    fun `실제 API 호출 - getWikiPages 통합 테스트`() = runTest {
        // 환경변수가 없으면 테스트 건너뛰기
        assumeTrue(hasRequiredEnv, "통합 테스트에 필요한 환경변수가 설정되지 않았습니다.")

        val projectId = env["DOORAY_PROJECT_ID"]!!

        // When - 실제 API 호출
        val response = doorayClient.getWikiPages(projectId)

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
