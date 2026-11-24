# Design Decisions

This page records the decisions behind the domain model, and — honestly — **where the
implementation has not caught up with the design yet**. Read it as a decision log, not a claim of
completeness.

`docs/club-uml.md` is the source for decisions 1–13; the reconciliation in section 2 comes from
reading the code in `backend/src`.

## 1. The thirteen decisions

### 1. An organiser is a role, not a separate account

A teacher upgrades an existing student to organiser of a club (B6), with a reason. The person keeps
the same account and can still register for activities as a student. One student can organise more
than one club. A club has at most three organisers.

*Consequence:* the database needs a role table (`club_roles`) rather than a second row in `users`,
and the three-organiser cap needs enforcing somewhere that no code path can bypass — see the
trigger on [Database Design](Database-Design).

### 2. Every teacher reviews every club

All teachers share one review queue. There is no per-teacher assignment to hide work from.

> **Note:** this is now what the code does — the queue returns every pending review with no club
> filter. An earlier version scoped the queue to the teacher's own clubs (which is what US11 asks
> for); that scoping was removed. See item 3 in section 2.

### 3. No separate visitor role

Browsing, searching and viewing an activity (B1–B3) are student functions that also work without
signing in. Only registering requires a session (B16).

### 4. Method A — services coordinate

`ActivityService` and `ReviewService` update activities, sign-ups and notifications. **`Activity`
never refers to `SignUp`.** Keeping the aggregate free of a back-reference is what lets the review
and booking services change independently.

### 5. What needs review, and what does not

| Change | Needs review |
|---|---|
| New activity | Yes (B10) |
| Title, introduction, participant requirements, visibility | Yes (B26) |
| Start time | No — but see decision 9 and the 24 h approval window (B30, B34) |
| Venue | No (B41) |
| Capacity, **increase only** | No (B40) |
| Registration deadline, **later only** | No (B39) |

### 6. Automatically approved changes are recorded

Every change made without review is recorded with old and new values, and teachers see them in a
separate column beside the queue, view only (B54).

> **Note:** `ChangeRecord` is designed but not yet in the schema. See item 6 in section 2.

### 7. Content and time are separate operations

A content request stores **only the changed content fields**, so approving it can never overwrite a
newer time, venue, capacity or deadline. This is why `activities.fields` is a JSON object and why
`reviews.proposed` can be a partial object.

### 8. Review queue ordering (B45)

Requests within 48 h of their review deadline are urgent and go first, earliest deadline first; the
rest are first come, first served.

### 9. The review deadline is computed, never stored

- New activity: the registration deadline **or** start − 12 h, whichever is earlier.
- Content edit: start − 12 h.

It is worked out from the activity's current times. A submission whose review deadline has already
passed is refused (B10).

> **Note:** the implementation computes this in the `review_requests` SQL view as start − 12 h for a
> new activity and start − **24** h for an edit. It never looks at the registration deadline, and the
> edit window is twice as long as designed. See the table in section 2.

### 10. Overdue requests are returned automatically (B46)

Through the same `returnWith()` a teacher's return uses, with `decidedBy = null` and a system
reason. The interface shows "Returned by: System".

### 11. Only one pending review per activity (B47)

Changes that need no review are still allowed while one is pending. Enforced by a partial unique
index, not by application logic.

### 12. The scheduler

Runs every 15 seconds **and before every request**, so deadlines still hold after downtime (B52):
release unconfirmed reservations (B32), send reminders (B37), return overdue reviews (B46).
`approve()` still checks `isExpired(now)` itself, because the worker and the request are not
synchronised.

### 13. Notifications go through one function

`Notifier.notify()` sends a message to a user. **Open item:** whether that is an in-app notice or an
email (B24, B29, B33, B37) is not decided. The implementation writes a `notices` row.

## 2. Reconciliation — design versus code

The design above is what the team agreed. The code has moved at different speeds. This section is
the honest list, checked against `backend/src` and `frontend/src`.

| # | Design says | Code does | Status |
|---|---|---|---|
| 1 | **Content** changes (title, introduction, requirements, visibility) go to review; venue, capacity, deadline and time take effect **at once** (decision 5) | `POST .../edits` accepts only `location`, `capacity`, `registrationDeadline`, `startsAt` and files **those** as a review item. Content fields are rejected outright | **Swapped.** The reviewable set and the immediate set are the wrong way round, and content is not editable at all once published |
| 2 | Start-time change takes effect at once (B30) | A start-time change is filed as a review item and applied by the teacher | **Divergence.** The 48 h rule (B34) is absent; the 24 h approval block is present |
| 3 | Every teacher reviews every club (decision 2) | The queue shows **every** pending review, with no club filter; `decide()` only requires the teacher role | **Fixed** — matches decision 2. US11's "assigned clubs only" is no longer enforced |
| 4 | A student may cancel **any time before the start** (backlog v1.2, B21) | Cancelling is refused inside 24 h (`TOO_LATE`) | **Divergence.** The 24 h limit comes from an earlier backlog revision |
| 5 | 48 h cutoff for changing a start time (B34) | No 48 h check on the organiser's change path | **Missing** |
| 6 | Overdue reviews auto-return (B46) | `expireOverdueReviews()` runs inside `processDue()` and returns them with a system reason | **Fixed** |
| 7 | Urgent-first queue ordering within 48 h (B45) | `isUrgent` is computed, and the queue orders urgent items by earliest deadline first | **Fixed**, with one difference — see below |
| 8 | Automatically approved changes recorded and shown (B54) | No `ChangeRecord` table or endpoint | **Missing** |
| 9 | A released reservation notifies the student (B33) | `releaseBooking()` only updates the row; no notice is written | **Missing** |
| 10 | Club member list is maintained (B38) | No member add/remove endpoint; memberships come from `seed.sql` | **Missing** |
| 11 | Club news and announcements (B42–B43) | No route, no page | **Not started** |
| 12 | Suspended accounts are blocked even with a session (US15) | `users.accountStatus` exists; no code reads it | **Missing** |
| 13 | Reminders may be email (B36–B37) | Reminders are written to `notices` and read in-app; no email is sent | **Simulated** (as allowed) |

