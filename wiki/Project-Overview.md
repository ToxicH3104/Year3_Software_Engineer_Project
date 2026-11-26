# Project Overview

## 1. The problem

Southwest Jiaotong University runs more than 100 student clubs. Each club arranges activities —
rehearsals, workshops, competitions, talks — and each one has to be announced, limited to a set
number of places, and filled. Today that happens through scattered WeChat groups and posters: a
student cannot tell whether a place is left, an organiser cannot tell who is coming, and the
university has no record of what was announced in its name.

Three things follow from that:

- **Students** need one place to find activities and one unambiguous answer to "am I registered?".
- **Organisers** need a participant list with **name and university ID**, because names repeat.
- **The university** needs every announcement reviewed before it reaches students.

## 2. The solution

A single web app with three faces over one database:

```
  organiser creates ──▶ teacher reviews ──▶ published ──▶ QR code ──▶ student scans and registers
```

Nothing reaches a student until a teacher approves it. Once approved, the activity gets a QR code
whose URL is the registration page, so the same link works from a poster, a projector or a phone.

## 3. Users and personas

| Role | Persona | Main device | Needs |
|---|---|---|---|
| **Student participant** | Yiming Chen, first-year | **Phone** | A clear booking result, current activity details, reminders |
| **Club organiser** | Jiaxin Liu (guitar club); Zihan Huang (ACM undersecretary) | Desktop (dorm PC) and phone | Complete posts, review feedback, an accurate participant list with **name + university ID** |
| **Teacher reviewer** | Qian Zhou | Desktop | One queue, full information, each decision tied to the version reviewed |
| **Account administrator** | Wei Zhang | Desktop | Find the right account, change roles, suspend/restore with a reason |

The administrator is described in the course material but **is not in the backlog**, so it is out
of scope for this release. Club membership and organiser grants are seeded or made by a teacher.

## 4. Scope

Scope is expressed in the [backlog](User-Stories-and-Backlog): **54 items, 137 points**.

| Priority | Items | Points | Meaning |
|---|---|---|---|
| 1 — must have | 35 | 91 | Named in the specification, or the core flow is unsafe without it |
| 2 — should have | 16 | 41 | |
| 3 — could have | 3 | 5 | |

**The MVP is priority 1.** Priority 2 and 3 items are built only after the core flow is stable.

### In scope

- Browsing, searching and viewing published activities without signing in (B1–B3).
- Sign-in for students, organisers and teachers through a **simulated** university identity service (B4).
- Creating activities, submitting them, and the full review cycle: approve or return with a reason (B8–B13).
- QR code generation after approval, and registration from the QR link or the Register button (B14–B18).
- "Registered Activities", cancellation, and reminders (B19, B21–B22, B36–B37).
- Cancelling an activity, and notifying registered students (B23–B25).
- Changing a published activity: content through review, and venue, capacity, deadline and start
  time directly (B26–B41).
- Accessibility to WCAG 2.2 AA and phone usability (B48–B49).

### Out of scope for this release

| Not built | Why |
|---|---|
| A live connection to the real university identity server | The specification accepts a local login (B4) |
| A waiting list for full activities | Not in the backlog |
| Attendance check-in at the venue | Not in the backlog |
| Payments and paid tickets | Not in the backlog |
| Administrator screens (US13–US15) | Not in the backlog; roles come from seed data |
| Club news and announcements (B42–B43) | Lower priority than the activity flow |
| Real email delivery | The team selected in-app notices; no mail is sent |

## 5. Status workflows at a glance

**Activity version** — `Draft → Awaiting review → Published | Returned for changes`. A returned
activity is edited and resubmitted as a new version.

**Published activity** — `Active | Cancelled`, with registration `Open | Full | Closed`.

**Registration** — `Confirmed | Cancelled`, plus `Reserved` after an approved start-time change.

**Account** — `Active | Suspended`.

The full state machines, with every transition, are on [Workflows](Workflows).

## 6. The rules that shape the UI

These are the business rules a page has to honour. They come from the backlog and are implemented
on the server; the interface only reports them.

| Rule | Effect on the screen |
|---|---|
| Only approved activities are visible to students | Unapproved activities never appear, even by direct URL |
| Registration closes at the deadline **or 12 h before the start** | The button explains which limit was hit |
| No duplicate bookings | Registering twice shows the existing booking, not an error |
| The last place cannot go to two students | A lost race returns `FULL`, not a broken page |
| A student may cancel ≥ 24 h before the start | The cancel button is disabled, with a reason, inside 24 h |
| A cancelled activity releases everyone | The QR code shows a cancellation message |
| Every review decision records reviewer, time, reason and version | History is read-only and auditable |
| A start-time change reserves places pending confirmation | Students confirm or decline; unconfirmed places free themselves 24 h before the new start |

## 7. Deliverables

- One tested, working feature per team member.
- This repository: the Ktor API, the React SPA, the tests and `docs/`.
- This wiki.
