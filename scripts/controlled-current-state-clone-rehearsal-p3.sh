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
APPLICATION_DATABASE_ROLE=""
APPLICATION_DATABASE_PASSWORD=""
CONTAINER_STARTED=0
CONTAINER_CLEANUP="NOT_STARTED"
EVIDENCE_DIR_PREPARED=0
CURRENT_STAGE="input-preflight"
FINAL_RESULT="BLOCKED"
TMP_DIR=""
DATASET_CLASS="${P3_DATASET_CLASS:-}"
INVENTORY_DATABASE_CLASS="SANITIZED_REHEARSAL"
SOURCE_DATASET_SUCCESS_STATUS="PASS_SANITIZED_RELEASE_LIKE_CLONE"
SOURCE_INVENTORY_SUCCESS_STATUS="PASS_READ_ONLY_RELEASE_LIKE_CLONE"
SUCCESS_RESULT="PASS_SANITIZED_RELEASE_LIKE_REHEARSAL"
FINAL_SANITIZED_CLONE_GATE="EVIDENCE_COLLECTED_PENDING_REVIEW"
RECOVERY_SUCCESS_STATUS="PASS_RESTORED_PRE_MIGRATION_STATE"

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
  realpath "${file_path}"
}

path_contains_symlink_or_dot_component() {
  local file_path="$1"
  case "${file_path}" in
    */../*|*/..|*/./*|*/.) return 0 ;;
  esac
  local current=""
  local remainder="${file_path#/}"
  local component
  local old_ifs="${IFS}"
  IFS='/'
  for component in ${remainder}; do
    [ -n "${component}" ] || continue
    current="${current}/${component}"
    if [ -L "${current}" ]; then
      IFS="${old_ifs}"
      return 0
    fi
  done
  IFS="${old_ifs}"
  return 1
}

blocked() {
  local result="$1"
  local exit_code="${2:-2}"
  FINAL_RESULT="${result}"
  echo "P3_RESULT: ${result}"
  echo "FAILED_STAGE: ${CURRENT_STAGE}"
  echo "P3_FINAL_SANITIZED_CLONE_GATE: BLOCKED_NOT_RUN"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit "${exit_code}"
}

enter_stage() {
  CURRENT_STAGE="$1"
  echo "P3_STAGE: ${CURRENT_STAGE}"
}

wait_for_bounded_pid() {
  local command_pid="$1"
  local timeout_seconds="$2"
  local elapsed_ticks=0
  local max_ticks=$((timeout_seconds * 10))
  while kill -0 "${command_pid}" >/dev/null 2>&1 \
    && [ "${elapsed_ticks}" -lt "${max_ticks}" ]; do
    sleep 0.1
    elapsed_ticks=$((elapsed_ticks + 1))
  done
  if kill -0 "${command_pid}" >/dev/null 2>&1; then
    kill "${command_pid}" >/dev/null 2>&1 || true
    wait "${command_pid}" >/dev/null 2>&1 || true
    return 124
  fi
  set +e
  wait "${command_pid}"
  local command_status=$?
  set -e
  return "${command_status}"
}

run_bounded() {
  local timeout_seconds="$1"
  shift
  "$@" &
  wait_for_bounded_pid "$!" "${timeout_seconds}"
}

run_bounded_with_input() {
  local timeout_seconds="$1"
  local input_file="$2"
  shift 2
  "$@" <"${input_file}" &
  wait_for_bounded_pid "$!" "${timeout_seconds}"
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
  if [ "${status}" -ne 0 ] && [ "${EVIDENCE_DIR_PREPARED}" -eq 1 ]; then
    {
      echo "P3_RESULT: ${FINAL_RESULT}"
      echo "FAILED_STAGE: ${CURRENT_STAGE}"
      echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
      echo "P3_FINAL_SANITIZED_CLONE_GATE: BLOCKED_NOT_RUN"
      echo "P4_ALLOWED: NO"
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
      EVIDENCE_DIR_PREPARED=1
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

capture_structure_fingerprint() {
  local database="$1"
  local output_file="$2"
  run_bounded_with_input 180 "${ROOT_DIR}/scripts/current-state-clone-fingerprint.sql" \
    docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${CONTAINER_NAME}" psql \
    --username="${USERNAME}" \
    --dbname="${database}" \
    --no-psqlrc \
    >"${output_file}"
}

capture_content_fingerprint() {
  local database="$1"
  local mode="$2"
  local output_file="$3"
  run_bounded_with_input 180 \
    "${ROOT_DIR}/scripts/current-state-clone-content-fingerprint.sql" \
    docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${CONTAINER_NAME}" psql \
    --username="${USERNAME}" \
    --dbname="${database}" \
    --no-psqlrc \
    --set="fingerprint_mode=${mode}" \
    >"${output_file}"
}

capture_restore_verification() {
  local database="$1"
  local output_file="$2"
  run_bounded_with_input 180 \
    "${ROOT_DIR}/scripts/current-state-clone-restore-verification.sql" \
    docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${CONTAINER_NAME}" psql \
    --username="${USERNAME}" \
    --dbname="${database}" \
    --no-psqlrc \
    >"${output_file}"
}

verification_value() {
  local key="$1"
  local file="$2"
  awk -F '|' -v key="${key}" '$1 == key { print $2; exit }' "${file}"
}

json_string_field() {
  local field_name="$1"
  local file="$2"
  local match
  match="$(grep -Eo "\"${field_name}\":\"[A-Z0-9_]+\"" "${file}" | head -n 1 || true)"
  if [ -z "${match}" ]; then
    echo "MISSING"
    return
  fi
  printf '%s\n' "${match}" | sed -E 's/^[^:]+:\"([^\"]+)\"$/\1/'
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
  HISTORICAL_TIME_INVENTORY_DATABASE_CLASS="${INVENTORY_DATABASE_CLASS}" \
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
  local structure_fingerprint_file="$1"
  local output_file="$2"
  grep '^TABLE_ROW_COUNT|' "${structure_fingerprint_file}" | sort >"${output_file}"
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
  echo "P3_FINAL_SANITIZED_CLONE_GATE: BLOCKED_NOT_RUN"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit 0
fi
if [ -z "${P3_SANITIZATION_ATTESTATION_FILE:-}" ]; then
  blocked "BLOCKED_MISSING_SANITIZATION_ATTESTATION"
fi
if [ -z "${P3_DATASET_ID:-}" ]; then
  blocked "BLOCKED_MISSING_DATASET_ID"
fi
case "${DATASET_CLASS}" in
  SANITIZED_RELEASE_LIKE)
    if [ "${P3_CONFIRM:-}" != "I_CONFIRM_SANITIZED_NON_PRODUCTION_RELEASE_LIKE_DATASET" ]; then
      blocked "BLOCKED_SANITIZED_DATASET_CONFIRMATION_REQUIRED"
    fi
    ;;
  GENERATED_RELEASE_LIKE)
    if [ "${P3_CONFIRM:-}" != "I_CONFIRM_GENERATED_NON_PRODUCTION_RELEASE_LIKE_DATASET" ]; then
      blocked "BLOCKED_GENERATED_DATASET_CONFIRMATION_REQUIRED"
    fi
    INVENTORY_DATABASE_CLASS="GENERATED_REHEARSAL"
    SOURCE_DATASET_SUCCESS_STATUS="GENERATED_RELEASE_LIKE_NOT_SANITIZED_CLONE"
    SOURCE_INVENTORY_SUCCESS_STATUS="PASS_READ_ONLY_GENERATED_RELEASE_LIKE"
    SUCCESS_RESULT="PASS_GENERATED_RELEASE_LIKE_REHEARSAL"
    FINAL_SANITIZED_CLONE_GATE="BLOCKED_NOT_RUN"
    RECOVERY_SUCCESS_STATUS="PASS_PRE_MIGRATION_COPY"
    ;;
  *) blocked "BLOCKED_INVALID_DATASET_CLASS" ;;
