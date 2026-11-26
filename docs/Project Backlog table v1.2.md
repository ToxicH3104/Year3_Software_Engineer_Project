# Project backlog - Campus Club Activity App

Group 17    Version 1.2, 24 November 2026

## How to read this backlog

Type: F - functional, N - non-functional.

Priority: 1 - must have (named in the specification, or the core flow cannot work safely without it), 2 - should have, 3 - could have. Items we will not build are listed at the end.

Estimate: XS = 1 point, S = 2, M = 3, L = 5, XL = 8. Estimates include testing.

Roles: Student, Organiser (a student with the organiser role in a club), Teacher (club management teacher), System (runs automatically), All.

Notify: the channel, in-app message or email, is still to be decided.

## Scope

| Priority | Items | Points |
|---|---|---|
| 1 - must have | 35 | 91 |
| 2 - should have | 16 | 41 |
| 3 - could have | 3 | 5 |
| Total | 54 | 137 |

## Time rules

| Before the start | Rule | Items |
|---|---|---|
| 48 hours (earlier of old and new start) | Last moment to change the start time | B30, B34 |
| 24 hours (new start) | Reserved places that are not confirmed are released | B32, B35 |
| 12 hours | Registration closes and a submission not reviewed is returned; for a new activity both happen earlier if the registration deadline is earlier | B17, B45, B46 |
| Until the start | Students can cancel; organisers can change the venue | B21, B41 |

## Backlog

