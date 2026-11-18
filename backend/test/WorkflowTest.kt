package campus

import campus.activities.processDue
import campus.db.Database
import campus.http.*
import io.kotest.core.spec.style.FreeSpec
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.nio.file.Files
import java.time.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*

private class TestClock(var at: Instant = Instant.parse("2030-05-01T00:00:00Z")) : Clock() {
    override fun instant() = at

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = Clock.fixed(at, zone)
}

private class Fixture(val client: HttpClient, val clock: TestClock, val db: Database) {
    suspend fun request(
        method: HttpMethod,
        path: String,
        cookie: String? = null,
        body: JsonObject? = null,
    ) =
        client.request("/api" + path) {
            this.method = method
            cookie?.let { header(HttpHeaders.Cookie, it) }
            body?.let {
                contentType(ContentType.Application.Json)
                setBody(it.toString())
            }
        }

    suspend fun login(id: String): String {
        val response =
            request(
                HttpMethod.Post,
                "/login",
                body = obj("universityId" to id, "password" to "demo-$id"),
            )
        response.expect(200)
        return response.headers[HttpHeaders.SetCookie]!!.substringBefore(';')
    }

    val fields =
        obj(
            "clubId" to "guitar",
            "title" to "Open rehearsal",
            "description" to "Join our practice.",
            "location" to "Room A",
            "requirements" to "Bring an instrument.",
            "capacity" to 1,
            "startsAt" to "2030-05-05T12:00:00Z",
            "registrationDeadline" to "2030-05-05T10:00:00Z",
            "visibility" to "public",
        )

    suspend fun submit(owner: String, overrides: JsonObject = obj()): String =
        request(
                HttpMethod.Post,
                "/organiser/activities",
                owner,
                fields.with(overrides).with(obj("submit" to true)),
            )
            .asObject()
            .text("activityId")!!

    suspend fun pending(teacher: String) =
        request(HttpMethod.Get, "/teacher/reviews", teacher).asArray().first().jsonObject

    suspend fun decision(
        teacher: String,
        review: String,
        decision: String = "approve",
        reason: String? = null,
    ) =
        request(
            HttpMethod.Post,
            "/teacher/reviews/$review/decision",
            teacher,
            obj("decision" to decision, "reason" to reason),
        )

    suspend fun published(
        owner: String,
        teacher: String,
        overrides: JsonObject = obj(),
    ): Pair<String, String> {
        val id = submit(owner, overrides)
        decision(teacher, pending(teacher).text("reviewId")!!).expect(200)
        val token =
            request(HttpMethod.Get, "/organiser/activities/$id", owner).asObject().text("qrToken")!!
        return id to token
    }
}

private suspend fun HttpResponse.expect(code: Int): HttpResponse {
    check(status.value == code) { "Expected $code, got $status: ${bodyAsText()}" }
    return this
}

private suspend fun HttpResponse.asObject() =
    Json.parseToJsonElement(expect(200).bodyAsText()).jsonObject

private suspend fun HttpResponse.asArray() =
    Json.parseToJsonElement(expect(200).bodyAsText()).jsonArray

private suspend fun HttpResponse.errorIs(code: Int, name: String) {
    check(Json.parseToJsonElement(expect(code).bodyAsText()).jsonObject.text("error") == name)
}

private fun scenario(block: suspend Fixture.() -> Unit) = testApplication {
    val file = Files.createTempFile("campus-api-test", ".db")
    try {
        val clock = TestClock()
        val db = Database(file.toString(), clock)
        application { campusApi(db, "http://localhost:5173", startWorker = false) }
        Fixture(client, clock, db).block()
    } finally {
        Files.deleteIfExists(file)
    }
}

