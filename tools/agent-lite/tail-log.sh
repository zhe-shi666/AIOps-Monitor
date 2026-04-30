#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="$DIR/agent.log"

if [ ! -f "$LOG_FILE" ]; then
  touch "$LOG_FILE"
fi

tail -f "$LOG_FILE"
