package com.bifos.dooray.mcp.utils

/**
 * 환경변수 접근 유틸리티
 *
 * System.getenv()를 직접 호출하는 대신 이 객체를 통해 접근합니다.
 */
object Env {
    /** 환경변수를 반환하거나 null을 반환합니다. */
    fun get(key: String): String? = System.getenv(key)

    /** 환경변수를 반환하거나, 없으면 IllegalArgumentException을 던집니다. */
    fun require(key: String): String =
        System.getenv(key) ?: throw IllegalArgumentException("$key is required.")

    /** 환경변수를 Long으로 파싱하거나 default를 반환합니다. */
    fun getLong(key: String, default: Long): Long =
        System.getenv(key)?.toLongOrNull() ?: default
}
