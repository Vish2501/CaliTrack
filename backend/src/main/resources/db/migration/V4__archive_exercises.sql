-- Deleting an exercise archives it, so its sets and history are kept.
ALTER TABLE exercises ADD COLUMN archived BOOLEAN NOT NULL DEFAULT false;

-- Exercise names become unique for each user, ignoring case. Earlier versions could create the
-- same exercise twice (the app re-created default exercises on screen load), so duplicates are
-- merged into the oldest one: their sets move to it, then the duplicates are removed.
WITH keepers AS (
    SELECT id, MIN(id) OVER (PARTITION BY user_id, lower(name)) AS keeper_id
    FROM exercises
)
UPDATE sets
SET exercise_id = keepers.keeper_id
FROM keepers
WHERE sets.exercise_id = keepers.id
  AND keepers.id <> keepers.keeper_id;

DELETE FROM exercises e
USING exercises older
WHERE older.user_id = e.user_id
  AND lower(older.name) = lower(e.name)
  AND older.id < e.id;

CREATE UNIQUE INDEX uq_exercises_user_id_lower_name ON exercises (user_id, lower(name));
