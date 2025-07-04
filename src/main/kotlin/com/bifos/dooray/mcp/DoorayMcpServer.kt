package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayHttpClient
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_API_KEY
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_BASE_URL
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
                Implementation(name = "dooray-mcp", version = "0.1.4"),
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

        // 기존 도구: 위키 페이지 목록 조회
        val wikiPagesTool = getWikiPagesTool()
        server.addTool(
            name = wikiPagesTool.name,
            description = wikiPagesTool.description ?: "",
            inputSchema = wikiPagesTool.inputSchema,
            handler = getWikiPagesHandler(doorayHttpClient)
        )

        // 새로운 도구: 접근 가능한 위키 목록 조회
        val wikisTool = getWikisTool()
        server.addTool(
            name = wikisTool.name,
            description = wikisTool.description ?: "",
            inputSchema = wikisTool.inputSchema,
            handler = getWikisHandler(doorayHttpClient)
        )

        // 새로운 도구: 위키 페이지 상세 조회
        val wikiPageTool = getWikiPageTool()
        server.addTool(
            name = wikiPageTool.name,
            description = wikiPageTool.description ?: "",
            inputSchema = wikiPageTool.inputSchema,
            handler = getWikiPageHandler(doorayHttpClient)
        )

        log.info("Successfully added ${3} tools to MCP server")
    }
}
