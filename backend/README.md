# CaliTrack Backend

Spring Boot 3 API for workout tracking with analytics and AI coaching powered by Google Gemini (free tier).

## Architecture

```text
src/main/java/com/calitrack/
├── config/         # Security, JWT, RestTemplate
├── controller/     # REST adapters (thin — no business logic)
├── dto/            # Request/response records
├── entity/         # JPA entities (normalised schema)
├── exception/      # GlobalExceptionHandler
├── projection/     # Analytics read models
├── repository/     # Spring Data JPA
└── service/        # Business logic (Repository/Service pattern)
```

- **Stateless** JWT auth (`SessionCreationPolicy.STATELESS`)
- **Versioned** endpoints: `/api/v1/**`
- **Secrets** via environment / `backend/.env` — never committed

## Database

Normalised tables: `workouts`, `exercises`, `sets` (3NF-style separation), plus
`coach_recommendations` (one per workout, enforced by a unique constraint) and
`coach_profiles` (goals and fitness level per user) for the async AI coach.

| Profile | Schema management |
|---------|-------------------|
| Local dev (default) | Hibernate `ddl-auto: update` |
| Docker / prod | Flyway migrations + `ddl-auto: validate` |

Migrations: `src/main/resources/db/migration/`

## API (`/api/v1`)

| Method | Path | Status |
|--------|------|--------|
| POST | `/workouts` | 201 |
| GET | `/workouts`, `/workouts/{id}` | 200 |
| PATCH | `/workouts/{id}/finish` | 200 |
| GET/POST | `/exercises` | 200 / 201 |
| DELETE | `/exercises/{id}` | 204 |
| POST | `/workouts/{id}/sets` | 201 |
| GET | `/coach/recommend` | 200 |
| GET | `/coach/recommendations/latest` | 200 |
| GET/PUT | `/coach/profile` | 200 |

Public: `GET /health`

## AI coach

- `GET /api/v1/coach/recommend` — analyses recent workouts via **Google Gemini** (OpenAI-compatible endpoint, free tier)
- Set `GEMINI_API_KEY` (free from [Google AI Studio](https://aistudio.google.com/apikey)) in environment or `backend/.env`; `GEMINI_MODEL` overrides the default `gemini-3.5-flash-lite`
- With `calitrack.coach.async-enabled` on, finishing a workout publishes a `WorkoutLogged` event
  to the `workout-logged` topic: JSON, keyed by user ID, sent only after the transaction commits.
  If Kafka is unreachable the request still succeeds and the failure is logged.
- The `coach-recommender` consumer reads those events, generates a recommendation from the user's
  last 5 workouts and `coach_profiles` entry (defaults if none), and saves it to
  `coach_recommendations`. Redelivered events are skipped before calling Gemini.
- `GET /api/v1/coach/recommendations/latest` returns the newest saved recommendation and a
  `pending` flag (true for up to 2 minutes after a finished workout that has no recommendation
  yet). `GET`/`PUT /api/v1/coach/profile` read and set the goals and fitness level it uses.
- Failures are retried 3 times with exponential backoff (1s, 2s, 4s), then sent to
  `workout-logged.DLT`. A missing or invalid Gemini key, Gemini's free-tier rate limit or daily quota, and malformed messages go straight to the DLT.

## Run locally

```bash
cp .env.example .env
mvn spring-boot:run
```

## Run with Docker

From repo root:

```bash
docker compose up --build
```

Uses profile `docker` (Flyway + Postgres and Kafka containers). With
`calitrack.coach.async-enabled` on, the backend creates the `workout-logged` and
`workout-logged.DLT` topics at startup.

## Tests

```bash
mvn test
```

- **Unit tests**: Mockito (`*ServiceTest`, `GlobalExceptionHandlerTest`)
- **Integration tests**: `@SpringBootTest` + Testcontainers PostgreSQL and Kafka (requires Docker).
  `SchemaMigrationIntegrationTest` runs the Flyway migrations with `ddl-auto: validate`, so an
  entity that drifts from the migrations fails the build.

## CI

GitHub Actions: `.github/workflows/ci.yml` runs `mvn test` and a Docker image build when
`backend/` changes, and the mobile typecheck when `mobile/` changes. A final `ci-passed` job
is the single required check for `main`; skipped jobs count as passed, failed ones fail it.

## Config files

| File | Use |
|------|-----|
| `application.yml` | Local defaults |
| `application-docker.yml` | Docker Compose |
| `application-prod.yml` | Production deploy |
| `application-test.yml` | Test profile |
| `.env.example` | Template for secrets |
