package campus.inbox

import campus.auth.requireSignedIn
import campus.db.Tx
import campus.http.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*

fun Route.inboxRoutes(api: Api) {
    route("/my/notices") {
        get { api.reply(call) { inbox(it.requireSignedIn().id) } }
        get("/unread") { api.reply(call) { unreadCount(it.requireSignedIn().id) } }
        put("/{id}/read") {
            api.reply(call) { markRead(it.requireSignedIn().id, call.param("id"), through = false) }
        }
        put("/read-through/{id}") {
            api.reply(call) { markRead(it.requireSignedIn().id, call.param("id"), through = true) }
        }
    }
}

private fun Tx.inbox(userId: String) = JsonArray(rows(
    """SELECT n.noticeId, n.activityId, n.type, n.message, n.createdAt, nr.readAt,
              json_extract(a.fields,'$.title') AS activityTitle, r.registrationId,
              CASE WHEN n.type='activity_published' AND a.status='published'
                   AND (json_extract(a.fields,'$.visibility')='public' OR EXISTS (
                       SELECT 1 FROM club_roles c WHERE c.clubId=a.clubId
                       AND c.userId=n.studentId AND c.relation='member'
                   )) THEN '/activities/' || a.activityId END AS activityLink
       FROM notices n JOIN activities a ON a.activityId=n.activityId
       LEFT JOIN notice_reads nr ON nr.noticeId=n.noticeId
       LEFT JOIN registrations r ON r.activityId=n.activityId AND r.studentId=n.studentId
       WHERE n.studentId=? ORDER BY n.rowid DESC""",
    userId,
))

private fun Tx.unreadCount(userId: String) = one(
    """SELECT count(*) AS unread FROM notices n LEFT JOIN notice_reads r USING(noticeId)
       WHERE n.studentId=? AND r.noticeId IS NULL""",
    userId,
)!!

private fun Tx.markRead(userId: String, noticeId: String, through: Boolean): JsonObject {
    val notice = one(
        "SELECT rowid AS seq FROM notices WHERE studentId=? AND noticeId=?", userId, noticeId
    ) ?: missing()
    // A message delivered after the displayed batch must keep its unread state.
    val comparison = if (through) "<=" else "="
    exec(
        """INSERT OR IGNORE INTO notice_reads(noticeId,readAt)
           SELECT noticeId,? FROM notices WHERE studentId=? AND rowid $comparison ?""",
        time, userId, notice.getValue("seq").jsonPrimitive.long,
    )
    return unreadCount(userId)
}
