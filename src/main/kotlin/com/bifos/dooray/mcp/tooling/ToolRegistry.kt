package com.bifos.dooray.mcp.tooling

import io.modelcontextprotocol.kotlin.sdk.server.Server

class ToolRegistry(
    private val profile: ToolProfile,
    private val modules: List<ToolModule>,
) {
    fun bindings(): List<ToolBinding> {
        val selected = modules
            .filter { profile.includes(it.toolSet) }
            .flatMap(ToolModule::bindings)
        val duplicates = selected
            .groupingBy { it.tool.name }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .sorted()

        require(duplicates.isEmpty()) {
            "Duplicate MCP tool names: ${duplicates.joinToString()}"
        }
        check(selected.size == profile.expectedToolCount) {
            "Tool profile '${profile.wireName}' is not available in this build: " +
                "expected ${profile.expectedToolCount} tools, registered ${selected.size}. " +
                "Use 'legacy' until all profile modules are installed."
        }
        return selected
    }

    fun register(server: Server): Int {
        val selected = bindings()
        selected.forEach { binding -> server.addTool(binding.tool, binding.handler) }
        return selected.size
    }
}
