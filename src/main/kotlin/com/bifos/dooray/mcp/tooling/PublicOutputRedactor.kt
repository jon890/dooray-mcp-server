package com.bifos.dooray.mcp.tooling

import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_API_KEY
import com.bifos.dooray.mcp.utils.Env
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class PublicOutputRedactor(
    secrets: Collection<String> = emptyList(),
) {
    private val secrets = secrets.filter { it.isNotBlank() }.distinct().sortedByDescending(String::length)

    fun sanitize(value: String): String {
        var sanitized = ANSI_ESCAPE.replace(value, "")
            .replace('\r', ' ')
            .replace('\n', ' ')
        secrets.forEach { secret -> sanitized = sanitized.replace(secret, "[REDACTED]") }
        sanitized = EMAIL.replace(sanitized) { match ->
            val local = match.groupValues[1]
            val visible = local.take(2)
            "$visible***@${match.groupValues[2]}"
        }
        sanitized = DOUBLE_SLASH_PATH.replace(sanitized) { match ->
            val segments = match.groupValues[1].split('/').filter(String::isNotBlank)
            "//…/" + segments.takeLast(2).joinToString("/")
        }
        sanitized = FILE_URI.replace(sanitized) { match ->
            val segments = match.groupValues[1].split('/').filter(String::isNotBlank)
            "file:///…/" + segments.takeLast(2).joinToString("/")
        }
        sanitized = ABSOLUTE_PATH.replace(sanitized) { match ->
            val segments = match.value.split('/').filter(String::isNotBlank)
            "…/" + segments.takeLast(2).joinToString("/")
        }
        return sanitized.trim()
    }

    fun sanitize(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> JsonObject(value.mapValues { sanitize(it.value) })
        is JsonArray -> JsonArray(value.map(::sanitize))
        is JsonPrimitive -> if (value.isString) JsonPrimitive(sanitize(value.content)) else value
        JsonNull -> JsonNull
    }

    companion object {
        private val ANSI_ESCAPE = Regex("\\u001B(?:\\[[0-?]*[ -/]*[@-~]|[@-_])")
        private val EMAIL = Regex("([A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})")
        private val DOUBLE_SLASH_PATH = Regex("(?<![A-Za-z0-9:])//((?:[^\\s/]+/)+[^\\s,;:]+)")
        private val FILE_URI = Regex("file://(?:[^/\\s]+)?/((?:[^\\s/]+/)+[^\\s,;:]+)")
        private val ABSOLUTE_PATH = Regex("(?<![A-Za-z0-9:/…])/(?:[^\\s/]+/)+[^\\s,;:]+")

        fun fromEnvironment(): PublicOutputRedactor =
            PublicOutputRedactor(listOfNotNull(Env.get(DOORAY_API_KEY)))
    }
}
