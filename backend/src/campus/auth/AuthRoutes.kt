package campus.auth

import campus.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(api: Api, secureCookie: Boolean) {
    post("/login") {
        val login = call.payload()
        val previous = call.request.cookies[sessionCookie]
        val id = login.text("universityId") ?: invalid("Enter a demo university ID.")
        val (session, user) =
            api.transaction { signIn(id, login.text("password").orEmpty(), previous) }
        call.response.cookies.append(
            sessionCookie,
            session,
            maxAge = 28800,
            path = "/api",
            httpOnly = true,
            secure = secureCookie,
            extensions = mapOf("SameSite" to "Lax"),
        )
        call.respond(user)
    }
    post("/logout") {
        val session = call.request.cookies[sessionCookie]
        api.transaction { exec("DELETE FROM sessions WHERE token=?", session) }
        call.response.cookies.append(
            sessionCookie,
            "",
            maxAge = 0,
            path = "/api",
            httpOnly = true,
            secure = secureCookie,
            extensions = mapOf("SameSite" to "Lax"),
        )
        call.respond(obj("signedOut" to true))
    }
    get("/me") {
        api.reply(call) {
            it?.profile ?: throw ApiError(401, "NOT_SIGNED_IN", "Sign in to continue.")
        }
    }
}
