import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  ArrowRight,
  CalendarDays,
  ChevronDown,
  Clock3,
  MapPin,
  Search,
  Star,
  Ticket,
  X,
} from "lucide-react";
import { useData, type Activity } from "../api";
import { Art, Badge, date, Empty, Heading, Load } from "../shared";

export default function Board() {
  const { data, error } = useData<Activity[]>("/activities");
  const [params, setParams] = useSearchParams();
  const q = params.get("q") || "",
    club = params.get("club") || "",
    day = params.get("date") || "";
  const [showDate, setShowDate] = useState(Boolean(day));
  const set = (k: string, v: string) => {
    const next = new URLSearchParams(params);
    if (v) next.set(k, v);
    else next.delete(k);
    setParams(next, { replace: true });
  };
  const filtered = Boolean(q || club || day);
  const today = (s: string) =>
    date(s, { year: "numeric", month: "2-digit", day: "2-digit" })
      .split("/")
      .reverse()
      .join("-");
  const list = (data ?? []).filter(
    (a) =>
      (a.title || "").toLowerCase().includes(q.trim().toLowerCase()) &&
      (!club || a.clubId === club) &&
      (!day || (a.startsAt && today(a.startsAt) === day)),
  );
  const featured = !filtered
    ? (list.find((a) => a.registrationState === "open") ?? list[0])
    : undefined;
  const clubs = [
    ...new Map((data ?? []).map((a) => [a.clubId, a.clubName])).entries(),
  ];
  return (
    <>
      <Heading
        title="Activity board"
        sub="Discover what’s happening across your campus clubs."
      />
      {featured && (
        <section className="feature" aria-label="Featured activity">
          <div className="feature-copy">
            <span className="feature-label">
              <Star size={13} /> Featured activity
            </span>
            <h2>{featured.title}</h2>
            <div className="feature-meta">
              <span>
                <CalendarDays />
                {date(featured.startsAt, {
                  weekday: "short",
                  day: "numeric",
                  month: "short",
                })}
              </span>
              <span>
                <MapPin />
                {featured.location}
              </span>
            </div>
            <Link
              className="btn light"
              to={"/activities/" + featured.activityId}
            >
              View activity <ArrowRight size={16} />
            </Link>
          </div>
          <div className="feature-img">
            <Art a={featured} />
          </div>
        </section>
      )}
      <section className="board-section">
        <div className="section-top">
          <h2>
            {filtered ? "Search results" : "All activities"}{" "}
            <span className="count">{list.length}</span>
          </h2>
          <div className="filters">
            <label className="searchbox">
              <Search size={16} />
              <input
                aria-label="Search activities"
                placeholder="Search"
                value={q}
                onChange={(e) => set("q", e.target.value)}
              />
            </label>
            <label className="selectwrap">
              <select
                aria-label="Filter by club"
                value={club}
                onChange={(e) => set("club", e.target.value)}
              >
                <option value="">All clubs</option>
                {clubs.map(([id, name]) => (
                  <option key={id} value={id}>
                    {name}
                  </option>
                ))}
              </select>
              <ChevronDown size={14} />
            </label>
            <button
              className={"icon-btn " + (showDate ? "selected" : "")}
              aria-label="Filter by date"
              title="Filter by date"
              aria-expanded={showDate}
              onClick={() => setShowDate(!showDate)}
            >
              <CalendarDays size={17} />
            </button>
          </div>
        </div>
        {(showDate || filtered) && (
          <div className="filterline">
            {showDate && (
              <label>
                Activity date{" "}
                <input
                  type="date"
                  value={day}
                  onChange={(e) => set("date", e.target.value)}
                />
              </label>
            )}
            {filtered && (
              <button
                className="textbtn"
                onClick={() => {
                  setParams({});
                  setShowDate(false);
                }}
              >
                <X size={14} />
                Clear filters
              </button>
            )}
          </div>
        )}
        {!data ? (
          <Load error={error} />
        ) : list.length === 0 ? (
          <Empty
            title={
              filtered
                ? "No matching activities"
                : "No published activities yet"
            }
          >
            <p>
              {filtered
                ? "Try another search or clear the filters."
                : "Approved club activities will appear here."}
            </p>
          </Empty>
        ) : (
          <div className="cards">
            {list.map((a) => (
              <Link
                className="activity-card"
                key={a.activityId}
                to={"/activities/" + a.activityId}
              >
                <div className="card-cover">
                  <Art a={a} thumbnail />
                  <div className="date-tile">
                    <span>{date(a.startsAt, { month: "short" })}</span>
                    <strong>{date(a.startsAt, { day: "2-digit" })}</strong>
                  </div>
                </div>
                <div className="card-body">
                  <span className="club-name">{a.clubName}</span>
                  <h3>{a.title}</h3>
                  <div className="line-meta">
                    <MapPin />
                    {a.location} ·{" "}
                    {date(a.startsAt, {
                      hour: "2-digit",
                      minute: "2-digit",
                      hour12: false,
                    })}
                  </div>
                  <div className="line-meta deadline">
                    <Clock3 /> Register by{" "}
                    {date(a.registrationDeadline, {
                      month: "short",
                      day: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </div>
                  <div className="card-end">
                    <Badge value={a.registrationState} />
                    <span>
                      <Ticket size={14} />
                      {a.remaining} places left
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </>
  );
}