class WorkflowTest :
    FreeSpec({
        "Unpublished versions stay private, decisions are scoped and linked to their snapshots" {
            scenario {
                val org = login("O001")
                val teacher = login("T001")
                val student = login("S001")
                val otherTeacher = login("T002")
                val otherOrg = login("O002")
                request(HttpMethod.Post, "/organiser/activities", student, fields)
                    .errorIs(403, "FORBIDDEN")
                request(
                        HttpMethod.Post,
                        "/organiser/activities",
                        org,
                        fields.with(obj("submit" to true, "capacity" to 0)),
                    )
                    .errorIs(422, "VALIDATION_FAILED")
                val draft =
                    request(
                            HttpMethod.Post,
                            "/organiser/activities",
                            org,
                            obj("clubId" to "guitar", "title" to "Draft"),
                        )
                        .asObject()
                val id = draft.text("activityId")!!
                check(draft.text("status") == "draft" && draft.text("qrToken") == null)
                request(HttpMethod.Get, "/organiser/activities/$id", otherOrg)
                    .errorIs(403, "FORBIDDEN")
                request(HttpMethod.Put, "/organiser/activities/$id", org, fields).expect(200)
                request(HttpMethod.Post, "/organiser/activities/$id/submit", org).expect(200)
                request(HttpMethod.Get, "/activities/$id").expect(404)
                check(request(HttpMethod.Get, "/teacher/reviews", otherTeacher).asArray().isEmpty())
                val first = pending(teacher).text("reviewId")!!
                decision(otherTeacher, first).errorIs(403, "FORBIDDEN")
                decision(teacher, first, "return").errorIs(422, "REASON_REQUIRED")
                decision(teacher, first, "return", "Clarify venue").expect(200)
                request(
                        HttpMethod.Put,
                        "/organiser/activities/$id",
                        org,
                        obj("location" to "Annex"),
                    )
                    .expect(200)
                request(HttpMethod.Post, "/organiser/activities/$id/submit", org).expect(200)
                val second = pending(teacher)
                check(second["previousFeedback"]!!.jsonObject.text("reason") == "Clarify venue")
                check(
                    "location" in
                        second["changedFields"]!!.jsonArray.map { it.jsonPrimitive.content }
                )
                decision(teacher, second.text("reviewId")!!).expect(200)
                decision(teacher, second.text("reviewId")!!).errorIs(409, "ALREADY_DECIDED")
                val detail = request(HttpMethod.Get, "/activities/$id").asObject()
                check(detail.text("location") == "Annex")
                request(
                        HttpMethod.Post,
                        "/organiser/activities/$id/edits",
                        org,
                        obj("location" to "Hall B"),
                    )
                    .expect(200)
                check(
                    request(HttpMethod.Get, "/activities/$id").asObject().text("location") ==
                        "Annex"
                )
                decision(teacher, pending(teacher).text("reviewId")!!).expect(200)
                val changed = request(HttpMethod.Get, "/activities/$id").asObject()
                check(
                    changed.text("registrationLink") == detail.text("registrationLink") &&
                        changed.text("location") == "Hall B"
                )
                check(request(HttpMethod.Get, "/teacher/history", teacher).asArray().size == 3)
                check(
                    request(HttpMethod.Get, "/teacher/reviews/$first", teacher)
                        .asObject()["proposed"]!!
                        .jsonObject
                        .text("location") == "Room A"
                )
            }
        }

        "The last place is atomic, repeats are idempotent, cancellation releases the place" {
            scenario {
                val org = login("O001")
                val teacher = login("T001")
                val students = listOf(login("S001"), login("S002"))
                val (id, key) = published(org, teacher)
                request(HttpMethod.Get, "/register/$key").errorIs(401, "NOT_SIGNED_IN")
                students.forEach { request(HttpMethod.Get, "/register/$key", it).expect(200) }
                val results = coroutineScope {
                    students
                        .map { async { request(HttpMethod.Post, "/register/$key", it) } }
                        .awaitAll()
                }
                val winner = results.indexOfFirst { it.status.value == 200 }
                check(winner >= 0)
                results[1 - winner].errorIs(409, "FULL")
                val booking = results[winner].asObject().text("registrationId")!!
                check(
                    request(HttpMethod.Post, "/register/$key", students[winner])
                        .asObject()
                        .text("registrationId") == booking
                )
                request(HttpMethod.Get, "/my/registrations/$booking", students[1 - winner])
                    .expect(404)
                check(
                    request(HttpMethod.Get, "/organiser/activities/$id/participants", org)
                        .asArray()
                        .size == 1
                )
                request(HttpMethod.Post, "/my/registrations/$booking/cancel", students[winner])
                    .expect(200)
                request(HttpMethod.Post, "/register/$key", students[1 - winner]).expect(200)
                val qr = request(HttpMethod.Get, "/organiser/activities/$id/qr.png", org)
                check(qr.expect(200).contentType()?.withoutParameters() == ContentType.Image.PNG)
                request(
                        HttpMethod.Post,
                        "/organiser/activities/$id/cancel",
                        org,
                        obj("reason" to "Building closed"),
                    )
                    .expect(200)
                check(
                    request(HttpMethod.Get, "/register/$key", students[1 - winner])
                        .asObject()
                        .text("cancelReason") == "Building closed"
                )
                request(HttpMethod.Post, "/register/$key", students[winner])
                    .errorIs(409, "CANCELLED")
                check(
                    request(HttpMethod.Get, "/my/notices", students[1 - winner]).asArray().any {
                        it.jsonObject.text("type") == "cancelled"
                    }
                )
            }
        }

        "Time changes reserve places, enforce the approval window, and expire unconfirmed places" {
            scenario {
                val org = login("O001")
                val teacher = login("T001")
                val student = login("S001")
                val second = login("S002")
                val (id, key) =
                    published(
                        org,
                        teacher,
                        obj("capacity" to 2, "registrationDeadline" to "2030-05-01T12:00:00Z"),
                    )
                val firstId =
                    request(HttpMethod.Post, "/register/$key", student)
                        .asObject()
                        .text("registrationId")!!
                val secondId =
                    request(HttpMethod.Post, "/register/$key", second)
                        .asObject()
                        .text("registrationId")!!
                request(
                        HttpMethod.Post,
                        "/organiser/activities/$id/edits",
                        org,
                        obj("startsAt" to "2030-05-01T18:00:00Z"),
                    )
                    .expect(200)
                val near = pending(teacher).text("reviewId")!!
                decision(teacher, near).errorIs(409, "TOO_CLOSE_TO_START")
                decision(teacher, near, "return", "Too soon").expect(200)
                request(
                        HttpMethod.Post,
                        "/organiser/activities/$id/edits",
                        org,
                        obj("startsAt" to "2030-05-06T12:00:00Z"),
                    )
                    .expect(200)
                decision(teacher, pending(teacher).text("reviewId")!!).expect(200)
                check(
                    request(HttpMethod.Get, "/my/registrations/$firstId", student)
                        .asObject()
                        .text("status") == "reserved"
                )
                request(HttpMethod.Post, "/my/registrations/$firstId/confirm", student).expect(200)
                clock.at = Instant.parse("2030-05-05T12:00:00Z")
                db.transaction { processDue() }
                check(
                    request(HttpMethod.Get, "/my/registrations/$secondId", login("S002"))
                        .asObject()
                        .text("status") == "cancelled"
                )
                check(
                    request(HttpMethod.Get, "/activities/$id").asObject().number("remaining") == 1
                )
                request(HttpMethod.Post, "/my/registrations/$firstId/cancel", login("S001"))
                    .expect(200)
            }
        }
        "Club access, reminder delivery, registration cutoff and sign out" {
            scenario {
                val org = login("O001")
                val teacher = login("T001")
                val member = login("S001")
                val outsider = login("S002")
                val (id, key) = published(org, teacher, obj("visibility" to "club_only"))
                check(request(HttpMethod.Get, "/activities", outsider).asArray().isEmpty())
                request(HttpMethod.Get, "/activities/$id", outsider).expect(404)
                request(HttpMethod.Post, "/register/$key", outsider).errorIs(403, "NOT_MEMBER")
                val b =
                    request(HttpMethod.Post, "/register/$key", member)
                        .asObject()
                        .text("registrationId")!!
                request(
                        HttpMethod.Put,
                        "/my/registrations/$b/reminder",
                        member,
                        obj("reminderAt" to "2029-01-01T00:00:00Z"),
                    )
                    .errorIs(422, "INVALID_REMINDER_TIME")
                request(
                        HttpMethod.Put,
                        "/my/registrations/$b/reminder",
                        member,
                        obj("reminderAt" to "2030-05-01T01:00:00Z"),
                    )
                    .expect(200)
                clock.at = clock.at.plusSeconds(3600)
                repeat(2) { db.transaction { processDue() } }
                check(request(HttpMethod.Get, "/my/notices", member).asArray().size == 1)
                val (secondId, secondKey) = published(org, teacher)
                clock.at = Instant.parse("2030-05-05T00:00:00Z")
                val renewed = login("S001")
                check(
                    request(HttpMethod.Get, "/activities/$secondId", renewed)
                        .asObject()
                        .text("registrationState") == "closed"
                )
                request(HttpMethod.Post, "/register/$secondKey", renewed).errorIs(409, "CLOSED")
                request(HttpMethod.Post, "/my/registrations/$b/cancel", renewed)
                    .errorIs(409, "TOO_LATE")
                request(HttpMethod.Post, "/logout", renewed).expect(200)
                request(HttpMethod.Get, "/me", renewed).expect(401)
            }
        }
    })
