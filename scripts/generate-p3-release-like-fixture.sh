#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INPUT_DIR="${ROOT_DIR}/.runtime/p3-input"
IMAGE_REF="postgres@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc"
HOST="127.0.0.1"
PORT="55434"
DATABASE="trade_model_v1_p3_generated_source"
USERNAME="p3_generated_operator"
SEED="${P3_GENERATED_FIXTURE_SEED:-20260715}"
CONTAINER_NAME="trade-model-v1-p3-generated-$(date -u +%Y%m%d%H%M%S)-$$"
DUMP_FILE="${INPUT_DIR}/generated-release-like-v6.dump"
ATTESTATION_FILE="${INPUT_DIR}/generated-release-like.attestation"
SUMMARY_FILE="${INPUT_DIR}/generated-release-like.summary"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/p3-generated-fixture.XXXXXX")"
CONTAINER_STARTED=0
CONTAINER_CLEANUP="NOT_STARTED"
FINAL_STATUS="BLOCKED"

cd "${ROOT_DIR}"

sha256_file() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    sha256sum "$1" | awk '{print $1}'
  fi
}

sha256_stream() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 | awk '{print $1}'
  else
    sha256sum | awk '{print $1}'
  fi
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
  remove_container
  rm -rf "${TMP_DIR}"
  if [ "${status}" -ne 0 ]; then
    echo "GENERATED_FIXTURE_STATUS: ${FINAL_STATUS}"
    echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
    echo "PRODUCTION_READINESS: BLOCKED"
  fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

blocked() {
  FINAL_STATUS="$1"
  exit "${2:-2}"
}

metric_value() {
  local key="$1"
  local file="$2"
  awk -F '|' -v key="${key}" '$1 == key {print $2; exit}' "${file}"
}

require_metric() {
  local key="$1"
  local expected="$2"
  local file="$3"
  local actual
  actual="$(metric_value "${key}" "${file}")"
  if [ "${actual}" != "${expected}" ]; then
    echo "GENERATED_FIXTURE_FAILED_METRIC: ${key}"
    echo "GENERATED_FIXTURE_EXPECTED_AGGREGATE: ${expected}"
    echo "GENERATED_FIXTURE_ACTUAL_AGGREGATE: ${actual:-MISSING}"
    blocked "BLOCKED_GENERATED_FIXTURE_VERIFICATION_${key}"
  fi
}

if [ "${SEED}" != "20260715" ]; then
  blocked "BLOCKED_UNAPPROVED_FIXTURE_SEED"
fi
if [ "${HOST}" != "127.0.0.1" ] || [ "${PORT}" != "55434" ]; then
  blocked "BLOCKED_UNAPPROVED_GENERATOR_TARGET"
fi
if [ "${DATABASE}" != "trade_model_v1_p3_generated_source" ]; then
  blocked "BLOCKED_UNAPPROVED_GENERATOR_DATABASE"
fi

for command_name in docker java; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    blocked "BLOCKED_REQUIRED_TOOL_MISSING"
  fi
done
if ! run_bounded 30 docker info >/dev/null 2>&1; then
  blocked "BLOCKED_DOCKER_DAEMON_UNAVAILABLE"
fi
if ! run_bounded 30 docker image inspect "${IMAGE_REF}" >/dev/null 2>&1; then
  blocked "BLOCKED_PINNED_POSTGRESQL_IMAGE_MISSING"
fi
if run_bounded 30 docker ps --format '{{.Ports}}' \
  | grep -q "127.0.0.1:${PORT}->"; then
  blocked "BLOCKED_GENERATOR_PORT_IN_USE"
fi

case "${INPUT_DIR}" in
  "${ROOT_DIR}/.runtime/p3-input") ;;
  *) blocked "BLOCKED_UNSAFE_GENERATED_INPUT_PATH" ;;
esac
mkdir -p "${INPUT_DIR}"
rm -f "${DUMP_FILE}" "${ATTESTATION_FILE}" "${SUMMARY_FILE}"

PASSWORD="p3-generated-$(openssl rand -hex 24 2>/dev/null || printf '%s-%s' "$RANDOM" "$(date -u +%s)")"
if ! run_bounded 300 docker run \
  --pull never \
  --name "${CONTAINER_NAME}" \
  --env "POSTGRES_DB=${DATABASE}" \
  --env "POSTGRES_USER=${USERNAME}" \
  --env "POSTGRES_PASSWORD=${PASSWORD}" \
  --publish "127.0.0.1:${PORT}:5432" \
  --detach "${IMAGE_REF}" >/dev/null; then
  blocked "BLOCKED_GENERATOR_CONTAINER_START"
