package com.bifos.dooray.mcp.utils

import kotlinx.serialization.json.Json

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
}
