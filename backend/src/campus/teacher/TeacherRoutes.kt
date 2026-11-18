package campus.teacher

import campus.http.*
import io.ktor.server.routing.*

fun Route.teacherRoutes(api: Api) =
    route("/teacher") {
        get("reviews") {
            api.role(call, "teacher") { reviewList(it, false, call.queryParameters["sort"]) }
        }
        get("history") { api.role(call, "teacher") { reviewList(it, true, null) } }
        get("reviews/{id}") { api.role(call, "teacher") { review(it, call.param("id")) } }
        post("reviews/{id}/decision") {
            val body = call.payload()
            api.role(call, "teacher") { decide(it, call.param("id"), body) }
        }
    }
