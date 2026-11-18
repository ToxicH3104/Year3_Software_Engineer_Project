package campus.student

import campus.activities.*
import campus.auth.*
import campus.db.Tx
import campus.http.*
import java.time.Duration
import kotlinx.serialization.json.*

private fun Tx.ownBooking(student: String, activity: String) =
    one("SELECT * FROM registrations WHERE studentId=? AND activityId=?", student, activity)

fun Tx.studentView(a: Activity, actor: Actor?): JsonObject {
    val mine = actor?.let { ownBooking(it.id, a.id) }
    return summary(a)
        .with(
            obj(
                "myRegistration" to mine?.only("registrationId", "status"),
                "registrationLink" to a.qr?.let { "/register/$it" },
            )
        )
}

fun Tx.browse(actor: Actor?, query: String?, date: String?, zone: java.time.ZoneId): JsonArray {
    val activities =
        rows(
            "$activitySelect WHERE a.status='published' ORDER BY json_extract(fields,'$.startsAt'),a.activityId"
        )
    return JsonArray(
        activities
            .map(::activityRow)
            .filter {
                it.visibleTo(this, actor) &&
                    it.fields
                        .text("title")
                        .orEmpty()
                        .contains(query.orEmpty(), ignoreCase = true) &&
                    (date.isNullOrEmpty() ||
                        it.start().atZone(zone).toLocalDate().toString() == date)
            }
            .map { studentView(it, actor) }
    )
}

fun Tx.publicDetail(actor: Actor?, id: String): JsonObject {
    val a = activity(id)
    if (a.status != "published" || !a.visibleTo(this, actor)) missing()
    return studentView(a, actor)
}

fun Tx.tokenDetail(actor: Actor, key: String): JsonObject {
    val a = forToken(key)
    if (!a.visibleTo(this, actor))
        throw ApiError(403, "NOT_MEMBER", "Only club members may register.")
    if (a.status !in setOf("published", "cancelled")) missing()
    return studentView(a, actor)
}

fun Tx.book(actor: Actor, key: String): JsonObject {
    val view = tokenDetail(actor, key)
    val id = view.text("activityId")!!
    val old = ownBooking(actor.id, id)
    if (old != null && old.text("status") != "cancelled") return bookingView(old)
    when (view.text("registrationState")) {
        "cancelled" -> conflict("CANCELLED", "This activity has been cancelled.")
        "full" -> conflict("FULL", "No places remain.")
        "closed" -> conflict("CLOSED", "Registration has closed.")
    }
    exec(
        """INSERT INTO registrations(registrationId,activityId,studentId,status,registeredAt) VALUES(?,?,?,'confirmed',?)
        ON CONFLICT(activityId,studentId) DO UPDATE SET status='confirmed',registeredAt=excluded.registeredAt,
        reservationDeadline=NULL,reminderAt=NULL""",
        token(),
        id,
        actor.id,
        time,
    )
    return bookingView(ownBooking(actor.id, id)!!)
}

private fun Tx.bookingView(row: JsonObject) =
    row.with(obj("activity" to summary(activity(row.text("activityId")!!))))

fun Tx.booking(actor: Actor, id: String): JsonObject =
    bookingView(
        one("SELECT * FROM registrations WHERE registrationId=? AND studentId=?", id, actor.id)
            ?: missing()
    )

fun Tx.bookings(actor: Actor) =
    JsonArray(
        rows(
                """SELECT r.* FROM registrations r JOIN activities a USING(activityId)
    WHERE studentId=? ORDER BY json_extract(a.fields,'$.startsAt'), registrationId""",
                actor.id,
            )
            .map { bookingView(it) }
    )

fun Tx.changeBooking(actor: Actor, id: String, action: String): JsonObject {
    val r = booking(actor, id)
    val a = activity(r.text("activityId")!!)
    when (action) {
        "cancel" -> {
            if (r.text("status") != "cancelled") {
                if (now > a.start().minus(Duration.ofHours(24)))
                    conflict("TOO_LATE", "Cancellation closes 24 hours before the start.")
                releaseBooking(id)
            }
        }
        "confirm",
        "decline" -> {
            if (r.text("status") != "reserved")
                conflict("NOT_RESERVED", "There is no reserved place to respond to.")
            if (action == "confirm")
                exec(
                    "UPDATE registrations SET status='confirmed',reservationDeadline=NULL WHERE registrationId=?",
                    id,
                )
            else releaseBooking(id)
        }
        else -> error("Unknown booking action")
    }
    return booking(actor, id)
}

fun Tx.setReminder(actor: Actor, id: String, body: JsonObject): JsonObject {
    val r = booking(actor, id)
    val a = activity(r.text("activityId")!!)
    if (r.text("status") != "confirmed" || a.status != "published")
        conflict("NOT_CONFIRMED", "A confirmed booking is required.")
    val at =
        if (body["reminderAt"] == JsonNull) null
        else {
            val value = instant(body.text("reminderAt"))
            if (value == null || value <= now || value >= a.start())
                throw ApiError(
                    422,
                    "INVALID_REMINDER_TIME",
                    "Choose a future time before the activity.",
                )
            timestamp(value)
        }
    exec("UPDATE registrations SET reminderAt=? WHERE registrationId=?", at, id)
    return booking(actor, id)
}

fun Tx.notices(actor: Actor) =
    JsonArray(
        rows(
            """SELECT noticeId,type,activityId,message,createdAt FROM notices
       WHERE studentId=? ORDER BY createdAt DESC,rowid DESC""",
            actor.id,
        )
    )
