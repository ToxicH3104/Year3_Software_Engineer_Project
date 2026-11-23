# Campus Club API

Kotlin/Ktor JSON API for the student, organiser and teacher workflows. Runs locally with Netty and SQLite. The project includes its Kotlin Toolchain wrapper.

## Run and test

From the team repository root:

```sh
cd backend
./kotlin run
```

Open http://localhost:8080/api/activities. A new database returns an empty JSON array until a teacher approves the first activity. Stop the server with Ctrl+C. Run `./kotlin test` in the same directory for the isolated API tests. On Windows use `kotlin.bat` in place of `./kotlin`. The first build needs internet access for the toolchain and libraries.

The default database is `campus.db` in the current working directory. Override its path with `CAMPUS_DB`. Netty defaults to `127.0.0.1:8080` (`HOST`, `PORT`). Set `PUBLIC_BASE_URL` to the **frontend origin**, by default `http://localhost:5173`. The QR code contains that origin. For a phone demo use the address reachable by the phone. Configure Vite to proxy `/api` to Ktor. Browser sessions use HttpOnly cookies with SameSite=Lax, an eight-hour lifetime and Secure on HTTPS. Browser write requests with an Origin must match the configured frontend.

## Local demonstration accounts

Students can create a local account through the sign-in page using their name, university ID and a password of at least eight characters. Signup starts a session immediately. University IDs are unique and case-insensitive for ASCII letters. New accounts start with the student role and no club memberships. The system provisions teacher accounts and assigns teachers to clubs. This coursework setup accepts a self-reported university ID.

| Role | University ID | Assigned club |
|---|---|---|
| Student | S001 | Guitar |
| Student | S002 | ACM |
| Student | S003 | No membership |
| Organiser | O001 | Guitar |
| Organiser | O002 | ACM |
| Teacher | T001 | Guitar |
| Teacher | T002 | ACM |

Each seeded demo account uses the password `demo-` followed by its university ID (e.g. `demo-O001`). These published credentials simulate campus login for the coursework demonstration. Roles and club assignments come from SQLite. Separate browser profiles can sign in as different users at the same time.

## Grant organiser access

Teachers can choose **Manage clubs → New club**, enter a name and create a club. The creator becomes its supervising teacher and can immediately assign an organiser.

All teachers share the activity review queue. A club's supervising teacher can grant access from **Review queue → Manage clubs**. The teacher looks up an existing account by exact university ID, checks the name and records a reason. Students keep their bookings and student access. A student with an open browser session can refresh to see **My activities**. Each club has up to three organisers, including system-provisioned organisers.

For a system grant, run the following from the backend directory with the same database path as the server. Substitute the club ID, the existing university ID and the reason.

```sh
CAMPUS_DB=campus-preview.db ./kotlin run -- grant-organiser guitar S003 "Elected club secretary"
```

Both paths record the club, target account, timestamp, reason and granting teacher (null for the system) in `organiser_grants`. Repeating a grant returns the existing assignment.

## Code layout

- `src/campus/auth/` handles login, session lookup and role/club permissions.
- `src/campus/organiser/`, `teacher/` and `student/` own the routes and service functions for each role.
- `src/campus/activities/` holds shared rules for activity fields, capacity, cancellation, time changes and due reminders.
- `src/campus/db/` handles JDBC transactions. The schema and initial demo data are in `resources/`.
- `src/campus/http/` provides JSON responses and common request handling. `Application.kt` wires the modules together and starts the deadline worker.

The [API contract](../docs/api-contract.md) lists endpoints, JSON fields, error codes and a suggested integration flow.
