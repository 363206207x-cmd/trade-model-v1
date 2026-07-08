#!/usr/bin/env bash
set -euo pipefail

# Controlled current-state migration + restore drill helper.
# Defaults to no-op evidence mode. It never prints DB host, database, user,
# password, or URL values. It runs backup/restore only when a disposable
# non-production source DB, a separate disposable recovery DB, and explicit
# confirmations are supplied.

source_vars=(
  CONTROLLED_CURRENT_STATE_DB_HOST
  CONTROLLED_CURRENT_STATE_DB_NAME
  CONTROLLED_CURRENT_STATE_DB_USERNAME
  CONTROLLED_CURRENT_STATE_DB_PASSWORD
)

recovery_vars=(
  CONTROLLED_RECOVERY_DB_HOST
  CONTROLLED_RECOVERY_DB_NAME
  CONTROLLED_RECOVERY_DB_USERNAME
  CONTROLLED_RECOVERY_DB_PASSWORD
)

print_presence() {
  local missing=0
  for name in "$@"; do
    if [ -z "${!name:-}" ]; then
      echo "${name}: MISSING"
      missing=1
    else
      echo "${name}: PRESENT_REDACTED"
    fi
  done
  return "$missing"
}

contains_production_indicator() {
  local value="$1"
  local value_lower
  value_lower=$(printf '%s' "$value" | tr '[:upper:]' '[:lower:]')
  printf '%s' "$value_lower" | grep -E '(prod|production|live|primary|main)' >/dev/null 2>&1
}

refuse_production_like_values() {
  local name value
  for name in \
    CONTROLLED_CURRENT_STATE_DB_HOST \
    CONTROLLED_CURRENT_STATE_DB_NAME \
    CONTROLLED_RECOVERY_DB_HOST \
    CONTROLLED_RECOVERY_DB_NAME; do
    value="${!name:-}"
    if [ -n "$value" ] && contains_production_indicator "$value"; then
      echo "CONTROLLED_CURRENT_STATE_DRILL_RESULT: BLOCKED_PRODUCTION_INDICATOR"
      echo "CONTROLLED_CURRENT_STATE_DRILL_ACTION: refusing because ${name} contains a production-like indicator"
      exit 2
    fi
  done
}

source_missing=0
recovery_missing=0

echo "CONTROLLED_CURRENT_STATE_SOURCE_ENV:"
print_presence "${source_vars[@]}" || source_missing=1

echo "CONTROLLED_CURRENT_STATE_RECOVERY_ENV:"
print_presence "${recovery_vars[@]}" || recovery_missing=1

if [ "$source_missing" -ne 0 ]; then
  echo "CONTROLLED_CURRENT_STATE_BACKUP_RESULT: SKIPPED_MISSING_CONTROLLED_DB"
  echo "CONTROLLED_CURRENT_STATE_RESTORE_RESULT: SKIPPED_MISSING_RECOVERY_DB"
  echo "CONTROLLED_CURRENT_STATE_MIGRATION_REHEARSAL_RESULT: SKIPPED"
  echo "CONTROLLED_CURRENT_STATE_DRILL_ACTION: no database access attempted"
  exit 0
fi

if [ "$recovery_missing" -ne 0 ]; then
  echo "CONTROLLED_CURRENT_STATE_BACKUP_RESULT: SKIPPED_MISSING_RECOVERY_DB"
  echo "CONTROLLED_CURRENT_STATE_RESTORE_RESULT: SKIPPED_MISSING_RECOVERY_DB"
  echo "CONTROLLED_CURRENT_STATE_MIGRATION_REHEARSAL_RESULT: SKIPPED"
  echo "CONTROLLED_CURRENT_STATE_DRILL_ACTION: no database access attempted"
  exit 0
fi

if [ "${CONTROLLED_CURRENT_STATE_DRILL_CONFIRM:-}" != "I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL" ]; then
  echo "CONTROLLED_CURRENT_STATE_DRILL_RESULT: BLOCKED_CONFIRMATION_REQUIRED"
  echo "CONTROLLED_CURRENT_STATE_DRILL_ACTION: refusing without explicit disposable non-production confirmation"
  exit 2
fi

if [ "${CONTROLLED_CURRENT_STATE_BACKUP_RUN:-}" != "I_UNDERSTAND_THIS_READS_CONTROLLED_DB_AND_WRITES_LOCAL_BACKUP" ]; then
  echo "CONTROLLED_CURRENT_STATE_BACKUP_RESULT: BLOCKED_BACKUP_CONFIRMATION_REQUIRED"
  echo "CONTROLLED_CURRENT_STATE_RESTORE_RESULT: SKIPPED"
  echo "CONTROLLED_CURRENT_STATE_MIGRATION_REHEARSAL_RESULT: SKIPPED"
  exit 2
