package com.bifos.dooray.mcp.exception

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/** Tool 예외 클래스 */
class ToolException(
    val type: String,
    message: String,
    val code: String? = null,
    val retryable: Boolean = false,
    val retryAfterMillis: Long? = null,
    val safeDetails: JsonObject = buildJsonObject {},
    cause: Throwable? = null,
) : Exception(message, cause) {

    constructor(
        errorCode: ToolErrorCode,
        message: String,
        retryAfterMillis: Long? = null,
        safeDetails: JsonObject = buildJsonObject {},
        cause: Throwable? = null,
    ) : this(
        type = errorCode.name,
        message = message,
        code = errorCode.name,
        retryable = errorCode.retryableByDefault,
        retryAfterMillis = retryAfterMillis,
        safeDetails = safeDetails,
        cause = cause,
    )

    val stableCode: String
        get() = code ?: type

    companion object {
        const val VALIDATION_ERROR = "VALIDATION_ERROR"
        const val API_ERROR = "API_ERROR"
        const val PARAMETER_MISSING = "PARAMETER_MISSING"
        const val INTERNAL_ERROR = "INTERNAL_ERROR"
        const val TIMEOUT = "TIMEOUT"

        fun invalidArgument(code: String, message: String): ToolException =
            ToolException(
                type = VALIDATION_ERROR,
                code = code,
                message = message,
                retryable = false,
            )
    }
}
