#!/usr/bin/env bash
set -euo pipefail

# One-shot Phase-6 validation suite.
#
# Example:
# USERNAME=admin PASSWORD=123456 AUTO_REGISTER=true \
# bash tools/phase-6/run-phase6-suite.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
USERNAME="${USERNAME:-}"
PASSWORD="${PASSWORD:-}"
TOKEN="${TOKEN:-}"
AUTO_REGISTER="${AUTO_REGISTER:-false}"
RUN_DRILL="${RUN_DRILL:-false}"
DRILL_SCENARIO="${DRILL_SCENARIO:-redis-restart}"

echo "[phase6-suite] root=$ROOT_DIR api=$API_BASE_URL"

echo "[step] smoke checks"
API_BASE_URL="$API_BASE_URL" \
USERNAME="$USERNAME" \
PASSWORD="$PASSWORD" \
TOKEN="$TOKEN" \
AUTO_REGISTER="$AUTO_REGISTER" \
node "$ROOT_DIR/tools/phase-6/smoke/smoke-check.mjs"

echo "[step] load baseline"
API_BASE_URL="$API_BASE_URL" \
USERNAME="$USERNAME" \
PASSWORD="$PASSWORD" \
TOKEN="$TOKEN" \
AUTO_REGISTER="$AUTO_REGISTER" \
CONCURRENCY="${CONCURRENCY:-20}" \
DURATION_SECONDS="${DURATION_SECONDS:-60}" \
MAX_ERROR_RATE="${MAX_ERROR_RATE:-0.05}" \
MAX_P95_MS="${MAX_P95_MS:-1200}" \
node "$ROOT_DIR/tools/phase-6/load/http-load.mjs"

if [[ "$RUN_DRILL" == "true" ]]; then
  echo "[step] fault drill scenario=$DRILL_SCENARIO"
  API_BASE_URL="$API_BASE_URL" \
  USERNAME="$USERNAME" \
  PASSWORD="$PASSWORD" \
  TOKEN="$TOKEN" \
  AUTO_REGISTER="$AUTO_REGISTER" \
  bash "$ROOT_DIR/tools/phase-6/drill/fault-drill.sh" "$DRILL_SCENARIO"
fi

echo "[phase6-suite] completed"
