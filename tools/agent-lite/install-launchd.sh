#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
PLIST="$HOME/Library/LaunchAgents/com.aiops.agent-lite.plist"
NODE_BIN="$(command -v node)"

mkdir -p "$HOME/Library/LaunchAgents"
cat > "$PLIST" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key><string>com.aiops.agent-lite</string>
  <key>WorkingDirectory</key><string>${DIR}</string>
  <key>ProgramArguments</key>
  <array>
    <string>${NODE_BIN}</string>
    <string>${DIR}/agent.mjs</string>
  </array>
  <key>EnvironmentVariables</key>
  <dict>
$(awk -F= '/^[A-Za-z_][A-Za-z0-9_]*=/ { printf "    <key>%s</key><string>%s</string>\n", $1, substr($0, index($0,$2)) }' "$DIR/.env")
  </dict>
  <key>RunAtLoad</key><true/>
  <key>KeepAlive</key><true/>
  <key>StandardOutPath</key><string>${DIR}/agent.log</string>
  <key>StandardErrorPath</key><string>${DIR}/agent.log</string>
</dict>
</plist>
PLIST

launchctl unload "$PLIST" >/dev/null 2>&1 || true
launchctl load "$PLIST"
launchctl start com.aiops.agent-lite

echo "[ok] launchd service installed: $PLIST"