| ID | Role | Description | Acceptance criteria | Type | Priority | Estimate |
|---|---|---|---|---|---|---|
| B1 | Student | Browse published public activities, with or without signing in | • Only published public activities are listed<br>• An empty list shows a message | F | 1 | M |
| B2 | Student | Search and filter activities | • Search by title and filter by date<br>• No match shows a no-results message | F | 1 | S |
| B3 | Student | View the details of an activity | • Shows introduction, start time, location, requirements, places left and registration deadline | F | 1 | S |
| B4 | All | Sign in with a university ID through the university identity service | • Students, organisers and teachers sign in the same way; a simulated identity service stands in for the real one<br>• A wrong ID or password is refused with a message<br>• The user can sign out | F | 1 | M |
| B5 | Organiser | Manage only the activities of my own club | • Opening or changing another club's activity is refused | F | 1 | S |
| B6 | Teacher | Make a student an organiser of a club | • A reason is required<br>• A fourth organiser for the same club is refused<br>• The student keeps the same account and can still register for activities | F | 2 | M |
| B7 | Organiser | Limit an activity to club members | • Non-members cannot see the activity or register for it<br>• Activities are public unless the organiser chooses club-only | F | 2 | M |
| B8 | Organiser | Create an activity with the required details | • Title, introduction, location, start time, participant requirements, capacity and registration deadline<br>• Can be saved as a draft | F | 1 | L |
| B9 | Organiser | See which activity details are missing or wrong | • Required fields cannot be empty<br>• Capacity is a positive whole number<br>• The start and the registration deadline are in the future, and the deadline is before the start<br>• Each problem is shown next to its field | F | 1 | S |
| B10 | Organiser | Submit a new activity for review | • Status becomes "awaiting review"<br>• It is not visible to students until approved<br>• Refused if its review deadline (B45) has already passed | F | 1 | S |
| B11 | Teacher | View submissions waiting for review, with full details | • All teachers share one review list of new activities and content changes, covering every club<br>• For a content change, the old and new values are shown together | F | 1 | M |
| B12 | Teacher | Approve a submission, or return it with a reason | • Approving a new activity publishes it<br>• Returning without a reason is refused<br>• The organiser can read the reason | F | 1 | M |
| B13 | Organiser | Revise a returned activity and submit it again | • The earlier reason stays visible while editing<br>• The new version goes back to the review list | F | 2 | S |
| B14 | Organiser | Get a registration QR code once the activity is approved | • No QR code exists before approval<br>• The organiser can view and download it<br>• The code keeps working after an approved change | F | 1 | M |
| B15 | Student | Register by scanning the QR code or using the Register button | • Both open the same activity page<br>• The student confirms and then sees a success message<br>• An unknown code shows an error message | F | 1 | M |
| B16 | Student | Be asked to sign in before confirming a registration | • After signing in the student returns to the same activity | F | 1 | S |
| B17 | System | Refuse registration when an activity is not open | • Refused when full, cancelled, past the registration deadline, or within 12 hours of the start<br>• Two students cannot both take the last place | F | 1 | M |
| B18 | Student | See my existing booking instead of registering twice | • A second attempt shows the existing booking and uses no extra place | F | 1 | S |
| B19 | Student | View my bookings in Registered Activities | • Shows current activity details and booking status | F | 1 | M |
| B20 | Organiser | View the registration status of my activity | • Shows registered count and places left<br>• Lists the name and university ID of each registered student | F | 1 | M |
| B21 | Student | Cancel my registration at any time before the start | • Cancelling after the start is refused with a message | F | 2 | S |
| B22 | System | Release the place when a registration is cancelled | • Places left increases by one straight away | F | 2 | XS |
| B23 | Organiser | Cancel a published activity | • A reason is required<br>• New registrations stop and existing bookings are cancelled<br>• Any content change waiting for review is withdrawn | F | 1 | M |
| B24 | Student | Be notified when an activity I registered for is cancelled | • The message includes the reason | F | 1 | M |
| B25 | Student | See clearly that an activity is cancelled | • Shown as text on the activity page and in Registered Activities, not by colour alone | F | 1 | S |
| B26 | Organiser | Change the content of a published activity through review | • Changes to the title, introduction, participant requirements or visibility need teacher approval<br>• Only the changed content fields are saved, so approval never overwrites a newer time, venue, capacity or deadline | F | 2 | M |
| B27 | Student | See the approved content until a content change is approved | • A waiting or returned change does not alter what students see | F | 2 | S |
| B28 | Student | Keep my registration when the start time does not change | • A venue, capacity, deadline or content change leaves bookings confirmed | F | 1 | S |
| B29 | Student | Be notified when an activity I registered for changes | • Sent at once for a time or venue change, and after approval for a content change<br>• Says what changed | F | 1 | M |
| B30 | Organiser | Change the start time of a published activity without review | • Takes effect at once<br>• If the registration deadline is no longer before the new start, a new deadline is required<br>• Every active booking becomes "reserved" and must be confirmed (B35)<br>• A reserved place still counts towards capacity | F | 2 | L |
| B31 | Student | Not be penalised for an unconfirmed place | • The student can register again while registration is open | F | 3 | XS |
| B32 | System | Release reserved places not confirmed 24 hours before the new start | • The booking becomes cancelled and the place is free again | F | 2 | M |
| B33 | Student | Be notified when my unconfirmed place is released | • Sent once, when the place is released | F | 3 | S |
| B34 | System | Refuse a start time change that is too late | • Refused less than 48 hours before the old or the new start, whichever is earlier<br>• The organiser is told to cancel the activity instead | F | 2 | S |
| B35 | Student | Confirm or decline a reserved place after a time change | • Possible until 24 hours before the new start<br>• Confirming makes the booking confirmed<br>• Declining cancels it and releases the place | F | 2 | M |
| B36 | Student | Set a reminder for a registered activity | • The time must be in the future and before the start<br>• The reminder can be changed or removed<br>• If the activity moves to or before the reminder time, the reminder is removed and the student is told | F | 1 | M |
| B37 | System | Send the reminder at the chosen time | • Sent once<br>• Not sent if the booking or activity is cancelled | F | 1 | M |
| B38 | Organiser | Keep the member list of my club | • Members can be added and removed<br>• The list decides who can use club-only activities | F | 2 | M |
| B39 | Organiser | Extend the registration deadline without review | • Takes effect at once<br>• The deadline can only move later and stays before the start, except when B30 requires a new one | F | 1 | S |
| B40 | Organiser | Increase the participant capacity without review | • Takes effect at once; a full activity opens again<br>• Capacity can only go up | F | 1 | S |
| B41 | Organiser | Change the venue without review | • Possible until the start<br>• Students see the new venue at once | F | 1 | S |
| B42 | Organiser | Publish club news and announcements | • Title and text are required; an image is optional and stored locally<br>• Only for my own club<br>• Can be edited or removed | F | 1 | M |
| B43 | Student | Read club news and announcements | • Newest first, showing the club name and date | F | 1 | S |
| B44 | Teacher | Create a club | • An empty or duplicate name is refused | F | 3 | S |
| B45 | Teacher | See urgent submissions first in the review list | • New activity: review deadline is the registration deadline or 12 hours before the start, whichever is earlier<br>• Content change: review deadline is 12 hours before the start<br>• Worked out from the current times when the list is opened<br>• Within 48 hours of the review deadline counts as urgent, earliest deadline first<br>• Others follow in order of submission | F | 2 | M |
| B46 | System | Return a submission not reviewed before its review deadline | • The reason says nobody reviewed it in time, shown as "Returned by: System"<br>• A published activity keeps its approved details<br>• The organiser is notified | F | 2 | M |
| B47 | System | Allow only one submission in review for each activity | • A second submission is refused with a message<br>• Changes that need no review are still allowed | F | 2 | XS |
| B48 | All | The pages are accessible | • Meets WCAG 2.2 AA; axe DevTools reports no critical or serious issues<br>• All core tasks work with a keyboard only | N | 1 | M |
| B49 | All | The app works on a phone | • No sideways scrolling at a width of 360 px | N | 1 | S |
| B50 | System | Every protected action is checked on the server | • Not signed in or wrong role is refused even if the button is hidden<br>• Covered by automated tests | N | 1 | M |
| B51 | System | Passwords and sessions are protected | • Passwords are stored only as salted hashes<br>• Sessions expire | N | 1 | S |
| B52 | System | Data and time rules survive a restart | • No registration is lost<br>• Deadlines missed while the server was off are applied when it starts | N | 1 | S |
| B53 | All | The activity list stays fast | • Loads within 2 seconds with 100 clubs and 500 activities | N | 2 | S |
| B54 | Teacher | View changes that took effect without review | • Shown in a separate column, "Automatically approved changes", next to the review list<br>• Shows the activity, club, organiser, time of change, and old and new values, newest first<br>• View only | F | 1 | M |

## Will not have in this release

- Live connection to the university identity server (the specification accepts a local login for the mini project; see B4)
- Waiting list for full activities
- Attendance check-in at the venue
- Payments and paid tickets

## Specification coverage

| Ref | Specification statement | Backlog items |
|---|---|---|
| R1 | Clubs publish news and announcements | B42, B43 |
| R2 | Activity posted with introduction, location, requirements, attendance limit and registration deadline | B8, B9 |
| R3 | Registration QR code generated | B14 |
| R4 | Students search and browse activities | B1, B2, B3 |
| R5 | Students scan the QR code to sign up | B15, B16, B17, B18 |
| R6 | Organisers view registration status | B20 |
| R7 | Organisers extend the deadline, increase capacity or change the location | B28, B39, B40, B41 |
| R8 | Organisers cancel an activity | B23, B25 |
| R9 | Registered Activities list | B19 |
| R10 | Students set reminders | B36, B37 |
| R11 | Teacher reviews before publication | B10, B11, B12, B54 |
| R12 | Unified identity authentication | B4 |
