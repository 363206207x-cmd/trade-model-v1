#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
container="trade-model-v14-smoke-$RANDOM-$$"
database="trade_model_v14_smoke"
db_user="trade_model_smoke"
db_password="db-$RANDOM-$RANDOM-$RANDOM"
db_port="${STANDARD_JAR_POSTGRES_PORT:-55438}"
app_port="${STANDARD_JAR_APP_PORT:-18091}"
app_pid=""
app_log="$(mktemp)"
build_log="$(mktemp)"
password_file="$(mktemp)"
rm -f -- "${password_file}"
chmod 600 "${app_log}" "${build_log}"

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM
  if [[ -n "${app_pid}" ]] && kill -0 "${app_pid}" 2>/dev/null; then
    kill -TERM "${app_pid}" 2>/dev/null || true
    wait "${app_pid}" 2>/dev/null || true
  fi
  docker rm -f "${container}" >/dev/null 2>&1 || true
  rm -f -- "${app_log}" "${build_log}" "${password_file}"
  exit "${exit_code}"
}
trap cleanup EXIT INT TERM

if ! docker info >/dev/null 2>&1; then
  printf '%s\n' "STANDARD_JAR_POSTGRESQL=BLOCKED_DOCKER_UNAVAILABLE"
  exit 2
fi

cd "${repo_root}"
if [[ "${1:-}" != "--skip-package" ]]; then
  if ! ./mvnw clean package -q >"${build_log}" 2>&1; then
    printf '%s\n' "STANDARD_CLEAN_PACKAGE=FAILED"
    exit 1
  fi
fi

jar_path="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"
if [[ -z "${jar_path}" ]]; then
  printf '%s\n' "STANDARD_JAR=NOT_FOUND"
  exit 1
fi
jar_listing="$(jar tf "${jar_path}")"
grep -F 'BOOT-INF/lib/flyway-core-' <<<"${jar_listing}" >/dev/null
grep -F 'BOOT-INF/lib/flyway-database-postgresql-' <<<"${jar_listing}" >/dev/null
for version in $(seq 1 15); do
  grep -F "BOOT-INF/classes/db/migration/V${version}__" <<<"${jar_listing}" >/dev/null
done
printf '%s\n' "STANDARD_JAR_FLYWAY_CONTENT=PASS"

java -cp target/classes org.example.trademodel.security.RuntimePasswordTool \
  generate --env-file "${password_file}" >/dev/null
# shellcheck disable=SC1090
source "${password_file}"
export TRADE_MODEL_INITIAL_PASSWORD

docker run --detach --rm \
  --name "${container}" \
  --publish "127.0.0.1:${db_port}:5432" \
  --env "POSTGRES_DB=${database}" \
  --env "POSTGRES_USER=${db_user}" \
  --env "POSTGRES_PASSWORD=${db_password}" \
  postgres:16-alpine >/dev/null

for _ in $(seq 1 60); do
  if docker exec "${container}" pg_isready -U "${db_user}" -d "${database}" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
docker exec "${container}" pg_isready -U "${db_user}" -d "${database}" >/dev/null

export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT="${app_port}"
export SERVER_ADDRESS=127.0.0.1
export PROD_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:${db_port}/${database}"
export PROD_DATASOURCE_USERNAME="${db_user}"
export PROD_DATASOURCE_PASSWORD="${db_password}"
export POSITION_PROVIDER_TYPE=BINANCE
export BINANCE_API_KEY="controlled-smoke-key"
export BINANCE_API_SECRET="controlled-smoke-secret"
export TRADE_MODEL_AUTH_ENABLED=true
export TRADE_MODEL_INITIAL_USERNAME="xuchao"
export TRADE_MODEL_SESSION_COOKIE_SECURE=true
export TRADE_MODEL_PRODUCTION_ALLOW_PUBLIC_BIND=false
export TRADE_MODEL_PRODUCTION_SCHEDULER_POLICY=LOCKED_DOWN
export TRADE_MODEL_SCHEDULERS_ENABLED=false
export TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED=false
export TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED=false
export TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED=false
export TRADE_MODEL_WATCHLIST_SCHEDULER_ENABLED=false
export TRADE_MODEL_ANALYSIS_SCHEDULER_ENABLED=false
export TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED=false
export TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_PUSH_RECHECK=PROD_BLOCKED
export TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_POSITION_SYNC=PROD_BLOCKED
export TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_MARKET_DATA=PROD_BLOCKED
export TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_OHLCV_INGESTION=PROD_BLOCKED
export TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_WATCHLIST=PROD_BLOCKED
export TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_POSITION_MONITOR=PROD_BLOCKED
export TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_ANALYSIS=PROD_BLOCKED
export TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_PROVIDER_SCAN=PROD_BLOCKED
export TRADE_MODEL_SECURITY_RATE_LIMIT_ENABLED=true
export TRADE_MODEL_SECURITY_RATE_LIMIT_RPM=120
export TRADE_MODEL_SECURITY_RATE_LIMIT_WINDOW_MS=60000
export TRADE_MODEL_AI_ENABLED=false
export TRADE_MODEL_PROVIDER_CALL_ENABLED=false
export TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED=false
export TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED=false
export TRADE_MODEL_PUBLIC_OHLCV_EXTERNAL_CALLS_ENABLED=false
export TRADE_MODEL_KRAKEN_OHLCV_ENABLED=false
export TRADE_MODEL_BINANCE_OHLCV_ENABLED=false
export TRADE_MODEL_COINGLASS_ENABLED=false

