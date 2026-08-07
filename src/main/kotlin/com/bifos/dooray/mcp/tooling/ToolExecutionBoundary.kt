package com.bifos.dooray.mcp.tooling

import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.exception.ToolErrorCode
import com.bifos.dooray.mcp.types.RequestContext
import com.bifos.dooray.mcp.types.ResultMode
import com.bifos.dooray.mcp.types.ToolExecutionSuccess
import com.bifos.dooray.mcp.types.ToolEffect
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory

class ToolExecutionBoundary(
    private val defaultTimeoutMillis: Long = 60_000,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val redactor: PublicOutputRedactor = PublicOutputRedactor.fromEnvironment(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val log = LoggerFactory.getLogger(ToolExecutionBoundary::class.java)

    suspend fun execute(
        operation: String,
        effect: ToolEffect = ToolEffect.READ,
        requestId: String? = null,
        resultMode: ResultMode? = null,
        dryRun: Boolean? = null,
        timeoutMillis: Long = defaultTimeoutMillis,
        arguments: JsonObject = buildJsonObject {},
        inputSchema: ToolSchema? = null,
        principalFingerprint: String? = null,
        block: suspend (RequestContext) -> ToolExecutionSuccess,
    ): CallToolResult {
        val rawRequestId = requestId ?: arguments["request_id"]?.jsonPrimitive?.contentOrNull
        val resolvedRequestId = rawRequestId ?: idFactory()
        val context = RequestContext(
            operation = operation,
            requestId = resolvedRequestId,
            correlationId = idFactory(),
            resultMode = resultMode ?: ResultMode.COMPACT,
            dryRun = dryRun ?: false,
            arguments = arguments,
            deadlineEpochMillis = Math.addExact(clock(), timeoutMillis),
            principalFingerprint = principalFingerprint,
        )

        return try {
            rawRequestId?.let(::validateRequestId)
            val resolvedContext = context.copy(
                resultMode = resultMode
                    ?: ResultMode.parse(arguments["result_mode"]?.jsonPrimitive?.contentOrNull),
                dryRun = dryRun ?: parseDryRun(arguments),
            )
            inputSchema?.let { ToolInputValidator.validate(it, arguments) }
            val success = withTimeout(timeoutMillis) { block(resolvedContext) }
            successResult(resolvedContext, success)
        } catch (error: TimeoutCancellationException) {
            errorResult(
                context,
                timeoutError(effect, error),
            )
        } catch (error: ToolException) {
            errorResult(context, error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logUnexpectedError(context.correlationId, error)
            errorResult(
                context,
                ToolException(
                    errorCode = ToolErrorCode.INTERNAL_ERROR,
                    message = "내부 오류가 발생했습니다.",
                    cause = error,
                )
            )
        }
    }

    suspend fun executeLegacy(
        effect: ToolEffect = ToolEffect.READ,
        block: suspend () -> CallToolResult,
    ): CallToolResult =
        try {
            withTimeout(defaultTimeoutMillis) { normalizeLegacyResult(block()) }
        } catch (error: TimeoutCancellationException) {
            legacyErrorResult(timeoutError(effect, error))
        } catch (error: ToolException) {
            legacyErrorResult(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val correlationId = idFactory()
            logUnexpectedError(correlationId, error)
            legacyErrorResult(
                ToolException(
                    errorCode = ToolErrorCode.INTERNAL_ERROR,
                    message = "내부 오류가 발생했습니다.",
                    cause = error,
                ),
                correlationId = correlationId,
            )
        }

    fun legacyErrorResult(
        error: ToolException,
        correlationId: String = idFactory(),
    ): CallToolResult {
        val structured = errorObject(
            operation = "legacy",
            requestId = idFactory(),
            correlationId = correlationId,
            error = error,
        )
        val compatibility = buildJsonObject {
            put("isError", true)
            putJsonObject("error") {
                put("type", error.type)
                put("code", error.stableCode)
                put("details", kotlinx.serialization.json.JsonNull)
                put("retryable", error.retryable)
            }
            putJsonObject("content") {
                put("type", "text")
                put("text", redactor.sanitize(error.message ?: "알 수 없는 오류가 발생했습니다."))
            }
        }
        return CallToolResult(
            content = listOf(TextContent(com.bifos.dooray.mcp.utils.JsonUtils.toJsonString(compatibility))),
            isError = true,
            structuredContent = structured,
        )
    }

    fun legacySuccessResult(
        data: JsonElement?,
        compatibilityText: String,
    ): CallToolResult {
        val structured = buildJsonObject {
            put("ok", true)
            put("operation", "legacy")
            putJsonArray("ids") {}
            data?.let { put("data", it) }
            putJsonObject("meta") {
                put("resultMode", ResultMode.FULL.wireName)
                put("dryRun", false)
                put("requestId", idFactory())
                putJsonArray("warnings") {}
            }
        }
        return CallToolResult(
            content = listOf(TextContent(compatibilityText)),
            isError = false,
            structuredContent = structured,
        )
    }

    private fun successResult(
        context: RequestContext,
        success: ToolExecutionSuccess,
    ): CallToolResult {
        val structured = buildJsonObject {
            put("ok", true)
            put("operation", context.operation)
            putJsonArray("ids") {
                success.ids.forEach { resultId ->
                    add(buildJsonObject {
                        put("kind", resultId.kind)
                        put("id", resultId.id)
                    })
                }
            }
            if (context.resultMode != ResultMode.IDS) {
                put("data", success.data)
            }
            putJsonObject("meta") {
                put("resultMode", context.resultMode.wireName)
                put("dryRun", context.dryRun)
                put("requestId", context.requestId)
                putJsonArray("warnings") { success.warnings.forEach { add(JsonPrimitive(it)) } }
            }
        }
        return CallToolResult(
            content = listOf(TextContent(success.summary)),
            isError = false,
            structuredContent = structured,
        )
    }

    private fun errorResult(context: RequestContext, error: ToolException): CallToolResult =
        CallToolResult(
            content = listOf(TextContent(redactor.sanitize(error.message ?: "알 수 없는 오류가 발생했습니다."))),
            isError = true,
            structuredContent = errorObject(
                operation = context.operation,
                requestId = context.requestId,
                correlationId = context.correlationId,
                error = error,
            ),
        )

    private fun errorObject(
        operation: String,
        requestId: String,
        correlationId: String,
        error: ToolException,
    ): JsonObject = buildJsonObject {
        put("ok", false)
        put("operation", operation)
        putJsonObject("error") {
            put("code", error.stableCode)
            put("message", redactor.sanitize(error.message ?: "알 수 없는 오류가 발생했습니다."))
            put("retryable", error.retryable)
            error.retryAfterMillis?.let { put("retry_after_ms", it) }
            put("details", redactor.sanitize(error.safeDetails))
        }
        putJsonObject("meta") {
            put("requestId", requestId)
            put("correlationId", correlationId)
        }
    }

    private fun normalizeLegacyResult(result: CallToolResult): CallToolResult {
        if (result.structuredContent != null) return result
        val text = (result.content.firstOrNull() as? TextContent)?.text ?: return result
        val structured = runCatching {
            com.bifos.dooray.mcp.utils.JsonUtils.json.parseToJsonElement(text).jsonObject
        }.getOrNull() ?: return result
        return result.copy(structuredContent = structured)
    }

    private fun validateRequestId(requestId: String) {
        try {
            UUID.fromString(requestId)
        } catch (_: IllegalArgumentException) {
            throw ToolException.invalidArgument(
                code = "INVALID_REQUEST_ID",
                message = "request_id는 UUID 형식이어야 합니다."
            )
        }
    }

    private fun parseDryRun(arguments: JsonObject): Boolean {
        val value = arguments["dry_run"] ?: return false
        return value.jsonPrimitive.booleanOrNull
            ?: throw ToolException.invalidArgument(
                code = "INVALID_DRY_RUN",
                message = "dry_run은 boolean 형식이어야 합니다."
            )
    }

    private fun timeoutError(effect: ToolEffect, cause: TimeoutCancellationException): ToolException =
        if (effect == ToolEffect.WRITE) {
            ToolException(
                errorCode = ToolErrorCode.OUTCOME_UNKNOWN,
                message = "변경 요청의 결과를 확인하지 못했습니다. 자동으로 재시도하지 마세요.",
                cause = cause,
            )
        } else {
            ToolException(
                errorCode = ToolErrorCode.TIMEOUT,
                message = "도구 실행 시간이 제한을 초과했습니다.",
                cause = cause,
            )
        }

    private fun logUnexpectedError(correlationId: String, error: Exception) {
        val safeMessage = redactor.sanitize(error.message.orEmpty())
        val safeStack = error.stackTrace
            .take(MAX_LOGGED_STACK_FRAMES)
            .joinToString(" <- ") { redactor.sanitize(it.toString()) }
        log.error(
            "도구 실행 실패 correlationId={} errorType={} message={} stack={}",
            correlationId,
            error::class.qualifiedName,
            safeMessage,
            safeStack,
        )
    }
}

object DefaultToolExecutionBoundary {
    val instance = ToolExecutionBoundary()
}

private const val MAX_LOGGED_STACK_FRAMES = 12
