# CaliTrack Backend

Spring Boot backend for `CaliTrack`, a workout tracking app inspired by Hevy and Strong.

## Stack

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Supabase Auth (JWT)
- Maven

## Current Features

- JWT-protected REST API
- Exercise CRUD
- Workout creation and retrieval
- Workout detail endpoint with sets
- Set create, update, delete
- Workout finish endpoint
- Analytics endpoints for volume, PRs, and workout frequency

## Architecture

Project structure:

```text
src/main/java/com/calitrack/
├── config/        # Security and app configuration
├── controller/    # REST controllers
├── dto/           # Request / response DTOs
├── entity/        # JPA entities
├── projection/    # Analytics projections
├── repository/    # Database access
└── service/       # Business logic
```

## Auth

This backend uses Supabase Auth for JWT issuance and Spring Security for JWT validation.

Protected endpoints live under `/api/**`.

## Running Locally

### Prerequisites

- Java 17+
- Maven
- PostgreSQL / Supabase database
- Supabase project configured

### Environment / Config

Set your database and Supabase JWT values in `src/main/resources/application.yml` or environment variables, depending on your local setup.

Typical values used by the project:

- `SUPABASE_JWT_ISSUER`
- `SUPABASE_JWT_AUDIENCE`
- `SUPABASE_JWK_SET_URI`

### Start the app

```bash
mvn spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

## Example Endpoints

- `POST /api/workouts`
- `GET /api/workouts`
- `GET /api/workouts/{id}`
- `PATCH /api/workouts/{id}/finish`
- `GET /api/exercises`
- `POST /api/exercises`
- `POST /api/workouts/{workoutId}/sets`
- `PATCH /api/workouts/{workoutId}/sets/{setId}`
- `DELETE /api/workouts/{workoutId}/sets/{setId}`

## Status

This project is in active development.

Current focus:

- improving workout flow
- template support
- frontend/backend integration
- stronger validation and polish
