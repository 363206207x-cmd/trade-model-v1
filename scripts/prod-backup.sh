#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
BACKUP_FILE="${BACKUP_FILE:-${BACKUP_DIR}/trade_model_v1_$(date +%Y%m%d_%H%M%S).dump}"

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

require_env PROD_DATASOURCE_HOST
require_env PROD_DATASOURCE_USERNAME
require_env PROD_DATASOURCE_PASSWORD
require_env PROD_DATASOURCE_DATABASE

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "pg_dump is required for backup" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"

PGPASSWORD="$PROD_DATASOURCE_PASSWORD" pg_dump \
  --host="$PROD_DATASOURCE_HOST" \
  --port="${PROD_DATASOURCE_PORT:-5432}" \
  --username="$PROD_DATASOURCE_USERNAME" \
  --dbname="$PROD_DATASOURCE_DATABASE" \
  --format=custom \
  --file="$BACKUP_FILE"

echo "PASS backup written to ${BACKUP_FILE}"
