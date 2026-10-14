#!/usr/bin/env bash
# Walks through the Kafka-backed AI coach against the local Docker Compose stack.
# Uses the fake model by default, so no OpenAI key or credit is needed.
#
#   docker compose up --build -d
#   export SUPABASE_PUBLISHABLE_KEY=... SUPABASE_EMAIL=... SUPABASE_PASSWORD=...
#   TOKEN=$(backend/scripts/get-token.sh) scripts/demo-coach.sh
set -euo pipefail

API="${API:-http://localhost:8080/api/v1}"
: "${TOKEN:?Set TOKEN to a Supabase access token, e.g. TOKEN=\$(backend/scripts/get-token.sh)}"

step() { printf '\n\033[1m%s\033[0m\n' "$*"; }
api() { curl -sf -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" "$@"; }
json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }

step "1. Start a workout"
workout_id=$(api -X POST "$API/workouts" | json 'd["id"]')
echo "workout $workout_id"

step "2. Log two sets of the first exercise in your library"
exercise_id=$(api "$API/exercises" | json 'd[0]["id"] if d else ""')
if [[ -z "$exercise_id" ]]; then
  exercise_id=$(api -X POST "$API/exercises" -d '{"name":"Pull-Up","category":"Back"}' | json 'd["id"]')
fi
for reps in 8 10; do
  api -X POST "$API/workouts/$workout_id/sets" \
    -d "{\"exerciseId\":$exercise_id,\"reps\":$reps,\"weight\":0,\"rpe\":8}" > /dev/null
done
echo "logged 2 sets of exercise $exercise_id"

step "3. Finish the workout: the API returns immediately and publishes WorkoutLogged"
time api -X PATCH "$API/workouts/$workout_id/finish" -d '{}' > /dev/null

step "4. The event on the workout-logged topic (key = user ID)"
docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic workout-logged --from-beginning \
  --property print.key=true --property key.separator=' => ' --timeout-ms 5000 2>/dev/null \
  | grep "\"workoutId\":$workout_id" || echo "(event not found)"

step "5. Poll the latest recommendation while the consumer works on it"
for _ in $(seq 1 30); do
  latest=$(api "$API/coach/recommendations/latest")
  done_for=$(echo "$latest" | json '(d["recommendation"] or {}).get("workoutId", "")')
  if [[ "$done_for" == "$workout_id" ]]; then
    echo "$latest" | python3 -m json.tool
    exit 0
  fi
  echo "pending=$(echo "$latest" | json 'd["pending"]')"
  sleep 1
done
echo "No recommendation after 30s. Check: docker compose logs backend | grep -i coach" >&2
exit 1
