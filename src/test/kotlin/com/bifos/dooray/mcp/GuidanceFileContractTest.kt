package com.bifos.dooray.mcp

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuidanceFileContractTest {
    @Test
    fun `Claude 지침은 AGENTS 단일 원본을 가리킨다`() {
        val agents = Path.of("AGENTS.md")
        val claude = Path.of("CLAUDE.md")

        assertTrue(Files.isRegularFile(agents), "AGENTS.md가 없습니다")
        assertTrue(Files.isSymbolicLink(claude), "CLAUDE.md는 심볼릭 링크여야 합니다")
        assertEquals(Path.of("AGENTS.md"), Files.readSymbolicLink(claude))
        assertEquals(Files.readString(agents), Files.readString(claude))

        assertTrue(Files.isDirectory(Path.of(".claude/commands")))
        listOf(
            ".claude/planning-overlay.md",
            ".claude/build-with-teams-overlay.md",
            ".claude/agents/dooray-mcp-docs-verifier.md",
        ).forEach { path ->
            assertTrue(Files.isRegularFile(Path.of(path)), "필수 보조 지침이 없습니다: $path")
        }
    }
}
