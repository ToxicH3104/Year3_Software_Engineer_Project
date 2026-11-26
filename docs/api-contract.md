# Campus Club API contract

All paths below include the prefix `/api`. Requests with bodies use `Content-Type: application/json`. Lists return JSON arrays, single items return JSON objects, and successful operations default to HTTP 200. Times are ISO 8601 with an offset, normalised to UTC in responses. Demo accounts and startup instructions are in [the backend README](../backend/README.md).

## Identity and permissions

| Method | Path | Result |
|---|---|---|
| POST | /api/signup | Send `displayName`, `universityId` and `password`. Creates a student account with no memberships, sets a session cookie and returns the profile with HTTP 201. An existing ID returns 409 `ID_IN_USE`. Invalid fields return 422 `VALIDATION_FAILED`. |
| POST | /api/login | Send `{"universityId":"S001","password":"demo-S001"}`. Returns the profile and sets a session cookie. |
| GET | /api/me | Current profile with userId, universityId, displayName, role (primary role), roles (all effective roles) and accountStatus. |
| POST | /api/logout | Invalidates the session. |

Anonymous visitors can browse published public activities. Club-only activities are hidden from visitors outside that club. A private registration token still requires student membership. Students may access only their own bookings. The system provisions teachers. A club's assigned teacher or the system can grant organiser access to an active student. Demo organisers also have the student role. The maximum is three organisers per club. Student accounts keep their personal bookings and can manage activities only for clubs with an organiser grant.

## Organiser

| Method | Path | Body or result |
|---|---|---|
| GET | /api/organiser/clubs | Clubs managed by the current organiser |
| GET | /api/organiser/activities | Managed activities, including drafts and pending changes |
| POST | /api/organiser/activities | Activity fields, clubId, and optional boolean submit (default false). Returns the activity. |
| GET, PUT | /api/organiser/activities/{id} | Read the management view, or update fields in a draft or returned activity |
| POST | /api/organiser/activities/{id}/submit | Submit the saved version for review |
| POST | /api/organiser/activities/{id}/edits | Changed fields for a published activity |
| GET | /api/organiser/activities/{id}/participants | Each active booking's registrationId, studentName, universityId and status |
| GET | /api/organiser/activities/{id}/qr.png | PNG of the registration link after first approval |
| POST | /api/organiser/activities/{id}/cancel | `{"reason":"Venue unavailable"}`. The frontend confirms the activity name before sending. |

Example submission for O001 (replace both dates with future dates when needed):

```json
{
  "clubId": "guitar",
  "title": "Open rehearsal",
  "description": "Practise together before the concert.",
  "startsAt": "2030-05-05T18:00:00+08:00",
  "location": "Room A",
  "requirements": "Bring an instrument.",
  "capacity": 10,
  "registrationDeadline": "2030-05-04T18:00:00+08:00",
  "visibility": "public",
  "submit": true
}
```

Drafts can be partial. Submission validates the full form. Activity status is draft, awaiting_review, returned, published or cancelled. Published edits can change venue or start, extend the deadline, or increase capacity. Only one version may be pending. The management view adds qrToken, pendingEdit, feedback, confirmedCount, reservedCount and remaining to the activity fields. QR tokens stay stable through edits and cancellation.

## Teacher

| Method | Path | Body or result |
|---|---|---|
| GET | /api/teacher/reviews | One pending queue for all teachers. Reviews due within 48 hours come first by reviewDeadline, followed by other requests in submission order. |
| GET | /api/teacher/reviews/{id} | Versioned submission, proposed fields, previous baseline and feedback, changedFields, reviewDeadline, isUrgent, canApprove and the recorded decision |
| POST | /api/teacher/reviews/{id}/decision | `{"decision":"approve"}` or `{"decision":"return","reason":"Specify the room"}` |
| GET | /api/teacher/history | All teacher decisions and automatic returns, newest first |
| GET | /api/teacher/clubs | Clubs assigned to this teacher |
| POST | /api/teacher/clubs | Send `clubName` (1–100 characters). Creates a club and assigns the signed-in teacher in one transaction. Returns its clubId and clubName. A duplicate name returns 409 CLUB_NAME_IN_USE. |
| GET | /api/teacher/clubs/{id}/organisers | Current organisers with displayName and universityId |
| GET | /api/teacher/clubs/{id}/organisers/candidate?universityId=S003 | Exact university ID lookup for an eligible account before granting access |
| POST | /api/teacher/clubs/{id}/organisers | Send universityId and reason. The grant is idempotent. A full club returns 409 ORGANISER_LIMIT. |

