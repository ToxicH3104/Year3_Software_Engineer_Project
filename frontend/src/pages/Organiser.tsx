import { useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowRight, Plus, Download } from "lucide-react";
import {
  api,
  useData,
  type Activity,
  type Club,
  type Participant,
} from "../api";
import {
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
} from "../shared";

const home = "/organiser/activities";
export function OrganiserHome() {
  const { data, error } = useData<Activity[]>(home);
  return (
    <>
      <Heading
        title="My activities"
        sub="Manage your club’s activities, submissions and participants."
        action={
          <Link className="btn primary" to={home + "/new"}>
            <Plus size={16} />
            Create activity
          </Link>
        }
      />
      {!data ? (
        <Load error={error} />
      ) : !data.length ? (
        <Empty title="No activities yet">
          <p>Create a draft to get started.</p>
        </Empty>
      ) : (
        <div className="stack">
          {data.map((a) => (
            <Link
              to={home + "/" + a.activityId}
              key={a.activityId}
              className="row-link"
            >
              <div>
                <small>{a.clubName}</small>
                <h3>{a.title || "Untitled draft"}</h3>
                <p>
                  {moment(a.startsAt)} · {a.location || "Venue pending"}
                </p>
              </div>
              <Badge value={a.status} />
              <ArrowRight size={16} />
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
export function OrganiserDetail() {
  const { id } = useParams(),
    base = home + "/" + id,
    [rev, reload] = useState(0),
    { data: a, error } = useData<Activity>(base, rev),
    act = useAction(),
    [cancel, setCancel] = useState(false),
    [reason, setReason] = useState(""),
    [confirm, setConfirm] = useState("");
  if (!a) return <Load error={error} />;
  const mutable = ["draft", "returned"].includes(a.status),
    published = a.status === "published";
  return (
    <>
      <Back to={home}>My activities</Back>
      <Heading
        title={a.title || "Untitled draft"}
        sub={a.clubName}
        action={
          mutable || (published && !a.pendingEdit) ? (
            <Link to={base + "/edit"} className="btn">
              {published ? "Propose changes" : "Edit activity"}
            </Link>
          ) : undefined
        }
      />
      {act.feedback}
      {a.feedback && (
        <p className="notice">Teacher feedback · {a.feedback.reason}</p>
      )}
      {a.pendingEdit && (
        <p className="notice">
          Changes are awaiting review. The approved version is still public.
        </p>
      )}
      <div className="detail-grid">
        <article className="panel">
          <div className="between">
            <h2>Activity details</h2>
            <Badge value={a.status} />
          </div>
          <Facts a={a} />
          <h3>Introduction</h3>
          <p>{a.description || "—"}</p>
          <h3>Requirements</h3>
          <p>{a.requirements || "—"}</p>
          {a.cancelReason && <p className="notice bad">{a.cancelReason}</p>}
          {mutable && (
            <button
              className="btn primary"
              disabled={act.busy}
              onClick={() =>
                act.run(async () => {
                  await api(base + "/submit", "POST");
                  reload((x) => x + 1);
                }, "Submitted for review.")
              }
            >
              Submit for review
            </button>
          )}
          {Object.entries(act.fields).map(([k, v]) => (
            <p className="fielderr" key={k}>
              {k}: {v}
            </p>
          ))}
        </article>
        {a.qrToken && (
          <aside className="panel">
            <h2>Registration link</h2>
            <img
              className="qr"
              alt="Activity registration QR code"
              src={"/api" + base + "/qr.png"}
            />
            <a className="textbtn" href={"/register/" + a.qrToken}>
              Open registration page <ArrowRight size={15} />
            </a>
            <a
              className="textbtn"
              download="registration-qr.png"
              href={"/api" + base + "/qr.png"}
            >
              <Download size={15} />
              Download QR code
            </a>
          </aside>
        )}
      </div>
      {(published || a.status === "cancelled") && (
        <Participants base={base} activity={a} />
      )}
      {published && (
        <section className="section">
          <button className="textbtn danger" onClick={() => setCancel(!cancel)}>
            {cancel ? "Keep activity" : "Cancel activity"}
          </button>
          {cancel && (
            <form
              className="panel form narrow"
              onSubmit={(e) => {
                e.preventDefault();
                act.run(async () => {
                  await api(base + "/cancel", "POST", { reason });
                  setCancel(false);
                  reload((x) => x + 1);
                }, "Activity cancelled.");
              }}
            >
              <Field label={"Type the activity name: " + a.title}>
                {(k) => (
                  <input
                    id={k}
                    required
                    value={confirm}
                    onChange={(e) => setConfirm(e.target.value)}
                  />
                )}
              </Field>
              <Field label="Cancellation reason">
                {(k) => (
                  <textarea
                    id={k}
                    required
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                  />
                )}
              </Field>
              <button
                className="btn danger"
                disabled={act.busy || confirm !== a.title || !reason.trim()}
              >
                Confirm cancellation
              </button>
            </form>
          )}
        </section>
      )}
    </>
  );
}
function Participants({
  base,
  activity,
}: {
  base: string;
  activity: Activity;
}) {
  const { data, error } = useData<Participant[]>(base + "/participants");
  return (
    <section className="section">
      <h2>Participants</h2>
      <p className="muted">
        {activity.confirmedCount} confirmed · {activity.reservedCount} reserved
        · {activity.capacity} capacity
      </p>
      {!data ? (
        <Load error={error} />
      ) : !data.length ? (
        <p>No participants yet.</p>
      ) : (
        data.map((p) => (
          <div className="row-link compact" key={p.registrationId}>
            <div>
              {p.studentName} · {p.universityId}
            </div>
            <Badge value={p.status} />
          </div>
        ))
      )}
    </section>
  );
}

export function ActivityForm() {
  const { id } = useParams();
  const clubs = useData<Club[]>("/organiser/clubs");
  if (!clubs.data) return <Load error={clubs.error} />;
  return id ? (
    <EditLoader id={id} clubs={clubs.data} />
  ) : (
    <Editor clubs={clubs.data} />
  );
}
function EditLoader({ id, clubs }: { id: string; clubs: Club[] }) {
  const { data, error } = useData<Activity>(home + "/" + id);
  return data ? (
    <Editor key={id} clubs={clubs} a={data} />
  ) : (
    <Load error={error} />
  );
}
function Editor({ clubs, a }: { clubs: Club[]; a?: Activity }) {
  const nav = useNavigate(),
    act = useAction(),
    published = a?.status === "published";
  const [f, set] = useState<Record<string, string>>({
    clubId: a?.clubId || clubs[0]?.clubId || "",
    title: a?.title || "",
    description: a?.description || "",
    requirements: a?.requirements || "",
    location: a?.location || "",
    capacity: String(a?.capacity ?? ""),
    startsAt: localInput(a?.startsAt),
    registrationDeadline: localInput(a?.registrationDeadline),
    visibility: a?.visibility || "public",
  });
  const update = (k: string, v: string) => set((prev) => ({ ...prev, [k]: v }));
  function field(k: string, label: string, type = "text", readonly = false) {
    return (
      <Field label={label} error={act.fields[k]}>
        {(id) => {
          const props = {
            id,
            value: f[k],
            disabled: readonly,
            onChange: (
              e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
            ) => update(k, e.target.value),
            "aria-invalid": !!act.fields[k],
            "aria-describedby": act.fields[k] ? id + "-error" : undefined,
          };
          return type === "textarea" ? (
            <textarea {...props} />
          ) : (
            <input
              {...props}
              type={type}
              min={type === "number" ? 1 : undefined}
            />
          );
        }}
      </Field>
    );
  }
  function save(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const submit =
      ((e.nativeEvent as SubmitEvent).submitter as HTMLButtonElement)?.value ===
      "submit";
    act.run(async () => {
      const fields = {
        ...f,
        startsAt: fromInput(f.startsAt),
        registrationDeadline: fromInput(f.registrationDeadline),
        capacity: f.capacity === "" ? null : Number(f.capacity),
      };
      let item: Activity;
      if (!a) item = await api<Activity>(home, "POST", { ...fields, submit });
      else if (published)
        item = await api<Activity>(
          home + "/" + a.activityId + "/edits",
          "POST",
          {
            location: f.location,
            capacity: fields.capacity,
            startsAt: fields.startsAt,
            registrationDeadline: fields.registrationDeadline,
          },
        );
      else {
        item = await api<Activity>(home + "/" + a.activityId, "PUT", fields);
        if (submit) await api(home + "/" + a.activityId + "/submit", "POST");
      }
      nav(home + "/" + item.activityId);
    });
  }
  return (
    <>
      <Back to={home}>My activities</Back>
      <Heading
        title={
          a
            ? published
              ? "Propose changes"
              : "Edit activity"
            : "Create activity"
        }
        sub={
          published
            ? "Proposed changes take effect after teacher approval."
            : "Save a draft or send the activity to your teacher for review."
        }
      />
      <form className="panel form editor" onSubmit={save} noValidate>
        {act.feedback}
        <Field label="Club">
          {(id) => (
            <select
              id={id}
              value={f.clubId}
              disabled={!!a}
              onChange={(e) => update("clubId", e.target.value)}
            >
              {clubs.map((c) => (
                <option key={c.clubId} value={c.clubId}>
                  {c.clubName}
                </option>
              ))}
            </select>
          )}
        </Field>
        {field("title", "Activity title", "text", published)}
        {field("description", "Description", "textarea", published)}
        {field("location", "Venue")}
        <div className="form-grid">
          {field("startsAt", "Start time (Shanghai)", "datetime-local")}
          {field(
            "registrationDeadline",
            "Registration deadline (Shanghai)",
            "datetime-local",
          )}
          {field("capacity", "Capacity", "number")}
          <Field label="Visibility">
            {(id) => (
              <select
                id={id}
                disabled={published}
                value={f.visibility}
                onChange={(e) => update("visibility", e.target.value)}
              >
                <option value="public">Public</option>
                <option value="club_only">Club members only</option>
              </select>
            )}
          </Field>
        </div>
        {field(
          "requirements",
          "Participation requirements",
          "textarea",
          published,
        )}
        <div className="actions">
          {!published && (
            <button className="btn" disabled={act.busy} value="draft">
              Save draft
            </button>
          )}
          <button className="btn primary" disabled={act.busy} value="submit">
            {published ? "Submit changes" : "Submit for review"}{" "}
            <ArrowRight size={15} />
          </button>
        </div>
      </form>
    </>
  );
}
