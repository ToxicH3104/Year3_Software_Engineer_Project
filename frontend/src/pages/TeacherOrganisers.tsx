import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api, useData, type Club, type User } from "../api";
import { Back, Empty, Field, Heading, Load, useAction } from "../shared";

type Account = Pick<User, "universityId" | "displayName">;

export function CreateClub() {
  const [name, setName] = useState("");
  const action = useAction();
  const navigate = useNavigate();
  return (
    <div className="narrow">
      <Back to="/teacher/organisers">Manage clubs</Back>
      <Heading
        title="Create a club"
        sub="You will be the teacher responsible for this club."
      />
      <form
        className="panel form"
        onSubmit={(e) => {
          e.preventDefault();
          action.run(async () => {
            const club = await api<Club>("/teacher/clubs", "POST", {
              clubName: name.trim(),
            });
            navigate(
              "/teacher/organisers?club=" + encodeURIComponent(club.clubId),
              { replace: true },
            );
          });
        }}
      >
        {action.feedback}
        <Field label="Club name">
          {(id) => (
            <input
              id={id}
              required
              maxLength={100}
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          )}
        </Field>
        <button className="btn primary" disabled={action.busy || !name.trim()}>
          Create club
        </button>
      </form>
    </div>
  );
}

function ClubAssignments({ club }: { club: Club }) {
  const base =
    "/teacher/clubs/" + encodeURIComponent(club.clubId) + "/organisers";
  const [version, setVersion] = useState(0);
  const { data: roster, error } = useData<Account[]>(base, version);
  const [id, setId] = useState("");
  const [candidate, setCandidate] = useState<Account | null>(null);
  const [reason, setReason] = useState("");
  const action = useAction();
  if (!roster) return <Load error={error} />;
  const alreadyAssigned = roster.some(
    (person) => person.universityId === candidate?.universityId,
  );
  return (
    <section className="panel stack">
      <div>
        <h2>{club.clubName} organisers</h2>
        <p className="muted">{roster.length} of 3 places assigned</p>
      </div>
      {roster.length > 0 && (
        <ul>
          {roster.map((person) => (
            <li key={person.universityId}>
              {person.displayName} · {person.universityId}
            </li>
          ))}
        </ul>
      )}
      {action.feedback}
      {roster.length < 3 && (
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault();
            if (candidate) {
              if (alreadyAssigned) return;
              action.run(
                async () => {
                  await api(base, "POST", {
                    universityId: candidate.universityId,
                    reason,
                  });
                  setCandidate(null);
                  setId("");
                  setReason("");
                  setVersion((v) => v + 1);
                },
                candidate.displayName +
                  " has organiser access for " +
                  club.clubName +
                  ".",
              );
            } else {
              action.run(async () => {
                setCandidate(
                  await api<Account>(
                    base +
                      "/candidate?universityId=" +
                      encodeURIComponent(id.trim()),
                  ),
                );
              });
            }
          }}
        >
          {candidate ? (
            <>
              <p>
                {candidate.displayName} · {candidate.universityId}
              </p>
              {alreadyAssigned ? (
                <p>This account already organises this club.</p>
              ) : (
                <>
                  <p>
                    This account will be able to manage activities and view
                    participants for {club.clubName}.
                  </p>
                  <Field label="Reason for assignment">
                    {(k) => (
                      <input
                        id={k}
                        required
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                      />
                    )}
                  </Field>
                </>
              )}
              <div className="actions">
                {!alreadyAssigned && (
                  <button
                    className="btn primary"
                    disabled={action.busy || !reason.trim()}
                  >
                    Grant organiser access
                  </button>
                )}
                <button
                  className="btn"
                  type="button"
                  disabled={action.busy}
                  onClick={() => {
                    setCandidate(null);
                    setReason("");
                  }}
                >
                  Change account
                </button>
              </div>
            </>
          ) : (
            <>
              <Field label="University ID">
                {(k) => (
                  <input
                    id={k}
                    required
                    value={id}
                    disabled={action.busy}
                    onChange={(e) => setId(e.target.value)}
                  />
                )}
              </Field>
              <button className="btn" disabled={action.busy || !id.trim()}>
                Find account
              </button>
            </>
          )}
        </form>
      )}
    </section>
  );
}

export default function TeacherOrganisers() {
  const { data: clubs, error } = useData<Club[]>("/teacher/clubs");
  const [params, setParams] = useSearchParams();
  const club =
    clubs?.find((c) => c.clubId === params.get("club")) ?? clubs?.[0];
  return (
    <>
      <Back to="/teacher/reviews">Review queue</Back>
      <Heading
        title="Manage clubs"
        action={
          <Link className="btn primary" to="/teacher/clubs/new">
            New club
          </Link>
        }
      />
      {!clubs ? (
        <Load error={error} />
      ) : !club ? (
        <Empty title="No clubs assigned" />
      ) : (
        <div className="editor stack">
          {clubs.length > 1 && (
            <Field label="Club">
              {(k) => (
                <select
                  id={k}
                  value={club.clubId}
                  onChange={(e) =>
                    setParams({ club: e.target.value }, { replace: true })
                  }
                >
                  {clubs.map((c) => (
                    <option key={c.clubId} value={c.clubId}>
                      {c.clubName}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          )}
          <ClubAssignments key={club.clubId} club={club} />
        </div>
      )}
    </>
  );
}
