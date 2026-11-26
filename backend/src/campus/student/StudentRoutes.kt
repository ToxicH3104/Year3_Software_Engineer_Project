package campus.student

import campus.http.*
import io.ktor.server.routing.*

fun Route.studentRoutes(api: Api) {
    get("/activities") {
        val query = call.queryParameters["q"]
        val date = call.queryParameters["date"]
        api.reply(call) { browse(it, query, date, api.zone) }
    }
    get("/activities/{id}") { api.reply(call) { publicDetail(it, call.param("id")) } }
    route("/register/{token}") {
        get { api.role(call, "student") { tokenDetail(it, call.param("token")) } }
        post { api.role(call, "student") { book(it, call.param("token")) } }
    }
    route("/my") {
        get("/registrations") { api.role(call, "student") { bookings(it) } }
        route("/registrations/{id}") {
            get { api.role(call, "student") { booking(it, call.param("id")) } }
            for (action in listOf("cancel", "confirm", "decline")) {
                post(action) {
                    api.role(call, "student") { changeBooking(it, call.param("id"), action) }
                }
            }
            put("reminder") {
                val input = call.payload()
                api.role(call, "student") { setReminder(it, call.param("id"), input) }
            }
        }
    }
}
