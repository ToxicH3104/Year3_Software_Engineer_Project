# User Stories and Backlog

The backlog is the source of truth for what gets built. It is version 1.2, dated 24 November 2026,
and lives in `docs/Project Backlog table v1.2.md`.

## 1. How to read it

| Column | Values |
|---|---|
| **Type** | `F` functional, `N` non-functional |
| **Priority** | `1` must have · `2` should have · `3` could have |
| **Estimate** | XS = 1 point, S = 2, M = 3, L = 5, XL = 8 — **testing included** |

Priority 1 means "named in the specification, or the core flow cannot work safely without it".
The MVP is priority 1.

## 2. Scope

| Priority | Items | Points |
|---|---|---|
| 1 — must have | 35 | 91 |
| 2 — should have | 16 | 41 |
| 3 — could have | 3 | 5 |
| **Total** | **54** | **137** |

## 3. The time rules

Four clocks govern almost every rule in the app. Getting them confused is the most common source of
bugs, so they are stated once here.

| Before the start | Rule | Items |
|---|---|---|
| **48 h** (earlier of old and new start) | Last moment to change the start time | B30, B34 |
| **24 h** (new start) | Unconfirmed reserved places are released | B32, B35 |
| **24 h** | Students can no longer cancel; approving a time change is refused | B21, B34 |
| **12 h** | Registration closes; an unreviewed submission is returned | B17, B45, B46 |
| Until the start | Students can cancel; organisers can change the venue | B21, B41 |

For a **new** activity, registration closes at the earlier of the registration deadline and
start − 12 h.

## 4. Backlog

### Student

| ID | Item | P | Size |
|---|---|---|---|
| B1 | Browse published public activities, with or without signing in | 1 | M |
| B2 | Search and filter activities | 1 | S |
| B3 | View the details of an activity | 1 | S |
| B15 | Register by scanning the QR code or using the Register button | 1 | M |
| B16 | Be asked to sign in before confirming a registration | 1 | S |
| B18 | See my existing booking instead of registering twice | 1 | S |
| B19 | View my bookings in Registered Activities | 1 | M |
| B21 | Cancel my registration at any time before the start | 2 | S |
| B24 | Be notified when an activity I registered for is cancelled | 1 | M |
| B25 | See clearly that an activity is cancelled | 1 | S |
| B27 | See the approved content until a content change is approved | 2 | S |
| B28 | Keep my registration when the start time does not change | 1 | S |
| B29 | Be notified when an activity I registered for changes | 1 | M |
| B31 | Not be penalised for an unconfirmed place | 3 | XS |
| B33 | Be notified when my unconfirmed place is released | 3 | S |
| B35 | Confirm or decline a reserved place after a time change | 2 | M |
| B36 | Set a reminder for a registered activity | 1 | M |
| B43 | Read club news and announcements | 1 | S |

### Organiser

| ID | Item | P | Size |
|---|---|---|---|
| B5 | Manage only the activities of my own club | 1 | S |
| B7 | Limit an activity to club members | 2 | M |
| B8 | Create an activity with the required details | 1 | L |
| B9 | See which activity details are missing or wrong | 1 | S |
| B10 | Submit a new activity for review | 1 | S |
| B13 | Revise a returned activity and submit it again | 2 | S |
| B14 | Get a registration QR code once the activity is approved | 1 | M |
| B20 | View the registration status of my activity | 1 | M |
| B23 | Cancel a published activity | 1 | M |
| B26 | Change the content of a published activity through review | 2 | M |
| B30 | Change the start time of a published activity without review | 2 | L |
| B38 | Keep the member list of my club | 2 | M |
| B39 | Extend the registration deadline without review | 1 | S |
| B40 | Increase the participant capacity without review | 1 | S |
| B41 | Change the venue without review | 1 | S |
| B42 | Publish club news and announcements | 1 | M |

### Teacher

| ID | Item | P | Size |
|---|---|---|---|
| B6 | Make a student an organiser of a club | 2 | M |
| B11 | View submissions waiting for review, with full details | 1 | M |
| B12 | Approve a submission, or return it with a reason | 1 | M |
| B44 | Create a club | 3 | S |
| B45 | See urgent submissions first in the review list | 2 | M |
| B54 | View changes that took effect without review | 1 | M |