esac
if [ "${P3_LOCAL_DB_RECREATE_CONFIRM:-}" != "I_UNDERSTAND_ONLY_LOCAL_P3_DATABASES_ARE_DROPPED" ]; then
  blocked "BLOCKED_LOCAL_RECREATE_CONFIRMATION_REQUIRED"
fi
if ! printf '%s' "${P3_DATASET_ID}" | grep -Eq '^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$'; then
  blocked "BLOCKED_INVALID_DATASET_ID"
fi
if printf '%s' "${P3_DATASET_ID}" \
  | grep -Eiq '(prod|production|primary|customer|client|host)' \
  || printf '%s' "${P3_DATASET_ID}" \
    | grep -Eiq '(^|[._-])live([._-]|$)'; then
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
if path_contains_symlink_or_dot_component "${P3_SANITIZED_DUMP_FILE}"; then
  blocked "BLOCKED_DUMP_SYMLINK_PATH"
fi
if path_contains_symlink_or_dot_component "${P3_SANITIZATION_ATTESTATION_FILE}"; then
  blocked "BLOCKED_ATTESTATION_SYMLINK_PATH"
fi

DUMP_FILE="$(canonical_existing_file "${P3_SANITIZED_DUMP_FILE}")"
ATTESTATION_FILE="$(canonical_existing_file "${P3_SANITIZATION_ATTESTATION_FILE}")"
if ! path_is_allowed "${DUMP_FILE}"; then
  blocked "BLOCKED_DUMP_PATH_INSIDE_REPOSITORY"
fi
if ! path_is_allowed "${ATTESTATION_FILE}"; then
  blocked "BLOCKED_ATTESTATION_PATH_INSIDE_REPOSITORY"
fi
if ! bash "${ROOT_DIR}/scripts/p3-attestation-validate.sh" \
  "${ATTESTATION_FILE}" "${DATASET_CLASS}"; then
  blocked "BLOCKED_SANITIZATION_ATTESTATION_MISMATCH"
fi

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/postgresql-p3-rehearsal.XXXXXX")"
for command_name in docker pg_restore psql curl jq java realpath; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    blocked "BLOCKED_REQUIRED_TOOL_MISSING"
  fi
done

enter_stage "dump-format-validation"
if ! run_bounded 120 pg_restore --list "${DUMP_FILE}" >"${TMP_DIR}/dump-list.txt" 2>"${TMP_DIR}/dump-list-error.txt"; then
  blocked "BLOCKED_INVALID_POSTGRESQL_CUSTOM_DUMP"
fi
if grep -Eiq '(DATABASE PROPERTIES|CREATE DATABASE|; [0-9]+ [0-9]+ DATABASE )' \
  "${TMP_DIR}/dump-list.txt"; then
  blocked "BLOCKED_DUMP_CONTAINS_DATABASE_CREATION"
fi
if ! bash "${ROOT_DIR}/scripts/p3-attestation-validate.sh" \
  "${ATTESTATION_FILE}" "${DATASET_CLASS}" "${TMP_DIR}/dump-list.txt" \
  >"${TMP_DIR}/attestation-dump-crosscheck.txt"; then
  blocked "BLOCKED_SANITIZATION_ATTESTATION_MISMATCH"
fi

DUMP_SHA256="$(sha256_file "${DUMP_FILE}")"
ATTESTATION_SHA256="$(sha256_file "${ATTESTATION_FILE}")"
DATASET_ID_SHA256="$(printf '%s' "${P3_DATASET_ID}" | sha256_text)"

