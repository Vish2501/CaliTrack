# CaliTrack

Full-stack fitness tracking app with a React Native mobile client and a versioned Spring Boot REST API.

## Overview

CaliTrack lets users create exercises, start workouts, log sets, review workout history, view basic training analytics, and request AI coaching recommendations from recent training data. The backend exposes JWT-protected REST endpoints under `/api/v1`, persists workout data in PostgreSQL, and documents the API with Swagger/OpenAPI.

## Stack

- React Native / Expo + TypeScript
- Spring Boot 3, Java 17, Spring Data JPA, Spring Security
- PostgreSQL, Flyway
- Supabase Auth (JWT)
- OpenAI API (AI coach)

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
- OpenAI coaching recommendations from recent workout history
- HTTP semantics: 201 Created, 204 No Content, consistent error responses
- Unit + integration tests (Testcontainers / PostgreSQL)

## Run locally

### Backend

```bash
cd backend
cp .env.example .env   # add OPENAI_API_KEY for AI coach
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

Requires Docker Desktop and `OPENAI_API_KEY` in your shell (optional, for AI coach):

```bash
export OPENAI_API_KEY=sk-...   # optional
docker compose up --build
```

- API: `http://localhost:8080`
- Postgres: `localhost:5432` (user/password/db: `calitrack`)

## Tests

```bash
cd backend
mvn test
```

Integration tests need Docker running (Testcontainers). Unit tests run without Docker.

## Configuration

| Variable | Purpose |
|----------|---------|
| `OPENAI_API_KEY` | AI coach (env or `backend/.env`) |
| Supabase JWT settings | `backend/src/main/resources/application.yml` |
| Database | Local Postgres or Docker Compose |

## API versioning

All resources live under **`/api/v1`** (e.g. `POST /api/v1/workouts`, `GET /api/v1/coach/recommend`).
