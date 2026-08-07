package com.bifos.dooray.mcp.tooling

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool

typealias ToolHandler = suspend (ClientConnection, CallToolRequest) -> CallToolResult

data class ToolBinding(
    val tool: Tool,
    val handler: ToolHandler,
)

interface ToolModule {
    val toolSet: ToolSet
    fun bindings(): List<ToolBinding>
}
