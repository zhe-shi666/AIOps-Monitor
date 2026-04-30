#!/usr/bin/env bash
set -euo pipefail

PLIST="$HOME/Library/LaunchAgents/com.aiops.agent-lite.plist"
launchctl unload "$PLIST" >/dev/null 2>&1 || true
rm -f "$PLIST"
echo "[ok] launchd service removed"
