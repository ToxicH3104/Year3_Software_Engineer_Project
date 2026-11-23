package campus

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
        "Teachers create clubs, appoint organisers and review the new club's activities" {
            scenario {
                val path = "/teacher/clubs"
                val teacher = login("T001")
                val otherTeacher = login("T002")
                val student = login("S003")
                val name = obj("clubName" to " 摄影社 ")
                request(HttpMethod.Post, path, null, name).expect(401)
                request(HttpMethod.Post, path, student, name).expect(403)
                request(HttpMethod.Post, path, login("O001"), name).expect(403)
                request(HttpMethod.Post, path, teacher, obj("clubName" to " ")).expect(422)
                request(HttpMethod.Post, path, teacher, obj("clubName" to " acM clUB "))
                    .errorIs(409, "CLUB_NAME_IN_USE")
                val attempts = coroutineScope {
                    (1..2)
                        .map { async { request(HttpMethod.Post, path, teacher, name) } }
                        .awaitAll()
                }
                check(attempts.map { it.status.value }.sorted() == listOf(200, 409))
                val club = attempts.single { it.status.value == 200 }.asObject()
                val id = club.text("clubId")!!
                check(club.text("clubName") == "摄影社")
                check(club in request(HttpMethod.Get, path, teacher).asArray())
                check(club !in request(HttpMethod.Get, path, otherTeacher).asArray())
                val roster = "$path/$id/organisers"
                check(request(HttpMethod.Get, roster, teacher).asArray().isEmpty())
                val grant = obj("universityId" to "S003", "reason" to "Founding organiser")
                request(HttpMethod.Post, roster, otherTeacher, grant).expect(403)
                request(HttpMethod.Post, roster, teacher, grant).expect(200)
                val (activity, qr) = published(student, otherTeacher, obj("clubId" to id))
                check(
                    request(HttpMethod.Get, "/activities/$activity").asObject().text("clubName") ==
                        "摄影社"
                )
                request(HttpMethod.Post, "/register/$qr", login("S001")).expect(200)
            }
        }

        "Club teachers grant organiser access without replacing a student's account" {
            scenario {
                val teacher = login("T001")
                val student = login("S001")
                val organiser = login("O001")
                val roster = "/teacher/clubs/guitar/organisers"
                val grant = obj("universityId" to "s001", "reason" to "Elected by club members")
                val (_, qr) = published(organiser, teacher)
                val booking = request(HttpMethod.Post, "/register/$qr", student).asObject()

                request(HttpMethod.Get, "/organiser/clubs", student).expect(403)
                request(HttpMethod.Post, roster, null, grant).expect(401)
                request(HttpMethod.Post, roster, student, grant).expect(403)
                request(HttpMethod.Post, roster, organiser, grant).expect(403)
                request(HttpMethod.Post, roster, login("T002"), grant).expect(403)
                check(
                    request(HttpMethod.Get, "/teacher/clubs", teacher)
                        .asArray()
                        .single()
                        .jsonObject
                        .text("clubId") == "guitar"
                )
                request(
                        HttpMethod.Get,
                        "/teacher/clubs/acm/organisers/candidate?universityId=S001",
                        teacher,
                    )
                    .expect(403)
                request(HttpMethod.Post, roster, teacher, grant.with(obj("universityId" to "T001")))
                    .errorIs(404, "NO_ELIGIBLE_ACCOUNT")
                request(HttpMethod.Post, roster, teacher, grant.with(obj("reason" to " ")))
                    .expect(422)
                request(HttpMethod.Post, roster, teacher, grant).expect(200)
                val roles = request(HttpMethod.Get, "/me", student).asObject()["roles"]!!.jsonArray
                check(
                    roles.map { it.jsonPrimitive.content }.toSet() == setOf("student", "organiser")
                )
                check(
                    request(HttpMethod.Get, "/organiser/clubs", student)
                        .asArray()
                        .single()
                        .jsonObject
                        .text("clubId") == "guitar"
                )
                request(
                        HttpMethod.Post,
                        "/organiser/activities",
                        student,
                        fields.with(obj("clubId" to "acm")),
                    )
                    .expect(403)
                submit(student)
                check(
                    request(HttpMethod.Get, "/my/registrations", student)
                        .asArray()
                        .single()
                        .jsonObject
                        .text("registrationId") == booking.text("registrationId")
                )
                request(HttpMethod.Get, "/teacher/reviews", student).expect(403)

                val lastPlace = coroutineScope {
                    listOf("S002", "S003")
                        .map { id ->
                            async {
                                request(
                                    HttpMethod.Post,
                                    roster,
                                    teacher,
                                    grant.with(obj("universityId" to id)),
                                )
                            }
                        }
                        .awaitAll()
                }
                check(lastPlace.map { it.status.value }.sorted() == listOf(200, 409))
                request(HttpMethod.Post, roster, teacher, grant).expect(200)
                check(request(HttpMethod.Get, roster, teacher).asArray().size == 3)
                val log =
                    db.transaction {
                        rows(
                            "SELECT * FROM organiser_grants WHERE clubId='guitar' AND grantedBy IS NOT NULL"
                        )
                    }
                check(
                    log.size == 2 &&
                        log.all {
                            it.text("grantedBy") == "t1" &&
                                it.text("reason") == "Elected by club members"
                        }
                )
            }
        }

        "Student signup starts a session, keeps IDs unique and grants only student access" {
            scenario {
                val credentials = obj("universityId" to "st2026", "password" to "example pass")
                val signup =
                    credentials.with(
                        obj(
                            "displayName" to " Lin Chen ",
                            "role" to "teacher",
                            "clubId" to "guitar",
                        )
                    )
                request(HttpMethod.Post, "/signup", body = signup.with(obj("password" to "short")))
                    .errorIs(422, "VALIDATION_FAILED")
                request(
                        HttpMethod.Post,
                        "/signup",
                        body = signup.with(obj("universityId" to "o001")),
                    )
                    .errorIs(409, "ID_IN_USE")
                val previous = login("S001")
                val attempts = coroutineScope {
                    (1..2)
                        .map { async { request(HttpMethod.Post, "/signup", previous, signup) } }
                        .awaitAll()
                }
                check(attempts.map { it.status.value }.sorted() == listOf(201, 409))
                val session =
                    attempts
                        .single { it.status.value == 201 }
                        .headers[HttpHeaders.SetCookie]!!
                        .substringBefore(';')
                val profile = request(HttpMethod.Get, "/me", session).asObject()
                check(
                    profile.text("role") == "student" && profile.text("displayName") == "Lin Chen"
                )
                request(HttpMethod.Get, "/me", previous).expect(401)
                request(HttpMethod.Get, "/teacher/reviews", session).expect(403)
                request(HttpMethod.Get, "/organiser/clubs", session).expect(403)
                request(HttpMethod.Get, "/my/registrations", session).expect(200)
                request(HttpMethod.Post, "/logout", session).expect(200)
                request(HttpMethod.Get, "/me", session).expect(401)
                request(
                        HttpMethod.Post,
                        "/login",
                        body = credentials.with(obj("password" to "wrong")),
                    )
                    .errorIs(401, "INVALID_LOGIN")
                val returning =
                    request(
                            HttpMethod.Post,
                            "/login",
                            body = credentials.with(obj("universityId" to "ST2026")),
                        )
                        .asObject()
                check(returning == profile)
            }
        }

        "Unpublished versions stay private and teachers share reviews linked to their snapshots" {
            scenario {
                val org = login("O001")
                val teacher = login("T001")
                val student = login("S001")
                val otherTeacher = login("T002")
                val otherOrg = login("O002")
                request(HttpMethod.Get, "/teacher/history", student).errorIs(403, "FORBIDDEN")
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
                request(HttpMethod.Post, "/organiser/activities/$id/submit", otherOrg)
                    .errorIs(403, "FORBIDDEN")
                request(HttpMethod.Post, "/organiser/activities/$id/submit", org).expect(200)
                request(HttpMethod.Post, "/organiser/activities/$id/submit", org)
                    .errorIs(409, "PENDING_REVIEW")
                request(HttpMethod.Get, "/activities/$id").expect(404)
                val first = pending(teacher).text("reviewId")!!
                check(pending(otherTeacher).text("reviewId") == first)
                request(HttpMethod.Get, "/teacher/reviews/$first", student).expect(403)
                decision(teacher, first, "return").errorIs(422, "REASON_REQUIRED")
                decision(otherTeacher, first, "return", "Clarify venue").expect(200)
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
                        otherOrg,
                        obj("location" to "Hall B"),
                    )
                    .errorIs(403, "FORBIDDEN")
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
                check(request(HttpMethod.Get, "/teacher/reviews", teacher).asArray().isEmpty())
                val near =
                    request(HttpMethod.Get, "/teacher/history", teacher)
                        .asArray()
                        .map { it.jsonObject }
                        .single { it.text("kind") == "edit" }
                check(near.text("status") == "returned")
                decision(teacher, near.text("reviewId")!!).errorIs(409, "REVIEW_EXPIRED")
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
                val released = request(HttpMethod.Get, "/my/notices", login("S002")).asArray()
                check(released.count { it.jsonObject.text("type") == "place_released" } == 1)
                request(HttpMethod.Post, "/my/registrations/$firstId/cancel", login("S001"))
                    .expect(200)
            }
        }
        "Legacy organiser sessions, bookings and pending reviews survive a restart" {
            scenario {
                val org = login("O001")
                val teacher = login("T001")
                val (_, qr) = published(org, teacher)
                val booking =
                    request(HttpMethod.Post, "/register/$qr", org).asObject().text("registrationId")
                val id =
                    submit(
                        org,
                        obj(
                            "startsAt" to "2030-05-01T13:00:00Z",
                            "registrationDeadline" to "2030-05-01T00:30:00Z",
                        ),
                    )
                val file =
                    db.transaction {
                        exec("UPDATE users SET role='organiser' WHERE userId='o1'")
                        exec("DROP VIEW review_requests")
                        one("PRAGMA database_list")!!.text("file")!!
                    }
                clock.at = clock.at.plus(Duration.ofHours(1))
                Database(file, clock).transaction { processDue() }
                check(request(HttpMethod.Get, "/me", org).asObject().text("role") == "student")
                check(
                    request(HttpMethod.Get, "/organiser/activities/$id", org)
                        .asObject()
                        .text("status") == "returned"
                )
                check(
                    request(HttpMethod.Get, "/my/registrations/$booking", org)
                        .asObject()
                        .text("status") == "confirmed"
                )
                check(request(HttpMethod.Get, "/my/notices", org).asArray().size == 1)
                check(
                    db.transaction {
                        one(
                                "SELECT count(*) AS n FROM organiser_grants WHERE clubId='guitar' AND userId='o1'"
                            )!!
                            .number("n")
                    } == 1
                )
            }
        }

        "Only the first decision on a shared submission is recorded" {
            scenario {
                val teachers = listOf(login("T001"), login("T002"))
                submit(login("O001"))
                val id = pending(teachers.first()).text("reviewId")!!
                val results = coroutineScope {
                    teachers.map { async { decision(it, id) } }.awaitAll()
                }
                check(results.map { it.status.value }.sorted() == listOf(200, 409))
                results.single { it.status.value == 409 }.errorIs(409, "ALREADY_DECIDED")
                check(
                    request(HttpMethod.Get, "/teacher/history", teachers.first()).asArray().size ==
                        1
                )
            }
        }

        "The shared queue orders urgent deadlines first and other requests in submission order" {
            scenario {
                val org = login("O001")
                val teacher = login("T002")
                suspend fun submitIn(hours: Long): String {
                    val start = clock.at.plus(Duration.ofHours(hours))
                    return submit(
                        org,
                        obj(
                            "startsAt" to timestamp(start),
                            "registrationDeadline" to timestamp(start.minusSeconds(3600)),
                        ),
                    )
                }
                val firstNormal = submitIn(120)
                val atThreshold = submitIn(60)
                val secondNormal = submitIn(61)
                val earliest = submitIn(18)
                val queue =
                    request(HttpMethod.Get, "/teacher/reviews", teacher).asArray().map {
                        it.jsonObject
                    }
                check(
                    queue.map { it.text("activityId") } ==
                        listOf(earliest, atThreshold, firstNormal, secondNormal)
                )
                check(queue.filter { it["isUrgent"] == JsonPrimitive(true) }.size == 2)
                check(
                    queue[1].text("reviewDeadline") ==
                        timestamp(clock.at.plus(Duration.ofHours(48)))
                )
            }
        }

        "Crossing a review cutoff returns the request even if the first API call fails" {
            scenario {
                val teacher = login("T002")
                val org = login("O001")
                val (publishedId, qr) =
                    published(
                        org,
                        teacher,
                        obj(
                            "startsAt" to "2030-05-02T02:00:00Z",
                            "registrationDeadline" to "2030-05-01T13:00:00Z",
                        ),
                    )
                val booking = request(HttpMethod.Post, "/register/$qr", org).asObject()
                check(
                    request(HttpMethod.Get, "/me", org)
                        .asObject()["roles"]!!
                        .jsonArray
                        .map { it.jsonPrimitive.content }
                        .toSet() == setOf("student", "organiser")
                )
                request(
                        HttpMethod.Post,
                        "/organiser/activities/$publishedId/edits",
                        org,
                        obj("location" to "Other campus"),
                    )
                    .expect(200)
                val newId =
                    submit(
                        org,
                        obj(
                            "startsAt" to "2030-05-01T14:00:00Z",
                            "registrationDeadline" to "2030-05-01T13:00:00Z",
                        ),
                    )
                val queue =
                    request(HttpMethod.Get, "/teacher/reviews", teacher).asArray().map {
                        it.jsonObject
                    }
                val cutoff = Instant.parse("2030-05-01T02:00:00Z")
                check(
                    queue.size == 2 && queue.all { it.text("reviewDeadline") == timestamp(cutoff) }
                )
                clock.at = cutoff.minusMillis(1)
                check(pending(teacher)["canApprove"] == JsonPrimitive(true))
                clock.at = cutoff
                decision(teacher, queue.first().text("reviewId")!!).errorIs(409, "REVIEW_EXPIRED")
                check(
                    db.transaction {
                        one("SELECT count(*) AS n FROM reviews WHERE status='returned'")!!.number(
                            "n"
                        )
                    } == 2
                )
                check(request(HttpMethod.Get, "/teacher/reviews", teacher).asArray().isEmpty())
                val history =
                    request(HttpMethod.Get, "/teacher/history", teacher)
                        .asArray()
                        .map { it.jsonObject }
                        .filter { it.text("status") == "returned" }
                check(
                    history.size == 2 &&
                        history.all {
                            val decision = it.getValue("decision").jsonObject
                            decision["reviewerId"] == JsonNull &&
                                decision.text("reviewerName") == "System" &&
                                !decision.text("reason").isNullOrBlank()
                        }
                )
                val returned =
                    request(HttpMethod.Get, "/organiser/activities/$newId", org).asObject()
                check(
                    returned.text("status") == "returned" &&
                        returned.getValue("feedback").jsonObject.text("reviewerName") == "System"
                )
                request(HttpMethod.Get, "/activities/$newId").expect(404)
                check(
                    request(HttpMethod.Get, "/activities/$publishedId")
                        .asObject()
                        .text("location") == "Room A"
                )
                check(
                    request(
                            HttpMethod.Get,
                            "/my/registrations/" + booking.text("registrationId"),
                            org,
                        )
                        .asObject()
                        .text("status") == "confirmed"
                )
                check(
                    request(HttpMethod.Get, "/my/notices", org).asArray().count {
                        it.jsonObject.text("type") == "review_returned"
                    } == 2
                )
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
                check(
                    request(HttpMethod.Get, "/activities/$secondId")
                        .asObject()
                        .text("registrationClosesAt") == "2030-05-05T00:00:00.000Z"
                )
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
