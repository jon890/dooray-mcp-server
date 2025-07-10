package com.bifos.dooray.mcp.client.dooray

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.client.DoorayHttpClient
import com.bifos.dooray.mcp.constants.EnvVariableConst
import com.bifos.dooray.mcp.util.parseEnv
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

/** Dooray Http Client 통합 테스트를 위한 추상 베이스 클래스 실제 HTTP 요청을 보내므로 환경변수가 설정되어야 함 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseDoorayIntegrationTest {

    protected lateinit var testProjectId: String
    protected lateinit var testWikiId: String
    protected lateinit var doorayClient: DoorayClient

    // 테스트에서 생성된 데이터들을 추적하여 나중에 삭제
    protected val createdPostIds = mutableListOf<String>()
    protected val createdWikiPageIds = mutableListOf<String>()

    @BeforeAll
    fun setup() {
        val env = parseEnv()

        val baseUrl =
            env[EnvVariableConst.DOORAY_BASE_URL]
                ?: throw IllegalStateException("DOORAY_BASE_URL 환경변수가 설정되지 않았습니다.")
        val apiKey =
            env[EnvVariableConst.DOORAY_API_KEY]
                ?: throw IllegalStateException("DOORAY_API_KEY 환경변수가 설정되지 않았습니다.")
        this.testProjectId =
            env[EnvVariableConst.DOORAY_TEST_PROJECT_ID]
                ?: throw IllegalStateException("DOORAY_TEST_PROJECT_ID 환경변수가 설정되지 않았습니다.")
        this.testWikiId =
            env[EnvVariableConst.DOORAY_TEST_WIKI_ID]
                ?: throw IllegalStateException("DOORAY_TEST_WIKI_ID 환경변수가 설정되지 않았습니다.")

        doorayClient = DoorayHttpClient(baseUrl, apiKey)
    }

    @AfterAll
    fun cleanup() = runTest {
        println("🧹 테스트 완료 후 생성된 데이터를 정리합니다...")
        cleanupCreatedData()
    }

    /** 테스트 중 생성된 데이터들을 삭제합니다. */
    private suspend fun cleanupCreatedData() {
        // 생성된 댓글들 삭제
        createdPostIds.forEach { postId ->
            try {
                // 댓글 삭제 기능이 있다면 여기에 구현
                println("  📝 Post ID: $postId - 수동으로 삭제해주세요")
            } catch (e: Exception) {
                println("  ❌ Post ID: $postId 삭제 중 오류 발생: ${e.message}")
            }
        }

        // 생성된 위키 페이지들 삭제
        createdWikiPageIds.forEach { pageId ->
            try {
                // 위키 페이지 삭제 기능이 있다면 여기에 구현
                println("  📄 Wiki Page ID: $pageId - 수동으로 삭제해주세요")
            } catch (e: Exception) {
                println("  ❌ Wiki Page ID: $pageId 삭제 중 오류 발생: ${e.message}")
            }
        }

        if (createdPostIds.isNotEmpty() || createdWikiPageIds.isNotEmpty()) {
            println("⚠️  생성된 테스트 데이터를 수동으로 삭제해주세요:")
            println("   - Posts: ${createdPostIds.joinToString(", ")}")
            println("   - Wiki Pages: ${createdWikiPageIds.joinToString(", ")}")
        }
    }
}