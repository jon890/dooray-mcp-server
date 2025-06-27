package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** Dooray API 공통 응답 래퍼 HTTP 에러 상태에서는 result가 없을 수 있음 */
@Serializable
data class DoorayApiResponse<T>(
    val header: DoorayApiHeader,
    val result: T? = null // 에러 응답에서는 result가 없을 수 있음
)

/** Dooray API 응답 헤더 */
@Serializable
data class DoorayApiHeader(
    val isSuccessful: Boolean,
    val resultCode: Int,
    val resultMessage: String
)