# Workflows

Three state machines and three flows that are easy to get wrong. The diagrams match
`docs/club-uml.md`; where the code differs, the difference is called out.

## 1. Activity status

```mermaid
stateDiagram-v2
    [*] --> Draft : organiser creates
    Draft --> AwaitingReview : organiser submits
    AwaitingReview --> Published : teacher approves
    AwaitingReview --> Returned : teacher returns with a reason
    Returned --> AwaitingReview : organiser revises and resubmits
    Published --> Cancelled : organiser cancels
    Cancelled --> [*]

    note right of Published
        A content change files a ReviewRequest.
        The approved version stays live until it is approved.
    end note
```

Two things to keep straight:

- **`Returned` is called "Needs changes" in the interface.** The backlog and the UML use
  `returned`; one user-story document used "Needs Changes". The state is `returned`, the label is
  "Needs changes".
- **Cancelling is not a review action.** It takes effect at once, withdraws any pending review, and
  releases every booking.

## 2. Review request

```mermaid
stateDiagram-v2
    [*] --> Pending : organiser submits
    Pending --> Approved : teacher approves
    Pending --> Returned : teacher returns with a reason
    Pending --> Returned : review deadline passed, decidedBy = system
    Pending --> Withdrawn : activity cancelled
    Approved --> [*]
    Returned --> [*]
    Withdrawn --> [*]
```

Only **one** `Pending` request per activity exists at a time — enforced by a partial unique index,
not by a check in the service. A second submission gets `PENDING_REVIEW`.

## 3. Registration

```mermaid
stateDiagram-v2
    [*] --> Confirmed : student registers
    Confirmed --> Reserved : start time changed
    Reserved --> Reserved : start time changed again, new deadline
    Reserved --> Confirmed : student confirms in time
    Reserved --> Cancelled : student declines
    Reserved --> Cancelled : confirmation deadline passed
    Confirmed --> Cancelled : student cancels ≥ 24 h before the start
    Confirmed --> Cancelled : activity cancelled
    Reserved --> Cancelled : activity cancelled
    Cancelled --> Confirmed : student registers again while open
```

Every move to `Cancelled` releases the place. A `Reserved` place still counts against capacity until
it is confirmed or released (B30) — otherwise a time change would let the activity be oversold while
students are deciding.

## 4. Registration, step by step

```mermaid
sequenceDiagram
    actor S as Student
    participant API as Ktor
    participant A as Activity
    participant DB as SQLite

    S->>API: POST /register/{token}
    API->>API: session? else 401 NOT_SIGNED_IN
    API->>A: resolve the token
    alt unknown token
        API-->>S: 404 NOT_FOUND
    else club-only and not a member
        API-->>S: 403 NOT_MEMBER
    else already booked
        API-->>S: 200 the existing booking (no new row)
    else open
        API->>A: registrationState
        A-->>API: open / full / closed / cancelled
        alt not open
            API-->>S: 409 FULL | CLOSED | CANCELLED
        else open
            API->>DB: INSERT registration (confirmed)
            Note over DB: UNIQUE(activityId, studentId)<br/>makes the last place atomic
            API-->>S: 200 booking confirmed
        end
    end
```

The last place is won by the `UNIQUE` constraint and a transaction, not by a count-then-insert in
application code. Two students racing get one success and one `FULL`.

## 5. Start-time change

This is the most involved flow in the project, because it touches all three roles and a timer.

