#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EVIDENCE_DIR="${ROOT_DIR}/.runtime/postgresql-flyway-v7-evidence"
IMAGE_TAG="postgres:16-alpine"
IMAGE_REF="postgres@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc"
CONTROLLED_PORT="55432"
DATABASE="trade_model_v1_test"
USERNAME="trade_model_test"
CONTAINER_NAME="trade-model-v1-pg-v7-$(date -u +%Y%m%d%H%M%S)-$$"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/postgresql-v7-evidence.XXXXXX")"
APP_PORT="18081"
APP_PID=""
CONTAINER_STARTED=0
CURRENT_STAGE="preflight"
FINAL_RESULT="FAIL"
CONTAINER_CLEANUP="NOT_STARTED"

safe_remove_evidence_dir() {
  case "${EVIDENCE_DIR}" in
    "${ROOT_DIR}/.runtime/postgresql-flyway-v7-evidence") rm -rf "${EVIDENCE_DIR}" ;;
    *) echo "CONTROLLED_POSTGRESQL_V7_RESULT: BLOCKED_UNSAFE_EVIDENCE_PATH"; exit 2 ;;
  esac
  mkdir -p "${EVIDENCE_DIR}"
}

stop_application() {
  if [ -n "${APP_PID}" ] && kill -0 "${APP_PID}" >/dev/null 2>&1; then
    kill "${APP_PID}" >/dev/null 2>&1 || true
    wait "${APP_PID}" >/dev/null 2>&1 || true
  fi
  APP_PID=""
}

remove_container() {
  if [ "${CONTAINER_STARTED}" -eq 1 ]; then
    if docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1; then
      CONTAINER_CLEANUP="PASS"
    else
      CONTAINER_CLEANUP="FAIL"
    fi
    CONTAINER_STARTED=0
  fi
}

cleanup() {
  status=$?
  stop_application
  remove_container
  rm -rf "${TMP_DIR}"
  if [ "${status}" -ne 0 ] && [ -d "${EVIDENCE_DIR}" ]; then
    {
      echo "CONTROLLED_POSTGRESQL_V7_RESULT: FAIL"
      echo "FAILED_STAGE: ${CURRENT_STAGE}"
      echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
      echo "PRODUCTION_READINESS: BLOCKED"
    } >"${EVIDENCE_DIR}/summary.txt"
  fi
}
trap cleanup EXIT INT TERM

fail() {
  echo "CONTROLLED_POSTGRESQL_V7_RESULT: FAIL"
  echo "FAILED_STAGE: ${CURRENT_STAGE}"
  exit 1
}

sha256_file() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    sha256sum "$1" | awk '{print $1}'
  fi
}

run_bounded() {
  timeout_seconds="$1"
  shift
  marker_file="${TMP_DIR}/bounded-timeout-$RANDOM"
  "$@" &
  command_pid=$!
  (
    sleep "${timeout_seconds}"
    if kill -0 "${command_pid}" >/dev/null 2>&1; then
      echo TIMEOUT >"${marker_file}"
      kill "${command_pid}" >/dev/null 2>&1 || true
    fi
  ) &
  watchdog_pid=$!
  set +e
  wait "${command_pid}"
  command_status=$?
  set -e
  kill "${watchdog_pid}" >/dev/null 2>&1 || true
  wait "${watchdog_pid}" >/dev/null 2>&1 || true
  if [ -f "${marker_file}" ]; then
    return 124
  fi
  return "${command_status}"
}

safe_remove_evidence_dir
cd "${ROOT_DIR}"

CURRENT_STAGE="docker-preflight"
for command_name in docker curl java; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "${command_name}: MISSING"
    fail
  fi
done
if ! docker info >/dev/null 2>&1; then
  echo "DOCKER_DAEMON: UNAVAILABLE"
  fail
fi
if docker ps --format '{{.Ports}}' | grep -q "127.0.0.1:${CONTROLLED_PORT}->"; then
  echo "CONTROLLED_PORT: IN_USE"
  fail
fi

CURRENT_STAGE="container-start"
DISPOSABLE_PASSWORD="controlled-$RANDOM-$RANDOM-$(date -u +%s)"
DISPOSABLE_ADMIN_PASSWORD="controlled-admin-$RANDOM-$RANDOM"
docker run \
  --name "${CONTAINER_NAME}" \
  --env "POSTGRES_DB=${DATABASE}" \
  --env "POSTGRES_USER=${USERNAME}" \
  --env "POSTGRES_PASSWORD=${DISPOSABLE_PASSWORD}" \
  --publish "127.0.0.1:${CONTROLLED_PORT}:5432" \
  --detach "${IMAGE_REF}" >/dev/null
