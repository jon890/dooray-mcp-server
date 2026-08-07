package com.bifos.dooray.mcp.tooling

import java.util.Locale

enum class ToolProfile(
    val wireName: String,
    val expectedToolCount: Int,
) {
    COMPACT("compact", 12),
    LEGACY("legacy", 20),
    ALL("all", 32);

    fun includes(toolSet: ToolSet): Boolean =
        this == ALL || (this == LEGACY && toolSet == ToolSet.LEGACY) ||
            (this == COMPACT && toolSet == ToolSet.COMPACT)

    companion object {
        fun parse(value: String?): ToolProfile {
            val normalized = value?.trim()?.takeIf(String::isNotEmpty)?.lowercase(Locale.ROOT)
                ?: LEGACY.wireName
            return entries.firstOrNull { it.wireName == normalized }
                ?: throw IllegalArgumentException(
                    "DOORAY_MCP_TOOL_PROFILE must be compact, legacy, or all: $normalized"
                )
        }
    }
}

enum class ToolSet {
    COMPACT,
    LEGACY,
}
