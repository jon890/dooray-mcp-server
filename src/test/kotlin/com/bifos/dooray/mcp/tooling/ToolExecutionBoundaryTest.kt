package com.bifos.dooray.mcp.tooling

import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.ResultId
import com.bifos.dooray.mcp.types.ResultMode
import com.bifos.dooray.mcp.types.ToolExecutionSuccess
import com.bifos.dooray.mcp.types.ToolEffect
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.util.concurrent.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class ToolExecutionBoundaryTest {
    @Test
    fun `성공은 구조화 결과와 간결 텍스트를 반환한다`() = runTest {
        val ids = ArrayDeque(listOf("request-1", "correlation-1"))
        val boundary = ToolExecutionBoundary(idFactory = { ids.removeFirst() })

        val result = boundary.execute(operation = "list", resultMode = ResultMode.COMPACT) {
            ToolExecutionSuccess(
                summary = "프로젝트 1개를 조회했습니다.",
                ids = listOf(ResultId("project", "1")),
                data = buildJsonObject { put("count", 1) },
            )
        }

        assertEquals(false, result.isError)
        assertEquals("프로젝트 1개를 조회했습니다.", (result.content.single() as TextContent).text)
        val structured = assertNotNull(result.structuredContent)
        assertTrue(structured["ok"]!!.jsonPrimitive.boolean)
        assertEquals("request-1", structured["meta"]!!.jsonObject["requestId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `ids 결과 모드는 data를 생략한다`() = runTest {
        val boundary = ToolExecutionBoundary(idFactory = { "id" })
        val result = boundary.execute(operation = "get", resultMode = ResultMode.IDS) {
            ToolExecutionSuccess(
                summary = "업무를 조회했습니다.",
                ids = listOf(ResultId("post", "1")),
                data = buildJsonObject { put("secretBody", "본문") },
            )
        }
        assertFalse(assertNotNull(result.structuredContent).containsKey("data"))
    }

    @Test
    fun `공통 메타 입력을 실행 문맥으로 파싱한다`() = runTest {
        val requestId = "123e4567-e89b-12d3-a456-426614174000"
        val arguments = buildJsonObject {
            put("request_id", requestId)
            put("result_mode", "ids")
            put("dry_run", true)
        }
        val boundary = ToolExecutionBoundary(idFactory = { "correlation" }, clock = { 1_000 })
        val result = boundary.execute(
            operation = "create",
            arguments = arguments,
            timeoutMillis = 30_000,
            principalFingerprint = "principal",
        ) { context ->
            assertEquals(requestId, context.requestId)
            assertEquals(ResultMode.IDS, context.resultMode)
            assertTrue(context.dryRun)
            assertEquals(31_000, context.deadlineEpochMillis)
            assertEquals("principal", context.principalFingerprint)
            ToolExecutionSuccess("변경 계획을 만들었습니다.")
        }
        val meta = result.structuredContent!!["meta"]!!.jsonObject
        assertEquals("ids", meta["resultMode"]!!.jsonPrimitive.content)
        assertTrue(meta["dryRun"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `잘못된 request_id는 실행 전에 거부한다`() = runTest {
        val boundary = ToolExecutionBoundary(idFactory = { "correlation" })
        val result = boundary.execute(
            operation = "create",
            arguments = buildJsonObject { put("request_id", "not-a-uuid") },
        ) { ToolExecutionSuccess("실행되면 안 됩니다.") }
        assertEquals(true, result.isError)
        assertEquals(
            "INVALID_REQUEST_ID",
            result.structuredContent!!["error"]!!.jsonObject["code"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `공통 메타 입력의 객체 값은 구조화된 입력 오류로 거부한다`() = runTest {
        val cases = listOf(
            "request_id" to "INVALID_REQUEST_ID",
            "result_mode" to "INVALID_RESULT_MODE",
            "dry_run" to "INVALID_DRY_RUN",
        )

        cases.forEach { (name, expectedCode) ->
            val result = ToolExecutionBoundary(idFactory = { "generated-id" }).execute(
                operation = "create",
                arguments = buildJsonObject {
                    putJsonObject(name) { put("unexpected", true) }
                },
            ) { ToolExecutionSuccess("실행되면 안 됩니다.") }

            assertEquals(true, result.isError)
            assertEquals(
                expectedCode,
                result.structuredContent!!["error"]!!.jsonObject["code"]!!.jsonPrimitive.content,
            )
        }
    }

    @Test
    fun `도구 오류는 isError와 안정 코드를 반환한다`() = runTest {
        val boundary = ToolExecutionBoundary(idFactory = { "id" })
        val result = boundary.execute(operation = "create") {
            throw ToolException.invalidArgument("INVALID_TITLE", "제목이 필요합니다.")
        }

        assertEquals(true, result.isError)
        val error = assertNotNull(result.structuredContent)["error"]!!.jsonObject
        assertEquals("INVALID_TITLE", error["code"]!!.jsonPrimitive.content)
        assertFalse(error["retryable"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `legacy 오류 텍스트는 기존 필드 집합을 유지한다`() {
        val result = ToolExecutionBoundary(idFactory = { "id" }).legacyErrorResult(
            ToolException.invalidArgument("INVALID_TITLE", "제목이 필요합니다."),
        )
        val text = (result.content.single() as TextContent).text
        val compatibility = Json.parseToJsonElement(text).jsonObject

        assertEquals(setOf("isError", "error", "content"), compatibility.keys)
        assertEquals(setOf("type", "code", "details"), compatibility["error"]!!.jsonObject.keys)
    }

    @Test
    fun `예상하지 못한 오류는 비밀과 스택을 노출하지 않는다`() = runTest {
        val secret = "dooray-secret-value"
        val boundary = ToolExecutionBoundary(
            idFactory = { "correlation" },
            redactor = PublicOutputRedactor(listOf(secret)),
        )
        val result = boundary.execute(operation = "send") {
            error("$secret at com.example.Secret.file(/Users/test/Secret.kt:10)")
        }
        val wire = result.structuredContent.toString() + (result.content.single() as TextContent).text
        assertFalse(wire.contains(secret))
        assertFalse(wire.contains("com.example"))
        assertFalse(wire.contains("/Users/test"))
        assertEquals("INTERNAL_ERROR", result.structuredContent!!["error"]!!.jsonObject["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `시간 초과는 재시도 가능한 안정 오류다`() = runTest {
        val boundary = ToolExecutionBoundary(defaultTimeoutMillis = 1, idFactory = { "id" })
        val result = boundary.execute(operation = "list") {
            delay(10)
            ToolExecutionSuccess("완료")
        }
        val error = result.structuredContent!!["error"]!!.jsonObject
        assertEquals("TIMEOUT", error["code"]!!.jsonPrimitive.content)
        assertTrue(error["retryable"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `쓰기 시간 초과는 결과 불명확이며 재시도할 수 없다`() = runTest {
        val boundary = ToolExecutionBoundary(defaultTimeoutMillis = 1, idFactory = { "id" })
        val result = boundary.execute(operation = "create", effect = ToolEffect.WRITE) {
            delay(10)
            ToolExecutionSuccess("완료")
        }
        val error = result.structuredContent!!["error"]!!.jsonObject
        assertEquals("OUTCOME_UNKNOWN", error["code"]!!.jsonPrimitive.content)
        assertFalse(error["retryable"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `호출 취소는 오류 결과로 바꾸지 않는다`() = runTest {
        val boundary = ToolExecutionBoundary(idFactory = { "id" })
        assertFailsWith<CancellationException> {
            boundary.execute(operation = "list") { throw CancellationException("cancel") }
        }
    }

    @Test
    fun `실패한 쓰기를 자동 재시도하지 않는다`() = runTest {
        var calls = 0
        val boundary = ToolExecutionBoundary(defaultTimeoutMillis = 1, idFactory = { "id" })
        val result = boundary.execute(operation = "create", effect = ToolEffect.WRITE) {
            calls++
            delay(10)
            ToolExecutionSuccess("완료")
        }
        assertEquals(1, calls)
        assertFalse(result.structuredContent!!["error"]!!.jsonObject["retryable"]!!.jsonPrimitive.boolean)
    }
}
