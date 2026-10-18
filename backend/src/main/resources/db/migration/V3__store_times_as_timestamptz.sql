-- Every time becomes an exact moment. Existing values were all written by the server's clock on
-- Render, which runs in UTC, so they are read as UTC rather than in the session's time zone.
ALTER TABLE workouts
    ALTER COLUMN start_time TYPE TIMESTAMPTZ USING start_time AT TIME ZONE 'UTC',
    ALTER COLUMN end_time   TYPE TIMESTAMPTZ USING end_time AT TIME ZONE 'UTC';

ALTER TABLE sets
    ALTER COLUMN timestamp TYPE TIMESTAMPTZ USING timestamp AT TIME ZONE 'UTC';

ALTER TABLE coach_recommendations
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE coach_profiles
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