CONTAINER_STARTED=1

ready=0
for _ in $(seq 1 60); do
  if docker exec "${CONTAINER_NAME}" pg_isready -U "${USERNAME}" -d "${DATABASE}" >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done
if [ "${ready}" -ne 1 ]; then
  echo "POSTGRESQL_READINESS: TIMEOUT"
  fail
fi

POSTGRESQL_VERSION="$(docker exec "${CONTAINER_NAME}" psql -U "${USERNAME}" -d "${DATABASE}" -Atqc 'SHOW server_version')"
POSTGRESQL_VERSION_NUM="$(docker exec "${CONTAINER_NAME}" psql -U "${USERNAME}" -d "${DATABASE}" -Atqc 'SHOW server_version_num')"
IMAGE_ARCH="$(docker image inspect "${IMAGE_REF}" --format '{{.Architecture}}')"

export CONTROLLED_POSTGRESQL_JDBC_URL="jdbc:postgresql://127.0.0.1:${CONTROLLED_PORT}/${DATABASE}"
export CONTROLLED_POSTGRESQL_USERNAME="${USERNAME}"
export CONTROLLED_POSTGRESQL_PASSWORD="${DISPOSABLE_PASSWORD}"
export CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM="I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL"
export CONTROLLED_POSTGRESQL_FLYWAY_RUN="I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB"
unset CONTROLLED_POSTGRESQL_KEEP_EVIDENCE_DATABASES || true

CURRENT_STAGE="targeted-postgresql-tests"
TARGETED_LOG="${TMP_DIR}/targeted-tests.log"
if ! run_bounded 600 ./mvnw -q \
  -Dtest=ControlledPostgreSqlFlywayV7EvidenceTest,ControlledPostgreSqlDashboardPlanValidityEvidenceTest,ControlledPostgreSqlHistoricalTimeInventorySemanticsTest,ControlledPostgreSqlFlywaySmokeTest,PostgreSqlFlywayMigrationSmokeTest \
  test >"${TARGETED_LOG}" 2>&1; then
  echo "CONTROLLED_POSTGRESQL_TESTS: FAIL_OR_TIMEOUT"
  fail
fi

report_files=(
  "target/surefire-reports/org.example.trademodel.postgresql.ControlledPostgreSqlFlywayV7EvidenceTest.txt"
  "target/surefire-reports/org.example.trademodel.service.impl.ControlledPostgreSqlDashboardPlanValidityEvidenceTest.txt"
  "target/surefire-reports/org.example.trademodel.postgresql.ControlledPostgreSqlHistoricalTimeInventorySemanticsTest.txt"
  "target/surefire-reports/org.example.trademodel.postgresql.ControlledPostgreSqlFlywaySmokeTest.txt"
  "target/surefire-reports/org.example.trademodel.postgresql.PostgreSqlFlywayMigrationSmokeTest.txt"
)
for report_file in "${report_files[@]}"; do
  if [ ! -f "${report_file}" ] || ! grep -q 'Failures: 0, Errors: 0, Skipped: 0' "${report_file}"; then
    echo "CONTROLLED_POSTGRESQL_REPORT: FAIL"
    fail
  fi
done

semantic_markers=(
  "INVENTORY_FIELD_POLICY_STATUS: PASS_EXPLICIT_14_FIELDS"
  "NORMAL_COOLDOWN_FALSE_POSITIVE_COUNT: 0"
  "NORMAL_FUTURE_VALID_FROM_FALSE_POSITIVE_COUNT: 0"
  "NORMAL_24H_EXPIRES_AT_MISMATCH_COUNT: 0"
  "NORMAL_VALIDITY_STATES: NOT_ACTIVE=1,ACTIVE=1,EXPIRED=1"
  "NORMAL_SESSION_TIMEZONE_CONSISTENCY: PASS"
  "TRUE_FUTURE_EVENT_ANOMALY_COUNT: 1"
  "AUDIT_ORDER_INVALID_COUNT: 1"
  "SCHEDULE_ORDER_INVALID_COUNT: 1"
  "VALIDITY_ORDER_INVALID_COUNT: 1"
  "VALIDITY_PARTIAL_NULL_COUNT: 1"
  "OFFSET_PATTERN_CANDIDATE_PLUS_8H_COUNT: 1"
  "ANOMALY_SESSION_TIMEZONE_CONSISTENCY: PASS"
)
for semantic_marker in "${semantic_markers[@]}"; do
  if ! grep -Fqx "${semantic_marker}" "${TARGETED_LOG}"; then
    echo "HISTORICAL_INVENTORY_SEMANTIC_EVIDENCE: FAIL"
    fail
  fi
