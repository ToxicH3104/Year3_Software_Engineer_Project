# Campus Club Activity App (club_app)

COMP2850 group mini-project: clubs publish activities (after teacher review); students register via QR code. This directory is the only workspace. **Do not read the parent directory** (denied in settings); everything needed is in `docs/requirements.md`.

## 回复语言 / Language
- 始终使用「中文 + English」双语回复 (Simplified Chinese + English). Code, CLI commands and file names stay in English.
- Explain the idea first, then the steps.

## 技术栈与架构 / Stack and architecture
- **前后端分离 / Separated frontend and backend** (team decision, replaces the earlier server-rendered Pebble plan). Ktor serves **JSON under `/api/...`**; the browser app is a React SPA.
- Backend: **Ktor + SQLite** (team decision; not CSV/txt). Kotlin, JDK 21, bundled Amper toolchain (`module.yaml`, `./run`, `./kotlin`) — no hand-written Gradle. Use Exposed (or JDBC) for SQLite. Pebble is no longer used for pages.
- Frontend: **React + TypeScript + Vite + Tailwind CSS**, in `frontend/` (backend in `backend/`) **[PROPOSED layout]**. Routing with React Router. No UI framework dependency unless the team agrees; Tailwind is the styling system. Vite and npm build tools are now **allowed**.
- **Frontend-first workflow**: build the student-side UI against a mock data layer, then derive the backend from it. All data access goes through one module (e.g. `frontend/src/api/`) so swapping mock → real `fetch('/api/...')` touches only that module. Keep the API contract in `docs/api-contract.md` and update it whenever a page needs a new field or endpoint.
- Mock data must include edge cases: full, deadline passed, cancelled, already registered, empty list, failed request. Field names and status values must match the planned SQLite tables. Per-role specs (derived from the team backlog, **the source of truth for features**): `docs/student-spec.md`, `docs/organiser-spec.md`, `docs/teacher-spec.md`. Build the student side first; organiser and teacher sides stay thin (see each spec's §1 and §9).
- Dev: Vite dev-server **proxy** forwards `/api` to Ktor, so no CORS config is needed. Production: build the frontend and serve the static files from Ktor.
- Static assets: images are stored locally, never hotlinked. Note: Amper keeps `resources/` at the module root, not `src/main/resources/` — configure it explicitly if the Ktor-served frontend build needs a Maven-style path.
- Auth: Ktor `auth` + `sessions` (**cookie session**, not JWT); university SSO may be simulated with a `users` table. Passwords are stored as hashes. The server must check role and record ownership on every protected endpoint — hiding buttons in the UI is not access control.
- Tests: Kotest + Ktor test host (backend); Vitest + React Testing Library (frontend).
- Style: idiomatic Kotlin (`val`, `when`); install Ktor plugins in small `configure*` functions. TypeScript: `strict` mode, no `any` unless justified; keep components small and readable — every team member must be able to explain the code.

## 设计质量与标准合规 / Design quality and standards
Apply the project plugins `frontend-design` and `modern-web-guidance` whenever writing frontend pages:
- **frontend-design**: first define restrained **design tokens** (colour, type scale, spacing, radius, focus ring) in the Tailwind theme (`tailwind.config` / CSS variables); clear type hierarchy; generous, consistent whitespace; every list/table gets an **empty state**; every action gets visible feedback (success/error/loading/disabled). Mobile-first (students use phones).
- **modern-web-guidance**: meet **WCAG 2.2 AA**; prefer semantic landmarks (`header nav main footer`), native `<dialog>`, the Popover API, standard ARIA and proper keyboard/focus management; use real `<button>`/`<a>`, not clickable `<div>`. As a SPA, also manage focus and the page `<title>` on route changes and announce async results (`role="alert"` / live regions). The old "works without JS" rule no longer applies.
- Accessibility checklist and per-page criteria live in `docs/requirements.md` §7.

## 全自动工作流 / Autonomous workflow
- 所有需求以 `docs/requirements.md` 为准 (requirements source of truth). If unclear or missing, ask rather than invent.
- Edit, build, run and test without asking. Start the server in the background, then stop it when finished (`lsof`/`kill` are allowed for your own dev server only).
- 完成模块后主动验证：后端启动服务并用 `curl` 检查本地 `/api` 接口 (`curl` only against `localhost`/`127.0.0.1`)，运行 `./kotlin test`；前端运行 `npm run build`、`npm run lint`、`npm test`（均在 `frontend/`）。Make sure there are no errors. If `.claude/settings.json` blocks `npm`/`npx`, tell the user instead of working around it.
- Then make a **local `git commit`** (small, descriptive message) before reporting to the user, and say what was verified.
- Hard limits (denied): `rm -rf`, `sudo`, `git push`, `git remote`, `ssh`/`scp`, `env`/`printenv`, reading `.env*`, anything outside this directory, and editing `.claude/**`.
- Tie each change to a story ID (US01…) or backlog ID (B1…). MVP first (backlog priority 1); don't gold-plate priority 2/3. Budget ~6 h/week/person until week 11.
