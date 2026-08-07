package com.bifos.dooray.mcp.types

import com.bifos.dooray.mcp.utils.JsonUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.tooling.ToolExecutionBoundary
import kotlinx.serialization.json.buildJsonObject

class ExecutionSchemaResourceTest {
    @Test
    fun `공통 JSON Schema가 입력 결과 오류 계약을 고정한다`() {
        val resource = assertNotNull(
            javaClass.getResource("/schema/execution-contract.schema.json"),
            "공통 실행 계약 JSON Schema가 없습니다",
        )
        val root = JsonUtils.json.parseToJsonElement(resource.readText()).jsonObject
        val definitions = root["\$defs"]!!.jsonObject
        assertEquals(setOf("resultMode", "bodyInput", "resultId", "success", "error"), definitions.keys)
        assertEquals(
            listOf("ids", "compact", "full"),
            definitions["resultMode"]!!.jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals(
            3,
            definitions["bodyInput"]!!.jsonObject["oneOf"]!!.jsonArray.size,
        )
    }

    @Test
    fun `실행 경계 결과가 JSON Schema 필수 필드를 모두 제공한다`() = runTest {
        val resource = assertNotNull(javaClass.getResource("/schema/execution-contract.schema.json"))
        val definitions = JsonUtils.json.parseToJsonElement(resource.readText()).jsonObject["\$defs"]!!.jsonObject
        val boundary = ToolExecutionBoundary(idFactory = { "id" })
        val success = boundary.execute(operation = "list") {
            ToolExecutionSuccess(summary = "완료", data = buildJsonObject {})
        }.structuredContent!!
        val failure = boundary.execute(operation = "list") {
            throw ToolException.invalidArgument("INVALID_ARGUMENT", "잘못된 입력입니다.")
        }.structuredContent!!

        assertRequiredFields(definitions["success"]!!.jsonObject, success)
        assertRequiredFields(definitions["error"]!!.jsonObject, failure)
        assertRequiredFields(
            definitions["error"]!!.jsonObject["properties"]!!.jsonObject["error"]!!.jsonObject,
            failure["error"]!!.jsonObject,
        )
    }

    private fun assertRequiredFields(schema: kotlinx.serialization.json.JsonObject, value: kotlinx.serialization.json.JsonObject) {
        val required = schema["required"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(required.all(value::containsKey), "필수 필드가 결과에 없습니다: required=$required actual=${value.keys}")
    }
}