enter_stage "docker-preflight"
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
  echo "DATASET_CLASS: ${DATASET_CLASS}"
  echo "DATASET_ID_SHA256: ${DATASET_ID_SHA256}"
  echo "ATTESTATION_SHA256: ${ATTESTATION_SHA256}"
  echo "ATTESTATION_REQUIRED_FIELDS: PASS"
  echo "ATTESTATION_UNIQUENESS_STATUS: PASS"
  echo "ATTESTATION_VERSION_CROSSCHECK: PENDING_SOURCE_RESTORE"
  echo "ATTESTATION_CONTENT: NOT_COPIED"
  echo "FINAL_SANITIZED_CLONE_ELIGIBILITY: $([ "${DATASET_CLASS}" = "SANITIZED_RELEASE_LIKE" ] && echo PENDING_REVIEW || echo NO)"
} >"${EVIDENCE_DIR}/input-attestation-summary.txt"
{
  echo "DUMP_FORMAT: POSTGRESQL_CUSTOM"
  echo "DUMP_SIZE_BYTES: $(file_size_bytes "${DUMP_FILE}")"
  echo "DUMP_SHA256: ${DUMP_SHA256}"
  echo "DUMP_PATH: REDACTED_NOT_RECORDED"
} >"${EVIDENCE_DIR}/input-dump-metadata.txt"

enter_stage "container-start"
DISPOSABLE_PASSWORD="p3-$(openssl rand -hex 24 2>/dev/null || printf '%s-%s-%s' "$RANDOM" "$RANDOM" "$(date -u +%s)")"
DISPOSABLE_ADMIN_PASSWORD="p3-admin-$(openssl rand -hex 24 2>/dev/null || printf '%s-%s' "$RANDOM" "$(date -u +%s)")"
if ! run_bounded 300 docker run \
  --pull never \
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

enter_stage "local-database-create"
for database_name in "${SOURCE_DATABASE}" "${REHEARSAL_DATABASE}" "${RECOVERY_DATABASE}"; do
  if ! run_bounded 30 docker exec "${CONTAINER_NAME}" createdb -U "${USERNAME}" "${database_name}"; then
    blocked "BLOCKED_LOCAL_DATABASE_CREATE_FAILED"
  fi
done

enter_stage "source-restore"
if ! run_bounded 60 docker cp \
  "${DUMP_FILE}" "${CONTAINER_NAME}:/tmp/p3-input.dump" >/dev/null 2>&1; then
  blocked "BLOCKED_SOURCE_DUMP_COPY_FAILED"
fi
if ! run_bounded 600 docker exec "${CONTAINER_NAME}" pg_restore \
  --username="${USERNAME}" \
  --dbname="${SOURCE_DATABASE}" \
  --clean \
  --if-exists \
  --no-owner \
  --no-acl \
  --exit-on-error \
  /tmp/p3-input.dump \
  >"${TMP_DIR}/source-restore.log" 2>"${TMP_DIR}/source-restore-error.log"; then
  blocked "BLOCKED_SOURCE_RESTORE_FAILED"
fi

enter_stage "source-identity"
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
if ! bash "${ROOT_DIR}/scripts/p3-attestation-validate.sh" \
  "${ATTESTATION_FILE}" "${DATASET_CLASS}" "${TMP_DIR}/dump-list.txt" \
  "${SOURCE_FLYWAY_VERSION}" >"${TMP_DIR}/attestation-version-crosscheck.txt"; then
  blocked "BLOCKED_ATTESTATION_VERSION_MISMATCH"
fi

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
  echo "DATASET_CLASS: ${DATASET_CLASS}"
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
  echo "ATTESTATION_VERSION_CROSSCHECK: PASS"
} >"${EVIDENCE_DIR}/source-identity.txt"
docker_psql "${SOURCE_DATABASE}" -AtF '|' -c \
  "SELECT installed_rank,version,description,success,checksum FROM flyway_schema_history ORDER BY installed_rank" \
  >"${EVIDENCE_DIR}/flyway-before.txt"

enter_stage "source-aggregate-validation"
capture_structure_fingerprint \
  "${SOURCE_DATABASE}" "${EVIDENCE_DIR}/source-structure-fingerprint.txt"
capture_content_fingerprint \
  "${SOURCE_DATABASE}" "FULL" "${EVIDENCE_DIR}/source-content-fingerprint.txt"
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

SOURCE_STRUCTURE_FINGERPRINT="$(sha256_file \
  "${EVIDENCE_DIR}/source-structure-fingerprint.txt")"
SOURCE_CONTENT_FINGERPRINT="$(sha256_file \
  "${EVIDENCE_DIR}/source-content-fingerprint.txt")"
SOURCE_INVENTORY_FINGERPRINT="$(awk -F '|' '$1 == "AGGREGATE_MD5" {print $2; exit}' "${EVIDENCE_DIR}/source-inventory.txt")"

enter_stage "controlled-backup"
BACKUP_FILE="${BACKUP_DIR}/p3-current-state.dump"
BACKUP_STARTED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
if ! run_bounded 600 docker exec "${CONTAINER_NAME}" pg_dump \
  --username="${USERNAME}" \
  --dbname="${SOURCE_DATABASE}" \
  --format=custom \
  --no-owner \
  --no-acl \
  --file=/tmp/p3-current-state.dump \
  >"${TMP_DIR}/backup.log" 2>"${TMP_DIR}/backup-error.log"; then
  blocked "BLOCKED_CONTROLLED_BACKUP_FAILED"
fi
if ! run_bounded 60 docker cp \
  "${CONTAINER_NAME}:/tmp/p3-current-state.dump" "${BACKUP_FILE}" >/dev/null 2>&1; then
  blocked "BLOCKED_CONTROLLED_BACKUP_COPY_FAILED"
fi
chmod 600 "${BACKUP_FILE}"
BACKUP_COMPLETED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
BACKUP_SHA256="$(sha256_file "${BACKUP_FILE}")"
if ! run_bounded 120 pg_restore --list "${BACKUP_FILE}" >/dev/null 2>&1; then
  blocked "BLOCKED_BACKUP_FORMAT_VALIDATION_FAILED"
