package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.DoorayApiHeader
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.jsonPrimitive

/**
 * 공통 tool handler 래퍼.
 * - ToolException → 적절한 에러 응답으로 변환
 * - Exception → INTERNAL_ERROR 응답으로 변환
 */
suspend fun toolHandler(block: suspend () -> CallToolResult): CallToolResult {
    return try {
        block()
    } catch (e: ToolException) {
        e.toCallToolResult()
    } catch (e: Exception) {
        ToolException(
            type = ToolException.INTERNAL_ERROR,
            message = "내부 오류가 발생했습니다: ${e.message}",
            details = e.stackTraceToString()
        ).toCallToolResult()
    }
}

/** ToolException을 CallToolResult로 변환 */
fun ToolException.toCallToolResult(): CallToolResult =
    CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(toErrorResponse()))))

/** 성공 응답 CallToolResult 생성 (데이터 있음) */
inline fun <reified T : Any> successResult(data: T, message: String): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(JsonUtils.toJsonString(
            ToolSuccessResponse(data = JsonUtils.toJsonElement(data), message = message)
        )))
    )

/** 성공 응답 CallToolResult 생성 (데이터 없음) */
fun successResult(message: String): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(JsonUtils.toJsonString(ToolSuccessResponse(message = message))))
    )

/** API 에러 응답 CallToolResult 생성 */
fun apiErrorResult(header: DoorayApiHeader): CallToolResult =
    ToolException(
        type = ToolException.API_ERROR,
        message = header.resultMessage,
        code = "DOORAY_API_${header.resultCode}"
    ).toCallToolResult()

/**
 * 필수 String 파라미터를 추출합니다.
 * 값이 없으면 ToolException(PARAMETER_MISSING)을 던집니다.
 */
fun CallToolRequest.requireParam(name: String, errorCode: String, errorMessage: String): String =
    arguments?.get(name)?.jsonPrimitive?.content
        ?: throw ToolException(
            type = ToolException.PARAMETER_MISSING,
            message = errorMessage,
            code = errorCode
        )

/**
 * 선택적 String 파라미터를 추출합니다.
 */
fun CallToolRequest.optionalParam(name: String): String? =
    arguments?.get(name)?.jsonPrimitive?.content
