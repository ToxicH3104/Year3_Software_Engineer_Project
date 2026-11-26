# Team and Contributions

Group 17, COMP2850 Software Engineering.

> This page is derived from `git log`. It records what the repository can prove, and says so where
> it cannot. Each member should correct their own row — a contribution table that overstates is
> worse than one that understates.

## 1. How to read this page

The repository's commit history does not map cleanly onto the four names in the course brief, for
two reasons:

- Some commits were made from a shared local identity (`dev <dev@local>`) before per-person
  configuration was set, so they are not attributable to a GitHub account.
- Documentation was committed in two large batches rather than incrementally, so commit count
  understates the work in `docs/`.

The table below therefore lists **what exists** and **who the history attributes it to**, and leaves
the per-person split for the team to confirm.

## 2. Who built what

| Area | Files | Attributed to |
|---|---|---|
| Backend core: database, HTTP helpers, activity rules, bookings, auth | `backend/src/campus/db/`, `http/`, `activities/`, `auth/`, `Application.kt`, `resources/` | `saanba` |
| Backend role surfaces: organiser and teacher routes and services, review logic, club and organiser grants | `backend/src/campus/organiser/`, `teacher/`, `auth/AuthRoutes.kt`, `auth/Identity.kt`, `auth/OrganiserGrants.kt` | `Zooox208` |
| Backend tests | `backend/test/WorkflowTest.kt` | `Zooox208` |
| Frontend: board, student, organiser and teacher pages, shared components, API module, Tailwind styling | `frontend/src/` | `Zooox208` |
| Documentation: requirements and the three role specs | `docs/requirements.md`, `docs/{student,organiser,teacher}-spec.md` | `dev <dev@local>` |
| Documentation: UML and the backlog | `docs/club-uml.md`, `docs/Project Backlog table v1.2.md` | `Toxic` |
| Repository setup, `.gitignore`, README | `.gitignore`, `README.md` | `ToxicH3104`, `Toxic` |

Commit counts, for context only:

| Identity | Commits |
|---|---|
| `Zooox208` | 6 |
| `dev <dev@local>` | 3 |
| `Toxic` | 2 |
| `ToxicH3104` | 1 |
| `saanba` | 1 |

A single large commit can carry far more work than six small ones — `saanba`'s `v1` and
`Zooox208`'s `v3_backend` are both substantial — so these numbers are **not** a ranking.

## 3. What each role owns

Rather than guess at individuals, this is the split by responsibility, which is what the backlog
actually assigns.

| Responsibility | Backlog | Where it lives |
|---|---|---|
| Student experience: browse, register, my registrations, cancel, remind | B1–B3, B15–B19, B21 | `frontend/src/pages/Board.tsx`, `Student.tsx`; `backend/src/campus/student/` |
| Organiser experience: create, submit, manage, participants, QR, cancel | B5, B8–B10, B14, B20, B23 | `frontend/src/pages/Organiser.tsx`; `backend/src/campus/organiser/` |
| Teacher experience: queue, review, history, clubs, organiser grants | B6, B11–B12, B44 | `frontend/src/pages/Teacher.tsx`, `TeacherOrganisers.tsx`; `backend/src/campus/teacher/` |
| Domain rules: capacity, deadlines, time change, reservations | B17, B22, B28–B35 | `backend/src/campus/activities/` |
| Data and security: schema, sessions, password hashing, role checks | B4, B50–B52 | `backend/src/campus/db/`, `auth/`, `http/` |
| Accessibility and responsiveness | B48–B49 | every page; see [Accessibility](Accessibility) |
| Documentation and design | — | `docs/`, this wiki |

## 4. How work was delivered

| Milestone | What it contained |
|---|---|
| `v1` | Backend skeleton: schema, seed data, database layer, HTTP helpers |
| `v1frontend`, `v2_frontend` | Board and student pages |
| `v1_backend`, `v2_backend` | Activity rules, booking, auth |
| `v3_backend`, `v3_frontend` | Review service, organiser and teacher surfaces, tests |
| Documentation batches | Requirements and role specs; then UML and the backlog |

## 5. Working agreements

| Agreement | Detail |
|---|---|
| Language | Code, commands and file names in English; documents may be Chinese + English |
| Backend style | Idiomatic Kotlin — `val` over `var`, `when` over chains; Ktor plugins in small `configure*` functions |
| Frontend style | TypeScript `strict`; no `any` without a reason; small components that every member can explain |
| Data access | Only through `frontend/src/api/`; never a bare `fetch` in a page |
| Contracts | The API is documented on [API Reference](API-Reference); a page that needs a new field updates it |
| Testing | Backend: Kotest + Ktor test host. Frontend: `tsc --noEmit` and a production build |
| Commits | Small and descriptive; each tied to a story or backlog ID where possible |
| Hard limits | No `rm -rf`, no `sudo`, no force-push to shared branches without agreement |

## 6. What to fill in

Each member should add their own row, with the features they personally built and tested:

| Member | GitHub handle | Features owned | Tested how |
|---|---|---|---|
| _(fill in)_ | | | |
| _(fill in)_ | | | |
| _(fill in)_ | | | |
| _(fill in)_ | | | |

The course requires **one tested working feature per member**. Naming that feature, and the test
that proves it, is the single most useful thing this page can contain.
