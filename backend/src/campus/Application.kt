package campus

import campus.activities.processDue
import campus.auth.authRoutes
import campus.db.Database
import campus.http.*
import campus.organiser.organiserRoutes
import campus.student.studentRoutes
import campus.teacher.teacherRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.net.URI
import java.time.ZoneId
import kotlinx.coroutines.*

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val url = System.getenv("PUBLIC_BASE_URL") ?: "http://localhost:5173"
    val db = Database(System.getenv("CAMPUS_DB") ?: "campus.db")
    embeddedServer(Netty, host = System.getenv("HOST") ?: "127.0.0.1", port = port) {
            campusApi(db, url)
        }
        .start(wait = true)
}

fun Application.campusApi(db: Database, publicUrl: String, startWorker: Boolean = true) {
    val base = publicUrl.trimEnd('/')
    val uri = URI(base)
    require(
        uri.scheme in setOf("http", "https") &&
            uri.host != null &&
            uri.rawQuery == null &&
            uri.rawFragment == null &&
            uri.rawUserInfo == null &&
            uri.path.isNullOrEmpty()
    ) {
        "PUBLIC_BASE_URL must be a site origin"
    }
    val api = Api(db, base, ZoneId.of("Asia/Shanghai"))
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<ApiError> { call, error ->
            call.respond(
                HttpStatusCode.fromValue(error.status),
                obj("error" to error.code, "message" to error.message, "fields" to error.fields),
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                obj("error" to "BAD_REQUEST", "message" to "Expected a valid JSON request."),
            )
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                obj("error" to "NOT_FOUND", "message" to "This resource was not found."),
            )
        }
    }
    intercept(ApplicationCallPipeline.Plugins) {
        val origin = call.request.header(HttpHeaders.Origin)
        if (
            origin != null &&
                call.request.httpMethod !in
                    setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options) &&
                origin != base
        )
            throw ApiError(
                403,
                "FORBIDDEN",
                "Request origin does not match the configured frontend.",
            )
    }
    routing {
        route("/api") {
            authRoutes(api, uri.scheme == "https")
            studentRoutes(api)
            organiserRoutes(api)
            teacherRoutes(api)
        }
    }
    if (startWorker)
        launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    db.transaction { processDue() }
                } catch (error: java.sql.SQLException) {
                    log.error("Could not process due reservations and reminders", error)
                }
                delay(15_000)
            }
        }
}
