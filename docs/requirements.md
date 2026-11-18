# Requirements — Campus Club Activity Release and Registration App

Single source of truth for frontend work. Distilled from the course brief, Group 15 user stories (US01–US15), the project backlog (B1–B37), the Week 8 wireframe/accessibility materials and the course Ktor conventions.
Items marked **[PROPOSED]** are our suggestions (routes, names), not agreed design. Items marked **[OPEN]** need a team decision.

## 1. Context
- Southwest Jiaotong University, 100+ student clubs. Clubs publish activities; students search and register; every activity is reviewed by a university club-management teacher **before** publication.
- After approval an **online registration QR code** is generated; scanning it opens the activity's registration page.
- Registered students see bookings in **"Registered Activities"** and can set reminders.
- University SSO may be simulated. Local storage is allowed; **our team uses SQLite**.
- Images/text may be reused from university pages, but images must be downloaded and stored locally (no hotlinking).
- Deliverable: one tested working feature per member + GitHub repo + wiki. Time: until week 11, ~6 h/week each. Scope flexes; MVP = backlog priority 1.

## 2. Roles and personas
| Role | Persona | Main device | Needs |
|---|---|---|---|
| Student participant | Yiming Chen (first-year) | **Phone** | Clear booking result, current details, reminders |
| Club organiser | Jiaxin Liu (guitar club); Zihan Huang (ACM undersecretary) | Desktop (dorm PC) and phone | Complete posts, review feedback, accurate participant list incl. **name + university ID** |
| Teacher reviewer | Qian Zhou | Desktop | One queue, full info, decision tied to the reviewed version |
| Account administrator | Wei Zhang | Desktop | Find the right account, change roles, suspend/restore with reason |

## 3. Status workflows
**Activity version:** `Draft → Awaiting review → Approved/Published | Returned for changes | Rejected`. Returned → edit → resubmit as a new version.
**Published activity states:** `Active | Cancelled` (and registration `Open | Full | Closed`).
**Edit of a published activity** = new review item; the old approved version stays live until approval.
**Registration:** `Confirmed | Cancelled` (and, after an approved time change, `Reserved – awaiting confirmation`).
**Account:** `Active | Suspended`.

## 4. Business rules that affect the UI
- Only teacher-approved activities appear to students. Unapproved ones never show.
- Registration refused with an explanatory message when: full, cancelled, closed, or deadline passed. Registration also **closes 12 h before start** (B17).
- No duplicate bookings; repeating/scanning again shows the existing booking. Last place cannot be given to two students.
- Student may cancel a registration **≥ 24 h before start**; the place is released (B21–22).
- Organiser may cancel an activity (confirm activity name + reason); registration closes immediately, no review needed; registered students notified (email) and see the reason; QR code then shows a cancellation message (B23–25).
- Each club has a member list and **at most 3 organisers**; club-only activities limited to members; organisers manage only their own club (B5–B7).
- Capacity must be a positive whole number; registration deadline before activity start; required fields validated (B9).
- Edits to a published activity (venue, later deadline, larger capacity, time) go to teacher review (B26–27). Unchanged time keeps registrations (B28). **Approved time change:** places reserved pending student confirm/decline; unconfirmed reservations auto-cancel 24 h before the new start; a time change cannot be approved < 24 h before the new start (B30–35).
- Reminder time must be in the future and before the activity starts; turning it off or cancelling the activity stops it (B36–37, priority 2).
- Every review decision stores reviewer, time, comments and reviewed version. Role changes and suspend/restore are logged (admin, target, time, reason, old/new).

## 5. Pages and routes **[PROPOSED]**
**Architecture change:** the frontend is a React + TypeScript SPA (Vite, Tailwind, React Router), separate from the Ktor JSON API. In the tables below: **Route** = browser path handled by React Router; the **Template** column now names the page component (e.g. `activities/list.peb` → `pages/activities/ActivityList.tsx`); every `POST` / "redirect" row becomes a call to a matching `/api/...` endpoint followed by client-side navigation or a flash message. The exact endpoints live in `docs/api-contract.md` (to be written; derive it from the three role specs). Shared `Layout` component: skip link, header nav by role, `<main>`, footer, flash messages. **Detailed per-role specs, written directly from the team backlog, take precedence over this file where they differ:** `docs/student-spec.md`, `docs/organiser-spec.md`, `docs/teacher-spec.md`. (The administrator pages in this section are not in the backlog and are out of scope.)

