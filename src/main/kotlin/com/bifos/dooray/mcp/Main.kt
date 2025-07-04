package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.constants.VersionConst

fun main() {
    // 시작 시 버전 정보 출력 (stderr로 출력, stdout은 MCP 통신용)
    System.err.println("🚀 Dooray MCP Server v${VersionConst.VERSION} starting...")

    DoorayMcpServer().initServer()
}
