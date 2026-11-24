# Campus Club Activity App – UML

Backlog items are written as B1, B2, ...

## Design decisions

1. **Organiser is a role, not a separate account.** A teacher upgrades a student to organiser of a club (B6), with a reason. The person keeps the same account and can still register for activities as a student. One student can organise more than one club. A club has at most 3 organisers.
2. **Every teacher reviews every club.** All teachers share one review queue.
3. **No separate visitor.** Browsing, searching and viewing activities (B1–B3) are student functions that also work without signing in. Registering requires sign-in (B16).
4. **Method A – services coordinate.** `ActivityService` and `ReviewService` update activities, sign-ups and notifications. `Activity` never refers to `SignUp`.
5. **What needs review.** New activities, and content changes to a published activity (title, introduction, participant requirements, visibility – B26). Changes to the start time (B30), venue (B41), capacity (B40, only up) and registration deadline (B39, only later) take effect at once without review.
6. **Automatically approved changes.** Every change made without review is recorded with old and new values. Teachers see them in a separate column next to the review queue, view only (B54).
7. **Content and time are separate operations.** A content request stores only the changed content fields, so approving it never overwrites a newer time, venue, capacity or deadline.
8. **Review queue (B45).** Requests within 48h of their review deadline are urgent and go first, earliest deadline first; the rest are first come, first served.
9. **Review deadline.** NEW: the registration deadline or start − 12h, whichever is earlier. CONTENT_EDIT: start − 12h. It is worked out from the activity's current times, never stored. A submission whose review deadline has already passed is refused (B10).
10. **Overdue requests are returned automatically** (B46), through the same `returnWith()` as a teacher's return, with `decidedBy = null` and a system reason. The UI shows "Returned by: System".
11. **Only one pending request per activity** (B47). Changes that need no review are still allowed while one is pending.
12. **Scheduler.** Runs every 15 seconds and before every request, so deadlines also hold after downtime (B52): release unconfirmed reservations (B32), send reminders (B37), return overdue reviews (B46). `approve()` still checks `isExpired(now)` itself.
13. **Notifications.** `Notifier.notify()` sends the message to the user. **Open item:** in-app notice or email (B24, B29, B33, B37) is not decided yet.

### Time rules

| Before the start | Rule | Items |
|---|---|---|
| 48h before the old or the new start, whichever is earlier | Last moment to change the start time; after that the activity can only be cancelled | B30, B34 |
| 24h before the new start | Reserved places that are not confirmed are released | B32, B35 |
| 12h | Registration closes and a request not reviewed is returned; for a new activity both happen earlier if the registration deadline is earlier | B17, B45, B46 |
| Until the start | Students can cancel; organisers can change the venue | B21, B41 |

---

## 1. Use case diagram

```mermaid
flowchart LR
    S["Student"]
    O["Organiser (a student with the organiser role)"]
    T["Teacher"]
    W["Scheduler (timer)"]

    O -- "is a" --> S

    subgraph APP["Campus Club Activity App"]
        direction TB
        U1(["Browse public activities, no sign-in (B1)"])
        U2(["Search activities, no sign-in (B2)"])
        U3(["View activity details, no sign-in (B3)"])
        U4(["Sign in (B4)"])
        U5(["Register via button or QR (B15, B16)"])
        U6(["View my registrations (B19)"])
        U7(["Cancel registration (B21)"])
        U8(["Confirm or decline reserved place (B35)"])
        U9(["Set reminder (B36)"])
        U10(["Create activity (B8, B9)"])
        U11(["Submit for review (B10)"])
        U12(["Revise and resubmit (B13)"])
        U13(["Change content, with review (B26)"])
        U24(["Change time, venue, capacity or deadline, no review (B30, B39, B40, B41)"])
        U25(["Keep the member list (B38)"])
        U26(["Publish club news (B42)"])
        U27(["Read club news (B43)"])
        U14(["Cancel activity (B23)"])
        U15(["View registrations and places (B20)"])
        U16(["Show QR code (B14)"])
        U17(["View review queue (B11)"])
        U18(["Approve or return with reason (B12)"])
        U19(["Create club (B44)"])
        U28(["View automatically approved changes (B54)"])
        U23(["Upgrade a student to organiser, max 3 (B6)"])
        U20(["Return overdue reviews (B46)"])
        U21(["Release unconfirmed reservations (B32)"])
        U22(["Send reminders (B37)"])
    end

    S --- U1
    S --- U2
    S --- U3
    S --- U4
    S --- U5
    S --- U6
    S --- U7
    S --- U8
    S --- U9
    S --- U27
    O --- U10
    O --- U11
    O --- U12
    O --- U13
    O --- U14
    O --- U15
    O --- U16
    O --- U24
    O --- U25
    O --- U26
    T --- U4
    T --- U17
    T --- U18
    T --- U19
    T --- U23
    T --- U28
    W --- U20
    W --- U21
    W --- U22
```

