-- A workout reaches the server only when it is finished, in one request. The phone generates
-- the workout's ID, so a retried finish can be recognised and not saved twice.
ALTER TABLE workouts ADD COLUMN client_id UUID;
UPDATE workouts SET client_id = gen_random_uuid();
ALTER TABLE workouts ALTER COLUMN client_id SET NOT NULL;
ALTER TABLE workouts ADD CONSTRAINT uq_workouts_user_id_client_id UNIQUE (user_id, client_id);

-- Every workout is now finished. Ones started under the old flow and never finished end when
-- they started.
UPDATE workouts SET end_time = start_time WHERE end_time IS NULL;
ALTER TABLE workouts ALTER COLUMN end_time SET NOT NULL;

-- Sets keep the order they were sent in. Existing sets were saved one by one, so their IDs
-- already follow that order.
ALTER TABLE sets ADD COLUMN position INTEGER;
UPDATE sets
SET position = numbered.position
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY workout_id ORDER BY id) - 1 AS position
    FROM sets
) numbered
WHERE sets.id = numbered.id;
ALTER TABLE sets ALTER COLUMN position SET NOT NULL;

-- A set's time is its workout's time.
ALTER TABLE sets DROP COLUMN timestamp;
