#!/usr/bin/env bash
set -euo pipefail

# Phase-6 canary gate
#
# Example:
# STABLE_API_BASE_URL=http://localhost:8080 \
# CANARY_API_BASE_URL=http://localhost:8081 \
# USERNAME=admin PASSWORD=123456 \
# bash tools/phase-6/release/canary-check.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
STABLE_API_BASE_URL="${STABLE_API_BASE_URL:-http://localhost:8080}"
CANARY_API_BASE_URL="${CANARY_API_BASE_URL:-http://localhost:8081}"
USERNAME="${USERNAME:-}"
PASSWORD="${PASSWORD:-}"
TOKEN="${TOKEN:-}"
AUTO_REGISTER="${AUTO_REGISTER:-false}"

SMOKE_SCRIPT="$ROOT_DIR/tools/phase-6/smoke/smoke-check.mjs"
LOAD_SCRIPT="$ROOT_DIR/tools/phase-6/load/http-load.mjs"

function run_smoke() {
  local base_url="$1"
  echo "[gate] smoke @ $base_url"
  API_BASE_URL="$base_url" \
  USERNAME="$USERNAME" \
  PASSWORD="$PASSWORD" \
  TOKEN="$TOKEN" \
  AUTO_REGISTER="$AUTO_REGISTER" \
  node "$SMOKE_SCRIPT"
}

function run_load() {
  local base_url="$1"
  echo "[gate] light load @ $base_url"
  API_BASE_URL="$base_url" \
  USERNAME="$USERNAME" \
  PASSWORD="$PASSWORD" \
  TOKEN="$TOKEN" \
  AUTO_REGISTER="$AUTO_REGISTER" \
  CONCURRENCY="${CONCURRENCY:-10}" \
  DURATION_SECONDS="${DURATION_SECONDS:-45}" \
  MAX_ERROR_RATE="${MAX_ERROR_RATE:-0.05}" \
  MAX_P95_MS="${MAX_P95_MS:-1500}" \
  node "$LOAD_SCRIPT"
}

run_smoke "$STABLE_API_BASE_URL"
run_smoke "$CANARY_API_BASE_URL"
run_load "$CANARY_API_BASE_URL"

cat <<EOF
[gate] canary checks passed.
Next recommendation:
1) Route 10% traffic to canary for 15 minutes
2) Route 30% traffic to canary for 30 minutes
3) Route 50% traffic to canary for 30 minutes
4) Full cutover after metrics remain healthy
EOF
