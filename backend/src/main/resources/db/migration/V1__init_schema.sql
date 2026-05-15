CREATE TABLE workouts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP,
    notes       TEXT
);

CREATE TABLE exercises (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    user_id  VARCHAR(255) NOT NULL
);

CREATE TABLE sets (
    id          BIGSERIAL PRIMARY KEY,
    workout_id  BIGINT NOT NULL REFERENCES workouts (id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises (id),
    reps        INTEGER,
    weight      DOUBLE PRECISION,
    rpe         DOUBLE PRECISION,
    timestamp   TIMESTAMP NOT NULL
);

CREATE INDEX idx_workouts_user_id ON workouts (user_id);
CREATE INDEX idx_exercises_user_id ON exercises (user_id);
CREATE INDEX idx_sets_workout_id ON sets (workout_id);
CREATE INDEX idx_sets_exercise_id ON sets (exercise_id);
