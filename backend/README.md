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

## Code layout

- `src/campus/auth/` handles login, session lookup and role/club permissions.
- `src/campus/organiser/`, `teacher/` and `student/` own the routes and service functions for each role.
- `src/campus/activities/` holds shared rules for activity fields, capacity, cancellation, time changes and due reminders.
- `src/campus/db/` handles JDBC transactions. The schema and initial demo data are in `resources/`.
- `src/campus/http/` provides JSON responses and common request handling. `Application.kt` wires the modules together and starts the deadline worker.

The [API contract](../docs/api-contract.md) lists endpoints, JSON fields, error codes and a suggested integration flow.
