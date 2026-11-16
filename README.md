# Field Incident Reporter

Field technicians photograph and report physical incidents from a native Android app that works offline and syncs with a local mock API.

## Quick start

Open the `android/` folder in Android Studio, not the repo root.

_More setup, seeded accounts and troubleshooting to follow._

## Mock server

A barebones [json-server](https://github.com/typicode/json-server) 0.17 API in `server/`, with one middlewares file for change ordering, version checks, photo uploads and sync hints. It needs Node 24 (`.nvmrc`).

```sh
cd server
npm ci
npm start          # http://localhost:3000, creates db.json from the seed on first run
npm run reset      # back to the seed; connected apps resync from scratch
```

Seeded accounts: `tech1`, `tech2` and `operator`, all with the password `password`.

- **Emulator:** the app's default address, `10.0.2.2:3000`, reaches the server on your machine.
- **Physical device:** run `adb reverse tcp:3000 tcp:3000`.
- **Slow or failing network:** `LATENCY_MS=1500 npm start` delays every request, and `FAILURE_RATE=0.3 npm start` answers 30% of requests with a 503, so you can see the app's pending and retry states.

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
- `core/ui`: theme, shared components and permission handling
- `feature/auth`: sign-in
- `feature/incidents/list`, `feature/incidents/detail`, `feature/incidents/form`: incident screens
- `feature/account`: account and settings
