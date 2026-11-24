# Revision Log

This page records **how the design changed**, not just what it is now. Design decisions that were
reversed, conflicts between the backlog and the code, and the open questions that are still open —
all of it is here rather than tidied away.

A project that only shows its final state looks like it was obvious from the start. It was not.

## 1. Timeline

| When | What changed | Why |
|---|---|---|
| Week 8 | Server-rendered Pebble pages, CSV storage | The original course plan |
| Week 9 | **Replaced by a React SPA + Ktor JSON API** | The student side is phone-first and interactive: live place counts, registration states, confirmation dialogs. A JSON API also lets the three role interfaces evolve independently |
| Week 9 | **CSV replaced by SQLite** | Concurrency. "Two students cannot both take the last place" (B17) needs a transaction and a unique constraint, not a file rewrite |
| Week 9 | Cookie sessions, not JWT | Suspension must take effect immediately, and signing out must actually revoke. A session row makes both trivial |
| Week 10 | Requirements distilled into `docs/requirements.md` plus three role specs | The course brief, the user stories and the backlog disagreed in places. The backlog was chosen as the arbiter |
| Week 10 | **Backlog becomes the source of truth** over the user stories | Where they conflicted (Reject, anonymous browsing), the backlog won. Both conflicts are recorded below |
| Week 11 | UML and backlog v1.2 committed | Design decisions 1–13, with the time rules extracted into one table |
| Week 11 | This wiki | The repository explained the code; nothing explained the project |

## 2. Conflicts that were resolved

Each of these was a genuine disagreement between two sources. The resolution is a decision, and the
losing option is kept visible so it is clear it was considered.

| Conflict | Option A | Option B | Resolved |
|---|---|---|---|
| Can a visitor browse without signing in? | Backlog B1: yes | User story US01: "signed-in student" | **Browsing is public, registering needs a sign-in.** B1 wins |
| Is there a Reject decision for teachers? | User story US05–US09: approve / return / reject | Backlog B12: approve or return only | **No Reject.** Only approve and return-with-reason. The organiser spec's open question 1 marks this closed |
| What is the returned state called? | User stories: "Needs Changes" | Group 15 stories: "Returned for changes" | **State `returned`, interface label "Needs changes"** |
| Is an organiser a separate account? | Natural reading of "organiser role" | B6 says a teacher promotes a student | **A role, not an account.** The student keeps their bookings |
| Does every teacher review every club? | Simplest queue | US11 says only assigned clubs | **Design says one shared queue; the code scopes per club.** Unresolved — see section 3 |
| Does a start-time change need review? | Backlog B30: takes effect at once | Implemented as a review item | **Unresolved.** The code files a review. See section 3 |
| Do club announcements count? | Group 17 draft stories mention news and rankings | Backlog B42–B43 include news, not rankings | **News is in the backlog; competition rankings are out of scope** |
| Wireframes | Week 8 sheets in the repository | They are blank templates | **No wireframes to follow.** The specs carry the layout requirements instead |

## 3. Conflicts that are still open

These are not resolved, and pretending otherwise would be dishonest.

### 3.1 Content changes are not reviewable

The design is explicit: **content** changes (title, introduction, participant requirements,
visibility) go to a teacher for review (B26, design decision 5), while **venue, capacity, the
registration deadline and the start time** take effect immediately (B30, B39–B41).

The code does the opposite. `POST /organiser/activities/{id}/edits` accepts only `location`,
`capacity`, `registrationDeadline` and `startsAt`, and files them for review. Title, description,
requirements and visibility are refused. So:

- a published activity's title cannot be changed at all, and
- the four fields that were meant to bypass review are the four that require it.

This is the most consequential gap in the project. It is not a missing feature at the edge of the
MVP — it is the review flow working on the wrong fields.

**The decision needed:** make the endpoint accept the content fields and file those for review,
while applying venue, capacity, deadline and time directly. That single change would bring the code
back in line with decisions 5 and 7 and with four backlog items at once.

