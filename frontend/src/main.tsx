import { useEffect, useRef, useState, type ReactNode } from "react";
import { createRoot } from "react-dom/client";
import {
  BrowserRouter,
  Link,
  NavLink,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import {
  ArrowRight,
  BookOpen,
  GraduationCap,
  LogOut,
  Music2,
  UserRound,
} from "lucide-react";
import { api, ApiError, hasRole, type Role, type User } from "./api";
import {
  Back,
  Field,
  Heading,
  Load,
  Session,
  dash,
  useAction,
  useSession,
} from "./shared";
import Board from "./pages/Board";
import { ActivityDetail, BookingDetail, Registrations } from "./pages/Student";
import {
  ActivityForm,
  OrganiserDetail,
  OrganiserHome,
} from "./pages/Organiser";
import { TeacherHome, TeacherReview } from "./pages/Teacher";
import TeacherOrganisers, { CreateClub } from "./pages/TeacherOrganisers";
import "./style.css";

function Brand() {
  return (
    <Link className="brand" to="/activities" aria-label="Campus Clubs home">
      <span className="brand-mark">
        <BookOpen size={21} strokeWidth={1.8} />
      </span>
      Campus Clubs
    </Link>
  );
}
function AccountForm({ signup = false }: { signup?: boolean }) {
  const [id, setId] = useState(""),
    [password, setPw] = useState(""),
    [displayName, setDisplayName] = useState(""),
    [params] = useSearchParams(),
    act = useAction(),
    { setUser } = useSession(),
    nav = useNavigate();
  const enter = (universityId: string, pw: string) =>
    act.run(async () => {
      const u = await api<User>(signup ? "/signup" : "/login", "POST", {
        universityId,
        password: pw,
        ...(signup && { displayName: displayName.trim() }),
      });
      setUser(u);
      const next = params.get("next");
      const allowed =
        next &&
        /^\/(?![\/\\])/.test(next) &&
        !next.includes("\\") &&
        (hasRole(u, "student") || !/^\/(register|my)\//.test(next));
      nav(allowed ? next! : dash(u), { replace: true });
    });
  return (
    <div className="login-shell">
      <Back />
      <Heading title={signup ? "Create a student account" : "Sign in"} />
      <div className="panel">
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault();
            enter(id.trim(), password);
          }}
        >
          {act.feedback}
          {signup && (
            <Field label="Full name" error={act.fields.displayName}>
              {(k) => (
                <input
                  required
                  autoComplete="name"
                  id={k}
                  maxLength={100}
                  aria-invalid={!!act.fields.displayName}
                  aria-describedby={
                    act.fields.displayName ? k + "-error" : undefined
                  }
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                />
              )}
            </Field>
          )}
          <Field label="University ID" error={act.fields.universityId}>
            {(k) => (
              <input
                required
                autoComplete="username"
                id={k}
                maxLength={signup ? 64 : undefined}
                aria-invalid={!!act.fields.universityId}
                aria-describedby={
                  act.fields.universityId ? k + "-error" : undefined
                }
                value={id}
                onChange={(e) => setId(e.target.value)}
              />
            )}
          </Field>
          <Field label="Password" error={act.fields.password}>
            {(k) => (
              <>
                <input
                  required
                  autoComplete={signup ? "new-password" : "current-password"}
                  type="password"
                  id={k}
                  minLength={signup ? 8 : undefined}
                  maxLength={256}
                  aria-invalid={!!act.fields.password}
                  aria-describedby={
                    act.fields.password
                      ? k + "-error"
                      : signup
                        ? k + "-hint"
                        : undefined
                  }
                  value={password}
                  onChange={(e) => setPw(e.target.value)}
                />
                {signup && (
                  <small id={k + "-hint"}>Use at least 8 characters.</small>
                )}
              </>
            )}
          </Field>
          <button className="btn primary" disabled={act.busy}>
            {signup ? "Create account" : "Sign in"} <ArrowRight size={16} />
          </button>
        </form>
        <p className="account-switch">
          {signup ? "Already have an account? " : "New student? "}
          <Link
            to={{
              pathname: signup ? "/login" : "/signup",
              search: params.toString(),
            }}
          >
            {signup ? "Sign in" : "Sign up"}
          </Link>
        </p>
        {!signup && (
          <div className="demo">
            <h3>Try a demo account</h3>
            {(
              [
                ["S001", "Student", UserRound],
                ["O001", "Organiser", Music2],
                ["T001", "Teacher", GraduationCap],
              ] as const
            ).map(([number, label, Icon]) => (
              <button
                key={number}
                disabled={act.busy}
                onClick={() => enter(number, "demo-" + number)}
              >
                <Icon size={17} />
                {label}
                <ArrowRight size={15} />
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
function Gate({ role, children }: { role: Role; children: ReactNode }) {
  const { user } = useSession(),
    loc = useLocation();
  if (!user)
    return (
      <Navigate
        replace
        to={"/login?next=" + encodeURIComponent(loc.pathname + loc.search)}
      />
    );
  return hasRole(user, role) ? (
    children
  ) : (
    <>
      <Heading
        title="Access restricted"
        sub={"This page requires a " + role + " account."}
      />
      <Link className="btn" to={dash(user)}>
        My workspace
      </Link>
    </>
  );
}
function App() {
  const [user, setUser] = useState<User | null>(null),
    [ready, setReady] = useState(false),
    [err, setErr] = useState(""),
    act = useAction(),
    loc = useLocation(),
    nav = useNavigate(),
    main = useRef<HTMLElement>(null);
  useEffect(() => {
    api<User>("/me")
      .then(setUser)
      .catch((e) => {
        if (!(e instanceof ApiError) || e.status !== 401) setErr(e.message);
      })
      .finally(() => setReady(true));
  }, []);
  useEffect(() => {
    if (!ready) return;
    window.scrollTo(0, 0);
    const target = main.current;
    target?.focus({ preventScroll: true });
    const update = () => {
      document.title =
        (target?.querySelector("h1")?.textContent || "Campus activities") +
        " · Campus Clubs";
    };
    update();
    const observer = new MutationObserver(update);
    if (target)
      observer.observe(target, {
        childList: true,
        subtree: true,
        characterData: true,
      });
    return () => observer.disconnect();
  }, [loc.pathname, ready]);
  return (
    <Session.Provider value={{ user, setUser }}>
      <a className="skip" href="#content">
        Skip to content
      </a>
      <header className="topbar">
        <div className="navinner">
          <Brand />
          <nav aria-label="Main navigation">
            <NavLink to="/activities">Activity board</NavLink>
            {(!user || hasRole(user, "student")) && (
              <NavLink to="/my/registrations">My registrations</NavLink>
            )}
            {user && hasRole(user, "organiser") && (
              <NavLink to="/organiser/activities">My activities</NavLink>
            )}
            {user?.role === "teacher" && (
              <NavLink to="/teacher/reviews">Review queue</NavLink>
            )}
          </nav>
          <div className="account">
            {user ? (
              <>
                <span>{user.displayName}</span>
                <button
                  className="icon-btn"
                  aria-label="Sign out"
                  title="Sign out"
                  disabled={act.busy}
                  onClick={() =>
                    act.run(async () => {
                      await api("/logout", "POST");
                      setUser(null);
                      nav("/activities");
                    })
                  }
                >
                  <LogOut size={17} />
                </button>
              </>
            ) : (
              <Link className="btn loginbtn" to="/login">
                <UserRound size={15} />
                Sign in
              </Link>
            )}
          </div>
        </div>
      </header>
      <main ref={main} id="content" tabIndex={-1} className="container-main">
        {act.feedback}
        {!ready || err ? (
          <Load error={err} />
        ) : (
          <Routes key={user?.userId || "visitor"}>
            <Route path="/" element={<Navigate to="/activities" replace />} />
            <Route path="/activities" element={<Board />} />
            <Route path="/activities/:id" element={<ActivityDetail />} />
            <Route path="/login" element={<AccountForm key="login" />} />
            <Route
              path="/signup"
              element={<AccountForm key="signup" signup />}
            />
            <Route
              path="/register/:token"
              element={
                <Gate role="student">
                  <ActivityDetail register />
                </Gate>
              }
            />
            <Route
              path="/my/registrations"
              element={
                <Gate role="student">
                  <Registrations />
                </Gate>
              }
            />
            <Route
              path="/my/registrations/:id"
              element={
                <Gate role="student">
                  <BookingDetail />
                </Gate>
              }
            />
            <Route
              path="/organiser/activities"
              element={
                <Gate role="organiser">
                  <OrganiserHome />
                </Gate>
              }
            />
            <Route
              path="/organiser/activities/new"
              element={
                <Gate role="organiser">
                  <ActivityForm />
                </Gate>
              }
            />
            <Route
              path="/organiser/activities/:id"
              element={
                <Gate role="organiser">
                  <OrganiserDetail />
                </Gate>
              }
            />
            <Route
              path="/organiser/activities/:id/edit"
              element={
                <Gate role="organiser">
                  <ActivityForm />
                </Gate>
              }
            />
            <Route
              path="/teacher/clubs/new"
              element={
                <Gate role="teacher">
                  <CreateClub />
                </Gate>
              }
            />
            <Route
              path="/teacher/organisers"
              element={
                <Gate role="teacher">
                  <TeacherOrganisers />
                </Gate>
              }
            />
            <Route
              path="/teacher/reviews"
              element={
                <Gate role="teacher">
                  <TeacherHome />
                </Gate>
              }
            />
            <Route
              path="/teacher/history"
              element={
                <Gate role="teacher">
                  <TeacherHome history />
                </Gate>
              }
            />
            <Route
              path="/teacher/reviews/:id"
              element={
                <Gate role="teacher">
                  <TeacherReview />
                </Gate>
              }
            />
            <Route
              path="*"
              element={
                <>
                  <Heading title="Page not found" />
                  <Back />
                </>
              }
            />
          </Routes>
        )}
      </main>
      <footer className="footer">
        <div>
          <Brand />
          <span>University of Leeds</span>
        </div>
      </footer>
    </Session.Provider>
  );
}
createRoot(document.getElementById("root")!).render(
  <BrowserRouter>
    <App />
  </BrowserRouter>,
);