An organiser can do everything a student can ("is a"), plus the organiser use cases for their own clubs.

---

## 2a. Class diagram – users and clubs

```mermaid
classDiagram
    class User {
        <<abstract>>
        -universityId: String
        -displayName: String
        -passwordHash: String
        -accountStatus: String
        +signIn(password: String) Boolean
    }
    class Student {
        -clubRoles: List~ClubRole~
        +isMemberOf(club: Club) Boolean
        +isOrganiserOf(club: Club) Boolean
    }
    class Teacher
    class ClubService {
        +createClub(teacher, name: String) Club
        +upgradeToOrganiser(teacher, universityId, club, reason: String)
        +organisers(club) List~Student~
    }
    class Club {
        -name: String
        +addMember(student: Student)
        +removeMember(student: Student)
        +addOrganiser(student: Student, grant: OrganiserGrant)
        +organiserCount() Int
    }
    class ClubRole {
        -relation: ClubRelation
    }
    class ClubRelation {
        <<enumeration>>
        MEMBER
        ORGANISER
    }
    class NewsPost {
        -title: String
        -text: String
        -image: String?
        -publishedAt: DateTime
    }
    class OrganiserGrant {
        -grantedAt: DateTime
        -reason: String
    }

    User <|-- Student
    User <|-- Teacher
    Student "1" --> "*" ClubRole : holds
    ClubRole "*" --> "1" Club : in
    ClubRole --> ClubRelation
    Teacher "1" --> "*" OrganiserGrant : grants
    OrganiserGrant "*" --> "1" Student : to
    OrganiserGrant "*" --> "1" Club : for
    Teacher "1" --> "*" Club : creates
    Teacher --> ClubService : manages clubs via
    ClubService ..> Club : updates
    Club "1" *-- "*" NewsPost : publishes
    Student "1" --> "*" NewsPost : writes as organiser
```

Rules:
- A club has at most 3 `ClubRole` with relation `ORGANISER` (B6). `addOrganiser()` refuses a 4th.
- A reason is required for every upgrade, and it is kept in `OrganiserGrant`.
- After the upgrade the student has both roles: student and organiser. No new account is needed.
- An organiser only manages activities, members (B38) and news (B42) of clubs they organise (B5). News needs no review.
- Club-only activities can only be seen and joined by members (B7).
- Teachers are not linked to clubs for reviewing: every teacher reviews every club.

---

## 2b. Class diagram – activities, review and sign-up

