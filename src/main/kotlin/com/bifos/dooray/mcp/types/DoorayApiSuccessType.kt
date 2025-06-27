package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** Dooray API 공통 응답 래퍼 */
@Serializable
data class DoorayApiResponse<T>(
    val header: DoorayApiHeader,
    val result: T
)

/** Dooray API 응답 헤더 */
@Serializable
data class DoorayApiHeader(
    val isSuccessful: Boolean,
    val resultCode: Int,
    val resultMessage: String
)