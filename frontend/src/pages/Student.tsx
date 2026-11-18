import { useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { ArrowRight, Bell, CalendarCheck } from "lucide-react";
import { api, useData, type Activity, type Booking, type Notice } from "../api";
import {
  Art,
  Back,
  Badge,
  Empty,
  Facts,
  Field,
  Heading,
  Load,
  fromInput,
  localInput,
  moment,
  useAction,
  useSession,
} from "../shared";

export function ActivityDetail({ register = false }: { register?: boolean }) {
  const { id, token } = useParams(),
    { user } = useSession(),
    nav = useNavigate(),
    loc = useLocation(),
    act = useAction();
  const { data: a, error } = useData<Activity>(
    register ? "/register/" + token : "/activities/" + id,
  );
  if (!a) return <Load error={error} />;
  const mine =
    a.myRegistration && a.myRegistration.status !== "cancelled"
      ? a.myRegistration
      : null;
  return (
    <>
      <Back />
      <div className="detail-grid">
        <article>
          <span className="club-name">{a.clubName}</span>
          <Heading title={a.title || "Activity"} />
          <div className="detail-img">
            <Art a={a} />
          </div>
          <h2>About this activity</h2>
          <p className="prose">{a.description}</p>
          <h2>What to bring</h2>
          <p className="prose">{a.requirements}</p>
        </article>
        <aside className="panel sticky">
          <div className="between">
            <h2>Registration</h2>
            <Badge value={a.registrationState} />
          </div>
          <Facts a={a} />
          {a.cancelReason && <p className="notice bad">{a.cancelReason}</p>}
          {act.feedback}
          {mine ? (
            <Link
              className="btn primary"
              to={"/my/registrations/" + mine.registrationId}
            >
              View my booking <ArrowRight size={15} />
            </Link>
          ) : a.registrationState !== "open" ? (
            <p className="notice">Registration is {a.registrationState}.</p>
          ) : user && user.role !== "student" ? (
            <p className="muted">
              Registration is available to student accounts.
            </p>
          ) : register ? (
            <button
              disabled={act.busy}
              className="btn primary"
              onClick={() =>
                act.run(async () => {
                  const b = await api<Booking>("/register/" + token, "POST");
                  nav("/my/registrations/" + b.registrationId, {
                    state: { registered: true },
                    replace: true,
                  });
                })
              }
            >
              Confirm registration
            </button>
          ) : (
            <Link
              className="btn primary"
              to={
                user
                  ? a.registrationLink!
                  : "/login?next=" +
                    encodeURIComponent(a.registrationLink || loc.pathname)
              }
            >
              {" "}
              {user ? "Register" : "Sign in to register"}{" "}
              <ArrowRight size={15} />
            </Link>
          )}
        </aside>
      </div>
    </>
  );
}

export function Registrations() {
  const { data, error } = useData<Booking[]>("/my/registrations"),
    notices = useData<Notice[]>("/my/notices");
  return (
    <>
      <Heading
        title="My registrations"
        sub="Your bookings and updates from club organisers."
      />
      {!data ? (
        <Load error={error} />
      ) : !data.length ? (
        <Empty title="No bookings yet">
          <Link to="/activities">Browse the activity board</Link>
        </Empty>
      ) : (
        <div className="stack">
          {data.map((b) => (
            <Link
              className="row-link"
              key={b.registrationId}
              to={"/my/registrations/" + b.registrationId}
            >
              <div>
                <small>{b.activity.clubName}</small>
                <h3>{b.activity.title}</h3>
                <p>
                  {moment(b.activity.startsAt)} · {b.activity.location}
                </p>
              </div>
              <Badge value={b.status} />
              <ArrowRight size={16} />
            </Link>
          ))}
        </div>
      )}
      {notices.error && <Load error={notices.error} />}
      {Boolean(notices.data?.length) && (
        <section className="section">
          <h2>
            <Bell size={19} /> Notices
          </h2>
          {notices.data?.map((n) => (
            <div className="list-item" key={n.noticeId}>
              <strong>
                {
                  data?.find((x) => x.activity.activityId === n.activityId)
                    ?.activity.title
                }
              </strong>
              <p>{n.message}</p>
              <small>{moment(n.createdAt)}</small>
            </div>
          ))}
        </section>
      )}
    </>
  );
}
export function BookingDetail() {
  const { id } = useParams(),
    [revision, refresh] = useState(0),
    { data: b, error } = useData<Booking>("/my/registrations/" + id, revision),
    act = useAction(),
    loc = useLocation();
  const [remind, setRemind] = useState<string | null>(null),
    [cancel, setCancel] = useState(false);
  if (!b) return <Load error={error} />;
  const a = b.activity,
    base = "/my/registrations/" + id;
  const change = (path: string, method = "POST", body?: unknown) =>
    act.run(async () => {
      await api(base + path, method, body);
      setRemind(null);
      setCancel(false);
      refresh((x) => x + 1);
    }, "Booking updated.");
  return (
    <>
      <Back to="/my/registrations">My registrations</Back>
      <Heading title={a.title || "My booking"} sub={a.clubName} />
      {loc.state?.registered && b.status === "confirmed" && (
        <p className="notice">
          <CalendarCheck size={18} />
          You are registered.
        </p>
      )}
      {act.feedback}
      <div className="detail-grid">
        <div className="panel">
          <div className="between">
            <h2>Booking details</h2>
            <Badge value={b.status} />
          </div>
          <Facts a={a} />
          <p className="muted">
            Booking reference <code>{b.registrationId}</code>
          </p>
          {a.cancelReason && <p className="notice bad">{a.cancelReason}</p>}
          {b.status === "reserved" && (
            <>
              <p>
                Confirm your place by {moment(b.reservationDeadline)} after the
                time change.
              </p>
              <div className="actions">
                <button
                  disabled={act.busy}
                  className="btn primary"
                  onClick={() => change("/confirm")}
                >
                  Confirm place
                </button>
                <button
                  className="btn"
                  disabled={act.busy}
                  onClick={() => change("/decline")}
                >
                  Decline
                </button>
              </div>
            </>
          )}
          {b.status === "confirmed" &&
            (new Date(a.startsAt!).getTime() - Date.now() >= 86400000 ? (
              cancel ? (
                <div className="actions">
                  <span>Release your place?</span>
                  <button
                    className="btn danger"
                    disabled={act.busy}
                    onClick={() => change("/cancel")}
                  >
                    Yes, cancel booking
                  </button>
                  <button className="btn" onClick={() => setCancel(false)}>
                    Keep booking
                  </button>
                </div>
              ) : (
                <button
                  className="textbtn danger"
                  onClick={() => setCancel(true)}
                >
                  Cancel registration
                </button>
              )
            ) : (
              <p className="muted">
                The 24-hour cancellation window has closed.
              </p>
            ))}
        </div>
        {b.status === "confirmed" && (
          <aside className="panel">
            <h2>
              <Bell size={18} /> Reminder
            </h2>
            <p className="muted">
              {b.reminderAt
                ? "Scheduled for " + moment(b.reminderAt)
                : "Choose when to receive an activity notice."}
            </p>
            <Field label="Reminder time (Shanghai)">
              {(key) => (
                <input
                  type="datetime-local"
                  id={key}
                  value={remind ?? localInput(b.reminderAt)}
                  onChange={(e) => setRemind(e.target.value)}
                />
              )}
            </Field>
            <div className="actions">
              <button
                className="btn primary"
                disabled={act.busy || !(remind ?? b.reminderAt)}
                onClick={() =>
                  change("/reminder", "PUT", {
                    reminderAt: fromInput(remind ?? localInput(b.reminderAt)),
                  })
                }
              >
                Save reminder
              </button>
              {b.reminderAt && (
                <button
                  className="btn"
                  disabled={act.busy}
                  onClick={() =>
                    change("/reminder", "PUT", { reminderAt: null })
                  }
                >
                  Turn off
                </button>
              )}
            </div>
          </aside>
        )}
      </div>
    </>
  );
}
