package com.bifos.dooray.mcp.exception

class CustomException(
        message: String? = null,
        val httpStatus: Int? = null,
        rootCause: Throwable? = null,
) : RuntimeException(buildMessage(message, httpStatus), rootCause) {

    companion object {
        private fun buildMessage(message: String?, httpStatus: Int?): String {
            return buildString {
                append("Dooray API 오류")
                if (httpStatus != null) {
                    append(" (HTTP $httpStatus)")
                }
                if (message != null) {
                    append(": $message")
                }
            }
        }
    }

    override fun toString(): String {
        return buildString {
            append("CustomException")
            if (httpStatus != null) {
                append(" [HTTP $httpStatus]")
            }
            append(": $message")
            if (cause != null) {
                append(" (caused by: ${cause})")
            }
        }
    }
}
