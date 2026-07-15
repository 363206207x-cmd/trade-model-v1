#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EVIDENCE_DIR="${ROOT_DIR}/.runtime/postgresql-p3-rehearsal"
BACKUP_DIR="${EVIDENCE_DIR}/backups"
IMAGE_REF="postgres@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc"
TARGET_HOST="${P3_TARGET_HOST:-127.0.0.1}"
TARGET_PORT="${P3_TARGET_PORT:-55433}"
SOURCE_DATABASE="${P3_SOURCE_DATABASE:-trade_model_v1_p3_source}"
REHEARSAL_DATABASE="${P3_REHEARSAL_DATABASE:-trade_model_v1_p3_rehearsal}"
RECOVERY_DATABASE="${P3_RECOVERY_DATABASE:-trade_model_v1_p3_recovery}"
USERNAME="p3_local_operator"
CONTAINER_NAME="trade-model-v1-p3-$(date -u +%Y%m%d%H%M%S)-$$"
APP_PORT="18083"
APP_PID=""
CONTAINER_STARTED=0
CONTAINER_CLEANUP="NOT_STARTED"
CURRENT_STAGE="input-preflight"
FINAL_RESULT="BLOCKED"
TMP_DIR=""

sha256_file() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    sha256sum "$1" | awk '{print $1}'
  fi
}

sha256_text() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 | awk '{print $1}'
  else
    sha256sum | awk '{print $1}'
  fi
}

file_size_bytes() {
  if stat -f '%z' "$1" >/dev/null 2>&1; then
    stat -f '%z' "$1"
  else
    stat -c '%s' "$1"
  fi
}

canonical_existing_file() {
  local file_path="$1"
  local directory
  directory="$(cd "$(dirname "${file_path}")" && pwd -P)"
  printf '%s/%s\n' "${directory}" "$(basename "${file_path}")"
}

blocked() {
  local result="$1"
  local exit_code="${2:-2}"
  FINAL_RESULT="${result}"
  echo "P3_RESULT: ${result}"
  echo "FAILED_STAGE: ${CURRENT_STAGE}"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit "${exit_code}"
}

run_bounded() {
  local timeout_seconds="$1"
  shift
  local marker_file="${TMP_DIR}/bounded-timeout-$RANDOM"
  "$@" &
  local command_pid=$!
  (
    sleep "${timeout_seconds}"
    if kill -0 "${command_pid}" >/dev/null 2>&1; then
      printf 'TIMEOUT\n' >"${marker_file}"
      kill "${command_pid}" >/dev/null 2>&1 || true
    fi
  ) &
  local watchdog_pid=$!
  set +e
  wait "${command_pid}"
  local command_status=$?
  set -e
  kill "${watchdog_pid}" >/dev/null 2>&1 || true
  wait "${watchdog_pid}" >/dev/null 2>&1 || true
  if [ -f "${marker_file}" ]; then
    return 124
  fi
  return "${command_status}"
}

stop_application() {
  if [ -n "${APP_PID}" ] && kill -0 "${APP_PID}" >/dev/null 2>&1; then
    kill "${APP_PID}" >/dev/null 2>&1 || true
    local stop_wait=0
    while kill -0 "${APP_PID}" >/dev/null 2>&1 && [ "${stop_wait}" -lt 15 ]; do
      sleep 1
      stop_wait=$((stop_wait + 1))
    done
    if kill -0 "${APP_PID}" >/dev/null 2>&1; then
      kill -9 "${APP_PID}" >/dev/null 2>&1 || true
    fi
    wait "${APP_PID}" >/dev/null 2>&1 || true
  fi
  APP_PID=""
}

remove_container() {
  if [ "${CONTAINER_STARTED}" -eq 1 ]; then
    if run_bounded 30 docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1; then
      CONTAINER_CLEANUP="PASS"
    else
      CONTAINER_CLEANUP="FAIL"
    fi
    CONTAINER_STARTED=0
  fi
}

cleanup() {
  local status=$?
  stop_application
  remove_container
  if [ -n "${TMP_DIR}" ] && [ -d "${TMP_DIR}" ]; then
    rm -rf "${TMP_DIR}"
  fi
  if [ "${status}" -ne 0 ] && [ -d "${EVIDENCE_DIR}" ]; then
    {
      echo "P3_RESULT: ${FINAL_RESULT}"
      echo "FAILED_STAGE: ${CURRENT_STAGE}"
      echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
      echo "PRODUCTION_READINESS: BLOCKED"
    } >"${EVIDENCE_DIR}/summary.txt"
  fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

safe_prepare_evidence_dir() {
  case "${EVIDENCE_DIR}" in
    "${ROOT_DIR}/.runtime/postgresql-p3-rehearsal")
      rm -rf "${EVIDENCE_DIR}"
      mkdir -p "${BACKUP_DIR}"
      ;;
    *) blocked "BLOCKED_UNSAFE_EVIDENCE_PATH" ;;
  esac
}

path_is_allowed() {
  local path="$1"
  case "${path}" in
    "${ROOT_DIR}/.runtime/p3-input/"*) return 0 ;;
    "${ROOT_DIR}/"*) return 1 ;;
    *) return 0 ;;
  esac
}

docker_psql() {
  local database="$1"
  shift
  run_bounded 180 docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${CONTAINER_NAME}" psql \
    --username="${USERNAME}" \
    --dbname="${database}" \
    --no-psqlrc "$@"
}

scalar_query() {
  local database="$1"
  local sql="$2"
  docker_psql "${database}" -Atqc "${sql}"
}

capture_fingerprint() {
  local database="$1"
  local output_file="$2"
  run_bounded 180 docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${CONTAINER_NAME}" psql \
    --username="${USERNAME}" \
    --dbname="${database}" \
    --no-psqlrc \
    <"${ROOT_DIR}/scripts/current-state-clone-fingerprint.sql" \
    >"${output_file}"
}

