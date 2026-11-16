# Field Incident Reporter

Field technicians photograph and report physical incidents from a native Android app that works offline and syncs with a local mock API.

The app is called CairnFG Incident Report on the device.

## Quick start

You need Node 24 and a recent Android Studio.

1. Start the mock server and leave it running:
   ```sh
   cd server
   npm ci
   npm start
   ```
2. Open the `android/` folder in Android Studio (not the repo root), pick an emulator and press Run.
3. Log in as `tech1` with the password `password`.
4. Optional: start the operator dashboard in a second terminal (see below) to change reports from the office side and watch the app react.

Seeded accounts, all with the password `password`:

| Username | Role | Use |
|---|---|---|
| `tech1` | Technician | The main demo account |
| `tech2` | Technician | A second technician, for a second device |
| `operator` | Operator | Recorded as the operator on dashboard deletes; the app only lets technicians in |

**If the app never syncs on an emulator:** the app only syncs on a network Android has validated as having internet. An emulator with no internet access never validates, so turn the check off and reconnect its Wi-Fi:

```sh
adb shell settings put global captive_portal_mode 0
adb shell svc wifi disable && adb shell svc wifi enable
```

## What it does

- Report an incident with up to five photos (camera or gallery), location, safety details and the people involved. Drafts save automatically.
- Works offline: reports and edits are saved on the device first and sent when there's a connection, with retries.
- Live updates: the server sends change hints over server-sent events, so other people's changes arrive without a refresh.
- Conflicts are handled rather than overwritten: if the office changed or deleted a report you edited offline, it lands in Needs attention with a clear choice.
- Reports the operator has moved past Assigned are locked for editing.
- Delete with undo, and filters, scope (mine or team open) and sort that all run on the device.
- Phone and tablet layouts: list and detail side by side on wide screens, a readable column on tablets in portrait, and a compact layout for phones in landscape.

## Tests

From `android/`:

```sh
./gradlew testDebugUnitTest            # JVM tests
./gradlew connectedDebugAndroidTest    # Room, repository and ViewModel tests on an emulator
./gradlew ktlintCheck lintDebug
```

## Mock server

A barebones [json-server](https://github.com/typicode/json-server) 0.17 API in `server/`, with one middlewares file for change ordering, version checks, photo uploads and sync hints. It needs Node 24 (`.nvmrc`).

```sh
cd server
npm ci
npm start          # http://127.0.0.1:3000, creates db.json from the seed on first run
npm run reset      # back to the seed; connected apps resync from scratch
```

- **Emulator:** the app's default address, `10.0.2.2:3000`, reaches the server on your machine.
- **Physical device:** run `adb reverse tcp:3000 tcp:3000`.
- **Slow or failing network:** `LATENCY_MS=1500 npm start` delays every request, and `FAILURE_RATE=0.3 npm start` answers 30% of requests with a 503, so you can see the app's pending and retry states.

## Operator dashboard

A deliberately small Next.js app in `dashboard/` for acting as an operator while you use the app: change an incident's status or assignee, resolve it, or delete it, and watch the app react. It needs the mock server running.

```sh
cd dashboard
npm ci
npm run dev        # http://localhost:3001
```

Moving a report past Assigned locks it in the app, and the table refreshes on its own when the app syncs a change. Set `NEXT_PUBLIC_API_URL` if the server is not on `127.0.0.1:3000`.

## Repo layout

- `android/`: the Android app (Gradle project)
- `server/`: the json-server mock API
- `dashboard/`: the Next.js operator dashboard

## Android package layout

Under `com.jonesl7l.carinfg`:

- `core/domain`: models and pure business rules
- `core/data`: repositories and the session/settings stores
- `core/database`: Room entities, DAOs and the database
- `core/network`: Retrofit API, DTOs and the SSE client
- `core/sync`: push/pull engines, the sync worker and scheduler
- `core/di`: Hilt modules
- `core/ui`: theme, layout helpers, photo loading and permission handling
- `feature/auth`: sign-in
- `feature/incident/list`, `feature/incident/detail`, `feature/incident/form`: incident screens
- `feature/account`: the account sheet and logout
