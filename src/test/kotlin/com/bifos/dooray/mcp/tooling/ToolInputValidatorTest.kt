package com.bifos.dooray.mcp.tooling

import com.bifos.dooray.mcp.exception.ToolException
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class ToolInputValidatorTest {
    private val schema = ToolSchema(
        properties = buildJsonObject {
            putJsonObject("name") { put("type", "string") }
            putJsonObject("count") {
                put("type", "integer")
                put("minimum", 1)
                put("maximum", 10)
            }
        },
        required = listOf("name"),
    )

    @Test
    fun `필수 입력과 형식과 범위를 검증한다`() {
        assertEquals(
            "MISSING_REQUIRED_PARAMETER",
            assertFailsWith<ToolException> {
                ToolInputValidator.validate(schema, buildJsonObject {})
            }.stableCode,
        )
        assertEquals(
            "INVALID_PARAMETER_TYPE",
            assertFailsWith<ToolException> {
                ToolInputValidator.validate(schema, buildJsonObject { put("name", 1) })
            }.stableCode,
        )
        assertEquals(
            "INVALID_PARAMETER_RANGE",
            assertFailsWith<ToolException> {
                ToolInputValidator.validate(
                    schema,
                    buildJsonObject {
                        put("name", "ok")
                        put("count", 11)
                    },
                )
            }.stableCode,
        )
        assertEquals(
            "INVALID_PARAMETER_TYPE",
            assertFailsWith<ToolException> {
                ToolInputValidator.validate(
                    schema,
                    buildJsonObject {
                        put("name", "ok")
                        put("count", "5")
                    },
                )
            }.stableCode,
        )
        ToolInputValidator.validate(
            schema,
            buildJsonObject {
                put("name", "ok")
                put("count", 2)
            },
        )
    }
}