fi
CONTAINER_STARTED=1

ready=0
for _ in $(seq 1 90); do
  if run_bounded 5 docker exec "${CONTAINER_NAME}" pg_isready \
    -U "${USERNAME}" -d "${DATABASE}" >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done
if [ "${ready}" -ne 1 ]; then
  blocked "BLOCKED_GENERATOR_POSTGRESQL_READINESS"
fi

if ! run_bounded 600 env \
  P3_GENERATED_POSTGRESQL_JDBC_URL="jdbc:postgresql://${HOST}:${PORT}/${DATABASE}" \
  P3_GENERATED_POSTGRESQL_USERNAME="${USERNAME}" \
  P3_GENERATED_POSTGRESQL_PASSWORD="${PASSWORD}" \
  P3_GENERATED_POSTGRESQL_DATABASE="${DATABASE}" \
  P3_GENERATED_FLYWAY_CONFIRM="I_CONFIRM_LOCAL_GENERATED_P3_V6_SCHEMA" \
    ./mvnw -q -Dtest=ControlledGeneratedReleaseLikeFixtureFlywayTest test \
    >"${TMP_DIR}/flyway-v6.log" 2>&1; then
  blocked "BLOCKED_GENERATOR_FLYWAY_V6"
fi

if ! run_bounded_with_input 180 "${ROOT_DIR}/scripts/p3-generated-fixture-data.sql" \
  docker exec -i \
  --env "PGOPTIONS=-c statement_timeout=180000 -c lock_timeout=5000" \
  "${CONTAINER_NAME}" psql --username="${USERNAME}" --dbname="${DATABASE}" --no-psqlrc \
  >"${TMP_DIR}/fixture-data.log" 2>"${TMP_DIR}/fixture-data-error.log"; then
  blocked "BLOCKED_GENERATED_FIXTURE_INSERT"
fi

if ! run_bounded_with_input 180 "${ROOT_DIR}/scripts/p3-generated-fixture-verification.sql" \
  docker exec -i \
  --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
  "${CONTAINER_NAME}" psql --username="${USERNAME}" --dbname="${DATABASE}" --no-psqlrc \
  >"${TMP_DIR}/generated-verification.txt"; then
  blocked "BLOCKED_GENERATED_FIXTURE_VERIFICATION"
fi

require_metric FIXTURE_SEED 20260715 "${TMP_DIR}/generated-verification.txt"
require_metric FLYWAY_VERSION 6 "${TMP_DIR}/generated-verification.txt"
require_metric FLYWAY_SUCCESS_COUNT 6 "${TMP_DIR}/generated-verification.txt"
require_metric V7_VALIDITY_COLUMN_COUNT 0 "${TMP_DIR}/generated-verification.txt"
require_metric ANALYSIS_TOTAL 138 "${TMP_DIR}/generated-verification.txt"
require_metric ANALYSIS_SUCCESS 120 "${TMP_DIR}/generated-verification.txt"
require_metric ANALYSIS_FAILED 12 "${TMP_DIR}/generated-verification.txt"
require_metric ANALYSIS_STARTED 6 "${TMP_DIR}/generated-verification.txt"
require_metric PRIMARY_ASSET_COUNT 6 "${TMP_DIR}/generated-verification.txt"
require_metric DECISION_TOTAL 120 "${TMP_DIR}/generated-verification.txt"
require_metric EXECUTION_PLAN_TOTAL 121 "${TMP_DIR}/generated-verification.txt"
require_metric EXECUTION_PLAN_UNSAFE 0 "${TMP_DIR}/generated-verification.txt"
require_metric EXECUTION_PLAN_SIBLING_COUNT 2 "${TMP_DIR}/generated-verification.txt"
require_metric ASSET_STATE_TOTAL 9 "${TMP_DIR}/generated-verification.txt"
require_metric ASSET_STATE_DISTINCT_STATUS 8 "${TMP_DIR}/generated-verification.txt"
require_metric USER_POSITION_TOTAL 7 "${TMP_DIR}/generated-verification.txt"
require_metric USER_POSITION_UNSAFE 0 "${TMP_DIR}/generated-verification.txt"
require_metric OPEN_BTC_POSITION_COUNT 2 "${TMP_DIR}/generated-verification.txt"
require_metric POSITION_MONITOR_TOTAL 8 "${TMP_DIR}/generated-verification.txt"
require_metric POSITION_MONITOR_REVALIDATION_REASON 1 "${TMP_DIR}/generated-verification.txt"
require_metric POSITION_MONITOR_BOUNDARY_REASON 1 "${TMP_DIR}/generated-verification.txt"
require_metric POSITION_MONITOR_SIBLING_B_REFERENCE 0 "${TMP_DIR}/generated-verification.txt"
require_metric POSITION_MONITOR_UNVERIFIED_SOURCE 1 "${TMP_DIR}/generated-verification.txt"
require_metric MONITOR_ALERT_TOTAL 6 "${TMP_DIR}/generated-verification.txt"
require_metric PUSH_SNAPSHOT_TOTAL 5 "${TMP_DIR}/generated-verification.txt"
require_metric PUSH_RECHECK_TOTAL 5 "${TMP_DIR}/generated-verification.txt"
require_metric HOT_RESET_TOTAL 6 "${TMP_DIR}/generated-verification.txt"
require_metric HOT_RESET_TIME_ORDER_INVALID 0 "${TMP_DIR}/generated-verification.txt"
require_metric AI_CALL_LOG_TOTAL 5 "${TMP_DIR}/generated-verification.txt"
require_metric AI_REQUIRED_STATE_MARKER_COUNT 5 "${TMP_DIR}/generated-verification.txt"
require_metric AI_CALL_LOG_UNSAFE 0 "${TMP_DIR}/generated-verification.txt"
require_metric OHLCV_TOTAL 1200 "${TMP_DIR}/generated-verification.txt"
require_metric OHLCV_COMBINATION_COUNT 24 "${TMP_DIR}/generated-verification.txt"
require_metric OHLCV_NON_SYNTHETIC 0 "${TMP_DIR}/generated-verification.txt"
require_metric DISPATCH_CONFIG_TOTAL 3 "${TMP_DIR}/generated-verification.txt"