done

semantic_report="target/surefire-reports/org.example.trademodel.postgresql.ControlledPostgreSqlHistoricalTimeInventorySemanticsTest.txt"
semantic_test_counts="$(sed -n -E \
  's/^Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+), Skipped: ([0-9]+).*/TESTS=\1,FAILURES=\2,ERRORS=\3,SKIPPED=\4/p' \
  "${semantic_report}")"
if [ "${semantic_test_counts}" != "TESTS=3,FAILURES=0,ERRORS=0,SKIPPED=0" ]; then
  echo "HISTORICAL_INVENTORY_SEMANTIC_TEST_COUNT: FAIL"
  fail
fi
{
  printf '%s\n' "${semantic_markers[@]}"
  echo "SESSION_TIMEZONES: UTC,Asia/Shanghai,America/New_York"
  echo "${semantic_test_counts}"
  echo "HISTORICAL_INVENTORY_SEMANTIC_FIXTURES: PASS_NOT_SKIPPED"
} >"${EVIDENCE_DIR}/inventory-semantic-results.txt"

CURRENT_STAGE="auxiliary-database-cleanup-check"
auxiliary_count="$(docker exec "${CONTAINER_NAME}" psql -U "${USERNAME}" -d postgres -Atqc \
  "SELECT COUNT(*) FROM pg_database WHERE datname IN ('trade_model_v1_fresh_test','trade_model_v1_upgrade_test')")"
if [ "${auxiliary_count}" != "0" ]; then
  echo "AUXILIARY_DATABASE_CLEANUP: FAIL"
  fail
fi

CURRENT_STAGE="evidence-snapshots"
docker exec "${CONTAINER_NAME}" psql -U "${USERNAME}" -d "${DATABASE}" -AtF '|' -c \
  "SELECT installed_rank, version, description, success, checksum FROM flyway_schema_history ORDER BY installed_rank" \
  >"${EVIDENCE_DIR}/flyway-history.txt"
docker exec "${CONTAINER_NAME}" psql -U "${USERNAME}" -d "${DATABASE}" -AtF '|' -c \
  "SELECT table_name, column_name, data_type, is_nullable FROM information_schema.columns WHERE table_schema='public' AND ((table_name='tm_decision_result' AND column_name IN ('valid_from','expires_at')) OR (table_name='tm_execution_plan' AND column_name IN ('manual_review_required','not_trade_instruction','not_executable','not_auto_trading','not_order_execution','not_user_position_creation'))) ORDER BY table_name, column_name" \
  >"${EVIDENCE_DIR}/schema-types.txt"
{
  echo "SESSION_TIMEZONES: UTC,Asia/Shanghai,America/New_York"
  echo "MIGRATION_TIMEZONE_RESULT: PASS_NOT_SKIPPED"
  echo "DASHBOARD_VALIDITY_RESULT: PASS_NOT_SKIPPED"
  echo "DASHBOARD_SCENARIOS: PLAN_NOT_ACTIVE,PLAN_EXPIRED,USABLE_REVIEW_PLAN,POSITION_HISTORY_EXPIRED,EQUIVALENT_OFFSETS"
  for report_file in "${report_files[@]}"; do
    grep 'Tests run:' "${report_file}"
  done
} >"${EVIDENCE_DIR}/timezone-results.txt"

docker exec \
  --env "PGOPTIONS=-c default_transaction_read_only=on -c statement_timeout=120000 -c lock_timeout=5000 -c idle_in_transaction_session_timeout=60000" \
  -i "${CONTAINER_NAME}" psql -U "${USERNAME}" -d "${DATABASE}" --no-psqlrc \
  <"scripts/historical-time-basis-inventory.sql" >"${EVIDENCE_DIR}/historical-inventory.txt"
