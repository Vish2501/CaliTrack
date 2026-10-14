# CaliTrack Backend

Spring Boot 3 API for workout tracking with analytics and OpenAI-powered coaching.

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

Public: `GET /health`

## AI coach

- `GET /api/v1/coach/recommend` — analyses recent workouts via **OpenAI GPT-3.5**
- Set `OPENAI_API_KEY` in environment or `backend/.env`

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
