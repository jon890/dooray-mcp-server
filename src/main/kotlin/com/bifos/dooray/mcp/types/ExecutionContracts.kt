package com.bifos.dooray.mcp.types

import com.bifos.dooray.mcp.exception.ToolException
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

@Serializable
enum class ResultMode {
    @SerialName("ids")
    IDS,

    @SerialName("compact")
    COMPACT,

    @SerialName("full")
    FULL;

    val wireName: String
        get() = name.lowercase(Locale.ROOT)

    companion object {
        fun parse(value: String?): ResultMode =
            entries.firstOrNull { it.wireName == (value ?: "compact").lowercase(Locale.ROOT) }
                ?: throw ToolException.invalidArgument(
                    code = "INVALID_RESULT_MODE",
                    message = "result_mode는 ids, compact, full 중 하나여야 합니다."
                )
    }
}

/** MCP 환경에서 편집기나 표준 입력을 대신하는 명시적 본문 입력이다. */
@Serializable
data class BodyInput(
    val body: String? = null,
    @SerialName("body_resource_uri") val bodyResourceUri: String? = null,
    @SerialName("body_local_path") val bodyLocalPath: String? = null,
) {
    fun validate(required: Boolean = true): BodyInput {
        val populated = listOf(body, bodyResourceUri, bodyLocalPath).count { it != null }
        val valid = if (required) populated == 1 else populated <= 1
        if (!valid) {
            throw ToolException.invalidArgument(
                code = "INVALID_BODY_INPUT",
                message = if (required) {
                    "body, body_resource_uri, body_local_path 중 정확히 하나가 필요합니다."
                } else {
                    "body, body_resource_uri, body_local_path는 동시에 사용할 수 없습니다."
                }
            )
        }
        return this
    }
}

@Serializable
data class ResultId(val kind: String, val id: String)

data class ToolExecutionSuccess(
    val summary: String,
    val ids: List<ResultId> = emptyList(),
    val data: JsonObject = buildJsonObject {},
    val warnings: List<String> = emptyList(),
)

enum class ToolEffect {
    READ,
    WRITE,
}

data class RequestContext(
    val operation: String,
    val requestId: String,
    val correlationId: String,
    val resultMode: ResultMode,
    val dryRun: Boolean,
    val arguments: JsonObject = buildJsonObject {},
    val deadlineEpochMillis: Long = Long.MAX_VALUE,
    val principalFingerprint: String? = null,
)
