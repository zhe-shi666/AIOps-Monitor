#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$DIR/../.." && pwd)"
OUT_DIR="$ROOT_DIR/dist"
PKG_DIR="$OUT_DIR/aiops-agent-lite"
PKG_FILE="$OUT_DIR/aiops-agent-lite.tar.gz"
MANIFEST_FILE="$OUT_DIR/agent-release.json"
SQL_FILE="$OUT_DIR/agent-release.sql"
VERSION="${AGENT_VERSION:-agent-lite-1.2.0-cross}"
SIGNING_KEY="${AGENT_SIGNING_KEY:-local-dev-agent-signing-key-change-me}"

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
SHA256="$(shasum -a 256 "$PKG_FILE" | awk '{print $1}')"
SIGNATURE="$(printf '%s' "$SHA256" | openssl dgst -sha256 -hmac "$SIGNING_KEY" -binary | xxd -p -c 256)"
CREATED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

cat > "$MANIFEST_FILE" <<JSON
{
  "latestVersion": "$VERSION",
  "packageName": "aiops-agent-lite.tar.gz",
  "sha256": "$SHA256",
  "signature": "$SIGNATURE",
  "mandatory": false,
  "supports": ["linux", "macos", "windows"],
  "createdAt": "$CREATED_AT",
  "releaseNotes": "Cross-platform real metrics agent with service install scripts."
}
JSON

cat > "$SQL_FILE" <<SQL
INSERT INTO agent_release (version, package_name, sha256, signature, release_notes, mandatory, active, created_at)
VALUES ('$VERSION', 'aiops-agent-lite.tar.gz', '$SHA256', '$SIGNATURE', 'Cross-platform real metrics agent with service install scripts.', 0, 1, NOW())
ON DUPLICATE KEY UPDATE
  package_name = VALUES(package_name),
  sha256 = VALUES(sha256),
  signature = VALUES(signature),
  release_notes = VALUES(release_notes),
  mandatory = VALUES(mandatory),
  active = VALUES(active);
SQL

echo "[ok] package created: $PKG_FILE"
echo "[ok] manifest created: $MANIFEST_FILE"
echo "[ok] release sql created: $SQL_FILE"