fi
{
  echo "BACKUP_STATUS: PASS"
  echo "BACKUP_STARTED_AT_UTC: ${BACKUP_STARTED_AT}"
  echo "BACKUP_COMPLETED_AT_UTC: ${BACKUP_COMPLETED_AT}"
  echo "PG_DUMP_VERSION_CLASS: $(docker exec "${CONTAINER_NAME}" pg_dump --version | awk '{print $3}')_CONTAINER_NATIVE"
  echo "BACKUP_FORMAT: POSTGRESQL_CUSTOM"
  echo "BACKUP_FILE: p3-current-state.dump"
  echo "BACKUP_SIZE_BYTES: $(file_size_bytes "${BACKUP_FILE}")"
  echo "BACKUP_SHA256: ${BACKUP_SHA256}"
  echo "SOURCE_FLYWAY_VERSION: ${SOURCE_FLYWAY_VERSION}"
  echo "SOURCE_STRUCTURE_FINGERPRINT: ${SOURCE_STRUCTURE_FINGERPRINT}"
  echo "SOURCE_CONTENT_FINGERPRINT: ${SOURCE_CONTENT_FINGERPRINT}"
  echo "BACKUP_RESTORE_EXECUTION_PATH: POSTGRESQL_16_CONTAINER_NATIVE"
  echo "PROD_BACKUP_SCRIPT: NOT_EXECUTED"
  echo "PROD_RESTORE_SCRIPT: NOT_EXECUTED"
  echo "OPERATIONAL_SCRIPT_GATE: BLOCKED"
} >"${EVIDENCE_DIR}/backup-metadata.txt"

restore_backup_to_database() {
  local target_database="$1"
  local log_prefix="$2"
  run_bounded 600 docker exec "${CONTAINER_NAME}" pg_restore \
    --username="${USERNAME}" \
    --dbname="${target_database}" \
    --clean \
    --if-exists \
    --no-owner \
    --no-acl \
    --exit-on-error \
    /tmp/p3-current-state.dump \
    >"${TMP_DIR}/${log_prefix}.log" 2>"${TMP_DIR}/${log_prefix}-error.log"
}

enter_stage "recovery-restore"
if ! restore_backup_to_database "${RECOVERY_DATABASE}" "recovery-restore"; then
  blocked "BLOCKED_RECOVERY_RESTORE_FAILED"
fi
capture_structure_fingerprint \
  "${RECOVERY_DATABASE}" "${EVIDENCE_DIR}/recovery-structure-fingerprint.txt"
capture_content_fingerprint \
  "${RECOVERY_DATABASE}" "FULL" "${EVIDENCE_DIR}/recovery-content-fingerprint.txt"
RECOVERY_STRUCTURE_FINGERPRINT="$(sha256_file \
  "${EVIDENCE_DIR}/recovery-structure-fingerprint.txt")"
RECOVERY_CONTENT_FINGERPRINT="$(sha256_file \
  "${EVIDENCE_DIR}/recovery-content-fingerprint.txt")"
if [ "${RECOVERY_STRUCTURE_FINGERPRINT}" != "${SOURCE_STRUCTURE_FINGERPRINT}" ] \
  || [ "${RECOVERY_CONTENT_FINGERPRINT}" != "${SOURCE_CONTENT_FINGERPRINT}" ]; then
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

enter_stage "rehearsal-restore"
if ! restore_backup_to_database "${REHEARSAL_DATABASE}" "rehearsal-restore"; then
  blocked "BLOCKED_REHEARSAL_RESTORE_FAILED"
fi
capture_structure_fingerprint \
  "${REHEARSAL_DATABASE}" "${EVIDENCE_DIR}/rehearsal-structure-fingerprint-before.txt"
capture_content_fingerprint \
  "${REHEARSAL_DATABASE}" "FULL" \
  "${EVIDENCE_DIR}/rehearsal-content-fingerprint-before.txt"
capture_content_fingerprint \
  "${REHEARSAL_DATABASE}" "MIGRATION_STABLE" \
  "${EVIDENCE_DIR}/rehearsal-stable-content-before.txt"
REHEARSAL_BEFORE_STRUCTURE_FINGERPRINT="$(sha256_file \
  "${EVIDENCE_DIR}/rehearsal-structure-fingerprint-before.txt")"
REHEARSAL_BEFORE_CONTENT_FINGERPRINT="$(sha256_file \
  "${EVIDENCE_DIR}/rehearsal-content-fingerprint-before.txt")"
if [ "${REHEARSAL_BEFORE_STRUCTURE_FINGERPRINT}" != "${SOURCE_STRUCTURE_FINGERPRINT}" ] \
  || [ "${REHEARSAL_BEFORE_CONTENT_FINGERPRINT}" != "${SOURCE_CONTENT_FINGERPRINT}" ]; then
  blocked "RESTORE_DATA_INTEGRITY_MISMATCH"
fi
capture_table_counts \
  "${EVIDENCE_DIR}/source-structure-fingerprint.txt" "${TMP_DIR}/source-table-counts.txt"

enter_stage "rehearsal-flyway"
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
  MIGRATION_NEW_VALIDITY_COLUMNS_STATUS="PASS_ALL_NULL"
else
  if [ "$((FLYWAY_ROWS_AFTER - FLYWAY_ROWS_BEFORE))" -ne 0 ]; then
    blocked "BLOCKED_UNEXPECTED_MIGRATION_COUNT"
  fi
  MIGRATION_PATH="V7_VALIDATE_IDEMPOTENT"
  MIGRATION_NEW_VALIDITY_COLUMNS_STATUS="NOT_APPLICABLE_SOURCE_ALREADY_V7"
fi

capture_structure_fingerprint \
  "${REHEARSAL_DATABASE}" "${EVIDENCE_DIR}/rehearsal-structure-fingerprint-after.txt"
