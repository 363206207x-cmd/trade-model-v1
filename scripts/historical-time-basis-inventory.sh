#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_FILE="${ROOT_DIR}/scripts/historical-time-basis-inventory.sql"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/historical-time-inventory.XXXXXX")"

cleanup() {
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT INT TERM

required_vars=(
  HISTORICAL_TIME_INVENTORY_HOST
  HISTORICAL_TIME_INVENTORY_PORT
  HISTORICAL_TIME_INVENTORY_DATABASE
  HISTORICAL_TIME_INVENTORY_USERNAME
  HISTORICAL_TIME_INVENTORY_PASSWORD
  HISTORICAL_TIME_INVENTORY_DATABASE_CLASS
)

missing=0
for name in "${required_vars[@]}"; do
  if [ -z "${!name:-}" ]; then
    echo "${name}: MISSING"
    missing=1
  else
    echo "${name}: PRESENT_REDACTED"
  fi
done

if [ "${missing}" -ne 0 ]; then
  echo "HISTORICAL_TIME_INVENTORY_RESULT: SKIPPED_MISSING_CONTROLLED_DATABASE"
  exit 0
fi

if [ "${HISTORICAL_TIME_INVENTORY_CONFIRM:-}" != "I_CONFIRM_READ_ONLY_NON_PRODUCTION_DATABASE" ]; then
  echo "HISTORICAL_TIME_INVENTORY_RESULT: BLOCKED_CONFIRMATION_REQUIRED"
  exit 2
fi

case "${HISTORICAL_TIME_INVENTORY_DATABASE_CLASS}" in
  RESTORE|STAGING_CLONE|SANITIZED_REHEARSAL|LOCAL_CONTROLLED) ;;
  *)
    echo "HISTORICAL_TIME_INVENTORY_RESULT: BLOCKED_DATABASE_CLASS"
    exit 2
    ;;
esac

target_identity="${HISTORICAL_TIME_INVENTORY_HOST}/${HISTORICAL_TIME_INVENTORY_DATABASE}"
target_lower="$(printf '%s' "${target_identity}" | tr '[:upper:]' '[:lower:]')"
if printf '%s' "${target_lower}" | grep -E '(prod|production|live|primary|main)' >/dev/null 2>&1; then
  echo "HISTORICAL_TIME_INVENTORY_RESULT: BLOCKED_PRODUCTION_INDICATOR"
  exit 2
fi

if ! printf '%s' "${HISTORICAL_TIME_INVENTORY_DATABASE}" \
  | grep -Eiq '(test|staging|rehearsal|restore|recovery|sanitized|clone|controlled)'; then
  echo "HISTORICAL_TIME_INVENTORY_RESULT: BLOCKED_UNVERIFIED_DATABASE_NAME"
  exit 2
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "HISTORICAL_TIME_INVENTORY_RESULT: BLOCKED_PSQL_MISSING"
  exit 2
fi

output_file="${TMP_DIR}/inventory.txt"
error_file="${TMP_DIR}/inventory-error.txt"
set +e
PGPASSWORD="${HISTORICAL_TIME_INVENTORY_PASSWORD}" \
PGOPTIONS="-c default_transaction_read_only=on" \
psql \
  --host="${HISTORICAL_TIME_INVENTORY_HOST}" \
  --port="${HISTORICAL_TIME_INVENTORY_PORT}" \
  --username="${HISTORICAL_TIME_INVENTORY_USERNAME}" \
  --dbname="${HISTORICAL_TIME_INVENTORY_DATABASE}" \
  --no-psqlrc \
  --file="${SQL_FILE}" >"${output_file}" 2>"${error_file}"
inventory_status=$?
set -e

if [ "${inventory_status}" -ne 0 ]; then
  echo "HISTORICAL_TIME_INVENTORY_RESULT: FAIL_REDACTED"
  exit "${inventory_status}"
fi

cat "${output_file}"
echo "HISTORICAL_TIME_INVENTORY_RESULT: PASS_READ_ONLY"
