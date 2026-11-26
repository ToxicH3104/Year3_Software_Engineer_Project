package campus.auth

import campus.db.Tx
import campus.http.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.HexFormat
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class Actor(val id: String, val roles: Set<String>, val profile: JsonObject)

private fun Tx.identity(user: JsonObject): Actor {
    val id = user.text("userId")!!
    val base = user.text("role")!!
    val roles = mutableSetOf(base)
    if (
        base == "student" &&
            one("SELECT 1 FROM club_roles WHERE userId=? AND relation='organiser' LIMIT 1", id) !=
                null
    )
        roles.add("organiser")
    return Actor(id, roles, user.with(obj("roles" to JsonArray(roles.map(::JsonPrimitive)))))
}

fun Tx.actor(session: String?): Actor? {
    val user =
        one(
            """SELECT u.* FROM users u JOIN sessions s USING(userId)
        WHERE s.token=? AND s.expiresAt>? AND u.accountStatus='active'""",
            session,
            time,
        ) ?: return null
    return identity(user)
}

fun Tx.signIn(id: String, password: String, previous: String?): Pair<String, JsonObject> {
    val user =
        one(
            "SELECT * FROM users WHERE universityId=? COLLATE NOCASE AND accountStatus='active'",
            id.trim(),
        ) ?: invalidLogin()
    val credential =
        one("SELECT salt,digest FROM demo_credentials WHERE userId=?", user.text("userId"))
            ?: forbidden()
    if (password.length !in 1..256) invalidLogin()
    val computed = passwordDigest(password, hex.parseHex(credential.text("salt")))
    if (!MessageDigest.isEqual(computed, hex.parseHex(credential.text("digest")))) invalidLogin()
    return startSession(user, previous)
}

private fun invalidLogin(): Nothing =
    throw ApiError(401, "INVALID_LOGIN", "Incorrect university ID or password.")

private val hex = HexFormat.of()
private val random = SecureRandom()

private fun passwordDigest(password: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt, 120000, 256)
    return try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}

fun Tx.signUpStudent(input: JsonObject, previous: String?): Pair<String, JsonObject> {
    val id = input.text("universityId").orEmpty().trim()
    val name = input.text("displayName").orEmpty().trim()
    val password = input.text("password").orEmpty()
    val errors = buildJsonObject {
        if (id.length !in 1..64 || id.any { it.isWhitespace() || it.isISOControl() })
            put("universityId", "Use 1–64 characters without spaces.")
        if (name.length !in 1..100) put("displayName", "Enter a name of up to 100 characters.")
        if (password.length !in 8..256 || password.isBlank())
            put("password", "Use a password of 8–256 characters.")
    }
    if (errors.isNotEmpty())
        throw ApiError(422, "VALIDATION_FAILED", "Check the highlighted fields.", errors)
    if (one("SELECT 1 FROM users WHERE universityId=? COLLATE NOCASE", id) != null)
        throw ApiError(
            409,
            "ID_IN_USE",
            "This university ID already has an account.",
            obj("universityId" to "Use your existing account to sign in."),
        )

    val userId = token()
    val salt = ByteArray(16).also(random::nextBytes)
    exec(
        "INSERT INTO users(userId, universityId, displayName, role) VALUES(?,?,?,'student')",
        userId,
        id,
        name,
    )
    exec(
        "INSERT INTO demo_credentials(userId, salt, digest) VALUES(?,?,?)",
        userId,
        hex.formatHex(salt),
        hex.formatHex(passwordDigest(password, salt)),
    )
    return startSession(one("SELECT * FROM users WHERE userId=?", userId)!!, previous)
}

private fun Tx.startSession(user: JsonObject, previous: String?): Pair<String, JsonObject> {
    exec("DELETE FROM sessions WHERE token=? OR expiresAt<=?", previous, time)
    val session = token()
    exec(
        "INSERT INTO sessions VALUES(?,?,?)",
        session,
        user.text("userId"),
        timestamp(now.plus(Duration.ofHours(8))),
    )
    return session to identity(user).profile
}

fun Actor?.requireSignedIn(): Actor =
    this ?: throw ApiError(401, "NOT_SIGNED_IN", "Sign in to continue.")

fun Actor?.requireRole(role: String): Actor =
    requireSignedIn().also { if (role !in it.roles) forbidden() }

fun Tx.hasClub(user: String, club: String, relation: String) =
    one(
        "SELECT 1 FROM club_roles WHERE userId=? AND clubId=? AND relation=?",
        user,
        club,
        relation,
    ) != null

fun Tx.requireClub(actor: Actor, club: String, relation: String) {
    actor.requireRole(relation)
    if (!hasClub(actor.id, club, relation)) forbidden()
}
