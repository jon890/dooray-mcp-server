package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayHttpClient
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_API_KEY
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_BASE_URL
import com.bifos.dooray.mcp.constants.VersionConst
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.tools.*
import com.bifos.dooray.mcp.utils.Env
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.slf4j.LoggerFactory

class DoorayMcpServer {

    private val log = LoggerFactory.getLogger(DoorayMcpServer::class.java)

    fun initServer() {
        log.info("Dooray MCP Server starting...")

        val doorayHttpClient =
            DoorayHttpClient(
                baseUrl = Env.require(DOORAY_BASE_URL),
                doorayApiKey = Env.require(DOORAY_API_KEY)
            )
        log.info("DOORAY_API_KEY, DOORAY_BASE_URL found, initializing HTTP client...")

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

        val transport =
            StdioServerTransport(System.`in`.asInput(), System.out.asSink().buffered())

        log.info("Starting MCP server on STDIO transport...")

        runBlocking {
            server.createSession(transport)
            log.info("MCP server connected and ready!")

            val done = Job()
            server.onClose {
                log.info("MCP server closing...")
                done.complete()
            }
            done.join()
        }
    }

    fun registerTool(server: Server, doorayHttpClient: DoorayHttpClient) {
        log.info("Adding tools...")

        val projectResolver = ProjectResolver(doorayHttpClient)

        var toolCount = 0

        fun addTool(tool: Tool, handler: suspend (ClientConnection, CallToolRequest) -> CallToolResult) {
            server.addTool(tool, handler)
            toolCount++
        }

        addTool(getWikisTool(), getWikisHandler(doorayHttpClient))
        addTool(getWikiPagesTool(), getWikiPagesHandler(doorayHttpClient))
        addTool(getWikiPageTool(), getWikiPageHandler(doorayHttpClient))
        addTool(createWikiPageTool(), createWikiPageHandler(doorayHttpClient))
        addTool(updateWikiPageTool(), updateWikiPageHandler(doorayHttpClient))
        addTool(getProjectPostsTool(), getProjectPostsHandler(doorayHttpClient, projectResolver))
        addTool(getProjectPostTool(), getProjectPostHandler(doorayHttpClient, projectResolver))
        addTool(createProjectPostTool(), createProjectPostHandler(doorayHttpClient, projectResolver))
        addTool(setProjectPostWorkflowTool(), setProjectPostWorkflowHandler(doorayHttpClient, projectResolver))
        addTool(setProjectPostDoneTool(), setProjectPostDoneHandler(doorayHttpClient, projectResolver))
        addTool(getProjectsTool(), getProjectsHandler(doorayHttpClient, projectResolver))
        addTool(updateProjectPostTool(), updateProjectPostHandler(doorayHttpClient, projectResolver))
        addTool(createPostCommentTool(), createPostCommentHandler(doorayHttpClient, projectResolver))
        addTool(getPostCommentsTool(), getPostCommentsHandler(doorayHttpClient, projectResolver))
        addTool(updatePostCommentTool(), updatePostCommentHandler(doorayHttpClient, projectResolver))
        addTool(deletePostCommentTool(), deletePostCommentHandler(doorayHttpClient, projectResolver))
        addTool(getProjectMembersTool(), getProjectMembersHandler(doorayHttpClient, projectResolver))
        addTool(getProjectWorkflowsTool(), getProjectWorkflowsHandler(doorayHttpClient, projectResolver))
        addTool(setProjectPostParentTool(), setProjectPostParentHandler(doorayHttpClient, projectResolver))

        log.info("Successfully added $toolCount tools to MCP server")
    }
}
