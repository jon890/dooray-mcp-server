package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayHttpClient
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_API_KEY
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_BASE_URL
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_MCP_TOOL_PROFILE
import com.bifos.dooray.mcp.constants.VersionConst
import com.bifos.dooray.mcp.tooling.LegacyToolModule
import com.bifos.dooray.mcp.tooling.ToolProfile
import com.bifos.dooray.mcp.tooling.ToolRegistry
import com.bifos.dooray.mcp.utils.Env
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.Sink
import kotlinx.io.Source
import org.slf4j.LoggerFactory

class DoorayMcpServer {

    private val log = LoggerFactory.getLogger(DoorayMcpServer::class.java)

    fun initServer(input: Source, output: Sink) {
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
                                    listChanged = false
                                )
                        )
                )
            )

        val profile = ToolProfile.parse(Env.get(DOORAY_MCP_TOOL_PROFILE))
        val toolCount = ToolRegistry(
            profile = profile,
            modules = listOf(LegacyToolModule(doorayHttpClient)),
        ).register(server)
        log.info("Successfully added {} tools for profile {}", toolCount, profile.wireName)

        val transport =
            StdioServerTransport(input = input, output = output)

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

}
