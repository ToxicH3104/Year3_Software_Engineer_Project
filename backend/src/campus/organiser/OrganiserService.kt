package campus.organiser

import campus.activities.*
import campus.auth.*
import campus.db.Tx
import campus.http.*
import kotlinx.serialization.json.*

fun Tx.managed(actor: Actor, id: String): Activity =
    activity(id).also { requireClub(actor, it.club, "organiser") }

fun Tx.manageView(a: Activity): JsonObject {
    val last =
        one(
            "SELECT reason,decidedAt,status FROM reviews WHERE activityId=? ORDER BY version DESC LIMIT 1",
            a.id,
        )
    val pending =
        one(
            "SELECT reviewId,proposed,submittedAt FROM reviews WHERE activityId=? AND kind='edit' AND status='pending'",
            a.id,
        )
    return summary(a)
        .with(
            obj(
                "qrToken" to a.qr,
                "feedback" to
                    last?.takeIf { it.text("status") == "returned" }?.only("reason", "decidedAt"),
                "pendingEdit" to
                    pending?.with(
                        obj("proposed" to Json.parseToJsonElement(pending.text("proposed")!!))
                    ),
            )
        )
}

fun Tx.createDraft(actor: Actor, body: JsonObject): JsonObject {
    val club = body.text("clubId") ?: invalid("Select a club.")
    requireClub(actor, club, "organiser")
    val fields = obj("visibility" to "public").with(formFields(body))
    val id = token()
    exec(
        "INSERT INTO activities(activityId,clubId,status,fields) VALUES(?,?,'draft',?)",
        id,
        club,
        fields.toString(),
    )
    if (body["submit"] == JsonPrimitive(true)) submitReview(actor, activity(id), fields, false)
    return manageView(activity(id))
}

fun Tx.editDraft(actor: Actor, id: String, body: JsonObject): JsonObject {
    val a = managed(actor, id)
    if (a.status !in setOf("draft", "returned"))
        conflict("NOT_EDITABLE", "Only drafts and returned activities can be edited.")
    if (body.text("clubId")?.let { it != a.club } == true)
        invalid("An activity cannot move between clubs.")
    exec(
        "UPDATE activities SET fields=? WHERE activityId=?",
        a.fields.with(formFields(body)).toString(),
        id,
    )
    return manageView(activity(id))
}

fun Tx.submitActivity(actor: Actor, id: String, edits: JsonObject? = null): JsonObject {
    val a = managed(actor, id)
    val proposed = if (edits == null) a.fields else a.fields.with(formFields(edits))
    submitReview(actor, a, proposed, edit = edits != null)
    return manageView(activity(id))
}

private fun Tx.submitReview(actor: Actor, a: Activity, proposed: JsonObject, edit: Boolean) {
    if (edit && a.status != "published" || !edit && a.status !in setOf("draft", "returned"))
        conflict("NOT_EDITABLE", "This activity cannot be submitted in its current state.")
    val last = one("SELECT * FROM reviews WHERE activityId=? ORDER BY version DESC LIMIT 1", a.id)
    if (last?.text("status") == "pending")
        conflict("PENDING_REVIEW", "A version is already awaiting review.")
    val fields = validFields(proposed)
    if (edit) {
        val allowed = setOf("location", "capacity", "registrationDeadline", "startsAt")
        if (fields == a.fields || fields.keys.any { it !in allowed && fields[it] != a.fields[it] })
            invalid("Change the venue, time, capacity or deadline.")
        if (fields.number("capacity")!! < a.fields.number("capacity")!!)
            invalid("Capacity may only increase.")
        if (
            instant(fields.text("registrationDeadline"))!! <
                instant(a.fields.text("registrationDeadline"))!!
        )
            invalid("The registration deadline may only be extended.")
    }
    val previous = last?.takeIf { it.text("status") == "returned" }
    exec(
        """INSERT INTO reviews(reviewId,activityId,version,kind,proposed,baseline,previousReason,previousDecidedAt,submittedBy,submittedAt)
        VALUES(?,?,?,?,?,?,?,?,?,?)""",
        token(),
        a.id,
        (last?.number("version") ?: 0) + 1,
        if (edit) "edit" else "new",
        fields.toString(),
        if (edit) a.fields.toString() else previous?.text("proposed"),
        previous?.text("reason"),
        previous?.text("decidedAt"),
        actor.id,
        time,
    )
    if (!edit)
        exec(
            "UPDATE activities SET status='awaiting_review',fields=? WHERE activityId=?",
            fields.toString(),
            a.id,
        )
}

fun Tx.participants(actor: Actor, id: String): JsonArray {
    managed(actor, id)
    return JsonArray(
        rows(
            """SELECT r.registrationId,u.displayName AS studentName,u.universityId,r.status
        FROM registrations r JOIN users u ON u.userId=r.studentId
        WHERE r.activityId=? AND r.status!='cancelled' ORDER BY u.displayName,u.universityId""",
            id,
        )
    )
}

fun Tx.cancelActivity(actor: Actor, id: String, reason: String?): JsonObject {
    val a = managed(actor, id)
    if (a.status != "published")
        conflict("NOT_EDITABLE", "Only a published activity can be cancelled.")
    if (reason.isNullOrBlank())
        throw ApiError(422, "REASON_REQUIRED", "Enter a cancellation reason.")
    closeActivity(a, reason.trim())
    return manageView(activity(id))
}

fun Tx.organiserClubs(actor: Actor) =
    JsonArray(
        rows(
            """SELECT clubId, clubName FROM clubs JOIN club_roles USING(clubId)
       WHERE userId=? AND relation='organiser' ORDER BY clubName""",
            actor.id,
        )
    )

fun Tx.organiserActivities(actor: Actor) =
    JsonArray(
        rows(
                """$activitySelect JOIN club_roles cr ON cr.clubId=a.clubId
       WHERE cr.userId=? AND cr.relation='organiser' ORDER BY a.rowid DESC""",
                actor.id,
            )
            .map { manageView(activityRow(it)) }
    )
