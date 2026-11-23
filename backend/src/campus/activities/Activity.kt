package campus.activities

import campus.auth.*
import campus.db.Tx
import campus.http.*
import java.time.Duration
import kotlinx.serialization.json.*

val activityKeys =
    arrayOf(
        "title",
        "description",
        "location",
        "requirements",
        "capacity",
        "startsAt",
        "registrationDeadline",
        "visibility",
    )
private val requiredText = listOf("title", "description", "location", "requirements")

fun formFields(input: JsonObject) = input.only(*activityKeys)

fun Tx.validFields(input: JsonObject): JsonObject {
    val errors = mutableMapOf<String, JsonElement>()
    fun problem(key: String, message: String) {
        errors[key] = JsonPrimitive(message)
    }
    requiredText
        .filter { input.text(it).isNullOrBlank() }
        .forEach { problem(it, "This field is required.") }
    if ((input.number("capacity") ?: 0) <= 0) problem("capacity", "Enter a positive whole number.")
    val start = instant(input.text("startsAt"))
    val deadline = instant(input.text("registrationDeadline"))
    if (start == null || start <= now)
        problem("startsAt", "Enter a future date and time with an offset.")
    if (deadline == null || (start != null && deadline >= start))
        problem("registrationDeadline", "The deadline must be before the start.")
    if (input.text("visibility") !in setOf("public", "club_only"))
        problem("visibility", "Choose public or club_only.")
    if (errors.isNotEmpty())
        throw ApiError(
            422,
            "VALIDATION_FAILED",
            "Check the highlighted fields.",
            JsonObject(errors),
        )
    return input.with(
        obj("startsAt" to timestamp(start!!), "registrationDeadline" to timestamp(deadline!!))
    )
}

data class Activity(
    val id: String,
    val club: String,
    val clubName: String,
    val status: String,
    val fields: JsonObject,
    val qr: String?,
    val reason: String?,
) {
    fun visibleTo(tx: Tx, actor: Actor?) =
        fields.text("visibility") == "public" ||
            (actor != null && tx.hasClub(actor.id, club, "member"))

    fun start() = instant(fields.text("startsAt"))!!
}

fun activityRow(row: JsonObject) =
    Activity(
        row.text("activityId")!!,
        row.text("clubId")!!,
        row.text("clubName")!!,
        row.text("status")!!,
        Json.parseToJsonElement(row.text("fields")!!).jsonObject,
        row.text("qrToken"),
        row.text("cancelReason"),
    )

const val activitySelect = "SELECT a.*,c.clubName FROM activities a JOIN clubs c USING(clubId)"

fun Tx.activity(id: String) =
    one("$activitySelect WHERE a.activityId=?", id)?.let(::activityRow) ?: missing()

fun Tx.forToken(key: String) =
    one("$activitySelect WHERE a.qrToken=?", key)?.let(::activityRow) ?: missing()

fun Tx.counts(id: String): Pair<Int, Int> {
    val row =
        one(
            """SELECT count(*) FILTER (WHERE status='confirmed') AS confirmed,
        count(*) FILTER (WHERE status='reserved') AS reserved FROM registrations WHERE activityId=?""",
            id,
        )!!
    return row.number("confirmed")!! to row.number("reserved")!!
}

fun Tx.summary(a: Activity): JsonObject {
    val (confirmed, reserved) = counts(a.id)
    val remaining = (a.fields.number("capacity") ?: 0) - confirmed - reserved
    val startsAt = instant(a.fields.text("startsAt"))
    val deadline = instant(a.fields.text("registrationDeadline"))
    val closesAt =
        if (startsAt != null && deadline != null)
            minOf(deadline, startsAt.minus(Duration.ofHours(12)))
        else null
    val state =
        when {
            a.status == "cancelled" -> "cancelled"
            a.status != "published" -> "closed"
            closesAt == null || now >= closesAt -> "closed"
            remaining <= 0 -> "full"
            else -> "open"
        }
    return a.fields.with(
        obj(
            "activityId" to a.id,
            "clubId" to a.club,
            "clubName" to a.clubName,
            "status" to a.status,
            "cancelReason" to a.reason,
            "confirmedCount" to confirmed,
            "reservedCount" to reserved,
            "registeredCount" to confirmed + reserved,
            "remaining" to remaining.coerceAtLeast(0),
            "registrationState" to state,
            "registrationClosesAt" to closesAt?.let(::timestamp),
        )
    )
}

fun Tx.notice(student: String, activity: String, kind: String, message: String) {
    exec("INSERT INTO notices VALUES(?,?,?,?,?,?)", token(), student, activity, kind, message, time)
}