capture_restore_verification() {
  local database="$1"
  local output_file="$2"
  run_bounded 180 docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${CONTAINER_NAME}" psql \
    --username="${USERNAME}" \
    --dbname="${database}" \
    --no-psqlrc \
    <"${ROOT_DIR}/scripts/current-state-clone-restore-verification.sql" \
    >"${output_file}"
}

verification_value() {
  local key="$1"
  local file="$2"
  awk -F '|' -v key="${key}" '$1 == key { print $2; exit }' "${file}"
}

require_zero_verification() {
  local key="$1"
  local file="$2"
  local value
  value="$(verification_value "${key}" "${file}")"
  if [ "${value}" != "0" ]; then
    echo "${key}: BLOCKED_NONZERO_OR_MISSING"
    blocked "RESTORE_DATA_INTEGRITY_MISMATCH"
  fi
}

run_inventory() {
  local database="$1"
  local output_file="$2"
  HISTORICAL_TIME_INVENTORY_HOST="${TARGET_HOST}" \
  HISTORICAL_TIME_INVENTORY_PORT="${TARGET_PORT}" \
  HISTORICAL_TIME_INVENTORY_DATABASE="${database}" \
  HISTORICAL_TIME_INVENTORY_USERNAME="${USERNAME}" \
  HISTORICAL_TIME_INVENTORY_PASSWORD="${DISPOSABLE_PASSWORD}" \
  HISTORICAL_TIME_INVENTORY_DATABASE_CLASS="SANITIZED_REHEARSAL" \
  HISTORICAL_TIME_INVENTORY_CONFIRM="I_CONFIRM_READ_ONLY_NON_PRODUCTION_DATABASE" \
    bash "${ROOT_DIR}/scripts/historical-time-basis-inventory.sh" >"${output_file}"
}

run_flyway_action() {
  local database="$1"
  local action="$2"
  P3_CONTROLLED_POSTGRESQL_JDBC_URL="jdbc:postgresql://${TARGET_HOST}:${TARGET_PORT}/${database}" \
  P3_CONTROLLED_POSTGRESQL_USERNAME="${USERNAME}" \
  P3_CONTROLLED_POSTGRESQL_PASSWORD="${DISPOSABLE_PASSWORD}" \
  P3_CONTROLLED_POSTGRESQL_DATABASE="${database}" \
  P3_CONTROLLED_SOURCE_FLYWAY_VERSION="${SOURCE_FLYWAY_VERSION:-}" \
  P3_CONTROLLED_FLYWAY_ACTION="${action}" \
  P3_CONTROLLED_FLYWAY_CONFIRM="I_CONFIRM_LOCAL_P3_FLYWAY_ACTION" \
    ./mvnw -q -Dtest=ControlledCurrentStateCloneFlywayActionTest test
}

capture_table_counts() {
  local fingerprint_file="$1"
  local output_file="$2"
  grep '^TABLE_ROW_COUNT|' "${fingerprint_file}" | sort >"${output_file}"
}

required_vars=(
  P3_SANITIZED_DUMP_FILE
  P3_SANITIZATION_ATTESTATION_FILE
  P3_DATASET_ID
  P3_DATASET_CLASS
  P3_CONFIRM
  P3_LOCAL_DB_RECREATE_CONFIRM
)
for variable_name in "${required_vars[@]}"; do
  if [ -n "${!variable_name:-}" ]; then
    echo "${variable_name}: PRESENT_REDACTED"
  else
    echo "${variable_name}: MISSING"
  fi
done

if [ -z "${P3_SANITIZED_DUMP_FILE:-}" ]; then
  echo "SOURCE_DATASET_STATUS: BLOCKED_MISSING_SANITIZED_RELEASE_LIKE_DUMP"
  echo "P3_RESULT: BLOCKED_MISSING_SANITIZED_RELEASE_LIKE_DUMP"
  echo "DATABASE_ACCESS: NOT_ATTEMPTED"
  echo "DOCKER_ACTION: NOT_ATTEMPTED"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit 0
fi
if [ -z "${P3_SANITIZATION_ATTESTATION_FILE:-}" ]; then
  blocked "BLOCKED_MISSING_SANITIZATION_ATTESTATION"
fi
if [ -z "${P3_DATASET_ID:-}" ]; then
  blocked "BLOCKED_MISSING_DATASET_ID"
fi
if [ "${P3_DATASET_CLASS:-}" != "SANITIZED_RELEASE_LIKE" ]; then
  blocked "BLOCKED_INVALID_DATASET_CLASS"
fi
if [ "${P3_CONFIRM:-}" != "I_CONFIRM_SANITIZED_NON_PRODUCTION_RELEASE_LIKE_DATASET" ]; then
  blocked "BLOCKED_SANITIZED_DATASET_CONFIRMATION_REQUIRED"
fi
if [ "${P3_LOCAL_DB_RECREATE_CONFIRM:-}" != "I_UNDERSTAND_ONLY_LOCAL_P3_DATABASES_ARE_DROPPED" ]; then
  blocked "BLOCKED_LOCAL_RECREATE_CONFIRMATION_REQUIRED"
fi
if ! printf '%s' "${P3_DATASET_ID}" | grep -Eq '^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$'; then
  blocked "BLOCKED_INVALID_DATASET_ID"
fi
if printf '%s' "${P3_DATASET_ID}" | grep -Eiq '(prod|production|live|primary|customer|client|host)'; then
  blocked "BLOCKED_SENSITIVE_OR_PRODUCTION_LIKE_DATASET_ID"
fi

if [ "${TARGET_HOST}" != "127.0.0.1" ]; then
  blocked "BLOCKED_NON_LOCALHOST_TARGET"
fi
if [ "${TARGET_PORT}" != "55433" ]; then
  blocked "BLOCKED_UNAPPROVED_LOCAL_PORT"
fi
if [ "${SOURCE_DATABASE}" != "trade_model_v1_p3_source" ] \
  || [ "${REHEARSAL_DATABASE}" != "trade_model_v1_p3_rehearsal" ] \
  || [ "${RECOVERY_DATABASE}" != "trade_model_v1_p3_recovery" ]; then
  blocked "BLOCKED_UNAPPROVED_DATABASE_NAME"
