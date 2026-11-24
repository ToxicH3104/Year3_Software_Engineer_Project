# Setup and Run

Two processes in development: Ktor on port 8080 and Vite on port 5173. Vite proxies `/api` to
Ktor, so everything is one origin and there is no CORS configuration to get wrong.

## 1. Requirements

| Tool | Notes |
|---|---|
| **JDK 21** | The Amper toolchain downloads its own JDK if needed |
| **Node.js + npm** | For the frontend |
| Internet on first run | The Kotlin toolchain and libraries are downloaded once |

There is no Gradle file to edit: `backend/kotlin` (or `kotlin.bat`) is the bundled toolchain
wrapper, and `backend/module.yaml` declares the dependencies and the main class.

## 2. Run the backend

```sh
cd backend
CAMPUS_DB=campus-preview.db PUBLIC_BASE_URL=http://localhost:5173 ./kotlin run
```

| Variable | Default | Meaning |
|---|---|---|
| `CAMPUS_DB` | `campus.db` | SQLite file path, relative to the working directory |
| `HOST` | `127.0.0.1` | Bind address |
| `PORT` | `8080` | Port |
| `PUBLIC_BASE_URL` | `http://localhost:5173` | The **frontend** origin. Used for the cookie/origin check and for the QR link |

Check it is alive:

```sh
curl -s http://127.0.0.1:8080/api/activities
```

A fresh database returns `[]` until a teacher approves the first activity. That is correct, not a
fault.

> For a phone demo, set `PUBLIC_BASE_URL` to the address the phone can actually reach, otherwise the
> QR code will point at `localhost`.

## 3. Run the frontend

In a second terminal:

```sh
cd frontend
npm ci
npm run demo:seed     # optional: five published examples and one pending review
npm run dev
```

Open <http://localhost:5173>.

`npm run demo:seed` talks to the running API and checks existing titles before inserting, so it is
safe to run twice. It is opt-in; the app works without it, just with an empty board.

`CAMPUS_API_URL` changes the proxy target (default `http://127.0.0.1:8080`).

## 4. Sign in

Sign up on the sign-in page to create a student account: name, university ID, password of at least
8 characters. Signup signs you in and returns you to the page you were opening.

The seeded demo accounts all use the password `demo-` followed by the ID:

| Role | University ID | Club |
|---|---|---|
| Student | `S001` | Guitar (member) |
| Student | `S002` | ACM (member) |
| Student | `S003` | no membership |
| Organiser | `O001` | Guitar |
| Organiser | `O002` | ACM |
| Teacher | `T001` | Guitar |
| Teacher | `T002` | ACM |

So `O001` / `demo-O001` signs in as the guitar club organiser. Separate browser profiles can hold
different users at the same time, which is the easiest way to walk a full review cycle by yourself.

Teacher accounts are provisioned, not self-service. A teacher can promote a student from **Review
queue → Manage clubs** by looking them up by exact university ID and recording a reason. The same
can be done from the command line:

```sh
CAMPUS_DB=campus-preview.db ./kotlin run -- grant-organiser guitar S003 "Elected club secretary"
```

## 5. Try the whole workflow

1. **Browse** the board. Search by title, filter by club or date.
2. **Register** — sign in as `S001`, open an activity, confirm. My registrations shows the booking,
   notices, reminders and the cancellation option.
3. **Create** — sign in as `O001`, create a draft, submit it. It is now "awaiting review" and has no
   QR code.
4. **Review** — sign in as `T001`, open the queue, approve it. The activity appears on the board and
   gains a QR code.
5. **Manage** — back as `O001`, view participants, download the QR code, propose a change, or cancel.
6. **Time change** — move the start time. Students with a place see it become `reserved` and can
   confirm or decline in My registrations.

## 6. Tests

### Backend

```sh
cd backend
./kotlin test
```

Seven end-to-end scenarios run against a temporary SQLite file with a controllable clock:

| Scenario | Covers |
|---|---|
| Teachers create clubs, appoint organisers and review the new club's activities | B6, B11, B12, B44; role checks; duplicate club names |
| Club teachers grant organiser access without replacing a student's account | B6; idempotent grants; the three-organiser cap |
| Student signup starts a session, keeps IDs unique and grants only student access | B4, B51; case-insensitive IDs; role escalation refused |
| Unpublished versions stay private, decisions are scoped and linked to their snapshots | B5, B11, B27; another club's teacher is refused |
| The last place is atomic, repeats are idempotent, cancellation releases the place | B17, B18, B21, B22; concurrent booking |
| Time changes reserve places, enforce the approval window, and expire unconfirmed places | B30, B32, B34, B35 |
| Club access, reminder delivery, registration cutoff and sign out | B7, B36, B37, B17 |

### Frontend

```sh
cd frontend
npm run build     # tsc --noEmit, then vite build
```

The build is the type check; there is no separate lint step configured.

## 7. Common problems

| Symptom | Cause |
|---|---|
| The board is empty | No activity has been approved yet. Sign in as `T001` and approve one, or run `npm run demo:seed` |
| The QR code points at `localhost` | `PUBLIC_BASE_URL` is still the default. Set it to the address the phone can reach |
| `403 Request origin does not match the configured frontend` | `PUBLIC_BASE_URL` does not match the origin you are browsing from |
| Every write fails after a restart | The cookie is 8 hours old, or `CAMPUS_DB` changed and the session row is gone. Sign in again |
| The first build takes minutes | The toolchain and dependencies are downloading |
| Port 8080 or 5173 already in use | Stop the other process, or set `PORT` / edit `vite.config.ts` |

## 8. Database files

`campus.db` is the default, `campus-preview.db` is the demo one. Both are ignored by git. Deleting
one gives you a clean database: the schema is recreated on start-up and `seed.sql` re-inserts the
demo accounts and clubs.
