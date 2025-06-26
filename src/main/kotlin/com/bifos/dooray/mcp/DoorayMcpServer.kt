package com.bifos.dooray.mcp

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.*


fun runDoorayMcpServer() {
    val baseUrl = "https://api.dooray.com"

    val doorayApiKey = System.getenv("DOORAY_API_KEY")
        ?: throw IllegalArgumentException("DOORAY_API_KEY environment variable is required.")

    val httpClient = HttpClient {
        defaultRequest {
            url(baseUrl)
            headers {
                append("Authorization", "dooray-api $doorayApiKey")
            }
            contentType(ContentType.Application.Json)
        }
        // install content negotiation plugin for JSON serialization/deserialization
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            }
            )
        }
    }

    val server = Server(
        Implementation(
            name = "dooray-mcp",
            version = "0.1.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    )

    server.addTool(
        name = "get_wiki",
        description = """
            Get wiki for a specific project.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("projectId") {
                    put("type", "string")
                }
            },
            required = listOf("projectId")
        )
    ) { request ->
        val projectId = request.arguments["projectId"]?.jsonPrimitive?.content
        if (projectId == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("The 'projectId' parameters are required."))
            )
        }

        val wiki = httpClient.get("/wiki/v1/wikis$projectId").body<Any>()

        CallToolResult(content = listOf(TextContent(wiki as String?)))
    }

    // Create a transport using standard IO for server communication
    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered()
    )

    runBlocking {
        server.connect(transport)
        val done = Job()
        server.onClose {
            done.complete()
        }
        done.join()
    }
}