package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayHttpClient
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_API_KEY
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_BASE_URL
import com.bifos.dooray.mcp.constants.VersionConst
import com.bifos.dooray.mcp.tools.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
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
        log.info("Dooray MCP Server starting...")

        val env = getEnv()

        log.info("DOORAY_API_KEY, DOORAY_BASE_URL found, initializing HTTP client...")
        val doorayHttpClient =
            DoorayHttpClient(
                baseUrl = env[DOORAY_BASE_URL]!!,
                doorayApiKey = env[DOORAY_API_KEY]!!
            )

        val server =
            Server(
                Implementation(
                    name = "dooray-mcp-server",
                    version = VersionConst.VERSION
                ),
                ServerOptions(
                    capabilities =
                        ServerCapabilities(
                            tools =
                                ServerCapabilities.Tools(
                                    listChanged = true
                                )
                        )
                )
            )

        registerTool(server, doorayHttpClient)

        // Create a transport using standard IO for server communication
        val transport =
            StdioServerTransport(System.`in`.asInput(), System.out.asSink().buffered())

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

        var toolCount = 0

        fun addTool(tool: Tool, handler: suspend (CallToolRequest) -> CallToolResult) {
            server.addTool(
                name = tool.name,
                description = tool.description ?: "",
                inputSchema = tool.inputSchema,
                handler = handler
            )
            toolCount++
        }

        // 1. 위키 프로젝트 목록 조회
        addTool(getWikisTool(), getWikisHandler(doorayHttpClient))

        // 2. 위키 페이지 목록 조회
        addTool(getWikiPagesTool(), getWikiPagesHandler(doorayHttpClient))

        // 3. 위키 페이지 상세 조회
        addTool(getWikiPageTool(), getWikiPageHandler(doorayHttpClient))

        // 4. 위키 페이지 생성
        addTool(createWikiPageTool(), createWikiPageHandler(doorayHttpClient))

        // 5. 위키 페이지 수정
        addTool(updateWikiPageTool(), updateWikiPageHandler(doorayHttpClient))

        // ============ 프로젝트 업무 관련 도구들 ============

        // 6. 프로젝트 업무 목록 조회
        addTool(getProjectPostsTool(), getProjectPostsHandler(doorayHttpClient))

        // 7. 프로젝트 업무 상세 조회
        addTool(getProjectPostTool(), getProjectPostHandler(doorayHttpClient))

        // 8. 프로젝트 업무 생성
        addTool(createProjectPostTool(), createProjectPostHandler(doorayHttpClient))

        // 9. 프로젝트 업무 상태 변경
        addTool(
            setProjectPostWorkflowTool(),
            setProjectPostWorkflowHandler(doorayHttpClient)
        )

        // 10. 프로젝트 업무 완료 처리
        addTool(setProjectPostDoneTool(), setProjectPostDoneHandler(doorayHttpClient))

        // 11. 프로젝트 목록 조회
        addTool(getProjectsTool(), getProjectsHandler(doorayHttpClient))

        // 12. 프로젝트 업무 수정
        addTool(updateProjectPostTool(), updateProjectPostHandler(doorayHttpClient))

        // ============ 업무 댓글 관련 도구들 ============

        // 13. 업무 댓글 생성
        addTool(createPostCommentTool(), createPostCommentHandler(doorayHttpClient))

        // 14. 업무 댓글 목록 조회
        addTool(getPostCommentsTool(), getPostCommentsHandler(doorayHttpClient))

        // 15. 업무 댓글 수정
        addTool(updatePostCommentTool(), updatePostCommentHandler(doorayHttpClient))

        // 16. 업무 댓글 삭제
        addTool(deletePostCommentTool(), deletePostCommentHandler(doorayHttpClient))

        log.info("Successfully added $toolCount tools to MCP server")
    }
}
