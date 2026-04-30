#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${AIOPS_AGENT_BASE_URL:-http://192.168.64.1:9000}"
HOME_DIR="${HOME:-/home/aiops}"
TARGET_DIR="$HOME_DIR/aiops-agent-lite"
PACKAGE_FILE="$HOME_DIR/aiops-agent-lite.tar.gz"
ENV_BACKUP="$HOME_DIR/.aiops-agent-lite.env.backup"

echo "[1/6] stopping old agent if exists"
if [ -x "$TARGET_DIR/stop.sh" ]; then
  "$TARGET_DIR/stop.sh" || true
fi
pkill -f "agent.mjs" >/dev/null 2>&1 || true

echo "[2/6] backing up .env if exists"
if [ -f "$TARGET_DIR/.env" ]; then
  cp "$TARGET_DIR/.env" "$ENV_BACKUP"
fi

echo "[3/6] downloading latest package"
rm -f "$PACKAGE_FILE"
if command -v curl >/dev/null 2>&1; then
  curl -fL "$BASE_URL/aiops-agent-lite.tar.gz" -o "$PACKAGE_FILE"
elif command -v wget >/dev/null 2>&1; then
  wget -O "$PACKAGE_FILE" "$BASE_URL/aiops-agent-lite.tar.gz"
else
  echo "[error] curl or wget is required"
  exit 1
fi

echo "[4/6] installing latest package"
rm -rf "$TARGET_DIR"
tar -xzf "$PACKAGE_FILE" -C "$HOME_DIR"

echo "[5/6] restoring .env"
if [ -f "$ENV_BACKUP" ]; then
  set -a
  # shellcheck source=/dev/null
  source "$ENV_BACKUP"
  set +a
  cat > "$TARGET_DIR/.env" <<EOF
# Required
API_BASE_URL=${API_BASE_URL:-http://192.168.64.1:8080}
AGENT_KEY=${AGENT_KEY:-replace-with-your-target-agent-key}

# Optional
HOSTNAME_OVERRIDE=${HOSTNAME_OVERRIDE:-demo-host-01}
AGENT_IP=${AGENT_IP:-}
AGENT_VERSION=agent-lite-1.2.0-cross
INTERVAL_MS=${INTERVAL_MS:-5000}
OBSERVABILITY_ENABLED=${OBSERVABILITY_ENABLED:-true}
LOG_EVERY_N_HEARTBEATS=${LOG_EVERY_N_HEARTBEATS:-12}
EOF
else
  cp "$TARGET_DIR/.env.example" "$TARGET_DIR/.env"
  echo "[warn] no old .env found. Please edit $TARGET_DIR/.env before start."
fi

echo "[6/6] starting agent"
cd "$TARGET_DIR"
./start.sh
./status.sh

echo "[ok] upgrade completed"
echo "[info] run logs: cd $TARGET_DIR && ./tail-log.sh"
