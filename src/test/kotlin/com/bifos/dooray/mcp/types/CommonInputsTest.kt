package com.bifos.dooray.mcp.types

import com.bifos.dooray.mcp.exception.ToolException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CommonInputsTest {
    @Test
    fun `본문 입력은 정확히 하나만 허용한다`() {
        BodyInput(body = "본문").validate()
        BodyInput(bodyResourceUri = "dooray://resource/1").validate()
        BodyInput(bodyLocalPath = "/data/in/body.md").validate()

        val missing = assertFailsWith<ToolException> { BodyInput().validate() }
        assertEquals("INVALID_BODY_INPUT", missing.stableCode)

        val conflict = assertFailsWith<ToolException> {
            BodyInput(body = "본문", bodyResourceUri = "dooray://resource/1").validate()
        }
        assertEquals("INVALID_BODY_INPUT", conflict.stableCode)
    }

    @Test
    fun `선택 본문은 빈 입력을 허용한다`() {
        BodyInput().validate(required = false)
    }

    @Test
    fun `결과 모드는 안정적인 열거형으로 파싱한다`() {
        assertEquals(ResultMode.COMPACT, ResultMode.parse(null))
        assertEquals(ResultMode.IDS, ResultMode.parse("IDS"))
        assertFailsWith<ToolException> { ResultMode.parse("quiet") }
    }
}
