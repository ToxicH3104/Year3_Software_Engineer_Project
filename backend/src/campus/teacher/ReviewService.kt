package campus.teacher

import campus.activities.*
import campus.auth.*
import campus.db.Tx
import campus.http.*
import java.time.Duration
import kotlinx.serialization.json.*

private const val reviewSelect =
    """SELECT r.*,a.clubId,c.clubName,u.displayName AS submitter,v.displayName AS reviewer
    FROM reviews r JOIN activities a USING(activityId) JOIN clubs c USING(clubId)
    JOIN users u ON u.userId=r.submittedBy LEFT JOIN users v ON v.userId=r.reviewerId"""

fun Tx.review(actor: Actor, id: String): JsonObject {
    val row = one("$reviewSelect WHERE r.reviewId=?", id) ?: missing()
    requireClub(actor, row.text("clubId")!!, "teacher")
    return reviewView(row)
}

private fun Tx.reviewView(row: JsonObject): JsonObject {
    val proposed = Json.parseToJsonElement(row.text("proposed")!!).jsonObject
    val baseline = row.text("baseline")?.let { Json.parseToJsonElement(it).jsonObject }
    val changed =
        baseline?.let { old -> proposed.keys.filter { proposed[it] != old[it] } }.orEmpty()
    val timeChange = row.text("kind") == "edit" && "startsAt" in changed
    val tooClose =
        timeChange && instant(proposed.text("startsAt"))!! < now.plus(Duration.ofHours(24))
    val decision =
        row.text("reviewerId")?.let {
            obj(
                "reviewerId" to it,
                "reviewerName" to row.text("reviewer"),
                "decidedAt" to row.text("decidedAt"),
                "decision" to row.text("status"),
                "reason" to row.text("reason"),
            )
        }
    return row.only(
            "reviewId",
            "activityId",
            "clubId",
            "clubName",
            "kind",
            "version",
            "submittedAt",
            "status",
        )
        .with(
            obj(
                "submittedBy" to row.text("submitter"),
                "proposed" to proposed,
                "title" to proposed.text("title"),
                "approved" to baseline.takeIf { row.text("kind") == "edit" },
                "previous" to baseline,
                "changedFields" to JsonArray(changed.map(::JsonPrimitive)),
                "previousFeedback" to
                    row.text("previousReason")?.let {
                        obj("reason" to it, "decidedAt" to row.text("previousDecidedAt"))
                    },
                "isTimeChange" to timeChange,
                "canApprove" to (row.text("status") == "pending" && !tooClose),
                "blockReason" to (if (tooClose) "TOO_CLOSE_TO_START" else null),
                "decision" to decision,
            )
        )
}

fun Tx.reviewList(actor: Actor, history: Boolean, sort: String?): JsonArray {
    val condition =
        if (history) "r.reviewerId=? AND r.status IN ('approved','returned')"
        else
            "r.status='pending' AND EXISTS(SELECT 1 FROM club_roles cr WHERE cr.clubId=a.clubId AND cr.userId=? AND cr.relation='teacher')"
    val order =
        when {
            history -> "r.decidedAt DESC"
            sort == "deadline" -> "json_extract(r.proposed,'$.registrationDeadline'), r.submittedAt"
            else -> "r.submittedAt"
        }
    return JsonArray(
        rows("$reviewSelect WHERE $condition ORDER BY $order,r.reviewId", actor.id).map {
            reviewView(it)
        }
    )
}

fun Tx.decide(actor: Actor, id: String, body: JsonObject): JsonObject {
    val item = review(actor, id)
    if (item.text("status") != "pending")
        conflict("ALREADY_DECIDED", "This submission is no longer pending.")
    val a = activity(item.text("activityId")!!)
    val reason = body.text("reason")?.trim()
    val decision =
        when (body.text("decision")) {
            "return" -> {
                if (reason.isNullOrEmpty())
                    throw ApiError(422, "REASON_REQUIRED", "Give the organiser a reason.")
                if (item.text("kind") == "new")
                    exec("UPDATE activities SET status='returned' WHERE activityId=?", a.id)
                "returned"
            }
            "approve" -> {
                if (item.text("blockReason") != null)
                    conflict("TOO_CLOSE_TO_START", "The new start is less than 24 hours away.")
                val f = item.getValue("proposed").jsonObject
                if (item.text("kind") == "edit") {
                    val shifted = item["isTimeChange"] == JsonPrimitive(true)
                    val kind =
                        if (shifted) "time_changed"
                        else if (f["location"] != a.fields["location"]) "venue_changed"
                        else "activity_changed"
                    val message =
                        if (shifted) "The start time changed. Please confirm your reserved place."
                        else "The approved activity details have changed."
                    notifyApprovedEdit(a, instant(f.text("startsAt"))!!, message, kind)
                }
                exec(
                    "UPDATE activities SET status='published',fields=?,qrToken=COALESCE(qrToken,?) WHERE activityId=?",
                    f.toString(),
                    token(),
                    a.id,
                )
                "approved"
            }
            else -> invalid("Choose approve or return.")
        }
    exec(
        "UPDATE reviews SET status=?,reviewerId=?,decidedAt=?,reason=? WHERE reviewId=?",
        decision,
        actor.id,
        time,
        reason,
        id,
    )
    return review(actor, id)
}