```mermaid
classDiagram
    class Activity {
        -title: String
        -introduction: String
        -requirements: String
        -location: String
        -capacity: Int
        -startsAt: DateTime
        -registrationDeadline: DateTime
        -visibility: Visibility
        -status: ActivityStatus
        -qrToken: String?
        -cancelReason: String?
        +submit()
        +approve()
        +returnForChanges()
        +applyContent(changes)
        +changeStart(newStart, newDeadline)
        +changeVenue(location: String)
        +increaseCapacity(capacity: Int)
        +extendDeadline(deadline: DateTime)
        +cancel(reason: String)
    }
    class ActivityStatus {
        <<enumeration>>
        DRAFT
        AWAITING_REVIEW
        RETURNED
        PUBLISHED
        CANCELLED
    }
    class Visibility {
        <<enumeration>>
        PUBLIC
        CLUB_ONLY
    }
    class ReviewRequest {
        -version: Int
        -kind: RequestKind
        -submittedAt: DateTime
        -status: RequestStatus
        -proposed
        -baseline
        +reviewDeadline() DateTime
        -decidedBy: Teacher?
        -reason: String?
        +isUrgent(now, window) Boolean
        +isExpired(now) Boolean
    }
    class RequestKind {
        <<enumeration>>
        NEW
        CONTENT_EDIT
    }
    class RequestStatus {
        <<enumeration>>
        PENDING
        APPROVED
        RETURNED
        WITHDRAWN
    }
    class ReviewQueue {
        -requests: List~ReviewRequest~
        -urgentWindow: Duration = 48h
        +append(request)
        +ordered(now) List~ReviewRequest~
        +overdue(now) List~ReviewRequest~
    }
    class ReviewService {
        +submitNew(activity, organiser)
        +submitContentEdit(activity, changes, organiser)
        +viewQueue() List~ReviewRequest~
        +approve(request, teacher)
        +returnWith(request, decidedBy: Teacher?, reason: String)
        +withdrawPending(activity)
        +expireOverdue(now)
    }
    class ActivityService {
        +changeTime(activity, newStart, newDeadline, organiser)
        +changeVenue(activity, location, organiser)
        +increaseCapacity(activity, capacity, organiser)
        +extendDeadline(activity, deadline, organiser)
        +cancel(activity, reason, organiser)
        +automaticChanges() List~ChangeRecord~
    }
    class ChangeRecord {
        -field: String
        -oldValue: String
        -newValue: String
        -changedAt: DateTime
    }
    class SignUpService {
        +register(student, activity) SignUp
        +findActive(activity) List~SignUp~
        +reserve(signUp, confirmBy)
        +confirm(signUp)
        +decline(signUp)
        +cancel(signUp)
        +releaseUnconfirmed(now)
        +setReminder(signUp, at)
        +clearReminder(signUp)
        +sendDueReminders(now)
    }
    class Notifier {
        +notify(user: User, message: String)
    }
    class SignUp {
        -registeredAt: DateTime
        -status: SignUpStatus
        -confirmBy: DateTime?
        -reminderAt: DateTime?
    }
    class SignUpStatus {
        <<enumeration>>
        CONFIRMED
        RESERVED
        CANCELLED
    }

    Club "1" *-- "*" Activity : runs
    Student "1" --> "*" Activity : creates as organiser
    Activity --> ActivityStatus
    Activity --> Visibility
    ReviewRequest --> RequestKind
    ReviewRequest --> RequestStatus
    ReviewRequest "*" --> "1" Activity : about
    ReviewQueue "1" o-- "*" ReviewRequest : holds
    ReviewService --> ReviewQueue : uses
    ReviewService --> SignUpService : uses
    ReviewService --> Notifier : uses
    Teacher --> ReviewService : decides via
    ActivityService --> Activity : changes
    ActivityService --> ReviewService : withdraws pending request
    ActivityService --> SignUpService : uses
    ActivityService --> Notifier : uses
    SignUpService --> Notifier : uses
    ActivityService ..> ChangeRecord : records
    ChangeRecord "*" --> "1" Activity : about
    ChangeRecord "*" --> "1" Student : changed by organiser
    Teacher --> ActivityService : views automatic changes
    SignUpService "1" o-- "*" SignUp : manages
    Student "1" --> "*" SignUp : makes
    SignUp "*" --> "1" Activity : for
    SignUp --> SignUpStatus
```

Design notes:
- **Method A**: `ActivityService` (changes without review) and `ReviewService` (reviewed submissions) coordinate. `Activity` has no link to `SignUp`.
- `reviewDeadline()`: NEW = the registration deadline or start − 12h, whichever is earlier. CONTENT_EDIT = start − 12h. Always from the current activity.
- `proposed` of a CONTENT_EDIT holds only the changed content fields.
- `changeTime()` is refused later than 48h before the old or the new start, whichever is earlier (B34). If the registration deadline is no longer before the new start, a new deadline is required (B30).
- A start time change makes every active sign-up `RESERVED` with `confirmBy` = new start − 24h. If it moves the start to or before `reminderAt`, the reminder is removed and the student is told (B36).
- A venue, capacity, deadline or content change leaves sign-ups as they are (B28).
- Students are notified at once of a time or venue change, and after approval of a content change (B29).
- Queue order: requests within 48h of `reviewDeadline` first, sorted by earliest deadline; others FIFO.
- `returnWith(reason: String)` – a reason is always required. `decidedBy = null` means the system returned it; the UI shows "Returned by: System".
- One `PENDING` request per activity at a time.
- Cancelling an activity marks its pending request `WITHDRAWN` and cancels all sign-ups.
- `version` and `baseline` let the teacher compare a new version with the approved one.
- `ChangeRecord` is only for changes made without review (B54).
- A student has at most one `SignUp` per activity (B18). Registering again returns the existing booking.
- Registration closes at the registration deadline or 12h before start, whichever is first, or when no places remain (B17).
- A `RESERVED` place still counts against capacity until it is confirmed or released (B30).

