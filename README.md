# CaliTrack

Full-stack fitness tracking app with a React Native mobile client and a versioned Spring Boot REST API.

## CV summary (accurate to this repo)

> **CaliTrack** — Full-stack workout tracker: Spring Boot 3 REST API (`/api/v1`, Repository/Service layer, stateless JWT via Supabase, normalised PostgreSQL schema) with React Native/Expo client; OpenAI-powered coaching recommendations (GPT-3.5); JUnit/Mockito unit tests and Testcontainers integration tests; Dockerised backend with Flyway migrations and GitHub Actions CI.

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

Point the app at your API (`mobile/src/lib/api.ts` defaults to `http://localhost:8080`).

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
