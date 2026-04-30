#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$DIR/../.." && pwd)"
OUT_DIR="$ROOT_DIR/dist"
PKG_DIR="$OUT_DIR/aiops-agent-lite"
PKG_FILE="$OUT_DIR/aiops-agent-lite.tar.gz"

rm -rf "$PKG_DIR"
mkdir -p "$PKG_DIR" "$OUT_DIR"

cp "$DIR/agent.mjs" "$PKG_DIR/"
cp "$DIR/.env.example" "$PKG_DIR/"
cp "$DIR/start.sh" "$PKG_DIR/"
cp "$DIR/stop.sh" "$PKG_DIR/"
cp "$DIR/status.sh" "$PKG_DIR/"
cp "$DIR/tail-log.sh" "$PKG_DIR/"
cp "$DIR/install-service.sh" "$PKG_DIR/"
cp "$DIR/uninstall-service.sh" "$PKG_DIR/"
cp "$DIR/install-launchd.sh" "$PKG_DIR/"
cp "$DIR/uninstall-launchd.sh" "$PKG_DIR/"
cp "$DIR/start.ps1" "$PKG_DIR/"
cp "$DIR/stop.ps1" "$PKG_DIR/"
cp "$DIR/status.ps1" "$PKG_DIR/"
cp "$DIR/tail-log.ps1" "$PKG_DIR/"
cp "$DIR/install-service.ps1" "$PKG_DIR/"
cp "$DIR/uninstall-service.ps1" "$PKG_DIR/"
cp "$DIR/README.md" "$PKG_DIR/"
cp "$DIR/install-or-upgrade.sh" "$OUT_DIR/"

chmod +x "$PKG_DIR/start.sh" "$PKG_DIR/stop.sh" "$PKG_DIR/status.sh" "$PKG_DIR/tail-log.sh"
chmod +x "$PKG_DIR/install-service.sh" "$PKG_DIR/uninstall-service.sh" "$PKG_DIR/install-launchd.sh" "$PKG_DIR/uninstall-launchd.sh"
chmod +x "$OUT_DIR/install-or-upgrade.sh"

tar -czf "$PKG_FILE" -C "$OUT_DIR" aiops-agent-lite
echo "[ok] package created: $PKG_FILE"
