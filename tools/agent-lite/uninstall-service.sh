#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="aiops-agent-lite"

if [ "$(id -u)" -ne 0 ]; then
  echo "[error] please run with sudo: sudo ./uninstall-service.sh"
  exit 1
fi

systemctl stop "${SERVICE_NAME}" >/dev/null 2>&1 || true
systemctl disable "${SERVICE_NAME}" >/dev/null 2>&1 || true
rm -f "/etc/systemd/system/${SERVICE_NAME}.service"
systemctl daemon-reload

echo "[ok] ${SERVICE_NAME} service removed"
