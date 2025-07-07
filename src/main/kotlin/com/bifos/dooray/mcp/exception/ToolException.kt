package com.bifos.dooray.mcp.exception

import com.bifos.dooray.mcp.types.ToolError
import com.bifos.dooray.mcp.types.ToolErrorContent
import com.bifos.dooray.mcp.types.ToolErrorResponse

/** Tool 예외 클래스 */
class ToolException(
    val type: String,
    message: String,
    val code: String? = null,
    val details: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    fun toErrorResponse(): ToolErrorResponse {
        return ToolErrorResponse(
            error = ToolError(type = type, code = code, details = details),
            content = ToolErrorContent(text = message ?: "알 수 없는 오류가 발생했습니다")
        )
    }

    companion object {
        const val VALIDATION_ERROR = "VALIDATION_ERROR"
        const val API_ERROR = "API_ERROR"
        const val PARAMETER_MISSING = "PARAMETER_MISSING"
        const val INTERNAL_ERROR = "INTERNAL_ERROR"
    }
}