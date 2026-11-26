package campus.teacher

import campus.activities.*
import campus.auth.Actor
import campus.auth.requireRole
import campus.db.Tx
import campus.http.*
import java.time.Duration
import kotlinx.serialization.json.*

private val urgentWindow = Duration.ofHours(48)
private const val expiredReason =
    "No teacher reviewed this before the review deadline. Please check the time and resubmit."

private const val reviewSelect =
    """SELECT r.*,a.clubId,c.clubName,u.displayName AS submitter,v.displayName AS reviewer
    FROM review_requests r JOIN activities a USING(activityId) JOIN clubs c USING(clubId)
    JOIN users u ON u.userId=r.submittedBy LEFT JOIN users v ON v.userId=r.reviewerId"""

private fun Tx.reviewRow(id: String) = one("$reviewSelect WHERE r.reviewId=?", id) ?: missing()

fun Tx.review(id: String) = reviewView(reviewRow(id))

private fun Tx.reviewView(row: JsonObject): JsonObject {
    val proposed = Json.parseToJsonElement(row.text("proposed")!!).jsonObject
    val baseline = row.text("baseline")?.let { Json.parseToJsonElement(it).jsonObject }
    val changed =
        baseline?.let { old -> proposed.keys.filter { proposed[it] != old[it] } }.orEmpty()
    val pending = row.text("status") == "pending"
    val deadline = instant(row.text("reviewDeadline"))!!
    val decision =
        row.text("decidedAt")?.let {
            obj(
                "reviewerId" to row.text("reviewerId"),
                "reviewerName" to (row.text("reviewer") ?: "System"),
                "decidedAt" to it,
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
            "reviewDeadline",
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
                "isTimeChange" to (row.text("kind") == "edit" && "startsAt" in changed),
                "isUrgent" to (pending && now < deadline && deadline <= now.plus(urgentWindow)),
                "canApprove" to (pending && now < deadline),
                "decision" to decision,
            )
        )
}

fun Tx.reviewList(history: Boolean): JsonArray {
    val items =
        if (history)
            rows(
                "$reviewSelect WHERE r.status IN ('approved','returned') ORDER BY r.decidedAt DESC,r.queueOrder DESC"
            )
        else
            rows(
                """$reviewSelect WHERE r.status='pending'
                ORDER BY CASE WHEN r.reviewDeadline<=? THEN r.reviewDeadline END ASC NULLS LAST,
                    r.submittedAt, r.queueOrder""",
                timestamp(now.plus(urgentWindow)),
            )
    return JsonArray(items.map { reviewView(it) })
}

private fun Tx.returnReview(row: JsonObject, reviewerId: String?, reason: String) {
    exec(
        "UPDATE reviews SET status='returned',reviewerId=?,decidedAt=?,reason=? WHERE reviewId=?",
        reviewerId,
        time,
        reason,
        row.text("reviewId"),
    )
    val activityId = row.text("activityId")!!
    if (row.text("kind") == "new")
        exec("UPDATE activities SET status='returned' WHERE activityId=?", activityId)
    val title = Json.parseToJsonElement(row.text("proposed")!!).jsonObject.text("title")
    notice(
        row.text("submittedBy")!!,
        activityId,
        "review_returned",
        "$title was returned for changes. $reason",
    )
}

fun Tx.expireOverdueReviews() {
    for (row in
        rows("SELECT * FROM review_requests WHERE status='pending' AND reviewDeadline<=?", time)) {
        returnReview(row, null, expiredReason)
    }
}

fun Tx.decide(actor: Actor, id: String, body: JsonObject): JsonObject {
    actor.requireRole("teacher")
    val row = reviewRow(id)
    val status = row.text("status")
    if (
        (status == "pending" && now >= instant(row.text("reviewDeadline"))!!) ||
            (status == "returned" && row.text("reviewerId") == null)
    )
        conflict("REVIEW_EXPIRED", "The review deadline has passed.")
    if (status != "pending") conflict("ALREADY_DECIDED", "This submission is no longer pending.")
    val reason = body.text("reason")?.trim()
    when (body.text("decision")) {
        "return" -> {
            if (reason.isNullOrEmpty())
                throw ApiError(422, "REASON_REQUIRED", "Give the organiser a reason.")
            returnReview(row, actor.id, reason)
        }
        "approve" -> {
            val a = activity(row.text("activityId")!!)
            val f = Json.parseToJsonElement(row.text("proposed")!!).jsonObject
            if (row.text("kind") == "edit") {
                val start = instant(f.text("startsAt"))!!
                val shifted = start != a.start()
                val kind =
                    if (shifted) "time_changed"
                    else if (f["location"] != a.fields["location"]) "venue_changed"
                    else "activity_changed"
                val message = buildList {
                    if (shifted) add("The start time changed. Please confirm your reserved place.")
                    if (f["location"] != a.fields["location"]) add("The new venue is ${f.text("location")}.")
                    if (isEmpty()) add("The approved activity details have changed.")
                }.joinToString(" ")
                notifyApprovedEdit(a, start, message, kind)
            }
            exec(
                "UPDATE activities SET status='published',fields=?,qrToken=COALESCE(qrToken,?) WHERE activityId=?",
                f.toString(),
                token(),
                a.id,
            )
            exec(
                "UPDATE reviews SET status='approved',reviewerId=?,decidedAt=?,reason=? WHERE reviewId=?",
                actor.id,
                time,
                reason,
                id,
            )
            if (row.text("kind") == "new") notifyPublication(activity(a.id))
        }
        else -> invalid("Choose approve or return.")
    }
    return review(id)
}
