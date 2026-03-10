#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${SUPABASE_PUBLISHABLE_KEY:-}" || -z "${SUPABASE_EMAIL:-}" || -z "${SUPABASE_PASSWORD:-}" ]]; then
  echo "Missing env vars. Please set SUPABASE_PUBLISHABLE_KEY, SUPABASE_EMAIL, SUPABASE_PASSWORD." >&2
  exit 1
fi

SUPABASE_URL="${SUPABASE_URL:-https://uzyjygnqzrfxgnijiufd.supabase.co}"

response="$(curl -s -X POST "${SUPABASE_URL}/auth/v1/token?grant_type=password" \
  -H "apikey: ${SUPABASE_PUBLISHABLE_KEY}" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${SUPABASE_EMAIL}\",\"password\":\"${SUPABASE_PASSWORD}\"}")"

if [[ -z "$response" ]]; then
  echo "Empty response from Supabase. Check SUPABASE_URL and network connectivity." >&2
  exit 1
fi

python - <<'PY' "$response"
import json, sys
raw = sys.argv[1]
try:
    payload = json.loads(raw)
except json.JSONDecodeError:
    print("Non-JSON response from Supabase:", raw, file=sys.stderr)
    raise SystemExit(1)

if "access_token" in payload:
    print(payload["access_token"])
else:
    print("Supabase error:", payload, file=sys.stderr)
    raise SystemExit(1)
PY
