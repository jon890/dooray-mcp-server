package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.constants.VersionConst
import com.bifos.dooray.mcp.util.parseEnv
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered


fun main(): Unit = runBlocking {
    val env = parseEnv()

    val processBuilder = ProcessBuilder("java", "-jar", "build/libs/dooray-mcp-server-${VersionConst.VERSION}-all.jar")
    processBuilder.environment().putAll(env)
    val process = processBuilder.start()

    val transport = StdioClientTransport(
        input = process.inputStream.asSource().buffered(),
        output = process.outputStream.asSink().buffered()
    )

    // Initialize the MCP client with client information
    val client = Client(
        clientInfo = Implementation(name = "Dooray MCP Client", version = "0.1.0"),
    )

    client.connect(transport)

    val toolsList = client.listTools()?.tools?.map { it.name }
    println("Available Tools = $toolsList")
}