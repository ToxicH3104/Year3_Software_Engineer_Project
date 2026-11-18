package campus.auth

import campus.db.Tx
import campus.http.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

fun Tx.organiserRoster(club: String) =
    JsonArray(
        rows(
            """SELECT u.universityId,u.displayName FROM club_roles cr JOIN users u USING(userId)
        WHERE cr.clubId=? AND cr.relation='organiser' ORDER BY u.displayName,u.universityId""",
            club,
        )
    )

fun Tx.organiserCandidate(universityId: String?): JsonObject {
    if (universityId.isNullOrBlank()) invalid("Enter the university ID of an existing account.")
    return one(
        """SELECT userId,universityId,displayName FROM users WHERE universityId=? COLLATE NOCASE
            AND accountStatus='active' AND role IN ('student','organiser')""",
        universityId.trim(),
    )
        ?: throw ApiError(
            404,
            "NO_ELIGIBLE_ACCOUNT",
            "No active student or organiser account matches that ID.",
        )
}

fun Tx.grantOrganiser(
    club: String,
    universityId: String?,
    reason: String?,
    grantedBy: String?,
): JsonObject {
    if (one("SELECT 1 FROM clubs WHERE clubId=?", club) == null) missing()
    val candidate = organiserCandidate(universityId)
    val userId = candidate.text("userId")!!
    if (reason.isNullOrBlank()) invalid("Enter a reason for this assignment.")
    val profile = candidate.only("displayName", "universityId")
    if (hasClub(userId, club, "organiser")) return profile
    if (
        one("SELECT COUNT(*) AS n FROM club_roles WHERE clubId=? AND relation='organiser'", club)!!
            .number("n")!! >= 3
    )
        conflict("ORGANISER_LIMIT", "This club already has three organisers.")
    exec("INSERT INTO club_roles(clubId,userId,relation) VALUES(?,?,'organiser')", club, userId)
    exec(
        "INSERT INTO organiser_grants(clubId,userId,grantedBy,grantedAt,reason) VALUES(?,?,?,?,?)",
        club,
        userId,
        grantedBy,
        time,
        reason.trim(),
    )
    return profile
}
