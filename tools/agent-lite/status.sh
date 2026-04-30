#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$DIR/agent.pid"
LOG_FILE="$DIR/agent.log"

if [ ! -f "$PID_FILE" ]; then
  echo "[status] stopped"
  exit 0
fi

PID="$(cat "$PID_FILE" 2>/dev/null || true)"
if [ -z "$PID" ]; then
  echo "[status] stopped (empty pid file)"
  exit 0
fi

if kill -0 "$PID" >/dev/null 2>&1; then
  echo "[status] running pid=$PID"
  echo "[log] $LOG_FILE"
  exit 0
fi

echo "[status] stopped (stale pid=$PID)"
exit 0
