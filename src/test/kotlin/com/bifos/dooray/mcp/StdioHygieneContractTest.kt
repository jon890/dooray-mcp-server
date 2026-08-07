package com.bifos.dooray.mcp

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StdioHygieneContractTest {
    @Test
    fun `시스템 표준 출력은 조립 지점 외에서 사용하지 않는다`() {
        val sourceRoot = Path.of("src/main/kotlin")
        val violations = Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .filter { it.fileName.toString() != "Main.kt" }
                .filter { path ->
                    val source = Files.readString(path)
                    source.contains("System.out") || source.contains("println(")
                }
                .toList()
        }
        assertTrue(violations.isEmpty(), "STDIO stdout 오염 가능 파일: $violations")
    }

    @Test
    fun `로그는 표준 오류로만 출력한다`() {
        val logback = Files.readString(Path.of("src/main/resources/logback.xml"))
        assertTrue(logback.contains("<target>System.err</target>"))
        assertFalse(logback.contains("<target>System.out</target>"))
    }
}
