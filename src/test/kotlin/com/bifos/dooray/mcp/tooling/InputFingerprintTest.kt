package com.bifos.dooray.mcp.tooling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class InputFingerprintTest {
    @Test
    fun `키 순서와 실행 메타는 입력 지문을 바꾸지 않는다`() {
        val first = buildJsonObject {
            put("title", "제목")
            put("request_id", "request-a")
            put("count", 1)
        }
        val second = buildJsonObject {
            put("count", 1)
            put("confirmation_token", "token")
            put("request_id", "request-b")
            put("title", "제목")
        }
        assertEquals(InputFingerprint.of(first), InputFingerprint.of(second))
    }

    @Test
    fun `업무 입력이 바뀌면 지문도 바뀐다`() {
        val first = buildJsonObject { put("title", "제목") }
        val second = buildJsonObject { put("title", "다른 제목") }
        assertNotEquals(InputFingerprint.of(first), InputFingerprint.of(second))
    }
}
