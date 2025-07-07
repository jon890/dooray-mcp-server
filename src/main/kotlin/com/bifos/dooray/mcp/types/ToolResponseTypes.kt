package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** MCP Tool 성공 응답 */
@Serializable
data class ToolSuccessResponse<T>(
    val success: Boolean = true,
    val data: T,
    val message: String? = null
)

/** MCP Tool 에러 응답 */
@Serializable
data class ToolErrorResponse(
    val isError: Boolean = true,
    val error: ToolError,
    val content: ToolErrorContent,
)

@Serializable
data class ToolErrorContent(
    val type: String = "text",
    val text: String
)

/** Tool 에러 정보 */
@Serializable
data class ToolError(val type: String, val code: String? = null, val details: String? = null)
