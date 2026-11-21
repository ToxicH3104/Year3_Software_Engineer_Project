package campus.teacher

import campus.auth.Actor
import campus.db.Tx
import campus.http.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

fun Tx.teacherClubs(teacher: Actor) =
    JsonArray(
        rows(
            """SELECT clubId,clubName FROM clubs JOIN club_roles USING(clubId)
            WHERE userId=? AND relation='teacher' ORDER BY clubName""",
            teacher.id,
        )
    )

fun Tx.createClub(teacher: Actor, rawName: String?): JsonObject {
    val name = rawName.orEmpty().trim()
    if (name.length !in 1..100 || name.any { it.isISOControl() })
        invalid("Enter a club name of 1–100 characters on one line.")
    if (one("SELECT 1 FROM clubs WHERE trim(clubName)=? COLLATE NOCASE", name) != null)
        conflict("CLUB_NAME_IN_USE", "A club with that name already exists.")
    val id = token()
    exec("INSERT INTO clubs(clubId,clubName) VALUES(?,?)", id, name)
    exec("INSERT INTO club_roles(clubId,userId,relation) VALUES(?,?,'teacher')", id, teacher.id)
    return obj("clubId" to id, "clubName" to name)
}
