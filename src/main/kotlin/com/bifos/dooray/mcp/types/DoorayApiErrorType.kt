package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** 에러 전용 응답 타입 (result 필드 없음) */
@Serializable
data class DoorayErrorResponse(val header: DoorayApiErrorHeader)

/** Dooray API 응답 헤더 */
@Serializable
data class DoorayApiErrorHeader(
    val resultMessage: String,
)