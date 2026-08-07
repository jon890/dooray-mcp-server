package com.bifos.dooray.mcp.tooling

import com.bifos.dooray.mcp.client.DoorayClient
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ToolRegistryTest {
    @Test
    fun `legacy 도구 목록과 순서를 고정한다`() {
        val module = LegacyToolModule(mockk<DoorayClient>())
        val bindings = ToolRegistry(ToolProfile.LEGACY, listOf(module)).bindings()

        assertEquals(LegacyToolModule.TOOL_NAMES, bindings.map { it.tool.name })
        assertEquals(20, bindings.size)
        bindings.forEach { binding ->
            assertEquals("object", binding.tool.inputSchema.type)
            assertTrue(binding.tool.description?.isNotBlank() == true)
        }
    }

    @Test
    fun `compact와 all 프로필은 정확한 수를 요구한다`() {
        val compact = fakeModule(ToolSet.COMPACT, "compact", 12)
        val legacy = fakeModule(ToolSet.LEGACY, "legacy", 20)
        assertEquals(12, ToolRegistry(ToolProfile.COMPACT, listOf(compact, legacy)).bindings().size)
        assertEquals(32, ToolRegistry(ToolProfile.ALL, listOf(compact, legacy)).bindings().size)
        val unavailable = assertFailsWith<IllegalStateException> {
            ToolRegistry(ToolProfile.COMPACT, listOf(legacy)).bindings()
        }
        assertTrue(unavailable.message!!.contains("Use 'legacy'"))
    }

    @Test
    fun `중복 도구 이름은 서버 시작 전에 거부한다`() {
        val first = fakeModule(ToolSet.COMPACT, "same", 12)
        val duplicate = object : ToolModule {
            override val toolSet = ToolSet.LEGACY
            override fun bindings() = List(20) { index -> binding(if (index == 0) "same-0" else "legacy-$index") }
        }
        val error = assertFailsWith<IllegalArgumentException> {
            ToolRegistry(ToolProfile.ALL, listOf(first, duplicate)).bindings()
        }
        assertTrue(error.message!!.contains("same-0"))
    }

    @Test
    fun `프로필 파싱은 legacy 기본값과 유효성 검사를 제공한다`() {
        assertEquals(ToolProfile.LEGACY, ToolProfile.parse(null))
        assertEquals(ToolProfile.LEGACY, ToolProfile.parse("  "))
        assertEquals(ToolProfile.COMPACT, ToolProfile.parse(" Compact "))
        assertFailsWith<IllegalArgumentException> { ToolProfile.parse("core") }
    }

    private fun fakeModule(set: ToolSet, prefix: String, count: Int) = object : ToolModule {
        override val toolSet = set
        override fun bindings() = List(count) { binding("$prefix-$it") }
    }

    private fun binding(name: String): ToolBinding = ToolBinding(
        tool = Tool(name = name, description = name, inputSchema = ToolSchema()),
        handler = { _, _ -> CallToolResult(content = listOf(TextContent("ok"))) },
    )
}
