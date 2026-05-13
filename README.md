# CaliTrack

Full-stack fitness tracking app with a React Native mobile client and a Spring Boot backend.

## Stack

- React Native / Expo
- TypeScript
- Spring Boot 3
- Java 17
- PostgreSQL
- Spring Data JPA
- Spring Security
- Supabase Auth

## Project Structure

```text
CaliTrack/
├── backend/   # Spring Boot API
└── mobile/    # React Native / Expo app
```

## Features

- JWT-protected workout tracking API
- Exercise CRUD
- Workout creation and completion flow
- Set create, update, and delete
- Workout history and detail views
- Analytics for volume, PRs, workout frequency, and profile insights
- Mobile UI for workout logging and progress review
- Persisted kg/lb unit preference

## Run Backend

```bash
cd backend
mvn spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

Configure PostgreSQL and Supabase JWT settings in:

```text
backend/src/main/resources/application.yml
```

## Run Mobile App

```bash
cd mobile
npm install
npm start
```

Then open the app with Expo Go or an emulator.

## Repositories

This monorepo preserves the histories of the original backend and mobile repositories:

- `CaliTrack-backend`
- `CaliTrack-mobile`
