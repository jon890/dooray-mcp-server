package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayHttpClient
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_API_KEY
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_BASE_URL
import com.bifos.dooray.mcp.constants.VersionConst
import com.bifos.dooray.mcp.tools.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.slf4j.LoggerFactory

class DoorayMcpServer {

    private val log = LoggerFactory.getLogger(DoorayMcpServer::class.java)

    fun initServer() {
        // 서버 시작 로그를 stderr로 출력 (stdout은 MCP 통신용이므로)
        log.info("Dooray MCP Server starting...")

        val env = getEnv()

        log.info("DOORAY_API_KEY found, initializing HTTP client...")
        val doorayHttpClient =
                DoorayHttpClient(
                        baseUrl = env[DOORAY_BASE_URL]!!,
                        doorayApiKey = env[DOORAY_API_KEY]!!
                )

        val server =
                Server(
                        Implementation(name = "dooray-mcp-server", version = VersionConst.VERSION),
                        ServerOptions(
                                capabilities =
                                        ServerCapabilities(
                                                tools = ServerCapabilities.Tools(listChanged = true)
                                        )
                        )
                )

        registerTool(server, doorayHttpClient)

        // Create a transport using standard IO for server communication
        val transport = StdioServerTransport(System.`in`.asInput(), System.out.asSink().buffered())

        log.info("Starting MCP server on STDIO transport...")

        runBlocking {
            server.connect(transport)
            log.info("MCP server connected and ready!")

            val done = Job()
            server.onClose {
                log.info("MCP server closing...")
                done.complete()
            }
            done.join()
        }
    }

    fun getEnv(): Map<String, String> {
        val baseUrl =
                System.getenv(DOORAY_BASE_URL)
                        ?: throw IllegalArgumentException("DOORAY_BASE_URL is required.")
        val apiKey =
                System.getenv(DOORAY_API_KEY)
                        ?: throw IllegalArgumentException("DOORAY_API_KEY is required.")

        return mapOf(
                DOORAY_BASE_URL to baseUrl,
                DOORAY_API_KEY to apiKey,
        )
    }

    fun registerTool(server: Server, doorayHttpClient: DoorayHttpClient) {
        log.info("Adding tools...")

        // 1. 위키 프로젝트 목록 조회
        val wikisTool = getWikisTool()
        server.addTool(
                name = wikisTool.name,
                description = wikisTool.description ?: "",
                inputSchema = wikisTool.inputSchema,
                handler = getWikisHandler(doorayHttpClient)
        )

        // 2. 위키 페이지 목록 조회
        val wikiPagesTool = getWikiPagesTool()
        server.addTool(
                name = wikiPagesTool.name,
                description = wikiPagesTool.description ?: "",
                inputSchema = wikiPagesTool.inputSchema,
                handler = getWikiPagesHandler(doorayHttpClient)
        )

        // 3. 위키 페이지 상세 조회
        val wikiPageTool = getWikiPageTool()
        server.addTool(
                name = wikiPageTool.name,
                description = wikiPageTool.description ?: "",
                inputSchema = wikiPageTool.inputSchema,
                handler = getWikiPageHandler(doorayHttpClient)
        )

        // 4. 위키 페이지 생성
        val createWikiPageTool = createWikiPageTool()
        server.addTool(
                name = createWikiPageTool.name,
                description = createWikiPageTool.description ?: "",
                inputSchema = createWikiPageTool.inputSchema,
                handler = createWikiPageHandler(doorayHttpClient)
        )

        // 5. 위키 페이지 수정
        val updateWikiPageTool = updateWikiPageTool()
        server.addTool(
                name = updateWikiPageTool.name,
                description = updateWikiPageTool.description ?: "",
                inputSchema = updateWikiPageTool.inputSchema,
                handler = updateWikiPageHandler(doorayHttpClient)
        )

        // 6. 위키 페이지 제목 수정
        val updateWikiPageTitleTool = updateWikiPageTitleTool()
        server.addTool(
                name = updateWikiPageTitleTool.name,
                description = updateWikiPageTitleTool.description ?: "",
                inputSchema = updateWikiPageTitleTool.inputSchema,
                handler = updateWikiPageTitleHandler(doorayHttpClient)
        )

        // 7. 위키 페이지 내용 수정
        val updateWikiPageContentTool = updateWikiPageContentTool()
        server.addTool(
                name = updateWikiPageContentTool.name,
                description = updateWikiPageContentTool.description ?: "",
                inputSchema = updateWikiPageContentTool.inputSchema,
                handler = updateWikiPageContentHandler(doorayHttpClient)
        )

        // 8. 위키 페이지 참조자 수정
        val updateWikiPageReferrersTool = updateWikiPageReferrersTool()
        server.addTool(
                name = updateWikiPageReferrersTool.name,
                description = updateWikiPageReferrersTool.description ?: "",
                inputSchema = updateWikiPageReferrersTool.inputSchema,
                handler = updateWikiPageReferrersHandler(doorayHttpClient)
        )

        // ============ 프로젝트 업무 관련 도구들 ============

        // 9. 프로젝트 업무 목록 조회
        val projectPostsTool = getProjectPostsTool()
        server.addTool(
                name = projectPostsTool.name,
                description = projectPostsTool.description ?: "",
                inputSchema = projectPostsTool.inputSchema,
                handler = getProjectPostsHandler(doorayHttpClient)
        )

        // 10. 프로젝트 업무 상세 조회
        val projectPostTool = getProjectPostTool()
        server.addTool(
                name = projectPostTool.name,
                description = projectPostTool.description ?: "",
                inputSchema = projectPostTool.inputSchema,
                handler = getProjectPostHandler(doorayHttpClient)
        )

        // 11. 프로젝트 업무 생성
        val createProjectPostTool = createProjectPostTool()
        server.addTool(
                name = createProjectPostTool.name,
                description = createProjectPostTool.description ?: "",
                inputSchema = createProjectPostTool.inputSchema,
                handler = createProjectPostHandler(doorayHttpClient)
        )

        // 12. 프로젝트 업무 상태 변경
        val setProjectPostWorkflowTool = setProjectPostWorkflowTool()
        server.addTool(
                name = setProjectPostWorkflowTool.name,
                description = setProjectPostWorkflowTool.description ?: "",
                inputSchema = setProjectPostWorkflowTool.inputSchema,
                handler = setProjectPostWorkflowHandler(doorayHttpClient)
        )

        // 13. 프로젝트 업무 완료 처리
        val setProjectPostDoneTool = setProjectPostDoneTool()
        server.addTool(
                name = setProjectPostDoneTool.name,
                description = setProjectPostDoneTool.description ?: "",
                inputSchema = setProjectPostDoneTool.inputSchema,
                handler = setProjectPostDoneHandler(doorayHttpClient)
        )

        log.info("Successfully added ${13} tools to MCP server")
    }
}