capture_content_fingerprint \
  "${REHEARSAL_DATABASE}" "FULL" \
  "${EVIDENCE_DIR}/rehearsal-content-fingerprint-after.txt"
capture_content_fingerprint \
  "${REHEARSAL_DATABASE}" "MIGRATION_STABLE" \
  "${EVIDENCE_DIR}/rehearsal-stable-content-after.txt"
capture_table_counts \
  "${EVIDENCE_DIR}/rehearsal-structure-fingerprint-after.txt" \
  "${TMP_DIR}/rehearsal-table-counts.txt"
if ! cmp -s "${TMP_DIR}/source-table-counts.txt" "${TMP_DIR}/rehearsal-table-counts.txt"; then
  blocked "BLOCKED_POST_MIGRATION_BUSINESS_ROW_CHANGE"
fi
if ! cmp -s "${EVIDENCE_DIR}/rehearsal-stable-content-before.txt" \
  "${EVIDENCE_DIR}/rehearsal-stable-content-after.txt"; then
  blocked "BLOCKED_POST_MIGRATION_STABLE_CONTENT_CHANGE"
fi
POST_MIGRATION_STRUCTURE_FINGERPRINT="$(sha256_file \
  "${EVIDENCE_DIR}/rehearsal-structure-fingerprint-after.txt")"
POST_MIGRATION_CONTENT_FINGERPRINT="$(sha256_file \
  "${EVIDENCE_DIR}/rehearsal-content-fingerprint-after.txt")"
MIGRATION_STABLE_CONTENT_FINGERPRINT="$(sha256_file \
  "${EVIDENCE_DIR}/rehearsal-stable-content-after.txt")"
docker_psql "${REHEARSAL_DATABASE}" -AtF '|' -c \
  "SELECT installed_rank,version,description,success,checksum FROM flyway_schema_history ORDER BY installed_rank" \
  >"${EVIDENCE_DIR}/flyway-after.txt"

enter_stage "application-smoke"
capture_content_fingerprint \
  "${REHEARSAL_DATABASE}" "FULL" "${TMP_DIR}/app-content-before.txt"
APPLICATION_DATABASE_ROLE="p3_app_readonly_$$_${RANDOM}"
APPLICATION_DATABASE_PASSWORD="p3-readonly-$(openssl rand -hex 24 2>/dev/null \
  || printf '%s-%s-%s' "$RANDOM" "$RANDOM" "$(date -u +%s)")"
if ! run_bounded_with_input 60 "${ROOT_DIR}/scripts/p3-application-readonly-role.sql" \
  docker exec -i \
  --env "PGOPTIONS=-c statement_timeout=30000 -c lock_timeout=5000" \
  "${CONTAINER_NAME}" psql \
  --username="${USERNAME}" \
  --dbname="${REHEARSAL_DATABASE}" \
  --no-psqlrc \
  --set="application_role=${APPLICATION_DATABASE_ROLE}" \
  --set="application_password=${APPLICATION_DATABASE_PASSWORD}" \
  --set="database_name=${REHEARSAL_DATABASE}" \
  >"${TMP_DIR}/application-role-create.log" 2>&1; then
  blocked "BLOCKED_APPLICATION_READONLY_ROLE_CREATE"
fi
APPLICATION_ROLE_CAPABILITIES="$(scalar_query "${REHEARSAL_DATABASE}" \
  "SELECT rolsuper::int || '|' || rolcreatedb::int || '|' || rolcreaterole::int || '|' || rolinherit::int FROM pg_roles WHERE rolname='${APPLICATION_DATABASE_ROLE}'")"
if [ "${APPLICATION_ROLE_CAPABILITIES}" != "0|0|0|0" ]; then
  blocked "BLOCKED_APPLICATION_READONLY_ROLE_CAPABILITIES"
fi
if [ "$(scalar_query "${REHEARSAL_DATABASE}" \
  "SELECT has_database_privilege('${APPLICATION_DATABASE_ROLE}','${REHEARSAL_DATABASE}','CONNECT') AND has_schema_privilege('${APPLICATION_DATABASE_ROLE}','public','USAGE') AND NOT has_schema_privilege('${APPLICATION_DATABASE_ROLE}','public','CREATE')")" != "t" ]; then
  blocked "BLOCKED_APPLICATION_READONLY_ROLE_PRIVILEGES"
fi
if [ "$(scalar_query "${REHEARSAL_DATABASE}" \
  "SELECT COUNT(*) FROM pg_database WHERE datallowconn AND has_database_privilege('${APPLICATION_DATABASE_ROLE}',datname,'CONNECT')")" != "1" ]; then
  blocked "BLOCKED_APPLICATION_READONLY_ROLE_DATABASE_SCOPE"
fi
if [ "$(scalar_query "${REHEARSAL_DATABASE}" \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND NOT has_table_privilege('${APPLICATION_DATABASE_ROLE}',format('%I.%I',table_schema,table_name),'SELECT')")" != "0" ]; then
  blocked "BLOCKED_APPLICATION_READONLY_ROLE_SELECT_GRANTS"
fi
if [ "$(scalar_query "${REHEARSAL_DATABASE}" \
  "SELECT COUNT(*) FROM information_schema.sequences WHERE sequence_schema='public' AND (NOT has_sequence_privilege('${APPLICATION_DATABASE_ROLE}',format('%I.%I',sequence_schema,sequence_name),'SELECT') OR has_sequence_privilege('${APPLICATION_DATABASE_ROLE}',format('%I.%I',sequence_schema,sequence_name),'USAGE'))")" != "0" ]; then
  blocked "BLOCKED_APPLICATION_READONLY_ROLE_SEQUENCE_GRANTS"
fi