---

## 3. State diagram – Activity

```mermaid
stateDiagram-v2
    [*] --> Draft : organiser creates
    Draft --> AwaitingReview : organiser submits
    AwaitingReview --> Published : teacher approves
    AwaitingReview --> Returned : teacher returns, or review deadline passed
    Returned --> AwaitingReview : organiser revises and resubmits
    Published --> Cancelled : organiser cancels
    Cancelled --> [*]
    note right of Published
        Content changes go through a ReviewRequest and the old content stays live until approved.
        Time, venue, capacity and deadline changes take effect at once.
    end note
```

## 4. State diagram – ReviewRequest

```mermaid
stateDiagram-v2
    [*] --> Pending : organiser submits
    state Pending {
        [*] --> Normal
        Normal --> Urgent : now reaches reviewDeadline minus 48h
    }
    Pending --> Approved : teacher approves
    Pending --> Returned : teacher returns with reason
    Pending --> Returned : review deadline passed, decidedBy = system
    Pending --> Withdrawn : activity cancelled
    Approved --> [*]
    Returned --> [*]
    Withdrawn --> [*]
```

## 4b. State diagram – SignUp

```mermaid
stateDiagram-v2
    [*] --> Confirmed : student registers
    Confirmed --> Reserved : start time changed
    Reserved --> Reserved : start time changed again, new confirmBy
    Reserved --> Confirmed : student confirms before confirmBy
    Reserved --> Cancelled : student declines
    Reserved --> Cancelled : confirmBy passed, place released
    Confirmed --> Cancelled : student cancels before the start
    Confirmed --> Cancelled : activity cancelled
    Reserved --> Cancelled : activity cancelled
    Cancelled --> Confirmed : student registers again while open
```

Every move to Cancelled releases the place (B22). The student is told when an unconfirmed place is released (B33).

---

## 5. Sequence diagram – start time change, no review (B30, B34, B35)

```mermaid
sequenceDiagram
    actor O as Organiser
    actor S as Student
    participant AS as ActivityService
    participant A as Activity
    participant SS as SignUpService
    participant N as Notifier

    Note over O,N: Phase 1 - Organiser changes the start time
    O->>AS: changeTime(activity, newStart, newDeadline, organiser)
    AS->>A: status, startsAt, registrationDeadline
    A-->>AS: current values
    alt not PUBLISHED
        AS-->>O: Error - only a published activity can be changed
    else later than 48h before the earlier of old and new start
        AS-->>O: Error - too late, cancel the activity instead
    else registration deadline would not be before newStart
        AS-->>O: Error - enter a new registration deadline
    else allowed
        AS->>A: changeStart(newStart, newDeadline)
        AS->>AS: add ChangeRecord with old and new time (B54)
        AS->>SS: findActive(activity)
        SS-->>AS: active SignUps
        loop each active SignUp
            AS->>SS: reserve(signUp, confirmBy = newStart - 24h)
            opt reminderAt is not before newStart
                AS->>SS: clearReminder(signUp)
            end
            AS->>N: notify(student, time changed, please confirm, reminder removed if it was)
        end
        AS-->>O: Saved
    end

    Note over O,N: Phase 2 - Student responds, at least 24h to do so
    alt student confirms
        S->>SS: confirm(signUp)
    else student declines
        S->>SS: decline(signUp)
    end
    alt RESERVED and now before confirmBy
        SS->>SS: status = CONFIRMED, or CANCELLED with the place released
        SS-->>S: Done
    else not reserved or too late
        SS-->>S: Error - nothing to respond to
    end
```

Venue, capacity and deadline changes follow Phase 1 without the 48h check and without `reserve()`. Each one applies its own rule from the design decisions and adds a `ChangeRecord`. Only a venue change must also notify every student at once.

## 5b. Sequence diagram – content change with review (B26, B27, B29)

