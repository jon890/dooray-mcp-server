package com.bifos.dooray.mcp.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

object JsonUtils {

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    inline fun <reified T> toJsonString(value: T): String {
        return json.encodeToString(value)
    }

    inline fun <reified T> fromJsonString(jsonString: String): T {
        return json.decodeFromString(jsonString)
    }

    inline fun <reified T> toJsonElement(value: T): JsonElement {
        return json.encodeToJsonElement(serializer<T>(), value)
    }

    /** JSON 배열 문자열을 문자열 리스트로 파싱합니다. 예: ["item1", "item2"] -> listOf("item1", "item2") */
    fun parseStringArray(jsonArrayString: String): List<String> {
        return try {
            val jsonArray = json.parseToJsonElement(jsonArrayString) as JsonArray
            jsonArray.map { it.jsonPrimitive.content }
        } catch (e: Exception) {
            // 파싱 실패 시 빈 리스트 반환
            emptyList()
        }
    }
}
