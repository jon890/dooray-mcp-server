package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.client.DoorayHttpClient
import com.bifos.dooray.mcp.exception.CustomException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll

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
            println("🔗 BASE_URL: $baseUrl")
            println("🔑 API_KEY: ${apiKey.take(10)}...")
            println("📁 PROJECT_ID: $projectId")
        } else {
            println("⚠️ 통합 테스트에 필요한 환경변수가 없습니다. 테스트를 건너뜁니다.")
            println("📋 필요한 환경변수:")
            println("  - DOORAY_BASE_URL: ${baseUrl ?: "❌ 없음"}")
            println("  - DOORAY_API_KEY: ${if (apiKey != null) "✅ 있음" else "❌ 없음"}")
            println("  - DOORAY_PROJECT_ID: ${projectId ?: "❌ 없음"}")
        }
    }

    @Test
    fun `실제 API 호출 - getWikiPages 통합 테스트`() = runTest {
        // 환경변수가 없으면 테스트 건너뛰기
        assumeTrue(hasRequiredEnv, "통합 테스트에 필요한 환경변수가 설정되지 않았습니다.")

        val projectId = env["DOORAY_PROJECT_ID"]!!

        try {
            println("🚀 API 호출 시작: /wiki/v1/wikis/$projectId/pages")

            // When - 실제 API 호출
            val response = doorayClient.getWikiPages(projectId)

            println("✅ API 호출 성공")
            println(
                    "📊 응답 상태: isSuccessful=${response.header.isSuccessful}, resultCode=${response.header.resultCode}"
            )
            println("📝 결과 메시지: ${response.header.resultMessage}")
            println("📄 페이지 수: ${response.result?.size ?: 0}")

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
        } catch (e: CustomException) {
            println("❌ CustomException 발생:")
            println("  메시지: ${e.message}")
            println("  원인: ${e.cause}")
            println("  스택 트레이스:")
            e.printStackTrace()
            throw e
        } catch (e: Exception) {
            println("❌ 예상치 못한 오류 발생:")
            println("  타입: ${e::class.simpleName}")
            println("  메시지: ${e.message}")
            println("  스택 트레이스:")
            e.printStackTrace()
            throw e
        }
    }
}