if ! run_bounded_with_input 180 \
  "${ROOT_DIR}/scripts/current-state-clone-restore-verification.sql" \
  docker exec -i \
  --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
  "${CONTAINER_NAME}" psql --username="${USERNAME}" --dbname="${DATABASE}" --no-psqlrc \
  >"${TMP_DIR}/sanitization-verification.txt"; then
  blocked "BLOCKED_GENERATED_SANITIZATION_VERIFICATION"
fi
for key in \
  DECISION_WITHOUT_ANALYSIS EXECUTION_PLAN_WITHOUT_ANALYSIS \
  POSITION_MONITOR_WITHOUT_POSITION POSITION_MONITOR_PLAN_ANALYSIS_MISMATCH \
  TYPED_POSITION_PLAN_REFERENCE_MISMATCH DUPLICATE_ANALYSIS_ID \
  DUPLICATE_DECISION_ID DUPLICATE_PLAN_ID SECRET_CANDIDATE_TOTAL \
  PII_CANDIDATE_TOTAL PRODUCTION_REFERENCE_CANDIDATE_TOTAL; do
  require_metric "${key}" 0 "${TMP_DIR}/sanitization-verification.txt"
done

if ! run_bounded_with_input 180 "${ROOT_DIR}/scripts/current-state-clone-fingerprint.sql" \
  docker exec -i \
  --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
  "${CONTAINER_NAME}" psql --username="${USERNAME}" --dbname="${DATABASE}" --no-psqlrc \
  >"${TMP_DIR}/fingerprint-first.txt"; then
  blocked "BLOCKED_GENERATED_FINGERPRINT"
fi
if ! run_bounded_with_input 180 "${ROOT_DIR}/scripts/current-state-clone-fingerprint.sql" \
  docker exec -i \
  --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
  "${CONTAINER_NAME}" psql --username="${USERNAME}" --dbname="${DATABASE}" --no-psqlrc \
  >"${TMP_DIR}/fingerprint-second.txt"; then
  blocked "BLOCKED_GENERATED_FINGERPRINT"
fi
if ! cmp -s "${TMP_DIR}/fingerprint-first.txt" "${TMP_DIR}/fingerprint-second.txt"; then
  blocked "BLOCKED_NONDETERMINISTIC_GENERATED_FINGERPRINT"
fi
SOURCE_FINGERPRINT="$(sha256_file "${TMP_DIR}/fingerprint-first.txt")"

POSTGRESQL_VERSION="$(run_bounded 30 docker exec "${CONTAINER_NAME}" psql \
  --username="${USERNAME}" --dbname="${DATABASE}" --no-psqlrc -Atqc 'SHOW server_version')"
if [ "${POSTGRESQL_VERSION}" != "16.14" ]; then
  blocked "BLOCKED_UNEXPECTED_POSTGRESQL_VERSION"
fi

