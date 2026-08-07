package com.bifos.dooray.mcp.tooling

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublicOutputRedactorTest {
    @Test
    fun `비밀 ANSI 이메일과 절대 경로를 공개 결과에서 제거한다`() {
        val redactor = PublicOutputRedactor(listOf("api-secret"))
        val result = redactor.sanitize(
            "\u001B[31mapi-secret\u001B[0m user@example.com /Users/test/private/file.txt " +
                "//Users/test/private/double.txt file:///Users/test/private/secret.txt " +
                "file://internal-host/Users/test/private/remote.txt " +
                "https://dooray.example.com/project/posts/1 dooray://resource/abc/def\nnext"
        )
        assertFalse(result.contains("\u001B"))
        assertFalse(result.contains("api-secret"))
        assertFalse(result.contains("/Users/test/private"))
        assertTrue(result.contains("us***@example.com"))
        assertTrue(result.contains("…/private/file.txt"))
        assertFalse(result.contains("//Users/test/private"))
        assertTrue(result.contains("//…/private/double.txt"))
        assertFalse(result.contains("file:///Users/test/private"))
        assertTrue(result.contains("file:///…/private/secret.txt"))
        assertFalse(result.contains("internal-host"))
        assertTrue(result.contains("file:///…/private/remote.txt"))
        assertTrue(result.contains("https://dooray.example.com/project/posts/1"))
        assertTrue(result.contains("dooray://resource/abc/def"))
    }
}