inventory_field_count="$(grep -c '^FIELD_SUMMARY|' "${EVIDENCE_DIR}/historical-inventory.txt" || true)"
inventory_policy_count="$(grep -c '^FIELD_POLICY|' "${EVIDENCE_DIR}/historical-inventory.txt" || true)"
if [ "${inventory_field_count}" != "14" ] \
  || [ "${inventory_policy_count}" != "14" ] \
  || grep -Eq 'VERIFIED_UTC|POST_CUTOVER_UTC|REFERENCE_MISMATCH|^OFFSET_PATTERN\|' \
    "${EVIDENCE_DIR}/historical-inventory.txt" \
  || ! grep -Eq '^AGGREGATE_MD5\|[0-9a-f]{32}$' "${EVIDENCE_DIR}/historical-inventory.txt"; then
  echo "HISTORICAL_TIME_INVENTORY: FAIL"
  fail
fi

CURRENT_STAGE="application-smoke"
export SPRING_DATASOURCE_URL="${CONTROLLED_POSTGRESQL_JDBC_URL}"
export SPRING_DATASOURCE_USERNAME="${USERNAME}"
export SPRING_DATASOURCE_PASSWORD="${DISPOSABLE_PASSWORD}"
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
export SPRING_FLYWAY_ENABLED=true
export SPRING_SQL_INIT_MODE=never
export APP_ADMIN_USERNAME=controlled_admin
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

APP_LOG="${TMP_DIR}/application.log"
./mvnw -q -Pflyway-migration \
  -Dspring-boot.run.arguments="--server.address=127.0.0.1 --server.port=${APP_PORT}" \
  spring-boot:run >"${APP_LOG}" 2>&1 &
APP_PID=$!

health_ready=0
for _ in $(seq 1 120); do
  if ! kill -0 "${APP_PID}" >/dev/null 2>&1; then
    echo "APPLICATION_STARTUP: PROCESS_EXITED"
    fail
  fi
  if curl --silent --fail --max-time 5 "http://127.0.0.1:${APP_PORT}/actuator/health" \
    >"${TMP_DIR}/health.json" 2>/dev/null; then
    health_ready=1
    break
  fi
  sleep 1
done
if [ "${health_ready}" -ne 1 ]; then
  echo "APPLICATION_STARTUP: TIMEOUT"
  fail
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

health_status="$(curl --config "${CURL_CONFIG}" --output "${TMP_DIR}/health-auth.json" \
  --write-out '%{http_code}' "http://127.0.0.1:${APP_PORT}/actuator/health")"
home_status="$(curl --config "${CURL_CONFIG}" --output "${TMP_DIR}/dashboard-home.json" \
  --write-out '%{http_code}' "http://127.0.0.1:${APP_PORT}/api/dashboard/home")"
baseline_status="$(curl --config "${CURL_CONFIG}" --output "${TMP_DIR}/run-baseline.json" \
  --write-out '%{http_code}' "http://127.0.0.1:${APP_PORT}/api/system/run-baseline")"

if [ "${health_status}" != "200" ] \
  || [ "${home_status}" != "200" ] \
  || [ "${baseline_status}" != "200" ]; then
  echo "APPLICATION_ENDPOINTS: FAIL"
  fail
fi
if ! grep -q '"status":"UP"' "${TMP_DIR}/health-auth.json"; then
  echo "APPLICATION_HEALTH: FAIL"
  fail
fi
for safety_field in notTradeInstruction notExecutable notAutoTrading notOrderExecution notUserPositionCreation notUserPositionMutation; do
  if ! grep -q "\"${safety_field}\":true" "${TMP_DIR}/dashboard-home.json"; then
    echo "DASHBOARD_SAFETY: FAIL"
    fail
  fi
done
if ! grep -Eq '"runStatus":"(NOT_CALLED|DISABLED)"' "${TMP_DIR}/dashboard-home.json"; then
  echo "DASHBOARD_AI_STATUS: FAIL"
  fail
fi