if run_bounded 30 docker exec \
  --env "PGPASSWORD=${APPLICATION_DATABASE_PASSWORD}" \
  "${CONTAINER_NAME}" psql \
  --host=127.0.0.1 \
  --port=5432 \
  --username="${APPLICATION_DATABASE_ROLE}" \
  --dbname="${REHEARSAL_DATABASE}" \
  --no-psqlrc \
  --set=VERBOSITY=verbose \
  --command="UPDATE tm_asset_state SET state=state WHERE FALSE" \
  >"${TMP_DIR}/application-write-probe.log" 2>"${TMP_DIR}/application-write-probe-error.log"; then
  blocked "BLOCKED_APPLICATION_READONLY_WRITE_PROBE_ALLOWED"
fi
if ! grep -Eq '(25006|42501)' "${TMP_DIR}/application-write-probe-error.log"; then
  blocked "BLOCKED_APPLICATION_READONLY_WRITE_PROBE_UNCLASSIFIED"
fi
{
  echo "APPLICATION_DATABASE_ROLE: READ_ONLY"
  echo "APPLICATION_DATABASE_ROLE_RANDOMIZED: YES"
  echo "APPLICATION_ROLE_SUPERUSER: NO"
  echo "APPLICATION_ROLE_CREATEDB: NO"
  echo "APPLICATION_ROLE_CREATEROLE: NO"
  echo "APPLICATION_ROLE_SCHEMA_CREATE: DENIED"
  echo "READ_ONLY_ROLE_WRITE_PROBE: DENIED"
  echo "READ_ONLY_ROLE_WRITE_PROBE_SQLSTATE: ACCEPTED_READ_ONLY_OR_PERMISSION_DENIAL"
  echo "FLYWAY_DURING_APP_SMOKE: DISABLED"
} >"${EVIDENCE_DIR}/application-database-role.txt"

