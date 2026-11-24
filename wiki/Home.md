# Campus Club Activity App

A web app for Southwest Jiaotong University's 100+ student clubs. Clubs publish activities, a
university club-management teacher reviews every activity **before** it becomes public, and
students register by scanning a QR code.

西南交通大学 100+ 学生社团的活动发布与报名系统。社团发布活动 → 负责老师审核通过 → 生成报名二维码 → 学生扫码报名。

| | |
|---|---|
| **Course** | COMP2850 Software Engineering — Group 17 |
| **Stack** | Kotlin/Ktor + SQLite · React 19 + TypeScript + Vite + Tailwind |
| **Delivery** | Until week 11, ~6 h/week per member |
| **Scope** | MVP = backlog priority 1 (35 items, 91 points) |

## What it does

1. **Browse** — anyone can read published activities without signing in (B1–B3).
2. **Register** — scan the organiser's QR code, sign in, confirm. Registration closes 12 h before the start (B15–B18).
3. **Review** — every new activity and every content change waits in a teacher's queue. Nothing unapproved is ever visible to students (B10–B12).
4. **Manage** — organisers track places, participants and the QR code, and can cancel or change venue, capacity and time (B20–B41).

## Roles

| Role | Persona | Device | What they need |
|---|---|---|---|
| Student | Yiming Chen, first-year | **Phone** | A clear booking result, current details, reminders |
| Organiser | Jiaxin Liu (guitar club), Zihan Huang (ACM) | Desktop + phone | Complete posts, review feedback, participant names **and** university IDs |
| Teacher | Qian Zhou | Desktop | One queue, full information, each decision tied to the version reviewed |

> An organiser is a **student with an extra role**, not a separate account (B6). A club has at most
> three organisers.

## Wiki contents

| Page | Read it for |
|---|---|
| **[Project Overview](Project-Overview)** | Background, problem, scope, what is in and out |
| **[Architecture](Architecture)** | How the React SPA and the Ktor API fit together, code layout |
| **[Database Design](Database-Design)** | The eight SQLite tables and why they look like that |
| **[API Reference](API-Reference)** | Every `/api` endpoint, field and error code |
| **[User Stories and Backlog](User-Stories-and-Backlog)** | B1–B54, priorities, estimates, coverage |
| **[Design Decisions](Design-Decisions)** | The thirteen decisions behind the domain model |
| **[Workflows](Workflows)** | State machines and the three hard flows, step by step |
| **[Accessibility](Accessibility)** | The WCAG 2.2 AA checklist we hold every page to |
| **[Setup and Run](Setup-and-Run)** | Clone, run, seed, sign in, run the tests |
| **[Team and Contributions](Team-and-Contributions)** | Who did what |
| **[Revision Log](Revision-Log)** | How the design changed, and which conflicts are still open |

## Repository layout

```
club_app/
├── backend/            Ktor JSON API (Kotlin, JDK 21, bundled Amper toolchain)
│   ├── src/campus/     auth · student · organiser · teacher · activities · db · http
│   ├── resources/      schema.sql, seed.sql, logback.xml
│   ├── test/           Kotest + Ktor test host
│   └── module.yaml     dependencies, main class
├── frontend/           React SPA (TypeScript, Vite, Tailwind CSS 4)
│   └── src/
│       ├── api/        the only module that talks to the backend
│       └── pages/      Board · Student · Organiser · Teacher
└── docs/               requirements, per-role specs, UML, backlog
```

## Where to start

- **New to the project?** Read [Project Overview](Project-Overview), then [Architecture](Architecture).
- **Want to run it?** Go straight to [Setup and Run](Setup-and-Run) — demo accounts are listed there.
- **Writing code?** [API Reference](API-Reference) and [Database Design](Database-Design).
- **Reviewing the design?** [Design Decisions](Design-Decisions) and [Workflows](Workflows).
