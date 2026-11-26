import { Link, NavLink } from "react-router-dom";
import { Mail, MailOpen } from "lucide-react";
import { api, hasRole, useData, type Notice } from "../api";
import { Heading, Load, moment, useAction, useSession } from "../shared";

const notices = "/my/notices";
const pollMs = 15_000;
const titles: Record<string, string> = {
  activity_published: "New activity",
  cancelled: "Activity cancelled",
  time_changed: "Time changed",
  venue_changed: "Venue changed",
  activity_changed: "Activity updated",
  review_returned: "Review feedback",
  place_released: "Place released",
  reminder: "Activity reminder",
};

export function InboxLink({ revision }: { revision: number }) {
  const { data } = useData<{ unread: number }>(
    notices + "/unread",
    revision,
    pollMs,
    true,
  );
  const count = data?.unread ?? 0;
  return (
    <NavLink
      to="/inbox"
      className="btn inbox-link"
      title="Inbox"
      aria-label={count ? `Inbox, ${count} unread` : "Inbox"}
    >
      <Mail size={19} aria-hidden="true" />
      <span className="inbox-label">Inbox</span>
      {count > 0 && (
        <span className="unread-count" aria-hidden="true">
          {count > 99 ? "99+" : count}
        </span>
      )}
    </NavLink>
  );
}

export function Inbox({
  revision,
  onRead,
}: {
  revision: number;
  onRead: () => void;
}) {
  const { data, error } = useData<Notice[]>(notices, revision, pollMs, true);
  const { user } = useSession();
  const action = useAction();
  const unread = data?.filter((n) => !n.readAt).length ?? 0;
  function mark(path: string) {
    action.run(async () => {
      await api(notices + path, "PUT");
      onRead();
    }, "Marked as read.");
  }
  return (
    <div className="inbox-view">
      <Heading
        title="Inbox"
        sub={data?.length ? unread + " unread" : undefined}
        action={
          data &&
          data.length > 0 && (
            <button
              className="btn"
              aria-disabled={action.busy || unread === 0}
              onClick={() => {
                if (unread && !action.busy)
                  mark("/read-through/" + data[0].noticeId);
              }}
            >
              Mark all as read
            </button>
          )
        }
      />
      {action.feedback}
      {!data ? (
        <Load error={error} />
      ) : !data.length ? (
        <div className="empty">
          <MailOpen size={26} aria-hidden="true" />
          <h2>No messages yet</h2>
          <p>Messages about your activities will appear here.</p>
        </div>
      ) : (
        <ul className="inbox-list">
          {data.map((n) => {
            const review = n.type === "review_returned";
            const activityLink = review && user && hasRole(user, "organiser")
              ? "/organiser/activities/" + n.activityId
              : n.activityLink;
            const destination = activityLink ||
              (n.registrationId ? "/my/registrations/" + n.registrationId : null);
            return (
              <li
                key={n.noticeId}
                className={"panel inbox-message" + (n.readAt ? "" : " unread")}
              >
                <div className="message-meta">
                  <span>{titles[n.type] || "Activity update"}</span>
                  <time dateTime={n.createdAt}>{moment(n.createdAt)}</time>
                </div>
                <h2>{n.activityTitle || "Club activity"}</h2>
                <p>{n.message}</p>
                <div className="message-actions">
                  {destination && (
                    <Link className="textbtn" to={destination}>
                      {activityLink ? "View activity" : "View booking"}
                    </Link>
                  )}
                  <button
                    className="textbtn read-button"
                    aria-disabled={!!n.readAt || action.busy}
                    aria-label={n.readAt ? "Read" : "Unread. Mark as read"}
                    onClick={() => {
                      if (!n.readAt && !action.busy)
                        mark("/" + n.noticeId + "/read");
                    }}
                  >
                    {n.readAt ? <MailOpen size={16} /> : <Mail size={16} />}
                    {n.readAt ? "Read" : "Mark as read"}
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
