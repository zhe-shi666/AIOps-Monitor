#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$DIR/agent.pid"

if [ ! -f "$PID_FILE" ]; then
  echo "[info] no pid file. agent-lite may not be running."
  exit 0
fi

PID="$(cat "$PID_FILE" 2>/dev/null || true)"
if [ -z "$PID" ]; then
  rm -f "$PID_FILE"
  echo "[info] empty pid file removed."
  exit 0
fi

if kill -0 "$PID" >/dev/null 2>&1; then
  kill "$PID"
  sleep 1
  if kill -0 "$PID" >/dev/null 2>&1; then
    kill -9 "$PID" >/dev/null 2>&1 || true
  fi
  echo "[ok] agent-lite stopped. pid=$PID"
else
  echo "[info] process not found. pid=$PID"
fi

rm -f "$PID_FILE"
