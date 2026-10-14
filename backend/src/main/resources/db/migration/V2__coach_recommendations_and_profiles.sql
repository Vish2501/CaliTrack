CREATE TABLE coach_recommendations (
    id             BIGSERIAL PRIMARY KEY,
    user_id        VARCHAR(255) NOT NULL,
    workout_id     BIGINT NOT NULL REFERENCES workouts (id) ON DELETE CASCADE,
    recommendation TEXT NOT NULL,
    focus_area     TEXT NOT NULL,
    next_exercise  TEXT NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    -- One recommendation per workout, so a redelivered WorkoutLogged event can't create a duplicate.
    CONSTRAINT uq_coach_recommendations_workout_id UNIQUE (workout_id)
);

CREATE INDEX idx_coach_recommendations_user_id ON coach_recommendations (user_id);

CREATE TABLE coach_profiles (
    user_id       VARCHAR(255) PRIMARY KEY,
    goals         VARCHAR(200) NOT NULL,
    fitness_level VARCHAR(50) NOT NULL,
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);