fi
for database_name in "${SOURCE_DATABASE}" "${REHEARSAL_DATABASE}" "${RECOVERY_DATABASE}"; do
  if printf '%s' "${database_name}" | grep -Eiq '(prod|production|live|primary|main)'; then
    blocked "BLOCKED_PRODUCTION_DATABASE_INDICATOR"
  fi
done

case "${P3_SANITIZED_DUMP_FILE}" in
  /*) ;;
  *) blocked "BLOCKED_INVALID_DUMP_PATH" ;;
esac
case "${P3_SANITIZATION_ATTESTATION_FILE}" in
  /*) ;;
  *) blocked "BLOCKED_INVALID_ATTESTATION_PATH" ;;
esac
if [ ! -f "${P3_SANITIZED_DUMP_FILE}" ]; then
  blocked "BLOCKED_DUMP_FILE_NOT_FOUND"
fi
if [ ! -f "${P3_SANITIZATION_ATTESTATION_FILE}" ]; then
  blocked "BLOCKED_ATTESTATION_FILE_NOT_FOUND"
fi

DUMP_FILE="$(canonical_existing_file "${P3_SANITIZED_DUMP_FILE}")"
ATTESTATION_FILE="$(canonical_existing_file "${P3_SANITIZATION_ATTESTATION_FILE}")"
if ! path_is_allowed "${DUMP_FILE}"; then
  blocked "BLOCKED_DUMP_PATH_INSIDE_REPOSITORY"
fi
if ! path_is_allowed "${ATTESTATION_FILE}"; then
  blocked "BLOCKED_ATTESTATION_PATH_INSIDE_REPOSITORY"
fi

attestation_keys=(
  DATA_SOURCE_CLASS
  SANITIZATION_OWNER_OR_PROCESS
  GENERATED_AT_UTC
  SOURCE_POSTGRESQL_VERSION
  SOURCE_FLYWAY_VERSION
  USER_IDENTIFIERS_REMOVED_OR_PSEUDONYMIZED
  SECRETS_REMOVED
  FREE_TEXT_CLEANED_OR_REPLACED
  LOCAL_CONTROLLED_REHEARSAL_ALLOWED
  NOT_PRODUCTION_AND_NOT_FOR_PRODUCTION_RESTORE
)
for attestation_key in "${attestation_keys[@]}"; do
  if ! grep -Eq "^${attestation_key}=[^[:space:]].*$" "${ATTESTATION_FILE}"; then
    blocked "BLOCKED_SANITIZATION_ATTESTATION_INCOMPLETE"
  fi
done
if ! grep -Eq '^DATA_SOURCE_CLASS=SANITIZED_RELEASE_LIKE$' "${ATTESTATION_FILE}" \
  || ! grep -Eq '^USER_IDENTIFIERS_REMOVED_OR_PSEUDONYMIZED=YES$' "${ATTESTATION_FILE}" \
  || ! grep -Eq '^SECRETS_REMOVED=YES$' "${ATTESTATION_FILE}" \
  || ! grep -Eq '^FREE_TEXT_CLEANED_OR_REPLACED=YES$' "${ATTESTATION_FILE}" \
  || ! grep -Eq '^LOCAL_CONTROLLED_REHEARSAL_ALLOWED=YES$' "${ATTESTATION_FILE}" \
  || ! grep -Eq '^NOT_PRODUCTION_AND_NOT_FOR_PRODUCTION_RESTORE=YES$' "${ATTESTATION_FILE}"; then
  blocked "BLOCKED_SANITIZATION_ATTESTATION_MISMATCH"
fi

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/postgresql-p3-rehearsal.XXXXXX")"
for command_name in docker pg_dump pg_restore psql curl java; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    blocked "BLOCKED_REQUIRED_TOOL_MISSING"
  fi
done

CURRENT_STAGE="dump-format-validation"
if ! run_bounded 120 pg_restore --list "${DUMP_FILE}" >"${TMP_DIR}/dump-list.txt" 2>"${TMP_DIR}/dump-list-error.txt"; then
  blocked "BLOCKED_INVALID_POSTGRESQL_CUSTOM_DUMP"
fi
if grep -Eiq '(DATABASE PROPERTIES|CREATE DATABASE)' "${TMP_DIR}/dump-list.txt"; then
  blocked "BLOCKED_DUMP_CONTAINS_DATABASE_CREATION"
fi

DUMP_SHA256="$(sha256_file "${DUMP_FILE}")"
ATTESTATION_SHA256="$(sha256_file "${ATTESTATION_FILE}")"
DATASET_ID_SHA256="$(printf '%s' "${P3_DATASET_ID}" | sha256_text)"

CURRENT_STAGE="docker-preflight"
if ! run_bounded 30 docker info >/dev/null 2>&1; then
  blocked "BLOCKED_DOCKER_DAEMON_UNAVAILABLE"
fi
if ! run_bounded 30 docker ps --format '{{.Ports}}' >"${TMP_DIR}/docker-ports.txt" 2>/dev/null; then
  blocked "BLOCKED_DOCKER_DAEMON_UNAVAILABLE"
fi
if grep -q "127.0.0.1:${TARGET_PORT}->" "${TMP_DIR}/docker-ports.txt"; then
  blocked "BLOCKED_LOCAL_PORT_IN_USE"
fi

safe_prepare_evidence_dir
cd "${ROOT_DIR}"

{
  echo "DATASET_CLASS: SANITIZED_RELEASE_LIKE"
  echo "DATASET_ID_SHA256: ${DATASET_ID_SHA256}"
  echo "ATTESTATION_SHA256: ${ATTESTATION_SHA256}"
  echo "ATTESTATION_REQUIRED_FIELDS: PASS"
  echo "ATTESTATION_CONTENT: NOT_COPIED"
} >"${EVIDENCE_DIR}/input-attestation-summary.txt"
{
  echo "DUMP_FORMAT: POSTGRESQL_CUSTOM"
  echo "DUMP_SIZE_BYTES: $(file_size_bytes "${DUMP_FILE}")"
  echo "DUMP_SHA256: ${DUMP_SHA256}"
  echo "DUMP_PATH: REDACTED_NOT_RECORDED"
} >"${EVIDENCE_DIR}/input-dump-metadata.txt"

CURRENT_STAGE="container-start"
DISPOSABLE_PASSWORD="p3-$(openssl rand -hex 24 2>/dev/null || printf '%s-%s-%s' "$RANDOM" "$RANDOM" "$(date -u +%s)")"
DISPOSABLE_ADMIN_PASSWORD="p3-admin-$(openssl rand -hex 24 2>/dev/null || printf '%s-%s' "$RANDOM" "$(date -u +%s)")"
if ! run_bounded 300 docker run \
  --name "${CONTAINER_NAME}" \
  --env "POSTGRES_DB=postgres" \
  --env "POSTGRES_USER=${USERNAME}" \
  --env "POSTGRES_PASSWORD=${DISPOSABLE_PASSWORD}" \
  --publish "127.0.0.1:${TARGET_PORT}:5432" \
  --detach "${IMAGE_REF}" >/dev/null; then
  blocked "BLOCKED_CONTAINER_START_FAILED"
fi
CONTAINER_STARTED=1

ready=0
for _ in $(seq 1 90); do
  if run_bounded 5 docker exec "${CONTAINER_NAME}" pg_isready \
    -U "${USERNAME}" -d postgres >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done
if [ "${ready}" -ne 1 ]; then
  blocked "BLOCKED_POSTGRESQL_READINESS_TIMEOUT"
fi

CURRENT_STAGE="local-database-create"
for database_name in "${SOURCE_DATABASE}" "${REHEARSAL_DATABASE}" "${RECOVERY_DATABASE}"; do
  if ! run_bounded 30 docker exec "${CONTAINER_NAME}" createdb -U "${USERNAME}" "${database_name}"; then
    blocked "BLOCKED_LOCAL_DATABASE_CREATE_FAILED"
  fi
done

CURRENT_STAGE="source-restore"
if ! run_bounded 600 env \
  RESTORE_DATASOURCE_HOST="${TARGET_HOST}" \
  RESTORE_DATASOURCE_PORT="${TARGET_PORT}" \
  RESTORE_DATASOURCE_USERNAME="${USERNAME}" \
  RESTORE_DATASOURCE_PASSWORD="${DISPOSABLE_PASSWORD}" \
  RESTORE_DATASOURCE_DATABASE="${SOURCE_DATABASE}" \
  RESTORE_BACKUP_FILE="${DUMP_FILE}" \
  RESTORE_CONFIRM="I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA" \
    bash "${ROOT_DIR}/scripts/prod-restore.sh" \
      >"${TMP_DIR}/source-restore.log" 2>"${TMP_DIR}/source-restore-error.log"; then
  blocked "BLOCKED_SOURCE_RESTORE_FAILED"
fi

CURRENT_STAGE="source-identity"
if [ "$(scalar_query "${SOURCE_DATABASE}" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='flyway_schema_history'")" != "1" ]; then
  blocked "BLOCKED_MISSING_FLYWAY_HISTORY"
fi
SOURCE_FLYWAY_VERSION="$(scalar_query "${SOURCE_DATABASE}" "SELECT COALESCE(MAX(version::integer),0) FROM flyway_schema_history WHERE success")"
FAILED_MIGRATIONS="$(scalar_query "${SOURCE_DATABASE}" "SELECT COUNT(*) FROM flyway_schema_history WHERE NOT success")"
if [ "${FAILED_MIGRATIONS}" != "0" ]; then
  blocked "BLOCKED_FAILED_FLYWAY_HISTORY"
fi
case "${SOURCE_FLYWAY_VERSION}" in
  6|7) ;;
  *) blocked "BLOCKED_UNSUPPORTED_SOURCE_FLYWAY_VERSION" ;;
esac

UNKNOWN_EXTENSIONS="$(scalar_query "${SOURCE_DATABASE}" "SELECT COUNT(*) FROM pg_extension WHERE extname <> 'plpgsql'")"
FOREIGN_SERVERS="$(scalar_query "${SOURCE_DATABASE}" "SELECT COUNT(*) FROM pg_foreign_server")"
UNAPPROVED_ROLES="$(scalar_query "${SOURCE_DATABASE}" "SELECT COUNT(*) FROM pg_roles WHERE rolname NOT LIKE 'pg_%' AND rolname NOT IN ('postgres','${USERNAME}')")"
UNAPPROVED_FUNCTION_LANGUAGES="$(scalar_query "${SOURCE_DATABASE}" "SELECT COUNT(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace JOIN pg_language l ON l.oid=p.prolang WHERE n.nspname='public' AND p.prokind='f' AND l.lanname NOT IN ('sql','plpgsql','internal')")"
NETWORK_FUNCTION_CANDIDATES="$(scalar_query "${SOURCE_DATABASE}" "SELECT COUNT(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public' AND p.prokind='f' AND pg_get_functiondef(p.oid) ~* '(dblink|postgres_fdw|http[_a-z]*[[:space:]]*\\(|curl|socket)' ")"
if [ "${UNKNOWN_EXTENSIONS}" != "0" ] \
  || [ "${FOREIGN_SERVERS}" != "0" ] \
  || [ "${UNAPPROVED_ROLES}" != "0" ] \
  || [ "${UNAPPROVED_FUNCTION_LANGUAGES}" != "0" ] \
  || [ "${NETWORK_FUNCTION_CANDIDATES}" != "0" ]; then
  blocked "BLOCKED_UNAPPROVED_DATABASE_CAPABILITY"
fi

if ! run_bounded 300 run_flyway_action "${SOURCE_DATABASE}" "VALIDATE" \
  >"${TMP_DIR}/source-flyway-validate.log" 2>&1; then
  blocked "BLOCKED_FLYWAY_CHECKSUM_OR_VALIDATE_FAILURE"
fi

POSTGRESQL_VERSION="$(scalar_query "${SOURCE_DATABASE}" "SHOW server_version")"
TABLE_COUNT="$(scalar_query "${SOURCE_DATABASE}" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")"
TM_TABLE_COUNT="$(scalar_query "${SOURCE_DATABASE}" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name LIKE 'tm\\_%' ESCAPE '\\'")"
{
  echo "POSTGRESQL_VERSION: ${POSTGRESQL_VERSION}"
  echo "DATABASE_IDENTITY: LOCAL_P3_SOURCE"
  echo "CURRENT_USER_CLASS: LOCAL_P3_OPERATOR"
  echo "SESSION_TIMEZONE: $(scalar_query "${SOURCE_DATABASE}" "SHOW timezone")"
  echo "TRANSACTION_READ_ONLY: $(scalar_query "${SOURCE_DATABASE}" "SHOW transaction_read_only")"
  echo "TABLE_COUNT: ${TABLE_COUNT}"
  echo "TM_TABLE_COUNT: ${TM_TABLE_COUNT}"
  echo "FLYWAY_VERSION: ${SOURCE_FLYWAY_VERSION}"
  echo "FAILED_MIGRATIONS: ${FAILED_MIGRATIONS}"
  echo "UNKNOWN_EXTENSIONS: ${UNKNOWN_EXTENSIONS}"
  echo "FOREIGN_SERVERS: ${FOREIGN_SERVERS}"
  echo "UNAPPROVED_ROLES: ${UNAPPROVED_ROLES}"
  echo "UNAPPROVED_FUNCTION_LANGUAGES: ${UNAPPROVED_FUNCTION_LANGUAGES}"
  echo "NETWORK_FUNCTION_CANDIDATES: ${NETWORK_FUNCTION_CANDIDATES}"
  echo "FLYWAY_VALIDATE: PASS"
} >"${EVIDENCE_DIR}/source-identity.txt"
docker_psql "${SOURCE_DATABASE}" -AtF '|' -c \
  "SELECT installed_rank,version,description,success,checksum FROM flyway_schema_history ORDER BY installed_rank" \
  >"${EVIDENCE_DIR}/flyway-before.txt"

CURRENT_STAGE="source-aggregate-validation"
capture_fingerprint "${SOURCE_DATABASE}" "${EVIDENCE_DIR}/source-fingerprint.txt"
capture_restore_verification "${SOURCE_DATABASE}" "${TMP_DIR}/source-verification.txt"
for verification_key in \
  DECISION_WITHOUT_ANALYSIS \
  EXECUTION_PLAN_WITHOUT_ANALYSIS \
  POSITION_MONITOR_WITHOUT_POSITION \
  POSITION_MONITOR_PLAN_ANALYSIS_MISMATCH \
  TYPED_POSITION_PLAN_REFERENCE_MISMATCH \
  DUPLICATE_ANALYSIS_ID \
  DUPLICATE_DECISION_ID \
  DUPLICATE_PLAN_ID; do
  require_zero_verification "${verification_key}" "${TMP_DIR}/source-verification.txt"
done
for sanitization_key in SECRET_CANDIDATE_TOTAL PII_CANDIDATE_TOTAL PRODUCTION_REFERENCE_CANDIDATE_TOTAL; do
  if [ "$(verification_value "${sanitization_key}" "${TMP_DIR}/source-verification.txt")" != "0" ]; then
    blocked "BLOCKED_SANITIZATION_ATTESTATION_MISMATCH"
  fi
done
if ! run_bounded 180 run_inventory "${SOURCE_DATABASE}" "${EVIDENCE_DIR}/source-inventory.txt"; then
  blocked "BLOCKED_SOURCE_INVENTORY_FAILED"
fi
if ! grep -q '^HISTORICAL_TIME_INVENTORY_RESULT: PASS_READ_ONLY$' "${EVIDENCE_DIR}/source-inventory.txt"; then
  blocked "BLOCKED_SOURCE_INVENTORY_FAILED"
fi

SOURCE_FINGERPRINT="$(sha256_file "${EVIDENCE_DIR}/source-fingerprint.txt")"
SOURCE_INVENTORY_FINGERPRINT="$(awk -F '|' '$1 == "AGGREGATE_MD5" {print $2; exit}' "${EVIDENCE_DIR}/source-inventory.txt")"

CURRENT_STAGE="controlled-backup"
BACKUP_FILE="${BACKUP_DIR}/p3-current-state.dump"
BACKUP_STARTED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
if ! run_bounded 600 env \
  PROD_DATASOURCE_HOST="${TARGET_HOST}" \
  PROD_DATASOURCE_PORT="${TARGET_PORT}" \
  PROD_DATASOURCE_USERNAME="${USERNAME}" \
  PROD_DATASOURCE_PASSWORD="${DISPOSABLE_PASSWORD}" \
  PROD_DATASOURCE_DATABASE="${SOURCE_DATABASE}" \
  BACKUP_DIR="${BACKUP_DIR}" \
  BACKUP_FILE="${BACKUP_FILE}" \
    bash "${ROOT_DIR}/scripts/prod-backup.sh" \
      >"${TMP_DIR}/backup.log" 2>"${TMP_DIR}/backup-error.log"; then
  blocked "BLOCKED_CONTROLLED_BACKUP_FAILED"
fi
BACKUP_COMPLETED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
BACKUP_SHA256="$(sha256_file "${BACKUP_FILE}")"
if ! run_bounded 120 pg_restore --list "${BACKUP_FILE}" >/dev/null 2>&1; then
  blocked "BLOCKED_BACKUP_FORMAT_VALIDATION_FAILED"
fi
{
  echo "BACKUP_STATUS: PASS"
  echo "BACKUP_STARTED_AT_UTC: ${BACKUP_STARTED_AT}"
  echo "BACKUP_COMPLETED_AT_UTC: ${BACKUP_COMPLETED_AT}"
  echo "PG_DUMP_VERSION_CLASS: $(pg_dump --version | awk '{print $3}')"
  echo "BACKUP_FORMAT: POSTGRESQL_CUSTOM"
  echo "BACKUP_FILE: p3-current-state.dump"
  echo "BACKUP_SIZE_BYTES: $(file_size_bytes "${BACKUP_FILE}")"
  echo "BACKUP_SHA256: ${BACKUP_SHA256}"
  echo "SOURCE_FLYWAY_VERSION: ${SOURCE_FLYWAY_VERSION}"
  echo "SOURCE_FINGERPRINT: ${SOURCE_FINGERPRINT}"
} >"${EVIDENCE_DIR}/backup-metadata.txt"

restore_backup_to_database() {
  local target_database="$1"
  local log_prefix="$2"
  run_bounded 600 env \
    RESTORE_DATASOURCE_HOST="${TARGET_HOST}" \
    RESTORE_DATASOURCE_PORT="${TARGET_PORT}" \
    RESTORE_DATASOURCE_USERNAME="${USERNAME}" \
    RESTORE_DATASOURCE_PASSWORD="${DISPOSABLE_PASSWORD}" \
    RESTORE_DATASOURCE_DATABASE="${target_database}" \
    RESTORE_BACKUP_FILE="${BACKUP_FILE}" \
    RESTORE_CONFIRM="I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA" \
      bash "${ROOT_DIR}/scripts/prod-restore.sh" \
      >"${TMP_DIR}/${log_prefix}.log" 2>"${TMP_DIR}/${log_prefix}-error.log"
}

CURRENT_STAGE="recovery-restore"
if ! restore_backup_to_database "${RECOVERY_DATABASE}" "recovery-restore"; then
  blocked "BLOCKED_RECOVERY_RESTORE_FAILED"
fi
capture_fingerprint "${RECOVERY_DATABASE}" "${EVIDENCE_DIR}/recovery-fingerprint.txt"
RECOVERY_FINGERPRINT="$(sha256_file "${EVIDENCE_DIR}/recovery-fingerprint.txt")"
if [ "${RECOVERY_FINGERPRINT}" != "${SOURCE_FINGERPRINT}" ]; then
  blocked "RESTORE_DATA_INTEGRITY_MISMATCH"
fi
capture_restore_verification "${RECOVERY_DATABASE}" "${TMP_DIR}/recovery-verification.txt"
if ! cmp -s "${TMP_DIR}/source-verification.txt" "${TMP_DIR}/recovery-verification.txt"; then
  blocked "RESTORE_DATA_INTEGRITY_MISMATCH"
fi
if ! run_bounded 180 run_inventory "${RECOVERY_DATABASE}" "${TMP_DIR}/recovery-inventory.txt"; then
  blocked "BLOCKED_RECOVERY_INVENTORY_FAILED"
fi
RECOVERY_INVENTORY_FINGERPRINT="$(awk -F '|' '$1 == "AGGREGATE_MD5" {print $2; exit}' "${TMP_DIR}/recovery-inventory.txt")"
if [ -z "${SOURCE_INVENTORY_FINGERPRINT}" ] \
  || [ "${SOURCE_INVENTORY_FINGERPRINT}" != "${RECOVERY_INVENTORY_FINGERPRINT}" ]; then
  blocked "RESTORE_DATA_INTEGRITY_MISMATCH"
fi
if ! run_bounded 300 run_flyway_action "${RECOVERY_DATABASE}" "VALIDATE" \
  >"${TMP_DIR}/recovery-flyway-validate.log" 2>&1; then
  blocked "BLOCKED_RECOVERY_FLYWAY_VALIDATE_FAILURE"
fi

CURRENT_STAGE="rehearsal-restore"
if ! restore_backup_to_database "${REHEARSAL_DATABASE}" "rehearsal-restore"; then
  blocked "BLOCKED_REHEARSAL_RESTORE_FAILED"
fi
capture_fingerprint "${REHEARSAL_DATABASE}" "${EVIDENCE_DIR}/rehearsal-fingerprint-before.txt"
REHEARSAL_BEFORE_FINGERPRINT="$(sha256_file "${EVIDENCE_DIR}/rehearsal-fingerprint-before.txt")"
if [ "${REHEARSAL_BEFORE_FINGERPRINT}" != "${SOURCE_FINGERPRINT}" ]; then
  blocked "RESTORE_DATA_INTEGRITY_MISMATCH"
fi
capture_table_counts "${EVIDENCE_DIR}/source-fingerprint.txt" "${TMP_DIR}/source-table-counts.txt"

CURRENT_STAGE="rehearsal-flyway"
FLYWAY_ROWS_BEFORE="$(scalar_query "${REHEARSAL_DATABASE}" "SELECT COUNT(*) FROM flyway_schema_history WHERE success")"
if ! run_bounded 600 run_flyway_action "${REHEARSAL_DATABASE}" "MIGRATE" \
  >"${TMP_DIR}/rehearsal-flyway.log" 2>&1; then
  blocked "BLOCKED_REHEARSAL_FLYWAY_FAILURE"
fi
REHEARSAL_FLYWAY_VERSION="$(scalar_query "${REHEARSAL_DATABASE}" "SELECT MAX(version::integer) FROM flyway_schema_history WHERE success")"
FLYWAY_ROWS_AFTER="$(scalar_query "${REHEARSAL_DATABASE}" "SELECT COUNT(*) FROM flyway_schema_history WHERE success")"
if [ "${REHEARSAL_FLYWAY_VERSION}" != "7" ]; then
  blocked "BLOCKED_REHEARSAL_FLYWAY_VERSION"
fi
if [ "${SOURCE_FLYWAY_VERSION}" = "6" ]; then
  if [ "$((FLYWAY_ROWS_AFTER - FLYWAY_ROWS_BEFORE))" -ne 1 ]; then
    blocked "BLOCKED_UNEXPECTED_MIGRATION_COUNT"
  fi
  if [ "$(scalar_query "${REHEARSAL_DATABASE}" "SELECT COUNT(*) FROM tm_decision_result WHERE valid_from IS NOT NULL OR expires_at IS NOT NULL")" != "0" ]; then
    blocked "BLOCKED_HISTORICAL_VALIDITY_REWRITE"
  fi
  MIGRATION_PATH="V6_TO_V7"
else
  if [ "$((FLYWAY_ROWS_AFTER - FLYWAY_ROWS_BEFORE))" -ne 0 ]; then
    blocked "BLOCKED_UNEXPECTED_MIGRATION_COUNT"
  fi
  MIGRATION_PATH="V7_VALIDATE_IDEMPOTENT"
fi

capture_fingerprint "${REHEARSAL_DATABASE}" "${EVIDENCE_DIR}/rehearsal-fingerprint-after.txt"
capture_table_counts "${EVIDENCE_DIR}/rehearsal-fingerprint-after.txt" "${TMP_DIR}/rehearsal-table-counts.txt"
if ! cmp -s "${TMP_DIR}/source-table-counts.txt" "${TMP_DIR}/rehearsal-table-counts.txt"; then
  blocked "BLOCKED_POST_MIGRATION_BUSINESS_ROW_CHANGE"
fi
POST_MIGRATION_FINGERPRINT="$(sha256_file "${EVIDENCE_DIR}/rehearsal-fingerprint-after.txt")"
docker_psql "${REHEARSAL_DATABASE}" -AtF '|' -c \
  "SELECT installed_rank,version,description,success,checksum FROM flyway_schema_history ORDER BY installed_rank" \
  >"${EVIDENCE_DIR}/flyway-after.txt"

CURRENT_STAGE="application-smoke"
capture_table_counts "${EVIDENCE_DIR}/rehearsal-fingerprint-after.txt" "${TMP_DIR}/app-before-table-counts.txt"
export SPRING_DATASOURCE_URL="jdbc:postgresql://${TARGET_HOST}:${TARGET_PORT}/${REHEARSAL_DATABASE}"
export SPRING_DATASOURCE_USERNAME="${USERNAME}"
export SPRING_DATASOURCE_PASSWORD="${DISPOSABLE_PASSWORD}"
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
export SPRING_FLYWAY_ENABLED=true
export SPRING_SQL_INIT_MODE=never
export APP_ADMIN_USERNAME=p3_controlled_admin
export APP_ADMIN_PASSWORD="${DISPOSABLE_ADMIN_PASSWORD}"
export TRADE_MODEL_AUTH_ENABLED=true
export TRADE_MODEL_SCHEDULERS_ENABLED=false
export TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED=false
export TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED=false
export TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED=false
export TRADE_MODEL_WATCHLIST_SCHEDULER_ENABLED=false
export TRADE_MODEL_ANALYSIS_SCHEDULER_ENABLED=false
export TRADE_MODEL_PROVIDER_CALL_ENABLED=false
export TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED=false
export TRADE_MODEL_PROFILE_ESCALATION_ENABLED=false
export TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED=false
export TRADE_MODEL_PROVIDER_AUTO_ESCALATION_ENABLED=false
export TRADE_MODEL_COINGLASS_ENABLED=false
export TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED=false
export TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED=false
export TRADE_MODEL_PUBLIC_OHLCV_EXTERNAL_CALLS_ENABLED=false
export TRADE_MODEL_AI_ENABLED=false
export TRADE_MODEL_AI_OPENAI_ENABLED=false
export TRADE_MODEL_AI_GEMINI_ENABLED=false
export TRADE_MODEL_AI_XAI_ENABLED=false

./mvnw -q -Pflyway-migration \
  -Dspring-boot.run.arguments="--server.address=127.0.0.1 --server.port=${APP_PORT}" \
  spring-boot:run >"${TMP_DIR}/application.log" 2>&1 &
APP_PID=$!

health_ready=0
for _ in $(seq 1 90); do
  if ! kill -0 "${APP_PID}" >/dev/null 2>&1; then
    blocked "BLOCKED_APPLICATION_STARTUP_FAILED"
  fi
  if curl --silent --fail --max-time 5 "http://127.0.0.1:${APP_PORT}/actuator/health" \
    >"${TMP_DIR}/health.json" 2>/dev/null; then
    health_ready=1
    break
  fi
  sleep 1
done
if [ "${health_ready}" -ne 1 ]; then
  blocked "BLOCKED_APPLICATION_STARTUP_TIMEOUT"
fi

CURL_CONFIG="${TMP_DIR}/curl-config"
chmod 700 "${TMP_DIR}"
{
  echo "user = \"${APP_ADMIN_USERNAME}:${APP_ADMIN_PASSWORD}\""
  echo "silent"
  echo "show-error"
  echo "connect-timeout = 5"
  echo "max-time = 10"
} >"${CURL_CONFIG}"
chmod 600 "${CURL_CONFIG}"

health_status="$(curl --config "${CURL_CONFIG}" --output "${TMP_DIR}/health-auth.json" --write-out '%{http_code}' "http://127.0.0.1:${APP_PORT}/actuator/health")"
home_status="$(curl --config "${CURL_CONFIG}" --output "${TMP_DIR}/dashboard-home.json" --write-out '%{http_code}' "http://127.0.0.1:${APP_PORT}/api/dashboard/home")"
baseline_status="$(curl --config "${CURL_CONFIG}" --output "${TMP_DIR}/run-baseline.json" --write-out '%{http_code}' "http://127.0.0.1:${APP_PORT}/api/system/run-baseline")"
if [ "${health_status}" != "200" ] || [ "${home_status}" != "200" ] || [ "${baseline_status}" != "200" ]; then
  blocked "BLOCKED_APPLICATION_ENDPOINT_SMOKE"
fi
if ! grep -q '"status":"UP"' "${TMP_DIR}/health-auth.json"; then
  blocked "BLOCKED_APPLICATION_HEALTH"
fi
for safety_field in notTradeInstruction notExecutable notAutoTrading notOrderExecution notUserPositionCreation notUserPositionMutation; do
  if ! grep -q "\"${safety_field}\":true" "${TMP_DIR}/dashboard-home.json"; then
    blocked "BLOCKED_DASHBOARD_SAFETY_FIELD"
  fi
done
if ! grep -Eq '"runStatus":"(NOT_CALLED|DISABLED)"' "${TMP_DIR}/dashboard-home.json"; then
  blocked "BLOCKED_AI_NOT_DISABLED"
fi

stop_application
capture_fingerprint "${REHEARSAL_DATABASE}" "${TMP_DIR}/app-after-fingerprint.txt"
capture_table_counts "${TMP_DIR}/app-after-fingerprint.txt" "${TMP_DIR}/app-after-table-counts.txt"
if ! cmp -s "${TMP_DIR}/app-before-table-counts.txt" "${TMP_DIR}/app-after-table-counts.txt"; then
  blocked "BLOCKED_UNEXPECTED_BUSINESS_WRITES"
fi
{
  echo "HEALTH: HTTP_200_UP"
  echo "DASHBOARD_HOME: HTTP_200_FAIL_CLOSED"
  echo "RUN_BASELINE: HTTP_200"
  echo "AI_PROVIDER_STATE: NOT_CALLED_OR_DISABLED"
  echo "SCHEDULERS: DISABLED"
  echo "EXTERNAL_PROVIDER_CALLS: DISABLED"
  echo "UNEXPECTED_BUSINESS_WRITES: 0"
} >"${EVIDENCE_DIR}/application-smoke.txt"

{
  echo "SOURCE_TO_RECOVERY_FINGERPRINT: MATCH"
  echo "SOURCE_TO_RECOVERY_INVENTORY: MATCH"
  echo "RECOVERY_FLYWAY_VERSION: ${SOURCE_FLYWAY_VERSION}"
  echo "RECOVERY_DATABASE_RESTORE: PASS"
  if [ "${SOURCE_FLYWAY_VERSION}" = "6" ]; then
    echo "PRIOR_APPLICATION_ARTIFACT_SMOKE: NOT_PROVEN_REQUIRES_SEPARATE_RELEASE_ARTIFACT"
  else
    echo "RECOVERY_APPLICATION_SMOKE: NOT_RUN_SEPARATE_READONLY_RESTORE_EVIDENCE"
  fi
} >"${EVIDENCE_DIR}/restore-verification.txt"
{
  echo "monitorAlertUtcWriterCutover: MISSING_OPERATIONAL_EVIDENCE"
  echo "hotResetUtcWriterCutover: MISSING_OPERATIONAL_EVIDENCE"
  echo "analysisTimeContractCutover: MISSING_OPERATIONAL_EVIDENCE"
  echo "LOCAL_CODE_STATUS: CODE_MERGED"
  echo "DEPLOYMENT_STATUS: NOT_PROVEN"
} >"${EVIDENCE_DIR}/cutover-register-summary.txt"

CURRENT_STAGE="container-cleanup"
remove_container
if [ "${CONTAINER_CLEANUP}" != "PASS" ]; then
  blocked "BLOCKED_CONTAINER_CLEANUP_FAILED"
fi

CURRENT_STAGE="summary"
EXECUTED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
{
  echo "P3_RESULT: PASS"
  echo "EXECUTED_AT_UTC: ${EXECUTED_AT}"
  echo "SOURCE_DATASET_STATUS: PASS_SANITIZED_RELEASE_LIKE_CLONE"
  echo "SOURCE_FLYWAY_VERSION: ${SOURCE_FLYWAY_VERSION}"
  echo "SOURCE_INVENTORY_STATUS: PASS_READ_ONLY_RELEASE_LIKE_CLONE"
  echo "SOURCE_FINGERPRINT: ${SOURCE_FINGERPRINT}"
  echo "BACKUP_STATUS: PASS"
  echo "BACKUP_SHA256: ${BACKUP_SHA256}"
  echo "RESTORE_STATUS: PASS"
  echo "RESTORE_FINGERPRINT_MATCH: MATCH"
  echo "MIGRATION_PATH: ${MIGRATION_PATH}"
  echo "MIGRATION_STATUS: PASS"
  echo "POST_MIGRATION_FINGERPRINT: ${POST_MIGRATION_FINGERPRINT}"
  echo "HISTORICAL_TIME_RESULTS: PASS_READ_ONLY_AGGREGATE"
  echo "APPLICATION_SMOKE_STATUS: PASS"
  echo "UNEXPECTED_BUSINESS_WRITES: 0"
  echo "RECOVERY_STATUS: PASS_RESTORED_PRE_MIGRATION_STATE"
  echo "WRITER_CUTOVER_STATUS: MISSING_OPERATIONAL_EVIDENCE"
  echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
  echo "PRODUCTION_READINESS: BLOCKED"
} >"${EVIDENCE_DIR}/summary.txt"

: >"${EVIDENCE_DIR}/checksums.txt"
for evidence_file in \
  summary.txt \
  input-attestation-summary.txt \
  input-dump-metadata.txt \
  source-identity.txt \
  flyway-before.txt \
  flyway-after.txt \
  source-fingerprint.txt \
  source-inventory.txt \
  backup-metadata.txt \
  restore-verification.txt \
  recovery-fingerprint.txt \
  rehearsal-fingerprint-before.txt \
  rehearsal-fingerprint-after.txt \
  application-smoke.txt \
  cutover-register-summary.txt; do
  echo "$(sha256_file "${EVIDENCE_DIR}/${evidence_file}")  ${evidence_file}" \
    >>"${EVIDENCE_DIR}/checksums.txt"
done

FINAL_RESULT="PASS"
cat "${EVIDENCE_DIR}/summary.txt"
echo "EVIDENCE_ARTIFACTS: .runtime/postgresql-p3-rehearsal"
