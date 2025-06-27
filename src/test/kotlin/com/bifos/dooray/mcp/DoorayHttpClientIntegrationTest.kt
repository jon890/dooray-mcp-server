package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.client.DoorayHttpClient
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_API_KEY
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_BASE_URL
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_PROJECT_ID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll

/** Dooray Http Client 통합 테스트 실제 HTTP 요청을 보내므로 환경변수가 설정되어야 함 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoorayHttpClientIntegrationTest {

    lateinit var env: Map<String, String>
    lateinit var doorayClient: DoorayClient

    @BeforeAll
    fun setup() {
        env = parseEnv()

        val baseUrl =
                env[DOORAY_BASE_URL]
                        ?: throw IllegalStateException("DOORAY_BASE_URL 환경변수가 설정되지 않았습니다.")
        val apiKey =
                env[DOORAY_API_KEY]
                        ?: throw IllegalStateException("DOORAY_API_KEY 환경변수가 설정되지 않았습니다.")

        doorayClient = DoorayHttpClient(baseUrl, apiKey)
    }

    @Test
    @DisplayName("내가 조회할 수 있는 위키 목록들이 조회된다")
    fun getWikisTest() = runTest {
        // when
        val response = doorayClient.getWikis()

        // given
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        response.result.let { wikis ->
            assertTrue { wikis.isNotEmpty() }
            wikis.forEach { wiki ->
                assertNotNull(wiki.id)
                assertNotNull(wiki.project.id)
                assertNotNull(wiki.name)
                assertNotNull(wiki.type)
            }
        }
    }

    @Test
    @DisplayName("특정 프로젝트의 위키들이 조회된다")
    fun getWikiPagesTest() = runTest {
        // given
        val projectId = env[DOORAY_PROJECT_ID]!!

        // when
        val response = doorayClient.getWikiPages(projectId)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        response.result.forEach { page ->
            assertNotNull(page.id)
            assertNotNull(page.wikiId)
            assertNotNull(page.subject)
            assertNotNull(page.creator)
        }
    }

    @Test
    @DisplayName("특정 프로젝트의 root의 하위 위키들이 조회된다")
    fun getWikiPagesWithParentPageIdTest() = runTest {
        // given
        val projectId = env[DOORAY_PROJECT_ID]!!

        val pagesResponse = doorayClient.getWikiPages(projectId)
        assertTrue(pagesResponse.result.isNotEmpty(), "테스트할 위키 페이지가 없습니다.")

        val parentPageId = pagesResponse.result.first().id
        val response = doorayClient.getWikiPages(projectId, parentPageId)

        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        assertNotNull(response.result)
    }
}