### Public / shared
| Route | Template | Purpose | Story |
|---|---|---|---|
| `GET /` | `home.peb` | Landing; featured/upcoming activities; link to Activities and Sign in | B1 |
| `GET /activities` | `activities/list.peb` | Browse + keyword search + date filter; no-results message with "clear filters" | US01, B1–2 |
| `GET /activities/{id}` | `activities/detail.peb` | Description, date/time, venue, requirements, remaining places, deadline, status; Register button | US01, B3, B15 |
| `GET /register/{token}` | `registration/confirm.peb` | QR target: shows activity + confirm action; sign-in first if needed; cancelled/closed message | US02, B14–16 |
| `POST /register/{token}` | redirect → result | Create booking (idempotent) | US02, B17–18 |
| `GET/POST /login`, `POST /logout` | `auth/login.peb` | Sign in (simulated SSO), return to the page the user wanted | B4 |
| `GET /403`, 404 | `errors/*.peb` | Clear, friendly error pages | — |

### Student
| Route | Template | Purpose | Story |
|---|---|---|---|
| `GET /my/registrations` | `student/registrations.peb` | "Registered Activities": own bookings only, upcoming first, status, change/cancel notices, empty state | US03, B19 |
| `POST /my/registrations/{id}/cancel` | redirect | Cancel ≥24 h before start (confirm step) | B21–22 |
| `POST /my/registrations/{id}/confirm` / `decline` | redirect | Respond to reserved place after time change | B35 |
| `GET/POST /my/registrations/{id}/reminder` | `student/reminder.peb` | Set / change / switch off reminder | US04, B36 |

### Organiser
| Route | Template | Purpose | Story |
|---|---|---|---|
| `GET /organiser/activities` | `organiser/list.peb` | "My Activities" with status badges (text + icon, not colour only) | US05–06 |
| `GET/POST /organiser/activities/new` | `organiser/form.peb` | Create/save draft/submit; per-field errors, values retained | US05, B8–10 |
| `GET/POST /organiser/activities/{id}/edit` | `organiser/form.peb` | Edit draft or returned post; propose changes to published one | US06, US08, B13, B26 |
| `GET /organiser/activities/{id}` | `organiser/detail.peb` | Status, teacher feedback, QR code (after approval), count/capacity, participant list (name + university ID) | US06–07, B14, B20 |
| `POST /organiser/activities/{id}/cancel` | `organiser/cancel.peb` | Confirm name + reason | US09, B23 |

### Teacher
| Route | Template | Purpose | Story |
|---|---|---|---|
| `GET /teacher/reviews` | `teacher/queue.peb` | Pending queue (club, title, submitted time, deadline); sort oldest / nearest deadline | US10, B11 |
| `GET /teacher/reviews/{id}` | `teacher/review.peb` | Full details; for edits/resubmissions show changed fields with old vs proposed values and previous feedback | US11–12 |
| `POST /teacher/reviews/{id}/decision` | redirect | Approve / Request changes / Reject (reason required for last two) | US11, B12 |
| `GET /teacher/history` | `teacher/history.peb` | Past decisions | US10, US12 |

### Administrator
| Route | Template | Purpose | Story |
|---|---|---|---|
| `GET /admin/accounts` | `admin/accounts.peb` | Search by exact university ID or name; filter by role/club/status; no-match message | US13 |
| `GET /admin/accounts/{id}` | `admin/account.peb` | Name, ID, roles, clubs, status | US13 |
| `POST /admin/accounts/{id}/roles` | redirect | Assign/remove organiser/reviewer per club with reason | US14 |
| `POST /admin/accounts/{id}/suspend` / `restore` | `admin/confirm.peb` | Confirm page showing name + ID + effect; reason required | US15 |

