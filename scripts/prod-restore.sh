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
require_env RESTORE_DATASOURCE_DATABASE
require_env RESTORE_BACKUP_FILE

password_file="${RESTORE_DATASOURCE_PASSWORD_FILE:-}"
if [ -n "${password_file}" ]; then
  if [ ! -f "${password_file}" ] || [ -L "${password_file}" ] || [ ! -s "${password_file}" ]; then
    echo "Invalid RESTORE_DATASOURCE_PASSWORD_FILE" >&2
    exit 1
  fi
  PGPASSFILE="$(mktemp)"
  trap 'rm -f "${PGPASSFILE}"' EXIT HUP INT TERM
  chmod 600 "${PGPASSFILE}"
  printf '%s:%s:%s:%s:%s\n' \
    "${RESTORE_DATASOURCE_HOST}" "${RESTORE_DATASOURCE_PORT:-5432}" \
    "${RESTORE_DATASOURCE_DATABASE}" "${RESTORE_DATASOURCE_USERNAME}" \
    "$(tr -d '\r\n' <"${password_file}")" >"${PGPASSFILE}"
  export PGPASSFILE
elif [ -n "${RESTORE_DATASOURCE_PASSWORD:-}" ]; then
  export PGPASSWORD="${RESTORE_DATASOURCE_PASSWORD}"
else
  echo "Missing required password input: RESTORE_DATASOURCE_PASSWORD_FILE" >&2
  exit 1
fi

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
    psql \
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
    pg_restore \
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