Approval publishes the submitted snapshot. Return requires a reason. Both store the reviewer, time and version. For edits, students continue to see the last approved snapshot while the teacher reviews the proposal. Cancellation withdraws any pending edit. The review deadline is 12 hours before the proposed start for a new activity and 24 hours before the proposed start for every edit. At the deadline a pending review is automatically returned. Its decision has a null reviewerId, reviewerName “System” and a reason. The submitting organiser receives an in-app notice.

## Public browsing and student registrations

| Method | Path | Body or result |
|---|---|---|
| GET | /api/activities?q=guitar&date=2030-05-05 | Published activities visible to this visitor. Both filters are optional. Date uses Asia/Shanghai. |
| GET | /api/activities/{id} | Current published detail, remaining, registrationState, registrationLink and myRegistration |
| GET | /api/register/{token} | Student session required. Preview the activity, including cancellation info, without taking a place. |
| POST | /api/register/{token} | Confirm or return the existing active booking, including when the activity has since filled up |
| GET | /api/my/registrations | Own bookings by activity start, including cancelled records |
| GET | /api/my/registrations/{id} | Own booking and current activity details |
| POST | /api/my/registrations/{id}/cancel | Cancel and release the place at least 24 hours before start |
| POST | /api/my/registrations/{id}/confirm | Confirm a reserved place after a time change |
| POST | /api/my/registrations/{id}/decline | Decline and release a reserved place |
| PUT | /api/my/registrations/{id}/reminder | Future reminderAt before the activity, or `{"reminderAt":null}` to switch off |

All signed-in roles can access their own inbox. First approval creates one unread `activity_published` notice for every active account for a public activity, including organisers and teachers. For club-only activities, recipients must be members of the club. The notice includes an `activityLink` while its recipient can open the published activity. Later approvals of edits use the existing booking notifications.

| Method | Path | Result |
|---|---|---|
| GET | /api/my/notices | Newest messages first, with activity title, optional booking ID and readAt (null while unread). |
| GET | /api/my/notices/unread | Unread count for the signed-in account. |
| PUT | /api/my/notices/{id}/read | Mark one message as read and return the unread count. |
| PUT | /api/my/notices/read-through/{id} | Mark the displayed batch as read through its newest message. Later arrivals stay unread. |

registrationClosesAt is the earlier of the stated registrationDeadline and 12 hours before the start. The student view displays this effective cutoff. Both confirmed and reserved places count towards capacity. An approved time change reserves existing places until 24 hours before the new start. At that cutoff unconfirmed places are released. Approved changes with the same start preserve existing registrations. SQLite write transactions protect the last seat and enforce one booking per student and activity. The server checks due reminders, expiring reservations and overdue reviews every 15 seconds and before API operations. After a restart it processes overdue work against persisted times.

## Errors and frontend integration

Example error: `{"error":"FULL","message":"No places remain.","fields":null}`. Validation failures can include field-level messages, e.g. `"fields":{"capacity":"Enter a positive whole number."}`.

| HTTP | Examples |
|---|---|
| 400 | BAD_REQUEST, malformed JSON |
| 401 | NOT_SIGNED_IN, INVALID_LOGIN |
| 403 | FORBIDDEN, NOT_MEMBER |
| 404 | NOT_FOUND (includes another student's booking) |
| 409 | FULL, CLOSED, CANCELLED, TOO_LATE, REVIEW_EXPIRED, NOT_EDITABLE, NOT_RESERVED, PENDING_REVIEW, ALREADY_DECIDED |
| 422 | VALIDATION_FAILED, REASON_REQUIRED, INVALID_REMINDER_TIME |

For a first integration test, submit an activity as O001 and save its activityId. Sign in as T001, fetch the pending reviewId, and approve it. Sign in as S001, open the published activity, and follow its registrationLink (the React page). That page first calls GET on the token API to preview, then POST to confirm. A second POST returns the same registrationId.
