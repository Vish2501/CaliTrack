# CaliTrack

[![CI](https://github.com/Vish2501/CaliTrack/actions/workflows/ci.yml/badge.svg)](https://github.com/Vish2501/CaliTrack/actions/workflows/ci.yml)

Full-stack fitness tracking app with a React Native mobile client and a versioned Spring Boot REST API.

## Overview

CaliTrack lets users create exercises, start workouts, log sets, review workout history, view basic training analytics, and request AI coaching recommendations from recent training data. The backend exposes JWT-protected REST endpoints under `/api/v1`, persists workout data in PostgreSQL, and documents the API with Swagger/OpenAPI.

## Stack

- React Native / Expo + TypeScript
- Spring Boot 3, Java 17, Spring Data JPA, Spring Security
- PostgreSQL, Flyway
- Supabase Auth (JWT)
- Google Gemini API, free tier (AI coach)

## Project structure

```text
CaliTrack/
├── backend/          # Spring Boot API
├── mobile/           # React Native / Expo app
├── docker-compose.yml
└── .github/workflows/
```

## Features

- Versioned REST API under `/api/v1`
- JWT-protected workouts, exercises, sets, analytics
- AI coaching recommendations (Gemini) from recent workout history
- HTTP semantics: 201 Created, 204 No Content, consistent error responses
- Unit + integration tests (Testcontainers / PostgreSQL)

## Run locally

### Backend

```bash
cd backend
cp .env.example .env   # add GEMINI_API_KEY for AI coach
mvn spring-boot:run
```

API: `http://localhost:8080` — health check: `GET /health`

### Mobile

```bash
cd mobile
npm install
npm start
```

Point the app at your API (`mobile/src/lib/api.ts` currently defaults to the deployed Render API URL).

## Run with Docker

Requires Docker Desktop. The AI coach uses demo replies by default (see below), so no key is needed:

```bash
docker compose up --build
```

- API: `http://localhost:8080`
- Postgres: `localhost:5432` (user/password/db: `calitrack`)
- Kafka: `localhost:9092` (single-node KRaft; the backend creates its topics on startup)

## Demo the AI coach locally (no API key needed)

Docker Compose runs the coach with a fake model by default: replies are canned, labelled
`[Demo reply, no AI call]`, and arrive after ~3s so the app's "generating" state is visible.
Everything else is real: the API, Kafka, the consumer and Postgres.

```bash
docker compose up --build -d
```

**Terminal walkthrough** (start, log sets, finish, see the Kafka event, poll the result):

```bash
export SUPABASE_PUBLISHABLE_KEY=... SUPABASE_EMAIL=... SUPABASE_PASSWORD=...
TOKEN=$(backend/scripts/get-token.sh) scripts/demo-coach.sh
```

**Mobile app against the local backend** (iOS Simulator; on a physical phone use your machine's
LAN IP instead of `localhost`):

```bash
cd mobile
EXPO_PUBLIC_API_URL=http://localhost:8080 npx expo start --ios
```

Finish a workout, open the Coach tab, and the recommendation appears after a few seconds.
To use real Gemini instead: `COACH_FAKE_MODEL=false GEMINI_API_KEY=... docker compose up -d`.

## Tests

```bash
cd backend
mvn test
```

Integration tests need Docker running (Testcontainers). Unit tests run without Docker.

## Configuration

| Variable | Purpose |
|----------|---------|
| `GEMINI_API_KEY` | AI coach; free key from [Google AI Studio](https://aistudio.google.com/apikey) (env or `backend/.env`) |
| `GEMINI_MODEL` | Optional; defaults to `gemini-3.5-flash-lite` |
| Supabase JWT settings | `backend/src/main/resources/application.yml` |
| Database | Local Postgres or Docker Compose |
| `COACH_ASYNC_ENABLED` | Kafka-backed AI coach; on in Docker Compose, off by default elsewhere |

## API versioning

All resources live under **`/api/v1`** (e.g. `POST /api/v1/workouts`, `GET /api/v1/coach/recommend`).