business_counts="$(docker exec "${CONTAINER_NAME}" psql -U "${USERNAME}" -d "${DATABASE}" -AtF '|' -c \
  "SELECT 'tm_analysis_run',COUNT(*) FROM tm_analysis_run UNION ALL SELECT 'tm_decision_result',COUNT(*) FROM tm_decision_result UNION ALL SELECT 'tm_execution_plan',COUNT(*) FROM tm_execution_plan UNION ALL SELECT 'tm_user_position',COUNT(*) FROM tm_user_position UNION ALL SELECT 'tm_monitor_alert',COUNT(*) FROM tm_monitor_alert UNION ALL SELECT 'tm_push_snapshot',COUNT(*) FROM tm_push_snapshot UNION ALL SELECT 'tm_push_recheck_log',COUNT(*) FROM tm_push_recheck_log UNION ALL SELECT 'tm_hot_reset_event',COUNT(*) FROM tm_hot_reset_event ORDER BY 1")"
if printf '%s\n' "${business_counts}" | awk -F '|' '$2 != 0 { exit 1 }'; then
  business_write_result="ZERO_BUSINESS_ROWS"
else
  echo "APPLICATION_BUSINESS_WRITE_CHECK: FAIL"
  fail
fi

{
  echo "HEALTH: HTTP_200_UP"
  echo "DASHBOARD_HOME: HTTP_200_FAIL_CLOSED"
  echo "RUN_BASELINE: HTTP_200"
  echo "AI_PROVIDER_STATE: NOT_CALLED_OR_DISABLED"
  echo "SCHEDULERS: DISABLED"
  echo "EXTERNAL_PROVIDER_CALLS: DISABLED"
  echo "BUSINESS_WRITE_RESULT: ${business_write_result}"
  printf '%s\n' "${business_counts}"
} >"${EVIDENCE_DIR}/application-smoke.txt"

stop_application

CURRENT_STAGE="container-cleanup"
remove_container
if [ "${CONTAINER_CLEANUP}" != "PASS" ]; then
  fail
fi

CURRENT_STAGE="summary"
EXECUTED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
RUNNER_SHA256="$(sha256_file "scripts/controlled-postgresql-flyway-v7-evidence.sh")"
INVENTORY_SEMANTIC_SHA256="$(sha256_file "${EVIDENCE_DIR}/inventory-semantic-results.txt")"
{
  echo "CONTROLLED_POSTGRESQL_V7_RESULT: PASS"
  echo "EXECUTED_AT_UTC: ${EXECUTED_AT}"
  echo "IMAGE_TAG: ${IMAGE_TAG}"
  echo "IMAGE_DIGEST: ${IMAGE_REF}"
  echo "POSTGRESQL_SERVER_VERSION: ${POSTGRESQL_VERSION}"
  echo "POSTGRESQL_SERVER_VERSION_NUM: ${POSTGRESQL_VERSION_NUM}"
  echo "CPU_ARCHITECTURE: ${IMAGE_ARCH}"
  echo "FRESH_V1_TO_V7: PASS"
  echo "V6_TO_V7: PASS"
  echo "SESSION_TIMEZONES: PASS_NOT_SKIPPED"
  echo "POSTGRESQL_DASHBOARD_VALIDITY: PASS_NOT_SKIPPED"
  echo "HISTORICAL_INVENTORY_SEMANTICS: PASS_NOT_SKIPPED"
  echo "MAPPER_COMPATIBILITY: PASS"
  echo "APPLICATION_SMOKE: PASS"
  echo "HISTORICAL_TIME_INVENTORY: PASS_READ_ONLY"
  echo "AUXILIARY_DATABASE_CLEANUP: PASS"
  echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
  echo "RUNNER_SHA256: ${RUNNER_SHA256}"
  echo "INVENTORY_SEMANTIC_EVIDENCE_SHA256: ${INVENTORY_SEMANTIC_SHA256}"
  echo "PRODUCTION_READINESS: BLOCKED"
} >"${EVIDENCE_DIR}/summary.txt"

: >"${EVIDENCE_DIR}/checksums.txt"
for evidence_file in \
  summary.txt \
  flyway-history.txt \
  schema-types.txt \
  timezone-results.txt \
  inventory-semantic-results.txt \
  application-smoke.txt \
  historical-inventory.txt; do
  echo "$(sha256_file "${EVIDENCE_DIR}/${evidence_file}")  ${evidence_file}" \
    >>"${EVIDENCE_DIR}/checksums.txt"
done

FINAL_RESULT="PASS"
cat "${EVIDENCE_DIR}/summary.txt"
echo "EVIDENCE_ARTIFACTS: .runtime/postgresql-flyway-v7-evidence"
