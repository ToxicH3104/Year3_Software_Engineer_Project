import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { api, useData, type Review, type Fields } from "../api";
import {
  Back,
  Badge,
  Empty,
  Field,
  Heading,
  Load,
  moment,
  useAction,
} from "../shared";
const labels: Record<keyof Fields, string> = {
  title: "Title",
  description: "Description",
  startsAt: "Start time",
  location: "Venue",
  requirements: "Requirements",
  capacity: "Capacity",
  registrationDeadline: "Registration deadline",
  visibility: "Visibility",
};
function format(k: keyof Fields, v: Fields[keyof Fields]) {
  return k === "startsAt" || k === "registrationDeadline"
    ? moment(String(v))
    : String(v ?? "—");
}
export function TeacherHome({ history = false }: { history?: boolean }) {
  const [sort, setSort] = useState("submitted"),
    { data, error } = useData<Review[]>(
      history ? "/teacher/history" : "/teacher/reviews?sort=" + sort,
    );
  return (
    <>
      <Heading
        title={history ? "Review history" : "Review queue"}
        action={
          <Link className="btn" to="/teacher/organisers">
            Club organisers
          </Link>
        }
        sub={
          history
            ? "Decisions you have made for your clubs."
            : "Activity submissions from the clubs assigned to you."
        }
      />
      <div className="between tools">
        <div className="tabs">
          <Link className={!history ? "on" : ""} to="/teacher/reviews">
            Pending
          </Link>
          <Link className={history ? "on" : ""} to="/teacher/history">
            History
          </Link>
        </div>
        {!history && (
          <select
            aria-label="Sort reviews"
            value={sort}
            onChange={(e) => setSort(e.target.value)}
          >
            <option value="submitted">Oldest submission</option>
            <option value="deadline">Nearest deadline</option>
          </select>
        )}
      </div>
      {!data ? (
        <Load error={error} />
      ) : !data.length ? (
        <Empty title={history ? "No decisions yet" : "You’re all caught up"}>
          <p>
            {history
              ? "Completed reviews will appear here."
              : "New submissions will appear here."}
          </p>
        </Empty>
      ) : (
        <div className="stack">
          {data.map((r) => (
            <Link
              className="row-link"
              key={r.reviewId}
              to={"/teacher/reviews/" + r.reviewId}
            >
              <div>
                <small>
                  {r.clubName} ·{" "}
                  {r.kind === "edit" ? "Proposed edit" : "New activity"} · v
                  {r.version}
                </small>
                <h3>{r.title}</h3>
                <p>
                  Submitted {moment(r.submittedAt)} · Deadline{" "}
                  {moment(r.proposed.registrationDeadline)}
                </p>
              </div>
              <Badge value={r.status} />
              <ArrowRight size={16} />
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
export function TeacherReview() {
  const { id } = useParams(),
    [rev, setRev] = useState(0),
    [reason, setReason] = useState(""),
    act = useAction(),
    base = "/teacher/reviews/" + id,
    { data: r, error } = useData<Review>(base, rev);
  if (!r) return <Load error={error} />;
  function decide(decision: string) {
    act.run(async () => {
      await api(base + "/decision", "POST", { decision, reason });
      setRev((x) => x + 1);
    }, "Decision recorded.");
  }
  return (
    <>
      <Back to="/teacher/reviews">Review queue</Back>
      <Heading
        title={r.title}
        sub={
          r.clubName +
          " · Version " +
          r.version +
          " · Submitted by " +
          r.submittedBy
        }
      />
      {act.feedback}
      <div className="detail-grid">
        <article className="panel">
          <div className="between">
            <h2>Submission</h2>
            <Badge value={r.status} />
          </div>
          {r.previousFeedback && (
            <p className="notice">
              Previous feedback · {r.previousFeedback.reason}
            </p>
          )}
          <dl className="review-fields">
            {(Object.keys(labels) as (keyof Fields)[]).map((k) => (
              <div key={k}>
                <dt>{labels[k]}</dt>
                <dd>
                  {r.previous && r.changedFields.includes(k) && (
                    <>
                      <small>Previous</small>{" "}
                      <del>{format(k, r.previous[k])}</del>
                      <br />
                      <small>Proposed</small>{" "}
                    </>
                  )}
                  {format(k, r.proposed[k])}
                </dd>
              </div>
            ))}
          </dl>
        </article>
        <aside className="panel sticky">
          <h2>{r.status === "pending" ? "Review decision" : "Decision"}</h2>
          {r.status === "pending" ? (
            <>
              <Field label="Feedback (required to return)">
                {(k) => (
                  <textarea
                    id={k}
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                  />
                )}
              </Field>
              {r.blockReason && (
                <p className="notice bad">
                  The new start is less than 24 hours away.
                </p>
              )}
              <div className="actions">
                <button
                  disabled={!r.canApprove || act.busy}
                  className="btn primary"
                  onClick={() => decide("approve")}
                >
                  Approve
                </button>
                <button
                  className="btn"
                  disabled={!reason.trim() || act.busy}
                  onClick={() => decide("return")}
                >
                  Return for changes
                </button>
              </div>
            </>
          ) : (
            <>
              <Badge value={r.status} />
              <p>
                {r.decision?.reviewerName} · {moment(r.decision?.decidedAt)}
              </p>
              <p>{r.decision?.reason}</p>
            </>
          )}
        </aside>
      </div>
    </>
  );
}
