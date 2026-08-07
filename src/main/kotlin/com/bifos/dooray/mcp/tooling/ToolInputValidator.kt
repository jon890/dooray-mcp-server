package com.bifos.dooray.mcp.tooling

import com.bifos.dooray.mcp.exception.ToolException
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object ToolInputValidator {
    fun validate(schema: ToolSchema, arguments: JsonObject) {
        schema.required.orEmpty().forEach { name ->
            if (name !in arguments) {
                throw ToolException.invalidArgument(
                    code = "MISSING_REQUIRED_PARAMETER",
                    message = "필수 입력 '$name'이(가) 없습니다."
                )
            }
        }
        schema.properties.orEmpty().forEach { (name, propertyElement) ->
            val value = arguments[name] ?: return@forEach
            val property = propertyElement as? JsonObject ?: return@forEach
            val expectedTypes = schemaTypes(property["type"])
            if (expectedTypes.isNotEmpty() && expectedTypes.none { matchesType(value, it) }) {
                throw ToolException.invalidArgument(
                    code = "INVALID_PARAMETER_TYPE",
                    message = "입력 '$name'의 형식이 올바르지 않습니다."
                )
            }
            val allowed = property["enum"] as? JsonArray
            if (allowed != null && value !in allowed) {
                throw ToolException.invalidArgument(
                    code = "INVALID_PARAMETER_VALUE",
                    message = "입력 '$name'의 값이 허용 범위를 벗어났습니다."
                )
            }
            val number = (value as? JsonPrimitive)?.doubleOrNull
            val minimum = property["minimum"]?.jsonPrimitive?.doubleOrNull
            val maximum = property["maximum"]?.jsonPrimitive?.doubleOrNull
            if (number != null && ((minimum != null && number < minimum) || (maximum != null && number > maximum))) {
                throw ToolException.invalidArgument(
                    code = "INVALID_PARAMETER_RANGE",
                    message = "입력 '$name'의 값이 허용 범위를 벗어났습니다."
                )
            }
        }
    }

    private fun schemaTypes(type: kotlinx.serialization.json.JsonElement?): List<String> = when (type) {
        is JsonPrimitive -> type.contentOrNull?.let(::listOf).orEmpty()
        is JsonArray -> type.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        else -> emptyList()
    }

    private fun matchesType(value: kotlinx.serialization.json.JsonElement, type: String): Boolean = when (type) {
        "string" -> value is JsonPrimitive && value.isString
        "integer" -> value is JsonPrimitive && !value.isString && value.intOrNull != null
        "number" -> value is JsonPrimitive && !value.isString && value.doubleOrNull != null
        "boolean" -> value is JsonPrimitive && !value.isString && value.booleanOrNull != null
        "array" -> value is JsonArray
        "object" -> value is JsonObject
        "null" -> value is JsonNull
        else -> true
    }
}
