#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${AIOPS_AGENT_BASE_URL:-http://192.168.64.1:9000}"
SIGNING_KEY="${AGENT_SIGNING_KEY:-local-dev-agent-signing-key-change-me}"
HOME_DIR="${HOME:-/home/aiops}"
TARGET_DIR="$HOME_DIR/aiops-agent-lite"
PACKAGE_FILE="$HOME_DIR/aiops-agent-lite.tar.gz"
MANIFEST_FILE="$HOME_DIR/agent-release.json"
ENV_BACKUP="$HOME_DIR/.aiops-agent-lite.env.backup"

fetch() {
  local url="$1"
  local out="$2"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$url" -o "$out"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$out" "$url"
  else
    echo "[error] curl or wget is required"
    exit 1
  fi
}

json_value() {
  local key="$1"
  node -e "const fs=require('fs'); const j=JSON.parse(fs.readFileSync(process.argv[1],'utf8')); console.log(j[process.argv[2]] || '')" "$MANIFEST_FILE" "$key"
}

sha256_file() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    echo "[error] shasum or sha256sum is required"
    exit 1
  fi
}

hmac_sha256() {
  if ! command -v openssl >/dev/null 2>&1; then
    echo "[error] openssl is required for signature verification"
    exit 1
  fi
  if command -v xxd >/dev/null 2>&1; then
    printf '%s' "$1" | openssl dgst -sha256 -hmac "$SIGNING_KEY" -binary | xxd -p -c 256
  else
    printf '%s' "$1" | openssl dgst -sha256 -hmac "$SIGNING_KEY" -binary | od -An -tx1 | tr -d ' \n'
  fi
}

echo "[1/8] downloading release manifest"
fetch "$BASE_URL/agent-release.json" "$MANIFEST_FILE"
EXPECTED_SHA="$(json_value sha256)"
EXPECTED_SIGNATURE="$(json_value signature)"
PACKAGE_NAME="$(json_value packageName)"
LATEST_VERSION="$(json_value latestVersion)"
PACKAGE_NAME="${PACKAGE_NAME:-aiops-agent-lite.tar.gz}"

if [ -z "$EXPECTED_SHA" ]; then
  echo "[error] manifest missing sha256"
  exit 1
fi
if [ -z "$EXPECTED_SIGNATURE" ]; then
  echo "[error] manifest missing signature"
  exit 1
fi

echo "[2/8] stopping old agent if exists"
if [ -x "$TARGET_DIR/stop.sh" ]; then
  "$TARGET_DIR/stop.sh" || true
fi
pkill -f "agent.mjs" >/dev/null 2>&1 || true

echo "[3/8] backing up .env if exists"
if [ -f "$TARGET_DIR/.env" ]; then
  cp "$TARGET_DIR/.env" "$ENV_BACKUP"
fi

echo "[4/8] downloading package $PACKAGE_NAME"
rm -f "$PACKAGE_FILE"
fetch "$BASE_URL/$PACKAGE_NAME" "$PACKAGE_FILE"

ACTUAL_SHA="$(sha256_file "$PACKAGE_FILE")"
if [ "$ACTUAL_SHA" != "$EXPECTED_SHA" ]; then
  echo "[error] checksum mismatch"
  echo "expected: $EXPECTED_SHA"
  echo "actual:   $ACTUAL_SHA"
  exit 1
fi
echo "[ok] checksum verified: $ACTUAL_SHA"

ACTUAL_SIGNATURE="$(hmac_sha256 "$ACTUAL_SHA")"
if [ "$ACTUAL_SIGNATURE" != "$EXPECTED_SIGNATURE" ]; then
  echo "[error] signature mismatch"
  echo "expected: $EXPECTED_SIGNATURE"
  echo "actual:   $ACTUAL_SIGNATURE"
  exit 1
fi
echo "[ok] signature verified"

echo "[5/8] installing latest package"
rm -rf "$TARGET_DIR"
tar -xzf "$PACKAGE_FILE" -C "$HOME_DIR"

echo "[6/8] restoring .env"
if [ -f "$ENV_BACKUP" ]; then
  set -a
  # shellcheck source=/dev/null
  source "$ENV_BACKUP"
  set +a
  cat > "$TARGET_DIR/.env" <<ENVEOF
# Required
API_BASE_URL=${API_BASE_URL:-http://192.168.64.1:8080}
AGENT_KEY=${AGENT_KEY:-replace-with-your-target-agent-key}

# Optional
HOSTNAME_OVERRIDE=${HOSTNAME_OVERRIDE:-demo-host-01}
AGENT_IP=${AGENT_IP:-}
AGENT_VERSION=${LATEST_VERSION:-agent-lite-1.2.0-cross}
INTERVAL_MS=${INTERVAL_MS:-5000}
OBSERVABILITY_ENABLED=${OBSERVABILITY_ENABLED:-true}
LOG_EVERY_N_HEARTBEATS=${LOG_EVERY_N_HEARTBEATS:-12}
ENVEOF
else
  cp "$TARGET_DIR/.env.example" "$TARGET_DIR/.env"
  echo "[warn] no old .env found. Please edit $TARGET_DIR/.env before start."
fi

echo "[7/8] starting agent"
cd "$TARGET_DIR"
./start.sh

if [ -f "$MANIFEST_FILE" ]; then
  cp "$MANIFEST_FILE" "$TARGET_DIR/agent-release.json"
fi

echo "[8/8] status"
./status.sh

echo "[ok] upgrade completed"
echo "[info] run logs: cd $TARGET_DIR && ./tail-log.sh"
