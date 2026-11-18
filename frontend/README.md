# Campus Clubs frontend

React, TypeScript, React Router and Vite with local images and a locally hosted DM Sans font. React calls the Ktor API through a same-origin Vite proxy. The interface and sample content are in English to match the course materials. Activity times use Asia/Shanghai.

## Run locally

From the team repository, start Ktor in one terminal:

```sh
cd backend
CAMPUS_DB=campus-preview.db PUBLIC_BASE_URL=http://localhost:5173 ./kotlin run
```

In another terminal:

```sh
cd frontend
npm ci
npm run demo:seed
npm run dev
```

Open http://localhost:5173. The optional seed command creates five published examples and one pending review using the running API. It checks existing titles before adding fixtures. Use the dedicated preview database above for this demonstration. The default API target is http://127.0.0.1:8080; set CAMPUS_API_URL to change it.

On the sign-in screen, choose **Sign up** to create a student account. Enter your name, university ID and a password of at least eight characters. Signup signs you in and brings you back to the page you were trying to open. The system provisions teacher accounts. From **Review queue → Club organisers**, a club's teacher can look up a student by university ID and grant organiser access for that club. The system can also grant access. Refresh the student's page to see **My activities** alongside **My registrations**.

The sign-in screen also includes local demo accounts for a student (S001), a Guitar Club organiser (O001) and their teacher (T001). To try the demo from another club, enter its account manually. The [backend guide](../backend/README.md) lists all demo accounts.

## Try the workflow

1. Explore the activity board. Search by activity title, select a club, or choose a date.
2. Open an activity, sign in as a student and confirm registration. My registrations shows the booking, change notices, reminders and cancellation options.
3. Sign in as an organiser to create a draft and submit it for review. Sign in as a teacher to approve it or return it with feedback. An approved activity then appears on the board.
4. Open a published activity as an organiser to view participants, download its QR code, propose changes or cancel. Students with a reserved place after a time change can confirm or decline in My registrations.

Run `npm run build` for the TypeScript check and production bundle. For a deployed SPA, serve index.html for browser routes and forward /api to Ktor. Set the backend PUBLIC_BASE_URL to the frontend origin for cookies, form submissions and registration links.

## Local assets

The cover photographs are illustrative. Sources: [acoustic session](https://images.unsplash.com/photo-1510915361894-db8b60106cb1), [guitar](https://images.unsplash.com/photo-1525201548942-d8732f6617a0), [coding](https://images.unsplash.com/photo-1515879218367-8466d910aaa4), [recording studio](https://images.unsplash.com/photo-1598488035139-bdbb2231ce04), [team workshop](https://images.unsplash.com/photo-1522071820081-009f0129c71c). DM Sans is distributed under the license in public/fonts/LICENSE.txt.
