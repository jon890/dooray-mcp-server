package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.tooling.DefaultToolExecutionBoundary
import com.bifos.dooray.mcp.types.DoorayApiHeader
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.types.ToolEffect
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

suspend fun toolHandler(
    effect: ToolEffect = ToolEffect.READ,
    block: suspend () -> CallToolResult,
): CallToolResult {
    return DefaultToolExecutionBoundary.instance.executeLegacy(effect, block)
}

fun ToolException.toCallToolResult(): CallToolResult =
    DefaultToolExecutionBoundary.instance.legacyErrorResult(this)

inline fun <reified T : Any> successResult(data: T, message: String): CallToolResult =
    ToolSuccessResponse(data = JsonUtils.toJsonElement(data), message = message).let { response ->
        DefaultToolExecutionBoundary.instance.legacySuccessResult(
            data = response.data,
            compatibilityText = JsonUtils.toJsonString(response),
        )
    }

fun successResult(message: String): CallToolResult =
    ToolSuccessResponse(message = message).let { response ->
        DefaultToolExecutionBoundary.instance.legacySuccessResult(
            data = null,
            compatibilityText = JsonUtils.toJsonString(response),
        )
    }

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

fun CallToolRequest.intParam(name: String, default: Int): Int =
    arguments?.get(name)?.jsonPrimitive?.content?.toIntOrNull() ?: default

fun CallToolRequest.intParamOrNull(name: String): Int? =
    arguments?.get(name)?.jsonPrimitive?.content?.toIntOrNull()

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