### The review deadline differs from the design

The design (decision 9) says a **new** activity's review deadline is *the registration deadline or
start − 12 h, whichever is earlier*. The implementation computes it in a SQL view:

```sql
CREATE VIEW review_requests AS
SELECT r.*, r.rowid AS queueOrder,
       strftime('%Y-%m-%dT%H:%M:%fZ', json_extract(r.proposed,'$.startsAt'),
           CASE r.kind WHEN 'edit' THEN '-24 hours' ELSE '-12 hours' END) AS reviewDeadline
FROM reviews r;
```

So a **new** activity is always start − 12 h (the registration deadline is not considered), and an
**edit** is start − 24 h rather than the designed start − 12 h.

The registration-deadline half of the rule is the one that matters in practice: an activity with a
registration deadline three days out has no review until 12 h before it starts, so an organiser can
wait until the last moment. This is a real difference, not a rounding error.

Items 1–2 and 4–5 change behaviour a reviewer would notice. Items 8–12 are gaps in priority-2 or
later backlog items, which is consistent with the MVP-first plan on
[User Stories and Backlog](User-Stories-and-Backlog).

### Rules the code does implement

Worth stating explicitly, because these are the ones the tests pin down:

| Rule | Where |
|---|---|
| Registration closes at the deadline **or** 12 h before the start — exposed as `registrationClosesAt` | `activities/Activity.kt` — `registrationState` |
| A student may cancel until 24 h before the start, else `TOO_LATE` | `student/StudentService.kt` |
| The last place is atomic; a lost race gets `FULL` | transaction + `UNIQUE(activityId, studentId)` |
| Registering twice returns the existing booking; a cancelled booking can be re-registered while open | `student/StudentService.kt` — `book()` |
| Cancelling an activity withdraws its pending review and releases every booking, with a notice | `activities/Bookings.kt` — `closeActivity()` |
| A start-time change reserves every active place with a 24 h confirmation deadline | `activities/Bookings.kt` |
| An unconfirmed reservation that expires writes a `place_released` notice | `activities/Bookings.kt` — `processDueBookings()` |
| Approving after the review deadline is refused (`REVIEW_EXPIRED`) | `teacher/ReviewService.kt` |
| An overdue review returns itself with a system reason, and the organiser is notified | `teacher/ReviewService.kt` — `expireOverdueReviews()` |
| The review queue is shared: no teacher may be limited to their own clubs | `teacher/ReviewService.kt` — `reviewList()` |
| Capacity may only increase; the deadline may only move later | `organiser/OrganiserService.kt` |
| Only one pending review per activity (`PENDING_REVIEW`) | partial unique index + service check |
| At most three organisers per club (`ORGANISER_LIMIT`) | SQLite trigger |
| Passwords are PBKDF2-HMAC-SHA256, 120 000 iterations, per-account salt | `auth/Identity.kt` |
| A write whose `Origin` is not the configured frontend is refused | `Application.kt` |
| Due work survives a failed request (due work is committed, the handler rolls back to a savepoint) | `http/Api.kt` |

## 3. Open questions still open

These are genuinely undecided, not forgotten. The first three are conflicts between two sources,
not gaps — each needs a decision rather than more code. They are written up in full on
[Revision Log](Revision-Log#3-conflicts-that-are-still-open).

1. **Content versus review** — the design says content needs review and venue/capacity/deadline/time
   do not; the code has it exactly the other way round. See
   [Revision Log § 3.1](Revision-Log#31-content-changes-are-not-reviewable).
2. **Cancellation window** — any time before the start (backlog v1.2), or 24 h (code and student
   spec)? See [Revision Log § 3.2](Revision-Log#32-cancellation-24-h-or-any-time).
3. **Whether B30 should keep its "no review" shape** — see item 2 above. Either the design or the
   code has to move. See [Revision Log § 3.3](Revision-Log#33-start-time-changes-review-or-not).

**Resolved since the deadlines commit:**

- **Notification channel** — the team selected **in-app delivery**. Backlog items B24, B29, B33 and
  B37 still say "email"; the code and `docs/requirements.md` now say in-app. The backlog wording is
  what remains out of date.
- **Reject** — not a decision. Only approve and return-with-reason exist.

Still genuinely open:

4. **Club-only activities** — hidden from non-members, or visible but not registrable? The code
   currently hides them (a non-member gets `404` on the detail page and `NOT_MEMBER` on register).
5. **Search by date** — the backlog says title only (B2); one user story asks for a date filter. The
   code now accepts a `date` parameter, so this is settled in practice.
6. **CSRF** — cookie sessions with `SameSite=Lax` plus an `Origin` check are the current answer. A
   token-based scheme is not implemented.
