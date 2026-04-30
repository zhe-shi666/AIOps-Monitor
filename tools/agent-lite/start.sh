#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$DIR/.env"
PID_FILE="$DIR/agent.pid"
LOG_FILE="$DIR/agent.log"

if ! command -v node >/dev/null 2>&1; then
  echo "[error] Node.js is not installed. Please install Node.js 18+ first."
  exit 1
fi

NODE_MAJOR="$(node -p "Number(process.versions.node.split('.')[0])")"
if [ "$NODE_MAJOR" -lt 18 ]; then
  echo "[error] Node.js version must be >= 18. Current: $(node -v)"
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  cp "$DIR/.env.example" "$ENV_FILE"
  echo "[info] .env created from .env.example, please edit it first:"
  echo "       $ENV_FILE"
  exit 1
fi

if [ -f "$PID_FILE" ]; then
  OLD_PID="$(cat "$PID_FILE" 2>/dev/null || true)"
  if [ -n "${OLD_PID}" ] && kill -0 "$OLD_PID" >/dev/null 2>&1; then
    echo "[info] agent-lite already running. pid=$OLD_PID"
    exit 0
  fi
fi

set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

if [ -z "${API_BASE_URL:-}" ] || [ -z "${AGENT_KEY:-}" ]; then
  echo "[error] API_BASE_URL and AGENT_KEY are required in .env"
  exit 1
fi

nohup node "$DIR/agent.mjs" >> "$LOG_FILE" 2>&1 &
PID=$!
echo "$PID" > "$PID_FILE"

echo "[ok] agent-lite started. pid=$PID"
echo "[info] log: $LOG_FILE"
echo "[info] stop: $DIR/stop.sh"