export SPRING_DATASOURCE_URL="jdbc:postgresql://${TARGET_HOST}:${TARGET_PORT}/${REHEARSAL_DATABASE}"
export SPRING_DATASOURCE_USERNAME="${APPLICATION_DATABASE_ROLE}"
export SPRING_DATASOURCE_PASSWORD="${APPLICATION_DATABASE_PASSWORD}"
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
export SPRING_DATASOURCE_HIKARI_READ_ONLY=true
export SPRING_FLYWAY_ENABLED=false
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
if [ "${DATASET_CLASS}" = "GENERATED_RELEASE_LIKE" ]; then
  no_position_status="$(curl --config "${CURL_CONFIG}" \
    --output "${TMP_DIR}/home-no-position.json" --write-out '%{http_code}' \
    "http://127.0.0.1:${APP_PORT}/api/dashboard/home?selectedSymbol=DOGEUSDT")"
  unique_position_status="$(curl --config "${CURL_CONFIG}" \
    --output "${TMP_DIR}/home-unique-position.json" --write-out '%{http_code}' \
    "http://127.0.0.1:${APP_PORT}/api/dashboard/home?selectedSymbol=ETHUSDT&positionId=1003")"
  multi_position_status="$(curl --config "${CURL_CONFIG}" \
    --output "${TMP_DIR}/home-multi-position.json" --write-out '%{http_code}' \
    "http://127.0.0.1:${APP_PORT}/api/dashboard/home?selectedSymbol=BTCUSDT")"
  position_a_status="$(curl --config "${CURL_CONFIG}" \
    --output "${TMP_DIR}/home-position-a.json" --write-out '%{http_code}' \
    "http://127.0.0.1:${APP_PORT}/api/dashboard/home?selectedSymbol=BTCUSDT&positionId=1001")"
  position_b_status="$(curl --config "${CURL_CONFIG}" \
    --output "${TMP_DIR}/home-position-b.json" --write-out '%{http_code}' \
    "http://127.0.0.1:${APP_PORT}/api/dashboard/home?selectedSymbol=BTCUSDT&positionId=1002")"
  revalidation_status="$(curl --config "${CURL_CONFIG}" \
    --output "${TMP_DIR}/home-revalidation.json" --write-out '%{http_code}' \
    "http://127.0.0.1:${APP_PORT}/api/dashboard/home?selectedSymbol=SOLUSDT&positionId=1004")"
  expired_status="$(curl --config "${CURL_CONFIG}" \
    --output "${TMP_DIR}/home-expired.json" --write-out '%{http_code}' \
    "http://127.0.0.1:${APP_PORT}/api/dashboard/home?selectedSymbol=XRPUSDT&positionId=1006")"
  for scenario_status in \
    "${no_position_status}" "${unique_position_status}" "${multi_position_status}" \
    "${position_a_status}" "${position_b_status}" "${revalidation_status}" \
    "${expired_status}"; do
    if [ "${scenario_status}" != "200" ]; then
      blocked "BLOCKED_GENERATED_DASHBOARD_SCENARIO_HTTP"
    fi
  done
  if ! grep -q '"positionSelectionStatus":"NO_POSITION"' "${TMP_DIR}/home-no-position.json" \
    || ! grep -q '"positionSelectionStatus":"EXACT_POSITION_SELECTED"' "${TMP_DIR}/home-unique-position.json" \
    || ! grep -q '"selectedPositionId":1003' "${TMP_DIR}/home-unique-position.json" \
    || ! grep -q '"positionSelectionStatus":"POSITION_SELECTION_REQUIRED"' "${TMP_DIR}/home-multi-position.json"; then
    echo "GENERATED_NO_POSITION_SELECTION_STATUS: $(json_string_field positionSelectionStatus "${TMP_DIR}/home-no-position.json")"
    echo "GENERATED_UNIQUE_POSITION_SELECTION_STATUS: $(json_string_field positionSelectionStatus "${TMP_DIR}/home-unique-position.json")"
    echo "GENERATED_MULTI_POSITION_SELECTION_STATUS: $(json_string_field positionSelectionStatus "${TMP_DIR}/home-multi-position.json")"
    blocked "BLOCKED_GENERATED_DASHBOARD_SELECTION_CONTRACT"
  fi
  position_a_plan_id="$(jq -r '.data.executionSuggestion.sourceExecutionPlanId // "MISSING"' \
    "${TMP_DIR}/home-position-a.json")"
  position_a_analysis_id="$(jq -r '.data.executionSuggestion.sourceAnalysisId // "MISSING"' \
    "${TMP_DIR}/home-position-a.json")"
  position_b_plan_id="$(jq -r '.data.executionSuggestion.sourceExecutionPlanId // "MISSING"' \
    "${TMP_DIR}/home-position-b.json")"
  position_b_analysis_id="$(jq -r '.data.executionSuggestion.sourceAnalysisId // "MISSING"' \
    "${TMP_DIR}/home-position-b.json")"
  if [ "${position_a_plan_id}" != "P3P-BTCUSDT-001-A" ] \
    || [ "${position_a_analysis_id}" != "P3A-BTCUSDT-001" ] \
    || [ "${position_b_plan_id}" != "P3P-BTCUSDT-002-A" ] \
    || [ "${position_b_analysis_id}" != "P3A-BTCUSDT-002" ]; then
    echo "GENERATED_POSITION_A_PLAN_ID_MATCH: $([ "${position_a_plan_id}" = "P3P-BTCUSDT-001-A" ] && echo YES || echo NO)"
    echo "GENERATED_POSITION_A_ANALYSIS_ID_MATCH: $([ "${position_a_analysis_id}" = "P3A-BTCUSDT-001" ] && echo YES || echo NO)"
    echo "GENERATED_POSITION_B_PLAN_ID_MATCH: $([ "${position_b_plan_id}" = "P3P-BTCUSDT-002-A" ] && echo YES || echo NO)"
    echo "GENERATED_POSITION_B_ANALYSIS_ID_MATCH: $([ "${position_b_analysis_id}" = "P3A-BTCUSDT-002" ] && echo YES || echo NO)"
    blocked "BLOCKED_GENERATED_DASHBOARD_PLAN_ISOLATION"
  fi
  if ! grep -q '"originalPlanCurrentValidity":"REVALIDATION_REQUIRED"' \
    "${TMP_DIR}/home-revalidation.json"; then
    blocked "BLOCKED_GENERATED_DASHBOARD_REVALIDATION_CONTRACT"
  fi
  if ! grep -q '"originalPlanCurrentValidity":"PLAN_INCOMPLETE"' \
    "${TMP_DIR}/home-unique-position.json"; then
    blocked "BLOCKED_GENERATED_DASHBOARD_INCOMPLETE_PLAN_CONTRACT"
  fi
  expired_plan_identity="$(jq -r \
    '.data.executionSuggestion.originalPlanIdentity // "MISSING"' \
    "${TMP_DIR}/home-expired.json")"
  expired_plan_validity="$(jq -r \
    '.data.executionSuggestion.originalPlanCurrentValidity // "MISSING"' \
    "${TMP_DIR}/home-expired.json")"
  expired_suggestion_status="$(jq -r \
    '.data.executionSuggestion.status // "MISSING"' \
    "${TMP_DIR}/home-expired.json")"
  expired_position_mode="$(jq -r \
    '.data.executionSuggestion.positionMode // "MISSING"' \
    "${TMP_DIR}/home-expired.json")"
  if [ "${expired_plan_identity}" != "VERIFIED" ] \
    || [ "${expired_plan_validity}" != "EXPIRED" ] \
    || [ "${expired_suggestion_status}" != "POSITION_MONITORING" ] \
    || [ "${expired_position_mode}" != "true" ]; then
    echo "GENERATED_EXPIRED_PLAN_IDENTITY: ${expired_plan_identity}"
    echo "GENERATED_EXPIRED_PLAN_VALIDITY: ${expired_plan_validity}"
    echo "GENERATED_EXPIRED_SUGGESTION_STATUS: ${expired_suggestion_status}"
    echo "GENERATED_EXPIRED_POSITION_MODE: ${expired_position_mode}"
    blocked "BLOCKED_GENERATED_DASHBOARD_EXPIRED_PLAN_CONTRACT"
  fi
  if grep -Eiq '(order submitted|自动开仓|自动平仓|自动反手|自动下单)' \
    "${TMP_DIR}"/home-*.json; then
    blocked "BLOCKED_GENERATED_DASHBOARD_TRADING_SEMANTICS"
  fi
fi

stop_application
capture_structure_fingerprint \
  "${REHEARSAL_DATABASE}" "${TMP_DIR}/app-structure-after.txt"
capture_content_fingerprint \
  "${REHEARSAL_DATABASE}" "FULL" "${TMP_DIR}/app-content-after.txt"
if ! cmp -s "${EVIDENCE_DIR}/rehearsal-structure-fingerprint-after.txt" \
  "${TMP_DIR}/app-structure-after.txt"; then
  blocked "BLOCKED_UNEXPECTED_DATABASE_STRUCTURE_CHANGE"
fi
if ! cmp -s "${TMP_DIR}/app-content-before.txt" "${TMP_DIR}/app-content-after.txt"; then
  blocked "BLOCKED_UNEXPECTED_BUSINESS_WRITES"
