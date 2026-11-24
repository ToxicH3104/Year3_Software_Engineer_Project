# Accessibility

**Target: WCAG 2.2 level AA.** Accessibility is a backlog item in its own right (B48, priority 1),
not a polish pass. Students use phones, often one-handed, sometimes with a screen reader.

The standard is applied per screen using the course accessibility-checker card, and verified three
ways: an automated scan, a keyboard-only pass, and a screen-reader pass.

## 1. The four principles

| Principle | What it means here |
|---|---|
| **Perceivable** | Text alternatives for images and the QR code; 4.5:1 contrast; nothing conveyed by colour alone |
| **Operable** | Everything reachable by keyboard, with a visible focus ring; targets ≥ 44×44 px |
| **Understandable** | Errors say what is wrong *and* how to fix it, next to the field |
| **Robust** | Semantic HTML first, ARIA only where HTML falls short |

## 2. Per-screen checklist

Every page is held to this list.

- [ ] One clear `<h1>`; heading order is logical, with no skipped levels.
- [ ] Every form field has a **visible `<label>`** linked by `for`/`id`. Required fields are marked in text, not by an asterisk alone.
- [ ] Nothing is conveyed **by colour alone** — status badges carry text and an icon.
- [ ] Buttons and links are at least 44×44 px and are real `<button>`/`<a>` elements, never a clickable `<div>`.
- [ ] DOM order matches visual order.
- [ ] Landmarks: `<header> <nav> <main> <footer>`, plus a skip-to-content link.
- [ ] Everything works with **Tab, Shift+Tab, Enter and Space**, with a clearly visible focus style.
- [ ] Error messages say what is wrong and how to fix it, are tied to the field with `aria-describedby`, and are summarised at the top with `role="alert"`.
- [ ] Responsive and mobile-first: usable at **320 px** width and at **400 % zoom** without horizontal scrolling; `<meta name="viewport">` is set.
- [ ] Text contrast ≥ 4.5:1; images have meaningful `alt` (decorative ones `alt=""`).
- [ ] The QR code has a **text link alternative** ("Registration link") — a code alone is not accessible.
- [ ] Semantic HTML first; `lang` on `<html>`; descriptive link text, never a bare "click here".

## 3. Single-page-app specifics

A SPA loses the browser's built-in page-change behaviour, so it has to be rebuilt:

| Requirement | How it is met |
|---|---|
| Move focus on route change | The layout focuses `<main>` after navigation, with `preventScroll` so the scroll reset still works |
| Update the page title | A `MutationObserver` sets `document.title` from the new page's `<h1>`, so a title is right even when content loads asynchronously |
| Announce async results | `role="alert"` on error and status messages |
| Loading and disabled states are visible | `useData` returns a distinct loading state; buttons disable while a request is in flight |
| Deep links and Back/Forward work | Real React Router routes, so a scanned QR URL is a real URL |

## 4. Where each rule is implemented

| Rule | Location |
|---|---|
| Skip link, landmarks, main focus, `document.title` | `frontend/src/main.tsx` |
| Field with visible label, error tied by `aria-describedby`, `aria-invalid` | `frontend/src/shared.tsx`, `main.tsx` |
| `role="alert"` for errors and flash messages | `frontend/src/shared.tsx` |
| Search, club and date filters with `aria-label` | `frontend/src/pages/Board.tsx` |
| Design tokens for colour, type, spacing, radius and focus ring | `frontend/src/style.css` (Tailwind theme) |

## 5. Known gaps

Stated plainly, because a checklist nobody verifies is worse than no checklist:

| Gap | Detail |
|---|---|
| **No `<dialog>` yet** | The cancellation confirmation and the organiser-grant flow use in-page sections rather than the native `<dialog>` element. Native `<dialog>` would give focus trapping and `Esc` for free. |
| **Not yet scanned** | `axe DevTools` has not been run across every page and recorded. B48 asks for zero critical or serious issues. |
| **Screen-reader pass not recorded** | The manual pass is planned, not documented. |
| **Contrast unverified** | The tokens are chosen for 4.5:1, but no measured audit is stored in the repository. |

## 6. How to verify

1. **Automated** — open each route, run axe DevTools "scan full page", and record any issue.
2. **Keyboard only** — unplug the mouse. Reach every action with Tab, Shift+Tab, Enter, Space. Confirm the focus ring is always visible and focus never disappears.
3. **Screen reader** — VoiceOver on macOS, or NVDA on Windows. Walk the register → confirm → my registrations path.
4. **Zoom and greyscale** — 400 % zoom and a greyscale filter at 320 px width. Check for horizontal scrolling and for anything that only made sense as colour.
