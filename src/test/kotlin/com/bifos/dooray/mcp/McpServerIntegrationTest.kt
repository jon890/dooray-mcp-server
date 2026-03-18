package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.constants.VersionConst
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * MCP 서버 통합 테스트
 *
 * 실제 JAR을 서브프로세스로 실행하여 MCP 프로토콜 동작을 검증합니다.
 * 가짜 자격증명을 사용하므로 실제 Dooray API는 호출하지 않습니다.
 *
 * 실행 전 shadowJar 빌드 필요: ./gradlew shadowJar
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("MCP Server 통합 테스트")
class McpServerIntegrationTest {

    private lateinit var process: Process
    private lateinit var client: Client

    companion object {
        private val EXPECTED_TOOLS = listOf(
            "dooray_wiki_list_projects",
            "dooray_wiki_list_pages",
            "dooray_wiki_get_page",
            "dooray_wiki_create_page",
            "dooray_wiki_update_page",
            "dooray_project_list_posts",
            "dooray_project_get_post",
            "dooray_project_create_post",
            "dooray_project_set_post_workflow",
            "dooray_project_set_post_done",
            "dooray_project_list_projects",
            "dooray_project_update_post",
            "dooray_project_create_post_comment",
            "dooray_project_get_post_comments",
            "dooray_project_update_post_comment",
            "dooray_project_delete_post_comment",
        )
    }

    @BeforeAll
    fun setup(): Unit = runBlocking {
        val jarPath = "build/libs/dooray-mcp-server-${VersionConst.VERSION}-all.jar"
        assumeTrue(
            java.io.File(jarPath).exists(),
            "shadowJar not found at $jarPath — run ./gradlew shadowJar first"
        )

        val pb = ProcessBuilder("java", "-jar", jarPath)
        pb.environment()["DOORAY_BASE_URL"] = "https://fake.dooray.test"
        pb.environment()["DOORAY_API_KEY"] = "fake-api-key-for-testing"
        pb.redirectError(ProcessBuilder.Redirect.DISCARD)
        process = pb.start()

        val transport = StdioClientTransport(
            input = process.inputStream.asSource().buffered(),
            output = process.outputStream.asSink().buffered(),
        )
        client = Client(clientInfo = Implementation(name = "test-client", version = "1.0.0"))
        client.connect(transport)
    }

    @AfterAll
    fun teardown(): Unit = runBlocking {
        runCatching { client.close() }
        process.destroyForcibly()
    }

    @Test
    @DisplayName("서버가 16개 도구를 모두 등록해야 한다")
    fun `server should register all 16 tools`(): Unit = runBlocking {
        val tools = client.listTools()?.tools
        assertNotNull(tools, "listTools() 응답이 null입니다")
        assertEquals(16, tools.size, "도구 수가 16개여야 합니다. 실제: ${tools.map { it.name }}")
    }

    @Test
    @DisplayName("모든 도구 이름이 올바르게 등록되어야 한다")
    fun `server should register tools with correct names`(): Unit = runBlocking {
        val toolNames = client.listTools()?.tools?.map { it.name } ?: emptyList()
        EXPECTED_TOOLS.forEach { expected ->
            assertContains(toolNames, expected, "도구 '$expected'가 등록되지 않았습니다")
        }
    }

    @Test
    @DisplayName("각 도구에 description이 있어야 한다")
    fun `each tool should have a description`(): Unit = runBlocking {
        val tools = client.listTools()?.tools ?: emptyList()
        tools.forEach { tool ->
            assertTrue(
                !tool.description.isNullOrBlank(),
                "도구 '${tool.name}'에 description이 없습니다"
            )
        }
    }

    @Test
    @DisplayName("각 도구에 inputSchema가 있어야 한다")
    fun `each tool should have an input schema`(): Unit = runBlocking {
        val tools = client.listTools()?.tools ?: emptyList()
        tools.forEach { tool ->
            assertNotNull(tool.inputSchema, "도구 '${tool.name}'에 inputSchema가 없습니다")
        }
    }

    @Test
    @DisplayName("위키 관련 도구는 5개여야 한다")
    fun `wiki tools should be 5`(): Unit = runBlocking {
        val wikiTools = client.listTools()?.tools?.filter { it.name.startsWith("dooray_wiki_") }
        assertEquals(5, wikiTools?.size, "위키 도구가 5개여야 합니다")
    }

    @Test
    @DisplayName("프로젝트 관련 도구는 11개여야 한다")
    fun `project tools should be 11`(): Unit = runBlocking {
        val projectTools = client.listTools()?.tools?.filter { it.name.startsWith("dooray_project_") }
        assertEquals(11, projectTools?.size, "프로젝트 도구가 11개여야 합니다")
    }
}