```mermaid
sequenceDiagram
    actor O as Organiser
    actor T as Teacher
    participant RS as ReviewService
    participant Q as ReviewQueue
    participant A as Activity
    participant SS as SignUpService
    participant N as Notifier

    Note over O,N: Phase 1 - Organiser submits a content change
    O->>RS: submitContentEdit(activity, changes, organiser)
    RS->>A: getStatus()
    A-->>RS: status
    alt not PUBLISHED
        RS-->>O: Error - only a published activity can be changed
    else a request for this activity is already PENDING
        RS-->>O: Error - a version is already awaiting review
    else review deadline has already passed
        RS-->>O: Error - too late for review
    else allowed
        RS->>Q: append(ReviewRequest CONTENT_EDIT, PENDING, changed content fields only)
        RS-->>O: Submitted for review
        Note over A: Stays PUBLISHED with the old content
    end

    Note over O,N: Phase 2 - Teacher reviews
    T->>RS: viewQueue()
    RS->>Q: ordered(now)
    Q-->>RS: urgent first, earliest deadline first
    RS-->>T: review list
    alt teacher returns it
        T->>RS: returnWith(request, teacher, reason)
        RS->>N: notify(organiser, returned with reason)
        Note over A: Published version unchanged
    else teacher approves but request.isExpired(now)
        T->>RS: approve(request, teacher)
        RS-->>T: Error - review deadline has passed
    else teacher approves in time
        T->>RS: approve(request, teacher)
        RS->>A: applyContent(changes)
        RS->>RS: status = APPROVED, decidedBy = teacher
        RS->>SS: findActive(activity)
        SS-->>RS: active SignUps
        loop each active SignUp
            RS->>N: notify(student, content changed)
        end
        RS-->>T: Approved
    end
```

## 6. Sequence diagram – scheduler: unconfirmed places, reminders and overdue reviews (B32, B33, B37, B46)

```mermaid
sequenceDiagram
    participant W as Scheduler
    participant RS as ReviewService
    participant Q as ReviewQueue
    participant A as Activity
    participant SS as SignUpService
    participant N as Notifier

    Note over W,N: Every 15 seconds, and before every API request
    W->>SS: releaseUnconfirmed(now)
    loop each RESERVED SignUp past confirmBy
        SS->>SS: status = CANCELLED, place released
        SS->>N: notify(student, place released)
    end

    W->>SS: sendDueReminders(now)
    loop each CONFIRMED or RESERVED SignUp with reminderAt reached
        SS->>N: notify(student, reminder)
        SS->>SS: clear reminderAt, so it is sent once
    end

    W->>RS: expireOverdue(now)
    RS->>Q: overdue(now)
    Q-->>RS: requests past reviewDeadline
    loop each overdue request
        RS->>RS: returnWith(request, null, "No teacher reviewed this before the review deadline. Please check the time and resubmit.")
        alt kind = NEW
            RS->>A: returnForChanges()
        else kind = CONTENT_EDIT
            Note over A: Published version unchanged
        end
        RS->>N: notify(organiser, returned with reason)
    end
```

## 7. Sequence diagram – teacher upgrades a student to organiser (B6)

```mermaid
sequenceDiagram
    actor T as Teacher
    participant TS as ClubService
    participant C as Club
    participant ST as Student

    T->>TS: upgradeToOrganiser(teacher, universityId, club, reason)
    TS->>ST: find active account by universityId
    alt no active account
        TS-->>T: Error - no eligible account
    else account found
        alt reason is blank
            TS-->>T: Error - enter a reason
        else already organiser of this club
            TS-->>T: OK, no change
        else club already has 3 organisers
            TS-->>T: Error - club already has three organisers
        else
            TS->>C: addOrganiser(student, grant with reason)
            TS-->>T: Student is now an organiser
            Note over ST: Same account, now has student and organiser roles
        end
    end
```

## 8. Sequence diagram – student registers via button or QR (B15–B18)

```mermaid
sequenceDiagram
    actor S as Student
    participant SS as SignUpService
    participant A as Activity

    S->>SS: register(student, activity from QR link or Register button)
    alt not signed in
        SS-->>S: Sign in to continue (B16)
    else club-only activity and student is not a member
        SS-->>S: Error - only club members may register (B7)
    else signed in and allowed
        SS->>SS: find existing SignUp for this student
        alt active SignUp exists
            SS-->>S: Show existing booking (B18)
        else no active SignUp
            SS->>A: registration state
            A-->>SS: open, full, closed or cancelled
            alt open
                SS->>SS: create SignUp, status = CONFIRMED
                SS-->>S: Registered
            else full
                SS-->>S: Error - no places remain
            else closed
                SS-->>S: Error - registration has closed
            else cancelled
                SS-->>S: Error - activity has been cancelled
            end
        end
    end
```
