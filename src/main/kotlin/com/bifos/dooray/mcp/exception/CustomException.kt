package com.bifos.dooray.mcp.exception

class CustomException(
    message: String? = null,
    httpStatus: Int? = null,
    rootCause: Throwable? = null,
) : RuntimeException(message, rootCause) {
}