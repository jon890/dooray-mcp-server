package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.DoorayApiHeader
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

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

fun ToolException.toCallToolResult(): CallToolResult =
    CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(toErrorResponse()))))

inline fun <reified T : Any> successResult(data: T, message: String): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(JsonUtils.toJsonString(
            ToolSuccessResponse(data = JsonUtils.toJsonElement(data), message = message)
        )))
    )

fun successResult(message: String): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(JsonUtils.toJsonString(ToolSuccessResponse(message = message))))
    )

fun apiErrorResult(header: DoorayApiHeader): CallToolResult =
    ToolException(
        type = ToolException.API_ERROR,
        message = header.resultMessage,
        code = "DOORAY_API_${header.resultCode}"
    ).toCallToolResult()

fun CallToolRequest.requireParam(name: String, errorCode: String, errorMessage: String): String =
    arguments?.get(name)?.jsonPrimitive?.content
        ?: throw ToolException(
            type = ToolException.PARAMETER_MISSING,
            message = errorMessage,
            code = errorCode
        )

fun CallToolRequest.optionalParam(name: String): String? =
    arguments?.get(name)?.jsonPrimitive?.content

// ── 공통 스키마 프로퍼티 빌더 ──────────────────────────────────────────────────

fun JsonObjectBuilder.projectIdProperty() {
    putJsonObject("project_id") {
        put("type", "string")
        put("description", "프로젝트 ID 또는 프로젝트 코드 (예: 'my-project' 또는 숫자 ID). 프로젝트 코드는 dooray_project_list_projects로 확인 가능합니다.")
    }
}

fun JsonObjectBuilder.postIdProperty(
    description: String = "업무 ID (dooray_project_list_posts로 조회 가능)"
) {
    putJsonObject("post_id") {
        put("type", "string")
        put("description", description)
    }
}

fun JsonObjectBuilder.paginationProperties(defaultSize: Int = 20, maxSize: Int? = null) {
    putJsonObject("page") {
        put("type", "integer")
        put("description", "페이지 번호 (기본값: 0)")
        put("default", 0)
    }
    putJsonObject("size") {
        put("type", "integer")
        val desc = if (maxSize != null) "페이지 크기 (기본값: $defaultSize, 최대: $maxSize)"
                   else "페이지 크기 (기본값: $defaultSize)"
        put("description", desc)
        put("default", defaultSize)
    }
}
