#!/usr/bin/env bash
set -euo pipefail
BACKUP_DIR="${BACKUP_DIR:-./backups/mysql}"
CONTAINER="${MYSQL_CONTAINER:-aiops-mysql}"
DB="${MYSQL_DATABASE:-aiops_monitor}"
USER="${MYSQL_USER:-root}"
PASSWORD="${MYSQL_PASSWORD:-root}"
TS="$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
OUT="$BACKUP_DIR/${DB}_${TS}.sql.gz"
docker exec "$CONTAINER" mysqldump -u"$USER" -p"$PASSWORD" --single-transaction --routines --triggers "$DB" | gzip > "$OUT"
echo "[ok] backup created: $OUT"
