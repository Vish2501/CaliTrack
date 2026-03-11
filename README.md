# CaliTrack Mobile

Expo / React Native mobile app for `CaliTrack`, a workout tracker inspired by Hevy and Strong.

## Stack

- React Native
- Expo
- TypeScript
- React Navigation
- Supabase Auth
- NativeWind

## Current Features

- Supabase email/password login
- Workout start flow connected to backend
- Active workout modal
- Add exercises to an active workout
- Local draft sets with reps / weight inputs
- Checkmark-based set completion flow
- Finish workout sync to backend

## App Structure

```text
src/
├── components/    # Reusable UI pieces
├── lib/           # API and Supabase helpers
├── screens/       # App screens
├── theme/         # Shared theme values
└── types/         # Shared TypeScript types
```

## Running Locally

### Prerequisites

- Node 20+
- npm
- Expo CLI via `npx expo`
- iOS Simulator or Expo Go
- Backend running locally on port `8080`

### Install

```bash
npm install
```

### Start the app

```bash
npx expo start
```

## Backend Dependency

The mobile app currently expects the backend API at:

```text
http://localhost:8080
```

If testing on a physical device, replace localhost with your machine's local IP.

## Auth

The app uses Supabase Auth for login and stores the session locally.

Required values live in the Supabase client config used by the app.

## Status

This project is in active development.

Current focus:

- refining workout UX
- template workflow
- workout draft persistence
- analytics integration
- stronger parity with Hevy / Strong flows