Access control: every protected route checks role **and** club ownership server-side (opening another club's management URL directly must fail, US07/US14); suspended accounts are blocked even with an existing session (US15).

## 6. Interaction details and acceptance criteria (frontend-relevant)
- **Browse/search (US01):** keyword search + date filter; cards/rows show title, date/time, venue, remaining places, deadline; "no results" message with a way to clear filters; readable on phone without horizontal scroll.
- **Register (US02):** QR → activity + confirm button; success page states clearly "You are registered"; full/closed/cancelled explains why; repeat shows existing booking.
- **Registered Activities (US03):** only my bookings; upcoming in date order; name, time, current venue, status; change/cancel notices visible; clear empty state.
- **Reminder (US04):** choose time; invalid time → explanation, correctable; can switch off.
- **Create/submit (US05):** save draft and reopen intact; on submit, errors shown **beside** each field, other values kept; after submit status "Awaiting review", no registration/QR yet.
- **Feedback (US06):** status visible (Draft / Awaiting review / Returned / Rejected / Published); teacher reason shown; resubmit as new version; QR appears only after approval.
- **Participants (US07):** list + total registered + capacity together; each participant once with university ID.
- **Revise published (US08):** venue, later deadline, larger capacity; pending edit shows approved details still in use; rejection keeps approved version and shows reason.
- **Cancel (US09):** confirm activity name + reason; students see cancellation + reason; QR shows cancellation message.
- **Queue/review (US10–12):** sortable queue; three decisions with required reason for two; changed fields highlighted, old vs proposed shown.
- **Admin (US13–15):** exact-ID search returns one account or no-match message; confirm dialog/page before role change, suspend, restore; results show university ID.
- **Notifications:** change notices and cancellation notices appear in Registered Activities (email sending is backend; B24, B29, B37 are priority 1–2 and may be simulated).

## 7. Accessibility and usability standard (WCAG 2.2 AA target; Week 8 lab)
Four principles: **perceivable, operable, understandable, robust**.
Per-screen checklist (course accessibility-checker card):
- One clear main `<h1>`; logical heading order.
- Every form field has a **visible `<label>`** linked by `for`/`id`; required fields indicated in text.
- Nothing conveyed **by colour alone** (status badges have text/icon).
- Buttons/links are big enough to tap (≥ 44×44 px target); real `<button>`/`<a>`, not clickable `<div>`.
- Page/DOM order matches visual order; landmarks (`<header> <nav> <main> <footer>`), skip-to-content link.
- Everything works by **keyboard** (Tab, Shift+Tab, Enter, Space) with a clearly visible focus style.
- Error messages say what is wrong and how to fix it, are tied to the field (`aria-describedby`), and summarised at the top (`role="alert"` / focus moved).
- Responsive, mobile-first: usable at 320 px width and at 400 % zoom **without horizontal scrolling**; `<meta name="viewport">`.
- Sufficient contrast (4.5:1 text); images have meaningful `alt` (decorative → `alt=""`); QR code has a text link alternative ("Registration link").
- Use semantic HTML first, ARIA only where needed; `lang` attribute on `<html>`; descriptive link text (no bare "click here").
- SPA specifics: on route change move focus to the new page's `<h1>` (or main) and update `document.title`; async results and errors are announced (`role="alert"` / `aria-live`); loading and disabled states are visible; browser Back/Forward and deep links (e.g. a scanned QR URL) must work. (The earlier "works without JavaScript" criterion was dropped when the team chose a React SPA.)
Verification: axe DevTools "scan full page" (target 0 automatic issues), manual keyboard-only pass, screen-reader pass, 400 % zoom + greyscale pass.

## 8. Frontend technical conventions
- React + TypeScript (`strict`) + Vite + Tailwind CSS in `frontend/`; React Router; shared `Layout`, and small reusable components for nav, flash message, form field (label + error), status badge, empty state.
- Tailwind theme holds the design tokens (colour, type scale, spacing, radius, focus ring); mobile-first; images in `frontend/public/img/` or `src/assets/` (local only, never hotlinked).
- Data access only through `frontend/src/api/`; mock implementation first, real `fetch('/api/...')` later (Vite proxy to Ktor in dev). Types for `Activity`, `Registration`, `User` mirror the SQLite tables.
- Forms: controlled components; validation errors shown beside each field with retained values; server errors mapped to clear messages (e.g. `FULL`, `CLOSED`, `ALREADY_REGISTERED`). CSRF handling with cookie sessions **[OPEN]**.
- Dates/times displayed in a consistent local format; always show remaining places and deadline.
- Course conventions: idiomatic Kotlin, Ktor plugins in small `configure*` functions, Exposed/JDBC for SQLite, Kotest + Ktor test host.

## 9. Out of scope / low priority
Chat, social features, competition rankings (appear only in an unrelated draft), real SSO, real email infrastructure (simulate), payments. Priority 2/3 items (search polish B2, resubmit B13, organiser counts B20, cancelled display B25, reminders B36–37) only after the MVP.

## 10. Open questions **[OPEN]**
1. Can anonymous visitors browse activities? Backlog B1 says yes; US01 says "signed-in student". Default: **browse public, sign in to register**.
2. Club-only activities: hidden from non-members or visible but not registrable?
3. Is "club announcement / news post" (spec, G17 stories) in scope in addition to activities?
4. Email: real sending or on-screen notice only?
5. Wireframes: Week 8 sheets in the repo are blank — are there team wireframes to follow?

## Appendix A — Backlog (F = functional; priority 1 must, 2 should; size XS–XL)
| ID | Item | P | Size |
|---|---|---|---|
| B1 | Browse published public activities without signing in | 1 | M |
| B2 | Search public activities; show no-results message | 2 | S |
| B3 | View description, time, location, requirements, places, deadline | 1 | M |
| B4 | Sign-in for students, club organisers and teachers | 1 | M |
| B5 | Only organisers manage their own club's activities | 1 | M |
| B6 | Club membership list; max three organisers per club | 1 | M |
| B7 | Club-only activities/registration restricted to members | 1 | M |
| B8 | Create public or club-only activities with required details | 1 | L |
| B9 | Validate required fields, positive capacity, valid date/time | 1 | M |
| B10 | Submit new activities for teacher review | 1 | M |
| B11 | Teachers view pending submissions + full details | 1 | M |
| B12 | Teachers approve or return with feedback | 1 | M |
| B13 | Organisers revise and resubmit returned activities | 2 | M |
| B14 | Generate registration QR code after approval | 1 | L |
| B15 | Register button and QR link to the same activity | 1 | M |
| B16 | Require sign-in before confirming registration | 1 | S |
| B17 | Check capacity; close registration 12 h before start | 1 | M |
| B18 | Prevent duplicate registrations; show existing booking | 1 | M |
| B19 | Registered Activities shows confirmed bookings + current details | 1 | M |
| B20 | Organisers see registration count and available places | 2 | S |
| B21 | Students cancel registration ≥ 24 h before start | 1 | M |
| B22 | Release place on cancellation | 1 | S |
| B23 | Organisers cancel activity; stop new registrations | 1 | M |
| B24 | Email registered students on cancellation | 1 | M |
| B25 | Show cancelled activities clearly | 2 | S |
| B26 | Published-activity changes go to teacher review as new item | 1 | L |
| B27 | Keep approved details until changes are approved | 1 | M |
| B28 | Keep registrations when time unchanged | 1 | M |
| B29 | Email registered students after approved change | 1 | M |
| B30 | Approved time change: reserve places pending student confirmation | 1 | L |
| B31 | Don't penalise unconfirmed time-change reservations | 1 | M |
| B32 | Cancel unconfirmed reservations 24 h before new start | 1 | L |
| B33 | Reuse cancellation process to release expired reservations | 1 | M |
| B34 | Don't approve time change < 24 h before new start | 1 | M |
| B35 | Students confirm or decline reserved place | 1 | M |
| B36 | Student sets email reminder | 2 | L |
| B37 | Send reminder email at chosen time | 2 | L |

## Appendix B — Source notes
Extracted once from the course `Project/` folder (spec, Group 15 stories, backlog, personas, Week 8 accessibility lab/role cards/wireframe sheets, planning slides) and `.cursor/rules/ktor-learning.mdc`. The Cursor rule "NO CLI execution" is intentionally **not** carried over. Wireframe sheets were blank templates; an unrelated draft story set (G17) mentions club announcements and competition rankings — treated as out of scope until the team confirms.
