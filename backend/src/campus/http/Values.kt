package campus.http

import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import java.util.UUID
import kotlinx.serialization.json.*

class ApiError(val status: Int, val code: String, message: String, val fields: JsonObject? = null) :
    RuntimeException(message)

fun missing(): Nothing = throw ApiError(404, "NOT_FOUND", "This item could not be found.")

fun forbidden(): Nothing =
    throw ApiError(403, "FORBIDDEN", "You do not have permission for this action.")

fun invalid(message: String): Nothing = throw ApiError(422, "VALIDATION_FAILED", message)

fun conflict(code: String, message: String): Nothing = throw ApiError(409, code, message)

fun token(): String = UUID.randomUUID().toString()

private val utc = DateTimeFormatterBuilder().appendInstant(3).toFormatter()

fun timestamp(value: Instant): String = utc.format(value)

fun instant(value: String?): Instant? = value?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

fun JsonObject.number(key: String) =
    (get(key) as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

fun obj(vararg fields: Pair<String, Any?>): JsonObject = buildJsonObject {
    for ((key, value) in fields) put(
        key,
        when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> error("Unsupported JSON value for $key")
        },
    )
}

fun JsonObject.with(extra: JsonObject) = JsonObject(this + extra)

fun JsonObject.only(vararg keys: String) = JsonObject(filterKeys { it in keys })
