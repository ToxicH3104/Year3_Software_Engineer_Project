import {
  createContext,
  useContext,
  useId,
  useState,
  type ReactNode,
} from "react";
import { Link } from "react-router-dom";
import {
  ArrowLeft,
  Check,
  CircleAlert,
  Clock3,
  MapPin,
  Users,
  CalendarDays,
} from "lucide-react";
import { ApiError, hasRole, type Activity, type User } from "./api";
export const Session = createContext<{
  user: User | null;
  setUser: (u: User | null) => void;
}>({ user: null, setUser: () => {} });
export const useSession = () => useContext(Session);
export const dash = (user: User) =>
  hasRole(user, "organiser")
    ? "/organiser/activities"
    : hasRole(user, "teacher")
      ? "/teacher/reviews"
      : "/activities";
export const date = (
  s?: string,
  opts: Intl.DateTimeFormatOptions = {
    month: "short",
    day: "numeric",
    year: "numeric",
  },
) => {
  const value = s ? new Date(s) : null;
  return value && Number.isFinite(value.getTime())
    ? new Intl.DateTimeFormat("en-GB", {
        timeZone: "Asia/Shanghai",
        ...opts,
      }).format(value)
    : "—";
};
export const moment = (s?: string) =>
  date(s, {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
export const localInput = (s?: string | null) => {
  const ms = s ? Date.parse(s) : NaN;
  return Number.isFinite(ms)
    ? new Date(ms + 8 * 3600000).toISOString().slice(0, 16)
    : "";
};
export function isCalendarDay(day: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) return false;
  const value = new Date(day + "T12:00:00Z");
  return (
    Number.isFinite(value.getTime()) && value.toISOString().slice(0, 10) === day
  );
}
export function fromInput(value: string, field: string) {
  if (!value) return "";
  if (
    !isCalendarDay(value.slice(0, 10)) ||
    !/^.{10}T([01]\d|2[0-3]):[0-5]\d$/.test(value)
  ) {
    const message = "Enter a date as YYYY-MM-DD and a 24-hour time as HH:mm.";
    throw new ApiError(message, 422, { [field]: message });
  }
  return new Date(value + ":00+08:00").toISOString();
}
export const names: Record<string, string> = {
  open: "Open",
  full: "Full",
  closed: "Closed",
  published: "Published",
  draft: "Draft",
  awaiting_review: "Awaiting review",
  returned: "Needs changes",
  reserved: "Awaiting confirmation",
  confirmed: "Confirmed",
  cancelled: "Cancelled",
  pending: "Pending",
  urgent: "Urgent",
  approved: "Approved",
  withdrawn: "Withdrawn",
};
export function Badge({ value }: { value: string }) {
  return (
    <span className={"badge " + value}>
      <i />
      {names[value] || value}
    </span>
  );
}
export function Heading({
  title,
  sub,
  action,
}: {
  title: string;
  sub?: string;
  action?: ReactNode;
}) {
  return (
    <header className="heading">
      <div>
        <h1 tabIndex={-1}>{title}</h1>
        {sub && <p>{sub}</p>}
      </div>
      {action}
    </header>
  );
}
export function Back({
  to = "/activities",
  children = "Activity board",
}: {
  to?: string;
  children?: ReactNode;
}) {
  return (
    <Link className="back" to={to}>
      <ArrowLeft size={15} />
      {children}
    </Link>
  );
}
export function Empty({
  title,
  children,
}: {
  title: string;
  children?: ReactNode;
}) {
  return (
    <div className="empty">
      <CalendarDays size={25} />
      <h3>{title}</h3>
      {children}
    </div>
  );
}
export function Load({ error }: { error?: string }) {
  return error ? (
    <p className="notice bad" role="alert">
      <CircleAlert size={16} />
      {error} <button onClick={() => location.reload()}>Retry</button>
    </p>
  ) : (
    <p role="status" className="muted pad">
      Loading…
    </p>
  );
}
export function Facts({ a }: { a: Activity }) {
  return (
    <dl className="facts">
      {[
        [CalendarDays, "Starts", moment(a.startsAt)],
        [MapPin, "Venue", a.location],
        [
          Users,
          "Places",
          a.remaining + " of " + (a.capacity ?? "—") + " available",
        ],
        [
          Clock3,
          "Register by",
          moment(a.registrationClosesAt ?? a.registrationDeadline),
        ],
      ].map(([I, n, v]) => {
        const Icon = I as typeof Clock3;
        return (
          <div key={String(n)}>
            <dt>
              <Icon size={15} />
              {String(n)}
            </dt>
            <dd>{String(v ?? "—")}</dd>
          </div>
        );
      })}
    </dl>
  );
}
export function useAction() {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [message, setMsg] = useState(""),
    [fields, setFields] = useState<Record<string, string>>({});
  async function run(fn: () => Promise<void>, success = "") {
    if (busy) return;
    setBusy(true);
    setError("");
    setMsg("");
    setFields({});
    try {
      await fn();
      setMsg(success);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Please try again.");
      if (e instanceof ApiError) setFields(e.fields);
    } finally {
      setBusy(false);
    }
  }
  const feedback = (
    <>
      {error && (
        <p role="alert" className="notice bad">
          <CircleAlert size={16} />
          {error}
        </p>
      )}
      {message && (
        <p role="status" className="notice">
          <Check size={16} />
          {message}
        </p>
      )}
    </>
  );
  return { busy, run, feedback, fields };
}
export function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: (id: string) => ReactNode;
}) {
  const id = useId();
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children(id)}
      {error && (
        <small className="fielderr" id={id + "-error"}>
          {error}
        </small>
      )}
    </div>
  );
}
export function Art({
  a,
  thumbnail = false,
}: {
  a: Activity;
  thumbnail?: boolean;
}) {
  const t = (a.title || "").toLowerCase();
  const file = t.includes("studio")
    ? "studio"
    : t.includes("team")
      ? "workshop"
      : t.includes("acoustic")
        ? thumbnail
          ? "guitar"
          : "session"
        : a.clubId === "acm"
          ? "coding"
          : a.clubId === "guitar"
            ? "guitar"
            : "workshop";
  return <img className="art" src={"/img/" + file + ".jpg"} alt="" />;
}
