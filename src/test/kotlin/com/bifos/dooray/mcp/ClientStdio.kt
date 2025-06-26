package com.bifos.dooray.mcp

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


fun main() : Unit = runBlocking {
    val process = ProcessBuilder("java", "-jar", "build/libs/dooray-mcp-server-0.1.0-all.jar")
        .start()

    val transport = StdioClientTransport(
        input = process.inputStream.asSource().buffered(),
        output = process.outputStream.asSink().buffered()
    )

    // Initialize the MCP client with client information
    val client = Client(
        clientInfo = Implementation(name = "Dooray MCP Server", version = "0.1.0"),
    )

    client.connect(transport)

    val toolsList = client.listTools()?.tools?.map { it.name }
    println("Available Tools = $toolsList")

    val getWikiResult = client.callTool(
        CallToolRequest(
            name = "get_wiki",
            arguments = JsonObject(mapOf("projectId" to JsonPrimitive("3647142034893802388")))
        )
    )
}