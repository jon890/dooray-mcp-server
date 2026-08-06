package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.constants.VersionConst
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.slf4j.LoggerFactory

fun main() {
    // 시스템 로깅 속성 설정 - stdout 오염 방지
    configureSystemLogging()

    val logger = LoggerFactory.getLogger("com.bifos.dooray.mcp.Main")
    logger.info("🚀 Dooray MCP Server v${VersionConst.VERSION} starting...")

    DoorayMcpServer().initServer(
        input = System.`in`.asSource().buffered(),
        output = System.out.asSink().buffered()
    )
}

/** 시스템 로깅 설정을 구성하여 stdout 오염을 방지합니다. */
private fun configureSystemLogging() {
    // java.util.logging을 stderr로 리다이렉트
    System.setProperty(
            "java.util.logging.SimpleFormatter.format",
            "%1\$tY-%1\$tm-%1\$td %1\$tH:%1\$tM:%1\$tS.%1\$tL [%4\$s] %2\$s - %5\$s%6\$s%n"
    )

    // 콘솔 핸들러를 stderr로 설정
    val rootLogger = java.util.logging.Logger.getLogger("")
    rootLogger.handlers.forEach { handler ->
        if (handler is java.util.logging.ConsoleHandler) {
            // Java의 ConsoleHandler는 기본적으로 stderr를 사용하므로 그대로 유지
        }
    }

    // 기타 시스템 속성 설정
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.err")
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.SSS")
}
