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
        val success: Boolean = false,
        val error: ToolError,
        val message: String
)

/** Tool 에러 정보 */
@Serializable
data class ToolError(val type: String, val code: String? = null, val details: String? = null)
