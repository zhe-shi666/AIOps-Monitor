#!/usr/bin/env bash
set -euo pipefail

# Phase-6 fault drill helper
#
# Examples:
# 1) Redis restart drill:
#    USERNAME=admin PASSWORD=123456 \
#    bash tools/phase-6/drill/fault-drill.sh redis-restart
#
# 2) MySQL restart drill:
#    USERNAME=admin PASSWORD=123456 \
#    bash tools/phase-6/drill/fault-drill.sh mysql-restart
#
# 3) Ollama restart drill:
#    USERNAME=admin PASSWORD=123456 \
#    bash tools/phase-6/drill/fault-drill.sh ollama-restart

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
USERNAME="${USERNAME:-}"
PASSWORD="${PASSWORD:-}"
TOKEN="${TOKEN:-}"
AUTO_REGISTER="${AUTO_REGISTER:-false}"
SMOKE_SCRIPT="$ROOT_DIR/tools/phase-6/smoke/smoke-check.mjs"

SCENARIO="${1:-}"
if [[ -z "$SCENARIO" ]]; then
  echo "usage: bash tools/phase-6/drill/fault-drill.sh <redis-restart|mysql-restart|ollama-restart>"
  exit 2
fi

function require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[fatal] command not found: $1"
    exit 2
  fi
}

function run_smoke() {
  echo "[step] smoke check"
  API_BASE_URL="$API_BASE_URL" \
  USERNAME="$USERNAME" \
  PASSWORD="$PASSWORD" \
  TOKEN="$TOKEN" \
  AUTO_REGISTER="$AUTO_REGISTER" \
  node "$SMOKE_SCRIPT"
}

function wait_mysql() {
  echo "[wait] mysql ready"
  for _ in $(seq 1 60); do
    if docker exec aiops-mysql mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; then
      echo "[ok] mysql ready"
      return
    fi
    sleep 2
  done
  echo "[fatal] mysql not ready"
  exit 1
}

function wait_redis() {
  echo "[wait] redis ready"
  for _ in $(seq 1 30); do
    if [[ "$(docker exec aiops-redis redis-cli ping 2>/dev/null)" == "PONG" ]]; then
      echo "[ok] redis ready"
      return
    fi
    sleep 2
  done
  echo "[fatal] redis not ready"
  exit 1
}

function wait_ollama() {
  echo "[wait] ollama ready"
  for _ in $(seq 1 45); do
    if curl -fsS "http://localhost:11434/api/tags" >/dev/null 2>&1; then
      echo "[ok] ollama ready"
      return
    fi
    sleep 2
  done
  echo "[fatal] ollama not ready"
  exit 1
}

require_cmd docker
require_cmd node
require_cmd curl

echo "[phase6-drill] scenario=$SCENARIO api=$API_BASE_URL"
run_smoke

case "$SCENARIO" in
  redis-restart)
    echo "[inject] restart aiops-redis"
    docker restart aiops-redis >/dev/null
    wait_redis
    ;;
  mysql-restart)
    echo "[inject] restart aiops-mysql"
    docker restart aiops-mysql >/dev/null
    wait_mysql
    ;;
  ollama-restart)
    echo "[inject] restart aiops-ollama"
    docker restart aiops-ollama >/dev/null
    wait_ollama
    ;;
  *)
    echo "[fatal] unsupported scenario: $SCENARIO"
    exit 2
    ;;
esac

run_smoke
echo "[phase6-drill] completed: $SCENARIO"
