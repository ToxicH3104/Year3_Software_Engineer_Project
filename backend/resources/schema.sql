CREATE TABLE IF NOT EXISTS users(userId TEXT PRIMARY KEY, universityId TEXT NOT NULL UNIQUE, displayName TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('student','organiser','teacher')), accountStatus TEXT NOT NULL DEFAULT 'active');

CREATE TABLE IF NOT EXISTS clubs(clubId TEXT PRIMARY KEY, clubName TEXT NOT NULL);

CREATE TABLE IF NOT EXISTS club_roles(clubId TEXT REFERENCES clubs, userId TEXT REFERENCES users, relation TEXT CHECK(relation IN ('member','organiser','teacher')), PRIMARY KEY(clubId,userId,relation));

CREATE TRIGGER IF NOT EXISTS limit_organisers BEFORE INSERT ON club_roles
WHEN NEW.relation='organiser' AND NOT EXISTS(SELECT 1 FROM club_roles WHERE clubId=NEW.clubId AND userId=NEW.userId AND relation='organiser') AND (SELECT count(*) FROM club_roles WHERE clubId=NEW.clubId AND relation='organiser') >= 3
BEGIN SELECT RAISE(ABORT,'At most three organisers per club'); END;

CREATE TABLE IF NOT EXISTS sessions(token TEXT PRIMARY KEY, userId TEXT NOT NULL REFERENCES users, expiresAt TEXT NOT NULL);

CREATE TABLE IF NOT EXISTS organiser_grants(clubId TEXT NOT NULL REFERENCES clubs, userId TEXT NOT NULL REFERENCES users, grantedBy TEXT REFERENCES users, grantedAt TEXT NOT NULL, reason TEXT NOT NULL);

CREATE TABLE IF NOT EXISTS activities(activityId TEXT PRIMARY KEY, clubId TEXT NOT NULL REFERENCES clubs, status TEXT NOT NULL CHECK(status IN ('draft','awaiting_review','returned','published','cancelled')), fields TEXT NOT NULL, qrToken TEXT UNIQUE, cancelReason TEXT);

CREATE TABLE IF NOT EXISTS reviews(reviewId TEXT PRIMARY KEY, activityId TEXT NOT NULL REFERENCES activities, version INTEGER NOT NULL, kind TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending', proposed TEXT NOT NULL, baseline TEXT, previousReason TEXT, previousDecidedAt TEXT, submittedBy TEXT NOT NULL REFERENCES users, submittedAt TEXT NOT NULL, reviewerId TEXT REFERENCES users, decidedAt TEXT, reason TEXT, UNIQUE(activityId, version));

CREATE UNIQUE INDEX IF NOT EXISTS one_pending_review ON reviews(activityId) WHERE status='pending';

CREATE TABLE IF NOT EXISTS registrations(registrationId TEXT PRIMARY KEY, activityId TEXT NOT NULL REFERENCES activities, studentId TEXT NOT NULL REFERENCES users, status TEXT NOT NULL CHECK(status IN ('confirmed','reserved','cancelled')), registeredAt TEXT NOT NULL, reminderAt TEXT, reservationDeadline TEXT, UNIQUE(activityId,studentId));

CREATE INDEX IF NOT EXISTS student_bookings ON registrations(studentId);

CREATE TABLE IF NOT EXISTS notices(noticeId TEXT PRIMARY KEY, studentId TEXT NOT NULL REFERENCES users, activityId TEXT NOT NULL REFERENCES activities, type TEXT NOT NULL, message TEXT NOT NULL, createdAt TEXT NOT NULL);

CREATE TABLE IF NOT EXISTS demo_credentials(userId TEXT PRIMARY KEY REFERENCES users,salt TEXT NOT NULL,digest TEXT NOT NULL);
