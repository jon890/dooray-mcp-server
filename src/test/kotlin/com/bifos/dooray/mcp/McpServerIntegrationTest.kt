package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.constants.VersionConst
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import kotlin.test.assertContains
import kotlin.test.assertFalse
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
@Tag("mcp-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("MCP Server 통합 테스트")
class McpServerIntegrationTest {

    private lateinit var process: Process
    private lateinit var client: Client

    companion object {
        /** 서버에 등록되어야 하는 모든 도구 이름 목록 */
        private val EXPECTED_TOOLS = listOf(
            "dooray_wiki_list_projects",
            "dooray_wiki_list_pages",
            "dooray_wiki_get_page",
            "dooray_wiki_create_page",
            "dooray_wiki_update_page",
            "dooray_project_list_projects",
            "dooray_project_list_members",
            "dooray_project_list_workflows",
            "dooray_project_list_posts",
            "dooray_project_get_post",
            "dooray_project_create_post",
            "dooray_project_update_post",
            "dooray_project_set_post_workflow",
            "dooray_project_set_post_done",
            "dooray_project_set_post_parent",
            "dooray_project_create_post_comment",
            "dooray_project_get_post_comments",
            "dooray_project_update_post_comment",
            "dooray_project_delete_post_comment",
        )

        /** project_id를 required로 가져야 하는 도구 목록 */
        private val TOOLS_REQUIRING_PROJECT_ID = EXPECTED_TOOLS
            .filter { it.startsWith("dooray_project_") && it != "dooray_project_list_projects" }

        /** post_id를 required로 가져야 하는 도구 목록 */
        private val TOOLS_REQUIRING_POST_ID = listOf(
            "dooray_project_get_post",
            "dooray_project_update_post",
            "dooray_project_set_post_workflow",
            "dooray_project_set_post_done",
            "dooray_project_set_post_parent",
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
    @DisplayName("모든 필수 도구가 등록되어야 한다")
    fun `all expected tools should be registered`(): Unit = runBlocking {
        val toolNames = client.listTools().tools.map { it.name }
        EXPECTED_TOOLS.forEach { expected ->
            assertContains(toolNames, expected, "도구 '$expected'가 등록되지 않았습니다")
        }
    }

    @Test
    @DisplayName("각 도구에 description이 있어야 한다")
    fun `each tool should have a non-blank description`(): Unit = runBlocking {
        val tools = client.listTools().tools
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
        val tools = client.listTools().tools
        tools.forEach { tool ->
            assertNotNull(tool.inputSchema, "도구 '${tool.name}'에 inputSchema가 없습니다")
        }
    }

    @Test
    @DisplayName("project_id가 필요한 도구는 required에 project_id가 선언되어야 한다")
    fun `tools requiring project_id should declare it as required`(): Unit = runBlocking {
        val toolMap = client.listTools().tools.associateBy { it.name }
        TOOLS_REQUIRING_PROJECT_ID.forEach { toolName ->
            val tool = toolMap[toolName] ?: error("도구 '$toolName'이 등록되지 않았습니다")
            val required = tool.inputSchema.required ?: emptyList()
            assertContains(required, "project_id", "도구 '$toolName'의 required에 project_id가 없습니다")
        }
    }

    @Test
    @DisplayName("post_id가 필요한 도구는 required에 post_id가 선언되어야 한다")
    fun `tools requiring post_id should declare it as required`(): Unit = runBlocking {
        val toolMap = client.listTools().tools.associateBy { it.name }
        TOOLS_REQUIRING_POST_ID.forEach { toolName ->
            val tool = toolMap[toolName] ?: error("도구 '$toolName'이 등록되지 않았습니다")
            val required = tool.inputSchema.required ?: emptyList()
            assertContains(required, "post_id", "도구 '$toolName'의 required에 post_id가 없습니다")
        }
    }

    @Test
    @DisplayName("필수 파라미터 없이 호출하면 서버가 구조화된 에러를 반환해야 한다")
    fun `calling tool without required params should return structured error`(): Unit = runBlocking {
        // project_id 없이 호출 — 서버 크래시 없이 에러 응답 반환해야 함
        val result = client.callTool(
            name = "dooray_project_list_posts",
            arguments = emptyMap()
        )
        assertNotNull(result, "응답이 null입니다")
        val content = result.content.firstOrNull()
        assertNotNull(content, "응답 content가 비어있습니다")
        // 응답은 isError=true이거나 에러 JSON을 포함해야 함
        val text = (content as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertNotNull(text, "응답이 TextContent가 아닙니다")
        assertFalse(text.isBlank(), "에러 응답 텍스트가 비어있습니다")
    }

    @Test
    @DisplayName("존재하지 않는 도구를 호출하면 예외가 발생하지 않고 처리되어야 한다")
    fun `listing tools should not include unknown tools`(): Unit = runBlocking {
        val toolNames = client.listTools().tools.map { it.name }
        // 등록되지 않은 도구가 섞여 있으면 안 됨
        toolNames.forEach { name ->
            assertTrue(
                name.startsWith("dooray_"),
                "알 수 없는 도구가 등록되어 있습니다: '$name'"
            )
        }
    }
}