fi
{
  echo "HEALTH: HTTP_200_UP"
  echo "DASHBOARD_HOME: HTTP_200_FAIL_CLOSED"
  if [ "${DATASET_CLASS}" = "GENERATED_RELEASE_LIKE" ]; then
    echo "GENERATED_DASHBOARD_SCENARIOS: PASS"
    echo "MULTI_POSITION_PLAN_ISOLATION: PASS"
    echo "INCOMPLETE_PLAN_FAIL_CLOSED: PASS"
    echo "EXPIRED_HISTORICAL_PLAN_FAIL_CLOSED: PASS"
    echo "REVALIDATION_PLAN_FAIL_CLOSED: PASS"
  fi
  echo "RUN_BASELINE: HTTP_200"
  echo "AI_PROVIDER_STATE: NOT_CALLED_OR_DISABLED"
  echo "SCHEDULERS: DISABLED"
  echo "EXTERNAL_PROVIDER_CALLS: DISABLED"
  echo "APPLICATION_DATABASE_ROLE: READ_ONLY"
  echo "READ_ONLY_ROLE_WRITE_PROBE: DENIED"
  echo "FLYWAY_DURING_APP_SMOKE: DISABLED"
  echo "APP_CONTENT_FINGERPRINT: MATCH"
  echo "UNEXPECTED_BUSINESS_WRITES: 0"
} >"${EVIDENCE_DIR}/application-smoke.txt"

{
  echo "SOURCE_TO_RECOVERY_STRUCTURE_FINGERPRINT: MATCH"
  echo "SOURCE_TO_RECOVERY_CONTENT: MATCH"
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

enter_stage "container-cleanup"
remove_container
if [ "${CONTAINER_CLEANUP}" != "PASS" ]; then
  blocked "BLOCKED_CONTAINER_CLEANUP_FAILED"
fi

enter_stage "summary"
EXECUTED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
{
  echo "P3_RESULT: ${SUCCESS_RESULT}"
  echo "EXECUTED_AT_UTC: ${EXECUTED_AT}"
  echo "SOURCE_DATASET_STATUS: ${SOURCE_DATASET_SUCCESS_STATUS}"
  echo "SOURCE_FLYWAY_VERSION: ${SOURCE_FLYWAY_VERSION}"
  echo "SOURCE_INVENTORY_STATUS: ${SOURCE_INVENTORY_SUCCESS_STATUS}"
  echo "SOURCE_STRUCTURE_FINGERPRINT: ${SOURCE_STRUCTURE_FINGERPRINT}"
  echo "SOURCE_CONTENT_FINGERPRINT: ${SOURCE_CONTENT_FINGERPRINT}"
  echo "STRUCTURE_FINGERPRINT_STATUS: PASS"
  echo "CONTENT_FINGERPRINT_STATUS: PASS"
  echo "BACKUP_STATUS: PASS"
  echo "BACKUP_SHA256: ${BACKUP_SHA256}"
  echo "BACKUP_RESTORE_EXECUTION_PATH: POSTGRESQL_16_CONTAINER_NATIVE"
  echo "PROD_BACKUP_SCRIPT: NOT_EXECUTED"
  echo "PROD_RESTORE_SCRIPT: NOT_EXECUTED"
  echo "OPERATIONAL_SCRIPT_GATE: BLOCKED"
  echo "RESTORE_STATUS: PASS"
  echo "SOURCE_TO_RECOVERY_STRUCTURE_FINGERPRINT: MATCH"
  echo "SOURCE_TO_RECOVERY_CONTENT: MATCH"
  echo "MIGRATION_PATH: ${MIGRATION_PATH}"
  echo "MIGRATION_STATUS: PASS"
  echo "POST_MIGRATION_STRUCTURE_FINGERPRINT: ${POST_MIGRATION_STRUCTURE_FINGERPRINT}"
  echo "POST_MIGRATION_CONTENT_FINGERPRINT: ${POST_MIGRATION_CONTENT_FINGERPRINT}"
  echo "MIGRATION_STABLE_CONTENT_FINGERPRINT: ${MIGRATION_STABLE_CONTENT_FINGERPRINT}"
  echo "PRE_TO_POST_MIGRATION_STABLE_CONTENT: MATCH"
  echo "MIGRATION_NEW_VALIDITY_COLUMNS_ALL_NULL: ${MIGRATION_NEW_VALIDITY_COLUMNS_STATUS}"
  echo "HISTORICAL_TIME_RESULTS: PASS_READ_ONLY_AGGREGATE"
  echo "APPLICATION_SMOKE_STATUS: PASS"
  echo "APPLICATION_DATABASE_ROLE: READ_ONLY"
  echo "READ_ONLY_ROLE_WRITE_PROBE: DENIED"
  echo "FLYWAY_DURING_APP_SMOKE: DISABLED"
  echo "APP_CONTENT_FINGERPRINT: MATCH"
  echo "ATTESTATION_UNIQUENESS_STATUS: PASS"
  echo "ATTESTATION_VERSION_CROSSCHECK: PASS"
  echo "UNEXPECTED_BUSINESS_WRITES: 0"
  echo "RECOVERY_STATUS: ${RECOVERY_SUCCESS_STATUS}"
  echo "WRITER_CUTOVER_STATUS: MISSING_OPERATIONAL_EVIDENCE"
  echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
  echo "P3_FINAL_SANITIZED_CLONE_GATE: ${FINAL_SANITIZED_CLONE_GATE}"
  echo "P4_ALLOWED: NO"
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
  source-structure-fingerprint.txt \
  source-content-fingerprint.txt \
  source-inventory.txt \
  backup-metadata.txt \
  restore-verification.txt \
  recovery-structure-fingerprint.txt \
  recovery-content-fingerprint.txt \
  rehearsal-structure-fingerprint-before.txt \
  rehearsal-content-fingerprint-before.txt \
  rehearsal-stable-content-before.txt \
  rehearsal-structure-fingerprint-after.txt \
  rehearsal-content-fingerprint-after.txt \
  rehearsal-stable-content-after.txt \
  application-database-role.txt \
  application-smoke.txt \
  cutover-register-summary.txt; do
  echo "$(sha256_file "${EVIDENCE_DIR}/${evidence_file}")  ${evidence_file}" \
    >>"${EVIDENCE_DIR}/checksums.txt"
done

FINAL_RESULT="${SUCCESS_RESULT}"
cat "${EVIDENCE_DIR}/summary.txt"
echo "EVIDENCE_ARTIFACTS: .runtime/postgresql-p3-rehearsal"
