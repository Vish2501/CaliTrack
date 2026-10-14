# CaliTrack Mobile

Mobile workout tracker built with Expo + React Native, inspired by Strong/Hevy workflows.

This app focuses on fast workout logging, exercise library management, workout history, and simple profile insights.

## Demo Scope (Portfolio)

Current implemented flow:

- Sign in or register with Supabase email/password auth
- Start a workout and manage an active workout state
- Add exercises and record sets (reps/weight/completed)
- Finish workouts and sync to backend
- Browse completed workout history and open workout details
- Manage personal exercise library (add + view)
- View profile insights (weekly workout frequency for the last 4 weeks)
- AI coach (from Profile): set your goal and fitness level, see the recommendation generated after
  your latest workout, or ask for one on demand
- Log out from Profile

Tab order in app:

1. Profile
2. History
3. Workouts
4. Exercises

## Tech Stack

- React Native 0.86
- Expo SDK 57
- TypeScript
- React Navigation (bottom tabs + native stack)
- Supabase Auth (`@supabase/supabase-js`)
- NativeWind (Tailwind-style RN styling)
- `@gorhom/bottom-sheet` for workout interaction UI

## Architecture Notes

- Frontend: Expo React Native app (this repository)
- Auth: Supabase session-based authentication
- API: Mobile app calls a backend API (`src/lib/api.ts`)
- Backend base URL: the deployed API by default; set `EXPO_PUBLIC_API_URL` to use another

Primary API areas used:

- `workouts` (start, list, details, finish, set logging)
- `exercises` (list/create, plus initial seed behavior)
- `analytics/workout-frequency` (profile insights)
- `coach` (latest recommendation, on-demand recommendation, coach profile)

## Local Setup

### Prerequisites

- Node.js 20+
- npm
- Expo (via `npx expo`)
- Expo Go on a phone (it must match the project's SDK, 57), or an iOS Simulator / Android Emulator
- Backend API running locally

### Install

```bash
npm install
```

### Run

```bash
npx expo start
```

Useful commands:

```bash
npx expo start --ios
npx expo start --android
npx tsc --noEmit
```

## Configuration

### API Base URL

Configured in [`src/lib/api.ts`](src/lib/api.ts). It defaults to the deployed API; to use a local
backend, set `EXPO_PUBLIC_API_URL` when starting Expo:

```bash
EXPO_PUBLIC_API_URL=http://<your-LAN-IP>:8080 npx expo start
```

On a physical phone, use your machine's LAN IP rather than `localhost`.

### Supabase

Supabase client is configured in [`src/lib/supabase.ts`](src/lib/supabase.ts).

For production hardening, move keys/URLs into environment-based config and avoid hardcoding.

## Project Structure

```text
src/
├── components/   # Reusable UI (active workout modal)
├── lib/          # API + Supabase client
├── screens/      # Login, Profile, History, Workouts, Exercises, Details
├── theme/        # Color system
└── types/        # Shared TS types (navigation + workout domain)
```

## Roadmap

- Previous set values while logging (last time's weight and reps as suggestions, like Strong/Hevy)
- Saved routines to replace the placeholder templates on the Workouts tab
- Rest timer after completing a set, and a summary when a workout finishes
- Persistent workout drafts across app restarts
- Test coverage for core data flows

## Screenshots

### Login
![Login screen](assets/screenshots/login.png)

### Workouts
![Workouts screen](assets/screenshots/workouts.png)

### History
![History screen](assets/screenshots/history.png)

### Workout Details
![Workout details screen](assets/screenshots/workout_details.png)

### Exercises
![Exercises screen](assets/screenshots/exercise.png)

### Profile
![Profile screen](assets/screenshots/profile.png)
