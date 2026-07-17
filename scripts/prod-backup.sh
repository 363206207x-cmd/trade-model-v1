#!/usr/bin/env bash
set -euo pipefail

umask 077

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
require_env PROD_DATASOURCE_DATABASE

password_file="${PROD_DATASOURCE_PASSWORD_FILE:-}"
if [ -n "${password_file}" ]; then
  if [ ! -f "${password_file}" ] || [ -L "${password_file}" ] || [ ! -s "${password_file}" ]; then
    echo "Invalid PROD_DATASOURCE_PASSWORD_FILE" >&2
    exit 1
  fi
  PGPASSFILE="$(mktemp)"
  trap 'rm -f "${PGPASSFILE}"' EXIT HUP INT TERM
  chmod 600 "${PGPASSFILE}"
  printf '%s:%s:%s:%s:%s\n' \
    "${PROD_DATASOURCE_HOST}" "${PROD_DATASOURCE_PORT:-5432}" \
    "${PROD_DATASOURCE_DATABASE}" "${PROD_DATASOURCE_USERNAME}" \
    "$(tr -d '\r\n' <"${password_file}")" >"${PGPASSFILE}"
  export PGPASSFILE
elif [ -n "${PROD_DATASOURCE_PASSWORD:-}" ]; then
  export PGPASSWORD="${PROD_DATASOURCE_PASSWORD}"
else
  echo "Missing required password input: PROD_DATASOURCE_PASSWORD_FILE" >&2
  exit 1
fi

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "pg_dump is required for backup" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"

pg_dump \
  --host="$PROD_DATASOURCE_HOST" \
  --port="${PROD_DATASOURCE_PORT:-5432}" \
  --username="$PROD_DATASOURCE_USERNAME" \
  --dbname="$PROD_DATASOURCE_DATABASE" \
  --format=custom \
  --file="$BACKUP_FILE"

chmod 600 "$BACKUP_FILE"

echo "PASS backup written to ${BACKUP_FILE}"
