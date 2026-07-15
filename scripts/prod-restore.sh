#!/usr/bin/env bash
set -euo pipefail

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

require_env RESTORE_DATASOURCE_HOST
require_env RESTORE_DATASOURCE_USERNAME
require_env RESTORE_DATASOURCE_PASSWORD
require_env RESTORE_DATASOURCE_DATABASE
require_env RESTORE_BACKUP_FILE

if [ "${RESTORE_CONFIRM:-}" != "I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA" ]; then
  echo "Refusing restore without RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA" >&2
  exit 1
fi

case "$RESTORE_BACKUP_FILE" in
  *.sql)
    if ! command -v psql >/dev/null 2>&1; then
      echo "psql is required for plain SQL restore" >&2
      exit 1
    fi
    PGPASSWORD="$RESTORE_DATASOURCE_PASSWORD" psql \
      --host="$RESTORE_DATASOURCE_HOST" \
      --port="${RESTORE_DATASOURCE_PORT:-5432}" \
      --username="$RESTORE_DATASOURCE_USERNAME" \
      --dbname="$RESTORE_DATASOURCE_DATABASE" \
      --file="$RESTORE_BACKUP_FILE"
    ;;
  *)
    if ! command -v pg_restore >/dev/null 2>&1; then
      echo "pg_restore is required for custom dump restore" >&2
      exit 1
    fi
    PGPASSWORD="$RESTORE_DATASOURCE_PASSWORD" pg_restore \
      --host="$RESTORE_DATASOURCE_HOST" \
      --port="${RESTORE_DATASOURCE_PORT:-5432}" \
      --username="$RESTORE_DATASOURCE_USERNAME" \
      --dbname="$RESTORE_DATASOURCE_DATABASE" \
      --clean \
      --if-exists \
      --no-owner \
      --no-acl \
      --exit-on-error \
      "$RESTORE_BACKUP_FILE"
    ;;
esac

echo "PASS restore completed for ${RESTORE_DATASOURCE_DATABASE}"