fi

if [ "${CONTROLLED_CURRENT_STATE_RESTORE_RUN:-}" != "I_UNDERSTAND_THIS_RESTORES_TO_DISPOSABLE_CONTROLLED_RECOVERY_DB" ]; then
  echo "CONTROLLED_CURRENT_STATE_BACKUP_RESULT: SKIPPED"
  echo "CONTROLLED_CURRENT_STATE_RESTORE_RESULT: BLOCKED_RESTORE_CONFIRMATION_REQUIRED"
  echo "CONTROLLED_CURRENT_STATE_MIGRATION_REHEARSAL_RESULT: SKIPPED"
  exit 2
fi

refuse_production_like_values

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "CONTROLLED_CURRENT_STATE_BACKUP_RESULT: FAIL_TOOL_MISSING_PG_DUMP"
  exit 1
fi

if ! command -v pg_restore >/dev/null 2>&1; then
  echo "CONTROLLED_CURRENT_STATE_RESTORE_RESULT: FAIL_TOOL_MISSING_PG_RESTORE"
  exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "CONTROLLED_CURRENT_STATE_MIGRATION_REHEARSAL_RESULT: FAIL_TOOL_MISSING_PSQL"
  exit 1
fi

backup_dir="${CONTROLLED_CURRENT_STATE_BACKUP_DIR:-./backups/controlled-live5}"
backup_file="${CONTROLLED_CURRENT_STATE_BACKUP_FILE:-${backup_dir}/trade_model_controlled_current_state_$(date +%Y%m%d_%H%M%S).dump}"
mkdir -p "$backup_dir"

echo "CONTROLLED_CURRENT_STATE_BACKUP_RESULT: STARTING_REDACTED"
PGPASSWORD="$CONTROLLED_CURRENT_STATE_DB_PASSWORD" pg_dump \
  --host="$CONTROLLED_CURRENT_STATE_DB_HOST" \
  --port="${CONTROLLED_CURRENT_STATE_DB_PORT:-5432}" \
  --username="$CONTROLLED_CURRENT_STATE_DB_USERNAME" \
  --dbname="$CONTROLLED_CURRENT_STATE_DB_NAME" \
  --format=custom \
  --file="$backup_file"
echo "CONTROLLED_CURRENT_STATE_BACKUP_RESULT: PASS"
echo "CONTROLLED_CURRENT_STATE_BACKUP_FILE: ${backup_file}"

echo "CONTROLLED_CURRENT_STATE_RESTORE_RESULT: STARTING_REDACTED"
PGPASSWORD="$CONTROLLED_RECOVERY_DB_PASSWORD" pg_restore \
  --host="$CONTROLLED_RECOVERY_DB_HOST" \
  --port="${CONTROLLED_RECOVERY_DB_PORT:-5432}" \
  --username="$CONTROLLED_RECOVERY_DB_USERNAME" \
  --dbname="$CONTROLLED_RECOVERY_DB_NAME" \
  --clean \
  --if-exists \
  --no-owner \
  "$backup_file"
echo "CONTROLLED_CURRENT_STATE_RESTORE_RESULT: PASS"

echo "CONTROLLED_CURRENT_STATE_MIGRATION_REHEARSAL_RESULT: READONLY_FLYWAY_HISTORY_CHECK_STARTING"
PGPASSWORD="$CONTROLLED_RECOVERY_DB_PASSWORD" psql \
  --host="$CONTROLLED_RECOVERY_DB_HOST" \
  --port="${CONTROLLED_RECOVERY_DB_PORT:-5432}" \
  --username="$CONTROLLED_RECOVERY_DB_USERNAME" \
  --dbname="$CONTROLLED_RECOVERY_DB_NAME" \
  --tuples-only \
  --no-align \
  --command="select coalesce(max(version), 'none') from flyway_schema_history where success = true;" >/tmp/controlled_current_state_flyway_version.txt

if grep -qx '3' /tmp/controlled_current_state_flyway_version.txt; then
  echo "CONTROLLED_CURRENT_STATE_MIGRATION_REHEARSAL_RESULT: PASS"
else
  echo "CONTROLLED_CURRENT_STATE_MIGRATION_REHEARSAL_RESULT: FAIL_UNEXPECTED_FLYWAY_VERSION"
  exit 1
fi

echo "CONTROLLED_CURRENT_STATE_DRILL_RESULT: PASS"