start_app() {
  : >"${app_log}"
  java -jar "${jar_path}" >"${app_log}" 2>&1 &
  app_pid=$!
}

wait_ready() {
  for _ in $(seq 1 90); do
    if curl --fail --silent "http://127.0.0.1:${app_port}/actuator/health/readiness" \
        | grep -q '"status":"UP"'; then
      return 0
    fi
    if ! kill -0 "${app_pid}" 2>/dev/null; then
      return 1
    fi
    sleep 1
  done
  return 1
}

stop_app() {
  kill -TERM "${app_pid}" 2>/dev/null || true
  wait "${app_pid}" 2>/dev/null || true
  app_pid=""
}

print_startup_failure() {
  printf '%s\n' "STANDARD_JAR_STARTUP_DIAGNOSTIC_BEGIN"
  grep -E 'APPLICATION FAILED|ERROR|Exception|Caused by:|Flyway|bootstrap|readiness' \
    "${app_log}" | tail -n 80 || true
  printf '%s\n' "STANDARD_JAR_STARTUP_DIAGNOSTIC_END"
}

start_app
if ! wait_ready; then
  print_startup_failure
  printf '%s\n' "STANDARD_JAR_EMPTY_DATABASE_START=FAILED"
  exit 1
fi
APP_URL="http://127.0.0.1:${app_port}" \
TRADE_MODEL_SMOKE_USERNAME="${TRADE_MODEL_INITIAL_USERNAME}" \
TRADE_MODEL_SMOKE_PASSWORD="${TRADE_MODEL_INITIAL_PASSWORD}" \
SMOKE_ALLOW_EXTERNAL_CALLS=false \
  bash scripts/prod-smoke.sh
printf '%s\n' "PACKAGED_JAR_LOGIN_SESSION_LOGOUT=PASS"
migration_count="$(docker exec "${container}" psql -U "${db_user}" -d "${database}" -Atc \
  "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE AND version IS NOT NULL")"
if [[ "${migration_count}" != "15" ]]; then
  printf '%s\n' "POSTGRESQL_V1_V15=FAILED"
  exit 1
fi
printf '%s\n' "POSTGRESQL_V1_V15=15/15_PASS"
stop_app

start_app
if ! wait_ready; then
  print_startup_failure
  printf '%s\n' "PACKAGED_JAR_EXISTING_V15_RESTART=FAILED"
  exit 1
fi
printf '%s\n' "PACKAGED_JAR_EXISTING_V15_RESTART=PASS"
stop_app

docker exec "${container}" psql -U "${db_user}" -d "${database}" -v ON_ERROR_STOP=1 -c \
  "UPDATE flyway_schema_history SET checksum = checksum + 1 WHERE version = '14'" >/dev/null
start_app
for _ in $(seq 1 45); do
  if ! kill -0 "${app_pid}" 2>/dev/null; then
    wait "${app_pid}" 2>/dev/null || true
    app_pid=""
    break
  fi
  sleep 1
done
if [[ -n "${app_pid}" ]] && kill -0 "${app_pid}" 2>/dev/null; then
  printf '%s\n' "PACKAGED_JAR_CHECKSUM_FAILURE=FAILED_APP_STILL_RUNNING"
  exit 1
fi
if curl --fail --silent "http://127.0.0.1:${app_port}/actuator/health/readiness" >/dev/null 2>&1; then
  printf '%s\n' "PACKAGED_JAR_MIGRATION_FAILURE_READINESS=FAILED"
  exit 1
fi
printf '%s\n' "PACKAGED_JAR_CHECKSUM_FAILURE=PASS_FAIL_CLOSED"
printf '%s\n' "PACKAGED_JAR_MIGRATION_FAILURE_READINESS=PASS"
printf '%s\n' "STANDARD_RELEASE_POSTGRESQL_SMOKE=PASS"