### System

| ID | Item | P | Size |
|---|---|---|---|
| B17 | Refuse registration when an activity is not open | 1 | M |
| B22 | Release the place when a registration is cancelled | 2 | XS |
| B32 | Release reserved places not confirmed 24 h before the new start | 2 | M |
| B34 | Refuse a start time change that is too late | 2 | S |
| B37 | Send the reminder at the chosen time | 1 | M |
| B46 | Return a submission not reviewed before its review deadline | 2 | M |
| B47 | Allow only one submission in review for each activity | 2 | XS |

### All roles / non-functional

| ID | Item | P | Size |
|---|---|---|---|
| B4 | Sign in with a university ID through the university identity service | 1 | M |
| B48 | The pages are accessible (WCAG 2.2 AA) | 1 | M |
| B49 | The app works on a phone (no sideways scroll at 360 px) | 1 | S |
| B50 | Every protected action is checked on the server | 1 | M |
| B51 | Passwords and sessions are protected | 1 | S |
| B52 | Data and time rules survive a restart | 1 | S |
| B53 | The activity list stays fast (2 s with 100 clubs, 500 activities) | 2 | S |

## 5. Status in this release

Not every backlog item is built. This table says which, and why.

| Status | Items |
|---|---|
| **Built** | B1–B5, B8–B12, B14–B20, B22–B25, B27, B28, B29, B31, B32, B35, B36, B37, B44, B47, B50–B52 |
| **Built, but not as the backlog describes** | B21 (24 h limit, backlog says any time before the start) · B26/B39/B40/B41 (these changes go through review instead of taking effect at once) · B30 (needs review) · B34 (only the 24 h approval block; no 48 h cutoff) |
| **Partly built** | B13 (a returned activity can be edited and resubmitted, but the earlier reason is not shown beside the form) · B45 (sorting works; the 48 h urgency window does not) · B48/B49 (implemented per page, never audited) |
| **Not built** | B6 is built, but **content fields cannot be changed at all** once published — see below · B33 (a released reservation sends no notice) · B38 · B42 · B43 · B46 · B53 · B54 |

### The B26 problem

B26 is the most consequential gap, and it is easy to miss because the endpoint looks like it works.

`POST /organiser/activities/{id}/edits` accepts **only four fields**: `location`, `capacity`,
`registrationDeadline`, `startsAt`. A request that changes `title`, `description`,
`requirements` or `visibility` is refused with "Change the venue, time, capacity or deadline."

So the review flow for *content* — the thing B26 and design decision 7 are built around — is not
reachable through the API. What does reach review is time, venue, capacity and deadline, which
design decision 5 says should take effect **without** review. The two are effectively swapped.

The detailed, item-by-item comparison of design versus code is on
[Design Decisions](Design-Decisions#2-reconciliation--design-versus-code).

## 6. Specification coverage

How the course specification maps onto backlog items:

| Ref | Specification statement | Backlog |
|---|---|---|
| R1 | Clubs publish news and announcements | B42, B43 |
| R2 | Activity posted with introduction, location, requirements, attendance limit and registration deadline | B8, B9 |
| R3 | Registration QR code generated | B14 |
| R4 | Students search and browse activities | B1, B2, B3 |
| R5 | Students scan the QR code to sign up | B15, B16, B17, B18 |
| R6 | Organisers view registration status | B20 |
| R7 | Organisers extend the deadline, increase capacity or change the location | B28, B39, B40, B41 |
| R8 | Organisers cancel an activity | B23, B25 |
| R9 | Registered Activities list | B19 |
| R10 | Students set reminders | B36, B37 |
| R11 | Teacher reviews before publication | B10, B11, B12, B54 |
| R12 | Unified identity authentication | B4 |

## 7. Will not have in this release

- A live connection to the university identity server — the specification accepts a local login (B4).
- A waiting list for full activities.
- Attendance check-in at the venue.
- Payments and paid tickets.