if ! run_bounded 300 docker exec "${CONTAINER_NAME}" pg_dump \
  --username="${USERNAME}" --dbname="${DATABASE}" --format=custom \
  --no-owner --no-acl --file=/tmp/generated-release-like-v6.dump; then
  blocked "BLOCKED_GENERATED_DUMP"
fi
if ! run_bounded 120 docker exec "${CONTAINER_NAME}" pg_restore \
  --list /tmp/generated-release-like-v6.dump >"${TMP_DIR}/dump-list.txt"; then
  blocked "BLOCKED_GENERATED_DUMP_FORMAT"
fi
if grep -Eiq '(DATABASE PROPERTIES|CREATE DATABASE|; [0-9]+ [0-9]+ DATABASE )' \
  "${TMP_DIR}/dump-list.txt"; then
  blocked "BLOCKED_GENERATED_DUMP_DATABASE_CREATION"
fi
if ! run_bounded 60 docker cp \
  "${CONTAINER_NAME}:/tmp/generated-release-like-v6.dump" "${DUMP_FILE}"; then
  blocked "BLOCKED_GENERATED_DUMP_COPY"
fi
chmod 600 "${DUMP_FILE}"

GENERATED_AT_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
cat >"${ATTESTATION_FILE}" <<EOF
DATA_SOURCE_CLASS=GENERATED_RELEASE_LIKE
SANITIZATION_OWNER_OR_PROCESS=DETERMINISTIC_REPOSITORY_FIXTURE_GENERATOR
GENERATED_AT_UTC=${GENERATED_AT_UTC}
SOURCE_POSTGRESQL_VERSION=16.14
SOURCE_FLYWAY_VERSION=6
USER_IDENTIFIERS_REMOVED_OR_PSEUDONYMIZED=YES
SECRETS_REMOVED=YES
FREE_TEXT_CLEANED_OR_REPLACED=YES
LOCAL_CONTROLLED_REHEARSAL_ALLOWED=YES
NOT_PRODUCTION_AND_NOT_FOR_PRODUCTION_RESTORE=YES
FIXTURE_SEED=20260715
REAL_USER_DATA_INCLUDED=NO
REAL_ACCOUNT_DATA_INCLUDED=NO
REAL_MARKET_PROVIDER_DATA_INCLUDED=NO
SUITABLE_FOR_FINAL_SANITIZED_CLONE_GATE=NO
EOF
chmod 600 "${ATTESTATION_FILE}"

GENERATED_DUMP_SHA256="$(sha256_file "${DUMP_FILE}")"
GENERATED_ATTESTATION_SHA256="$(sha256_file "${ATTESTATION_FILE}")"
FIXTURE_GENERATOR_SHA256="$(cat \
  "${ROOT_DIR}/scripts/generate-p3-release-like-fixture.sh" \
  "${ROOT_DIR}/scripts/p3-generated-fixture-data.sql" \
  "${ROOT_DIR}/scripts/p3-generated-fixture-verification.sql" \
  "${ROOT_DIR}/src/test/java/org/example/trademodel/postgresql/ControlledGeneratedReleaseLikeFixtureFlywayTest.java" \
  | sha256_stream)"

remove_container
if [ "${CONTAINER_CLEANUP}" != "PASS" ]; then
  blocked "BLOCKED_GENERATOR_CONTAINER_CLEANUP"
fi

cat >"${SUMMARY_FILE}" <<EOF
GENERATED_FIXTURE_STATUS: PASS
FIXTURE_SEED: 20260715
DATASET_CLASS: GENERATED_RELEASE_LIKE
SOURCE_FLYWAY_VERSION: 6
SOURCE_ROW_COUNTS: analysis=138,decision=120,plan=121,position=7,monitor=8,ohlcv=1200
SOURCE_FINGERPRINT: ${SOURCE_FINGERPRINT}
GENERATED_DUMP_SHA256: ${GENERATED_DUMP_SHA256}
GENERATED_ATTESTATION_SHA256: ${GENERATED_ATTESTATION_SHA256}
FIXTURE_GENERATOR_SHA256: ${FIXTURE_GENERATOR_SHA256}
SECRET_CANDIDATE_TOTAL: 0
PII_CANDIDATE_TOTAL: 0
PRODUCTION_REFERENCE_CANDIDATE_TOTAL: 0
FINAL_SANITIZED_CLONE_ELIGIBILITY: NO
CONTAINER_CLEANUP: PASS
PRODUCTION_READINESS: BLOCKED
EOF

FINAL_STATUS="PASS"
cat "${SUMMARY_FILE}"
echo "GENERATED_DUMP_FILE: ${DUMP_FILE}"
echo "GENERATED_ATTESTATION_FILE: ${ATTESTATION_FILE}"
