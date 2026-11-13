# Field Incident Reporter

Field technicians photograph and report physical incidents from a native Android app that works offline and syncs with a local mock API.

## Quick start

Open the `android/` folder in Android Studio, not the repo root.

_More setup, seeded accounts and troubleshooting to follow._

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
