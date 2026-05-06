#!/usr/bin/env bash
set -euo pipefail
if [ $# -lt 1 ]; then
  echo "usage: $0 backup.sql.gz"
  exit 1
fi
BACKUP_FILE="$1"
CONTAINER="${MYSQL_CONTAINER:-aiops-mysql}"
DB="${MYSQL_DATABASE:-aiops_monitor}"
USER="${MYSQL_USER:-root}"
PASSWORD="${MYSQL_PASSWORD:-root}"
if [[ "$BACKUP_FILE" == *.gz ]]; then
  gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER" mysql -u"$USER" -p"$PASSWORD" "$DB"
else
  docker exec -i "$CONTAINER" mysql -u"$USER" -p"$PASSWORD" "$DB" < "$BACKUP_FILE"
fi
echo "[ok] restore completed: $BACKUP_FILE"
