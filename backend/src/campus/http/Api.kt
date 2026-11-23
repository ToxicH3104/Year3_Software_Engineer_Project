package campus.http

import campus.auth.*
import campus.db.*
import campus.processDue
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

const val sessionCookie = "club_session"

class Api(val db: Database, val publicUrl: String, val zone: java.time.ZoneId) {
    suspend fun <T> transaction(block: Tx.() -> T): T =
        withContext(Dispatchers.IO) {
            val result =
                db.transaction {
                    processDue()
                    // Keep due work even when the request fails a business rule.
                    exec("SAVEPOINT request")
                    val response =
                        try {
                            Result.success(block())
                        } catch (e: ApiError) {
                            exec("ROLLBACK TO request")
                            Result.failure(e)
                        }
                    exec("RELEASE request")
                    response
                }
            result.getOrThrow()
        }

    suspend fun reply(call: ApplicationCall, block: Tx.(Actor?) -> JsonElement) {
        val session = call.request.cookies[sessionCookie]
        val result = transaction { block(actor(session)) }
        call.respond(result)
    }

    suspend fun role(call: ApplicationCall, role: String, block: Tx.(Actor) -> JsonElement) {
        reply(call) { user -> block(user.requireRole(role)) }
    }
}

fun ApplicationCall.param(name: String) = parameters[name] ?: missing()

suspend fun ApplicationCall.payload() = receive<JsonObject>()
