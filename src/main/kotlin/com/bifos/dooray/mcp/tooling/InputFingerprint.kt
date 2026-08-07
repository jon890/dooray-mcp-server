package com.bifos.dooray.mcp.tooling

import java.security.MessageDigest
import java.util.Base64
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object InputFingerprint {
    private val defaultExcludedKeys = setOf("request_id", "confirmation_token")

    fun of(
        arguments: JsonObject,
        excluding: Set<String> = defaultExcludedKeys,
    ): String {
        val canonical = canonicalize(JsonObject(arguments.filterKeys { it !in excluding }))
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toString().toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun canonicalize(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.toSortedMap().mapValues { canonicalize(it.value) })
        is JsonArray -> JsonArray(element.map(::canonicalize))
        else -> element
    }
}
