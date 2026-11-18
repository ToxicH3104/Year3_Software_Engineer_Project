package campus.auth

import campus.db.Tx
import campus.http.*
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.serialization.json.JsonObject

data class Actor(val id: String, val role: String, val profile: JsonObject)

fun Tx.actor(session: String?): Actor? {
    val user =
        one(
            """SELECT u.* FROM users u JOIN sessions s USING(userId)
        WHERE s.token=? AND s.expiresAt>? AND u.accountStatus='active'""",
            session,
            time,
        ) ?: return null
    return Actor(user.text("userId")!!, user.text("role")!!, user)
}

fun Tx.signIn(id: String, password: String, previous: String?): Pair<String, JsonObject> {
    val user =
        one("SELECT * FROM users WHERE universityId=? AND accountStatus='active'", id)
            ?: throw ApiError(401, "INVALID_LOGIN", "Invalid demo credentials.")
    val credential =
        one("SELECT salt,digest FROM demo_credentials WHERE userId=?", user.text("userId"))
            ?: forbidden()
    val hex = HexFormat.of()
    if (password.length !in 1..256)
        throw ApiError(401, "INVALID_LOGIN", "Invalid demo credentials.")
    val spec =
        PBEKeySpec(password.toCharArray(), hex.parseHex(credential.text("salt")), 120000, 256)
    val computed =
        try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    if (!MessageDigest.isEqual(computed, hex.parseHex(credential.text("digest"))))
        throw ApiError(401, "INVALID_LOGIN", "Invalid demo credentials.")
    exec("DELETE FROM sessions WHERE token=? OR expiresAt<=?", previous, time)
    val session = token()
    exec(
        "INSERT INTO sessions VALUES(?,?,?)",
        session,
        user.text("userId"),
        timestamp(now.plus(Duration.ofHours(8))),
    )
    return session to user
}

fun Actor?.requireRole(role: String): Actor {
    if (this == null) throw ApiError(401, "NOT_SIGNED_IN", "Sign in to continue.")
    if (this.role != role) forbidden()
    return this
}

fun Tx.hasClub(user: String, club: String, relation: String) =
    one(
        "SELECT 1 FROM club_roles WHERE userId=? AND clubId=? AND relation=?",
        user,
        club,
        relation,
    ) != null

fun Tx.requireClub(actor: Actor, club: String, relation: String) {
    if (actor.role != relation || !hasClub(actor.id, club, relation)) forbidden()
}
