package campus.auth

import campus.http.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(api: Api, secureCookie: Boolean) {
    post("/signup") {
        val input = call.payload()
        val previous = call.request.cookies[sessionCookie]
        val (session, user) = api.transaction { signUpStudent(input, previous) }
        call.setSessionCookie(session, secureCookie)
        call.respond(HttpStatusCode.Created, user)
    }
    post("/login") {
        val login = call.payload()
        val previous = call.request.cookies[sessionCookie]
        val id = login.text("universityId") ?: invalid("Enter your university ID.")
        val (session, user) =
            api.transaction { signIn(id, login.text("password").orEmpty(), previous) }
        call.setSessionCookie(session, secureCookie)
        call.respond(user)
    }
    post("/logout") {
        val session = call.request.cookies[sessionCookie]
        api.transaction { exec("DELETE FROM sessions WHERE token=?", session) }
        call.setSessionCookie("", secureCookie, maxAge = 0)
        call.respond(obj("signedOut" to true))
    }
    get("/me") {
        api.reply(call) {
            it?.profile ?: throw ApiError(401, "NOT_SIGNED_IN", "Sign in to continue.")
        }
    }
}

private fun ApplicationCall.setSessionCookie(value: String, secure: Boolean, maxAge: Long = 28800) {
    response.cookies.append(
        sessionCookie,
        value,
        maxAge = maxAge,
        path = "/api",
        httpOnly = true,
        secure = secure,
        extensions = mapOf("SameSite" to "Lax"),
    )
}
