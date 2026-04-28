#!/usr/bin/env bash
set -euo pipefail

# Phase-6 fast rollback helper
#
# This script does not enforce a specific deployment platform. It provides
# a safe rollback sequence and supports two common canary process forms:
# 1) canary PID file
# 2) canary Docker container (aiops-backend-canary)
#
# Example:
# STABLE_API_BASE_URL=http://localhost:8080 \
# USERNAME=admin PASSWORD=123456 \
# bash tools/phase-6/release/rollback-fast.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
STABLE_API_BASE_URL="${STABLE_API_BASE_URL:-http://localhost:8080}"
CANARY_PID_FILE="${CANARY_PID_FILE:-$ROOT_DIR/run/canary.pid}"
CANARY_CONTAINER="${CANARY_CONTAINER:-aiops-backend-canary}"
USERNAME="${USERNAME:-}"
PASSWORD="${PASSWORD:-}"
TOKEN="${TOKEN:-}"
AUTO_REGISTER="${AUTO_REGISTER:-false}"
SMOKE_SCRIPT="$ROOT_DIR/tools/phase-6/smoke/smoke-check.mjs"

echo "[rollback] begin"

if [[ -f "$CANARY_PID_FILE" ]]; then
  CANARY_PID="$(cat "$CANARY_PID_FILE" || true)"
  if [[ -n "${CANARY_PID:-}" ]] && kill -0 "$CANARY_PID" >/dev/null 2>&1; then
    echo "[rollback] stop canary process pid=$CANARY_PID"
    kill "$CANARY_PID" || true
  fi
  rm -f "$CANARY_PID_FILE" || true
fi

if command -v docker >/dev/null 2>&1; then
  if docker ps -a --format '{{.Names}}' | grep -q "^${CANARY_CONTAINER}$"; then
    echo "[rollback] stop canary container $CANARY_CONTAINER"
    docker rm -f "$CANARY_CONTAINER" >/dev/null 2>&1 || true
  fi
fi

echo "[rollback] verify stable"
API_BASE_URL="$STABLE_API_BASE_URL" \
USERNAME="$USERNAME" \
PASSWORD="$PASSWORD" \
TOKEN="$TOKEN" \
AUTO_REGISTER="$AUTO_REGISTER" \
node "$SMOKE_SCRIPT"

cat <<EOF
[rollback] completed.
Follow-up actions:
1) Keep traffic fully routed to stable.
2) Create incident + postmortem for canary failure.
3) Freeze canary pipeline until root cause is resolved.
EOF