### 3.2 Cancellation: 24 h or any time?

Backlog v1.2 (B21) says a student may cancel **at any time before the start**. The code refuses
cancellation inside 24 h with `TOO_LATE`, which is what an earlier backlog revision said and what
the student spec still says.

The two rules give very different products. A 24 h limit protects an organiser from a place freed
too late to be useful; an unlimited rule protects the student. One of the two documents is out of
date and it is not clear which.

**The decision needed:** pick one and update either the code or the backlog.

### 3.3 Start-time changes: review or not?

The backlog (B30) says an organiser changes the start time of a published activity **without
review**, effective immediately, with every place becoming reserved. The code files the change as a
review item and a teacher applies it.

Both are defensible. Immediate effect matches the backlog and keeps organisers autonomous; review
protects students from a late, arbitrary move. But they cannot both be true, and right now the
documentation says one thing and the product does another.

**The decision needed:** either implement B30 as written (move the start-time path out of the review
flow, add the 48 h cutoff from B34), or amend the backlog to say a time change needs review. Leaving
it is the worst option.

### 3.4 Who does a teacher review?

Design decision 2 says all teachers share one queue covering every club. US11 says a teacher only
decides on their own assigned clubs. The code follows US11.

**The decision needed:** confirm US11 is the intended behaviour and amend design decision 2, or
remove the club scoping. The code is the more defensible choice — a reviewer should not be able to
approve an activity for a club they know nothing about — so the design text is probably what should
change.

### 3.5 The notification channel

B24, B29, B33 and B37 all say "email". Nothing decides whether email is real or simulated. The code
writes an in-app notice row. The backlog's own note says the channel "is still to be decided".

**The decision needed:** accept in-app notices for this release, and say so in the backlog.

## 4. Gaps between design and code

Full detail is on [Design Decisions](Design-Decisions#2-reconciliation--design-versus-code). In
summary, these are designed but not built:

| Item | Backlog | Note |
|---|---|---|
| Content changes are not editable | B26 | The endpoint accepts venue, capacity, deadline and time only — see 3.1 |
| 48 h cutoff for changing a start time | B34 | No such check exists |
| Automatic return of overdue reviews | B46 | The worker handles reservations and reminders only |
| Urgent-first queue ordering | B45 | Sorting exists; the 48 h urgency window does not |
| Recording automatically approved changes | B54 | No `ChangeRecord` table |
| Notice when a reservation is released | B33 | `releaseBooking()` writes no notice |
| Club member list maintenance | B38 | Memberships come from seed data |
| Club news and announcements | B42–B43 | No route, no page |
| Blocking suspended accounts | US15 | The column exists; nothing reads it |

None of these blocks the core flow except B26, which is priority 1 and is discussed in 3.1.

## 5. Documentation history

`docs/` was committed in two batches rather than incrementally, so the git history shows less
process than actually happened:

| Date | Documents | Commit |
|---|---|---|
| 2026-11-16 | `requirements.md`, `student-spec.md`, `organiser-spec.md`, `teacher-spec.md` | Batched into `04d6746` |
| 2026-11-18 | The same four, first pushed to GitHub | `04d6746` |
| 2026-11-24 | `club-uml.md`, `Project Backlog table v1.2.md` | `c72079c` |

The file modification times are the honest record of when the work happened; the commit timestamps
of the second batch were set to match them. This page exists partly so that the sequence is written
down somewhere, rather than inferred from a commit graph that flattens it.

## 6. How to keep this page honest

When a decision is reversed:

1. Do not edit the old row. Add a new one, and mark the old one superseded.
2. If a conflict is resolved, move it from section 3 to section 2 and state what won.
3. If a gap is closed, remove it from section 4 — but say in the commit message that it closed a
   gap recorded here.

A revision log that only ever grows is a log. One that gets quietly rewritten is marketing.