```mermaid
sequenceDiagram
    actor O as Organiser
    actor T as Teacher
    actor S as Student
    participant API as Ktor
    participant W as Worker

    O->>API: POST /organiser/activities/{id}/edits {startsAt, registrationDeadline}
    API->>API: capacity only up? deadline only later? deadline before start?
    API->>API: file a review item (kind = edit)
    Note over API: approved version stays live<br/>students still see the old time
    T->>API: GET /teacher/reviews
    API-->>T: canApprove = false when the new start is < 24 h away
    alt teacher approves in time
        T->>API: POST /teacher/reviews/{id}/decision {approve}
        API->>API: apply the new fields
        loop every active booking
            API->>API: status = reserved, reservationDeadline = newStart - 24 h
            API->>S: notice "The start time changed. Please confirm."
        end
    else returned, or the deadline passed
        T->>API: POST .../decision {return, reason}
        Note over API: the published version is unchanged
    end
    S->>API: POST /my/registrations/{id}/confirm
    API-->>S: confirmed
    W->>API: processDue() every 15 s, and before every request
    API->>API: reserved and past reservationDeadline -> cancelled, place released
```

### Where the code and the design disagree

| | Design | Code |
|---|---|---|
| Which fields need review? | **Content** — title, introduction, requirements, visibility (B26) | Only `location`, `capacity`, `registrationDeadline`, `startsAt` are accepted; content fields are refused |
| Does a start-time change need review? | No — takes effect at once (B30) | Yes — filed as an `edit` review item |
| 48 h cutoff (B34) | Refuse later than 48 h before the earlier of old and new start | Not implemented |
| 24 h approval block (B34) | Refuse approving inside 24 h of the new start | Implemented — `TOO_CLOSE_TO_START` |

The practical consequences, and they are serious:

- An organiser cannot change a published activity's **title or description at all**.
- An organiser cannot move an activity without a teacher, even though the backlog says they should
  be able to.
- The four fields that were meant to bypass review are the four that require it.

This is the most consequential divergence in the project, and it is written up on
[Revision Log § 3.1](Revision-Log#31-content-changes-are-not-reviewable) and
[Design Decisions](Design-Decisions#2-reconciliation--design-versus-code).

## 6. The worker

```mermaid
flowchart LR
    T["every 15 s"] --> P["processDue()"]
    R["before every API request"] --> P
    P --> A["release reserved bookings<br/>past reservationDeadline"]
    P --> B["send reminders<br/>whose reminderAt has passed"]
    A --> N["write a notice"]
    B --> N
```

Running `processDue()` **inside each request's transaction** is what makes deadlines survive
downtime: after a restart, the first request to arrive applies everything that fell due while the
server was off, before it answers.

The worker deliberately does **not** handle overdue reviews, even though design decision 12 says it
should. `processDue()` covers reservations and reminders only; `B46` is not implemented. See
[Design Decisions](Design-Decisions#2-reconciliation--design-versus-code).

## 7. Cancellation

One function does all the work, and every caller goes through it:

```
closeActivity(activity, reason)
├── status = cancelled, cancelReason = reason
├── withdraw any pending review          (status = withdrawn)
└── for every booking that is not cancelled
    ├── write a "cancelled" notice with the reason
    └── release the booking
```

Because the QR code resolves the activity rather than a booking, a scanned code on a cancelled
activity lands on the cancellation message with no extra work. Because pending reviews are
withdrawn, a teacher cannot later approve an activity that no longer exists in public view.

## 8. Organiser promotion

```mermaid
sequenceDiagram
    actor T as Teacher
    participant API as Ktor
    participant DB as SQLite

    T->>API: POST /teacher/clubs/{id}/organisers {universityId, reason}
    API->>API: role = teacher? club = mine?
    API->>API: reason blank? -> 422 REASON_REQUIRED
    API->>DB: find an active account by universityId
    alt no account
        API-->>T: 422 NO_ELIGIBLE_ACCOUNT
    else already an organiser
        API-->>T: 200, no change (idempotent)
    else
        API->>DB: INSERT club_roles (organiser)
        Note over DB: trigger aborts a fourth organiser<br/>-> ORGANISER_LIMIT
        API->>DB: INSERT organiser_grants (who, by whom, when, why)
        API-->>T: 200
    end
```

The student keeps the same account and keeps their bookings; they gain the organiser interface on
their next request. The reason is stored, not just validated, so the grant is auditable.
