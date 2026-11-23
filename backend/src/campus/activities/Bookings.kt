package campus.activities

import campus.db.Tx
import campus.http.*
import java.time.Duration
import java.time.Instant

fun Tx.releaseBooking(id: String) {
    exec(
        "UPDATE registrations SET status='cancelled', reminderAt=NULL, reservationDeadline=NULL WHERE registrationId=?",
        id,
    )
}

fun Tx.closeActivity(a: Activity, reason: String) {
    exec("UPDATE activities SET status='cancelled',cancelReason=? WHERE activityId=?", reason, a.id)
    exec("UPDATE reviews SET status='withdrawn' WHERE activityId=? AND status='pending'", a.id)
    for (r in
        rows("SELECT * FROM registrations WHERE activityId=? AND status!='cancelled'", a.id)) {
        notice(r.text("studentId")!!, a.id, "cancelled", reason)
        releaseBooking(r.text("registrationId")!!)
    }
}

fun Tx.notifyApprovedEdit(a: Activity, newStart: Instant, message: String, kind: String) {
    val shifted = a.start() != newStart
    if (shifted) {
        val deadline = timestamp(newStart.minus(Duration.ofHours(24)))
        exec(
            """UPDATE registrations SET status='reserved', reservationDeadline=?,
            reminderAt=CASE WHEN reminderAt>=? THEN NULL ELSE reminderAt END
            WHERE activityId=? AND status IN ('confirmed','reserved')""",
            deadline,
            timestamp(newStart),
            a.id,
        )
    }
    for (r in
        rows(
            "SELECT studentId FROM registrations WHERE activityId=? AND status!='cancelled'",
            a.id,
        )) {
        notice(r.text("studentId")!!, a.id, kind, message)
    }
}

fun Tx.processDueBookings() {
    for (r in
        rows(
            "SELECT registrationId, studentId, activityId FROM registrations WHERE status='reserved' AND reservationDeadline<=?",
            time,
        )) {
        releaseBooking(r.text("registrationId")!!)
        val a = activity(r.text("activityId")!!)
        notice(
            r.text("studentId")!!,
            a.id,
            "place_released",
            "Your reserved place for " +
                a.fields.text("title") +
                " was released because the confirmation deadline passed.",
        )
    }
    for (r in
        rows("SELECT * FROM registrations WHERE status='confirmed' AND reminderAt<=?", time)) {
        val a = activity(r.text("activityId")!!)
        if (a.status == "published" && a.start() > now) {
            notice(
                r.text("studentId")!!,
                a.id,
                "reminder",
                "Upcoming activity: " + a.fields.text("title"),
            )
        }
        exec(
            "UPDATE registrations SET reminderAt=NULL WHERE registrationId=?",
            r.text("registrationId"),
        )
    }
}
