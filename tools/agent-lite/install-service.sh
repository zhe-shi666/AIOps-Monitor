#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
SERVICE_NAME="aiops-agent-lite"
USER_NAME="${SUDO_USER:-$(whoami)}"

if ! command -v systemctl >/dev/null 2>&1; then
  echo "[error] systemd not found. Use ./start.sh on non-systemd systems."
  exit 1
fi

if [ "$(id -u)" -ne 0 ]; then
  echo "[error] please run with sudo: sudo ./install-service.sh"
  exit 1
fi

cat > "/etc/systemd/system/${SERVICE_NAME}.service" <<UNIT
[Unit]
Description=AIOps Agent Lite
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=${USER_NAME}
WorkingDirectory=${DIR}
EnvironmentFile=${DIR}/.env
ExecStart=$(command -v node) ${DIR}/agent.mjs
Restart=always
RestartSec=5
StandardOutput=append:${DIR}/agent.log
StandardError=append:${DIR}/agent.log

[Install]
WantedBy=multi-user.target
UNIT

systemctl daemon-reload
systemctl enable "${SERVICE_NAME}"
systemctl restart "${SERVICE_NAME}"
systemctl status "${SERVICE_NAME}" --no-pager
