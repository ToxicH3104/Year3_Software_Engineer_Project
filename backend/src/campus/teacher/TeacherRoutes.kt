package campus.teacher

import campus.auth.*
import campus.http.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonArray

fun Route.teacherRoutes(api: Api) =
    route("/teacher") {
        get("clubs") {
            api.role(call, "teacher") { actor ->
                JsonArray(
                    rows(
                        """SELECT clubId, clubName FROM clubs JOIN club_roles USING(clubId)
                    WHERE userId=? AND relation='teacher' ORDER BY clubName""",
                        actor.id,
                    )
                )
            }
        }
        route("clubs/{id}/organisers") {
            get {
                val club = call.param("id")
                api.role(call, "teacher") { actor ->
                    requireClub(actor, club, "teacher")
                    organiserRoster(club)
                }
            }
            get("candidate") {
                val club = call.param("id")
                api.role(call, "teacher") { actor ->
                    requireClub(actor, club, "teacher")
                    organiserCandidate(call.queryParameters["universityId"])
                        .only("displayName", "universityId")
                }
            }
            post {
                val club = call.param("id")
                val input = call.payload()
                api.role(call, "teacher") { actor ->
                    requireClub(actor, club, "teacher")
                    grantOrganiser(club, input.text("universityId"), input.text("reason"), actor.id)
                }
            }
        }
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
