// Opt-in local fixtures. All activities go through the normal submission and approval API.
const base = process.env.CAMPUS_API_URL || "http://127.0.0.1:8080";
if (!["localhost", "127.0.0.1", "[::1]"].includes(new URL(base).hostname))
  throw Error("Use a local demo server.");
async function request(path, method = "GET", cookie, body) {
  const r = await fetch(base + "/api" + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(cookie ? { cookie } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!r.ok) throw Error(path + " " + r.status + " " + (await r.text()));
  return path === "/login"
    ? r.headers.get("set-cookie").split(";")[0]
    : r.json();
}
const login = (id) =>
  request("/login", "POST", null, { universityId: id, password: "demo-" + id });
const sessions = {};
for (const id of ["O001", "O002", "T001", "T002"])
  sessions[id] = await login(id);
const events = [
  [
    "guitar",
    "Acoustic courtyard session",
    "Courtyard, Student Centre",
    4,
    36,
    "An evening of unplugged sets and open-mic performances. Bring a friend, find a seat, and hear student musicians play in the courtyard.",
  ],
  [
    "acm",
    "Algorithm practice lab",
    "Computing Lab 204",
    6,
    24,
    "Work through a set of contest problems in pairs, then compare solutions with the ACM Club.",
  ],
  [
    "guitar",
    "Inside the recording studio",
    "Music Studio 02",
    8,
    16,
    "Try microphone placement, record a short take and listen back with the Guitar Club.",
  ],
  [
    "acm",
    "Team programming night",
    "Innovation Hub",
    10,
    32,
    "Form a team and solve a short practice contest together. Students of all experience levels are welcome.",
  ],
  [
    "guitar",
    "Fingerstyle fundamentals",
    "Arts Building, Room 3",
    13,
    18,
    "Explore fingerpicking patterns in a small, hands-on guitar workshop.",
  ],
];
for (const [club, title, location, days, capacity, description] of events) {
  const owner = sessions[club === "guitar" ? "O001" : "O002"],
    teacher = sessions[club === "guitar" ? "T001" : "T002"];
  const list = await request("/organiser/activities", "GET", owner);
  let item = list.find((x) => x.title === title);
  if (item && item.status !== "awaiting_review") continue;
  const start = new Date();
  start.setUTCDate(start.getUTCDate() + days);
  start.setUTCHours(10, 30, 0, 0);
  item ??= await request("/organiser/activities", "POST", owner, {
    title,
    clubId: club,
    location,
    description,
    startsAt: start.toISOString(),
    capacity,
    registrationDeadline: new Date(+start - 86400000).toISOString(),
    requirements:
      club === "acm" ? "Bring a laptop." : "All experience levels welcome.",
    visibility: "public",
    submit: true,
  });
  const review = (await request("/teacher/reviews", "GET", teacher)).find(
    (r) => r.activityId === item.activityId,
  );
  if (review)
    await request(
      "/teacher/reviews/" + review.reviewId + "/decision",
      "POST",
      teacher,
      { decision: "approve" },
    );
}
console.log("Demo board is ready.");

const queueTitle = "Songwriting circle";
const drafts = await request("/organiser/activities", "GET", sessions.O001);
if (!drafts.some((a) => a.title === queueTitle)) {
  const start = new Date(Date.now() + 15 * 86400000);
  await request("/organiser/activities", "POST", sessions.O001, {
    clubId: "guitar",
    title: queueTitle,
    description:
      "Share a short original piece and get feedback from other student musicians.",
    location: "Arts Building, Room 4",
    requirements: "Bring a song idea.",
    startsAt: start.toISOString(),
    registrationDeadline: new Date(+start - 86400000).toISOString(),
    capacity: 20,
    visibility: "public",
    submit: true,
  });
}
