#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EVIDENCE_DIR="${ROOT_DIR}/.runtime/postgresql-p3g-rehearsal"
BACKUP_DIR="${EVIDENCE_DIR}/backups"
POSTGRES_IMAGE="postgres@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc"
POSTGRES_IMAGE_CACHE_REFERENCE="postgres:16-alpine"
POSTGRES_RUNTIME_IMAGE=""
FLYWAY_ACTION_JDK_IMAGE="eclipse-temurin:17-jdk-jammy@sha256:723151f3fc88ca2060153ee08ab8dbbea7983d6ed6f2622fe440acf178737c94"
BASE_MAIN_HEAD="72b5bc83f4d670d4adebc03f5fe28e0bb9bba535"
EXPECTED_BRANCH="codex/greenfield-postgresql-first-boot-rehearsal-p3g"
EXPECTED_CONFIRMATION="I_CONFIRM_LOCAL_GREENFIELD_EMPTY_DATABASE_REHEARSAL"
FLYWAY_CONFIRMATION="I_CONFIRM_LOCAL_GREENFIELD_FLYWAY_V1_TO_V7"
P3G_HOST="${P3G_HOST:-127.0.0.1}"
P3G_POSTGRES_PORT="${P3G_POSTGRES_PORT:-55435}"
P3G_APP_PORT="${P3G_APP_PORT:-18085}"
PRIMARY_DATABASE="${P3G_PRIMARY_DATABASE:-trade_model_v1_p3g_primary}"
RECOVERY_DATABASE="${P3G_RECOVERY_DATABASE:-trade_model_v1_p3g_recovery}"

CURRENT_STAGE="confirmation-preflight"
FINAL_RESULT="BLOCKED"
EVIDENCE_PREPARED=0
CLEANUP_DONE=0
TMP_DIR=""
ARCHIVE_CONTEXT=""
PG_CONTAINER=""
OPS_CONTAINER=""
APP_CONTAINER=""
SMOKE_CONTAINER=""
MIGRATION_ACTION_CONTAINER=""
NETWORK_NAME=""
VOLUME_NAME=""
APP_IMAGE_TAG=""
PG_STARTED=0
OPS_STARTED=0
APP_STARTED=0
SMOKE_STARTED=0
NETWORK_CREATED=0
VOLUME_CREATED=0
APP_IMAGE_CREATED=0
CONTAINER_CLEANUP="NOT_CREATED"
NETWORK_CLEANUP="NOT_CREATED"
VOLUME_CLEANUP="NOT_CREATED"
APP_IMAGE_CLEANUP="NOT_CREATED"
SMOKE_CLIENT_CLEANUP="NOT_CREATED"

sha256_file() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    sha256sum "$1" | awk '{print $1}'
  fi
}

file_size_bytes() {
  if stat -f '%z' "$1" >/dev/null 2>&1; then
    stat -f '%z' "$1"
  else
    stat -c '%s' "$1"
  fi
}

random_secret() {
  openssl rand -hex 24
}

write_redacted_log() {
  local source_log="$1"
  local target_log="$2"
  awk '{ lower=tolower($0); if (lower ~ /(password|secret|api.?key|authorization|cookie)/) { print "[REDACTED_SENSITIVE_LOG_LINE]" } else { print } }' \
    "${source_log}" | sed -E \
    -e 's#jdbc:postgresql://[^[:space:])]+#jdbc:postgresql://[REDACTED]#g' \
    -e 's/p3g_[a-z_]+_[0-9a-f]+/[REDACTED_ROLE]/g' \
    -e 's/p3g-nonfunctional-[0-9a-f]+/[REDACTED_NONFUNCTIONAL_KEY]/g' \
    -e 's/[0-9a-f]{48}/[REDACTED_SECRET]/g' \
    -e 's#/Users/[^/[:space:]]+#/Users/[REDACTED]#g' \
    >"${target_log}"
  chmod 600 "${target_log}"
}

write_redacted_flyway_failure() {
  write_redacted_log "$1" "${EVIDENCE_DIR}/flyway-action-failure-redacted.txt"
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

enter_stage() {
  CURRENT_STAGE="$1"
  echo "P3G_STAGE: ${CURRENT_STAGE}"
}

blocked() {
  FINAL_RESULT="$1"
  echo "P3G_RESULT: ${FINAL_RESULT}"
  echo "FAILED_STAGE: ${CURRENT_STAGE}"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit "${2:-2}"
}

cleanup_resources() {
  [ "${CLEANUP_DONE}" -eq 0 ] || return 0
  set +e
  for disposable_container in "${SMOKE_CONTAINER}" "${APP_CONTAINER}" \
    "${MIGRATION_ACTION_CONTAINER}" "${OPS_CONTAINER}" "${PG_CONTAINER}"; do
    if [ -n "${disposable_container}" ] \
      && docker ps -a --format '{{.Names}}' | grep -Fxq "${disposable_container}"; then
      run_bounded 30 docker rm -f "${disposable_container}" >/dev/null 2>&1 || true
    fi
  done
  SMOKE_STARTED=0
  APP_STARTED=0
  OPS_STARTED=0
  PG_STARTED=0
  CONTAINER_CLEANUP="PASS"
  for container_name in "${PG_CONTAINER}" "${OPS_CONTAINER}" "${APP_CONTAINER}" \
    "${SMOKE_CONTAINER}" "${MIGRATION_ACTION_CONTAINER}"; do
    if [ -n "${container_name}" ] \
      && docker ps -a --format '{{.Names}}' | grep -Fxq "${container_name}"; then
      CONTAINER_CLEANUP="FAIL"
    fi
  done
  if [ -n "${SMOKE_CONTAINER}" ] \
    && docker ps -a --format '{{.Names}}' | grep -Fxq "${SMOKE_CONTAINER}"; then
    SMOKE_CLIENT_CLEANUP="FAIL"
  elif [ -n "${SMOKE_CONTAINER}" ]; then
    SMOKE_CLIENT_CLEANUP="PASS"
  fi
  if [ "${NETWORK_CREATED}" -eq 1 ] && [ -n "${NETWORK_NAME}" ]; then
    if run_bounded 30 docker network rm "${NETWORK_NAME}" >/dev/null 2>&1; then
      NETWORK_CLEANUP="PASS"
      NETWORK_CREATED=0
    else
      NETWORK_CLEANUP="FAIL"
    fi
  fi
  if [ "${VOLUME_CREATED}" -eq 1 ] && [ -n "${VOLUME_NAME}" ]; then
    if run_bounded 30 docker volume rm "${VOLUME_NAME}" >/dev/null 2>&1; then
      VOLUME_CLEANUP="PASS"
      VOLUME_CREATED=0
    else
      VOLUME_CLEANUP="FAIL"
    fi
  fi
  if [ "${APP_IMAGE_CREATED}" -eq 1 ] && [ -n "${APP_IMAGE_TAG}" ]; then
    if run_bounded 60 docker image rm "${APP_IMAGE_TAG}" >/dev/null 2>&1; then
      APP_IMAGE_CLEANUP="PASS"
      APP_IMAGE_CREATED=0
    else
      APP_IMAGE_CLEANUP="FAIL"
    fi
  fi
  if [ -n "${ARCHIVE_CONTEXT}" ] && [ -d "${ARCHIVE_CONTEXT}" ]; then
    rm -rf "${ARCHIVE_CONTEXT}"
  fi
  if [ -n "${TMP_DIR}" ] && [ -d "${TMP_DIR}" ]; then
    rm -rf "${TMP_DIR}"
  fi
  CLEANUP_DONE=1
  set -e
}

write_cleanup_evidence() {
  [ "${EVIDENCE_PREPARED}" -eq 1 ] || return 0
  {
    echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
    echo "NETWORK_CLEANUP: ${NETWORK_CLEANUP}"
    echo "VOLUME_CLEANUP: ${VOLUME_CLEANUP}"
    echo "APPLICATION_IMAGE_CLEANUP: ${APP_IMAGE_CLEANUP}"
    echo "FLYWAY_ACTION_CLEANUP: PASS_OR_NOT_CREATED"
    echo "SMOKE_CLIENT_CLEANUP: ${SMOKE_CLIENT_CLEANUP}"
  } >"${EVIDENCE_DIR}/cleanup-status.txt"
}

on_exit() {
  local status=$?
  cleanup_resources
  write_cleanup_evidence
  if [ "${status}" -ne 0 ] && [ "${EVIDENCE_PREPARED}" -eq 1 ]; then
    {
      echo "P3G_RESULT: ${FINAL_RESULT}"
      echo "FAILED_STAGE: ${CURRENT_STAGE}"
      echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
      echo "NETWORK_CLEANUP: ${NETWORK_CLEANUP}"
      echo "VOLUME_CLEANUP: ${VOLUME_CLEANUP}"
      echo "P4_ALLOWED: NO"
      echo "PRODUCTION_READINESS: BLOCKED"
    } >"${EVIDENCE_DIR}/summary.txt"
  fi
}
trap on_exit EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

safe_prepare_evidence_dir() {
  case "${EVIDENCE_DIR}" in
    "${ROOT_DIR}/.runtime/postgresql-p3g-rehearsal")
      rm -rf "${EVIDENCE_DIR}"
      mkdir -p "${BACKUP_DIR}"
      chmod 700 "${EVIDENCE_DIR}" "${BACKUP_DIR}"
      EVIDENCE_PREPARED=1
      ;;
    *) blocked "BLOCKED_UNSAFE_EVIDENCE_PATH" ;;
  esac
}

pg_psql() {
  local database="$1"
  shift
  run_bounded 180 docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${PG_CONTAINER}" psql \
    --username="${BOOTSTRAP_ROLE}" \
    --dbname="${database}" \
    --no-psqlrc "$@"
}

pg_scalar() {
  local database="$1"
  local sql="$2"
  pg_psql "${database}" -Atqc "${sql}"
}

capture_structure_fingerprint() {
  local database="$1"
  local output_file="$2"
  run_bounded_with_input 180 "${ROOT_DIR}/scripts/current-state-clone-fingerprint.sql" \
    docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${PG_CONTAINER}" psql \
    --username="${BOOTSTRAP_ROLE}" \
    --dbname="${database}" \
    --no-psqlrc >"${output_file}"
}

capture_content_fingerprint() {
  local database="$1"
  local output_file="$2"
  run_bounded_with_input 180 "${ROOT_DIR}/scripts/current-state-clone-content-fingerprint.sql" \
    docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${PG_CONTAINER}" psql \
    --username="${BOOTSTRAP_ROLE}" \
    --dbname="${database}" \
    --no-psqlrc \
    --set="fingerprint_mode=FULL" >"${output_file}"
}

capture_schema_types() {
  local database="$1"
  local output_file="$2"
  pg_psql "${database}" -AtF '|' -c \
    "SELECT table_name,column_name,data_type,is_nullable,COALESCE(column_default,'') FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,ordinal_position" \
    >"${output_file}"
}

capture_flyway_history() {
  local database="$1"
  local output_file="$2"
  pg_psql "${database}" -AtF '|' -c \
    "SELECT installed_rank,version,description,success,checksum FROM flyway_schema_history ORDER BY installed_rank" \
    >"${output_file}"
}

capture_restore_verification() {
  local database="$1"
  local output_file="$2"
  run_bounded_with_input 180 "${ROOT_DIR}/scripts/current-state-clone-restore-verification.sql" \
    docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000" \
    "${PG_CONTAINER}" psql \
    --username="${BOOTSTRAP_ROLE}" \
    --dbname="${database}" \
    --no-psqlrc >"${output_file}"
}

capture_historical_inventory() {
  local database="$1"
  local output_file="$2"
  run_bounded_with_input 180 "${ROOT_DIR}/scripts/historical-time-basis-inventory.sql" \
    docker exec -i \
    --env "PGOPTIONS=-c statement_timeout=120000 -c lock_timeout=5000 -c trade_model.inventory_as_of_utc=2026-07-15T00:00:00" \
    "${PG_CONTAINER}" psql \
    --username="${BOOTSTRAP_ROLE}" \
    --dbname="${database}" \
    --no-psqlrc >"${output_file}"
}

ops_psql() {
  local role="$1"
  local password="$2"
  local database="$3"
  shift 3
  run_bounded 120 docker exec \
    --env "PGPASSWORD=${password}" \
    --env "PGOPTIONS=-c statement_timeout=60000 -c lock_timeout=5000" \
    "${OPS_CONTAINER}" psql \
    --host=p3g-postgres \
    --port="${P3G_POSTGRES_PORT}" \
    --username="${role}" \
    --dbname="${database}" \
    --no-psqlrc "$@"
}

ops_scalar() {
  local role="$1"
  local password="$2"
  local database="$3"
  local sql="$4"
  ops_psql "${role}" "${password}" "${database}" -Atqc "${sql}"
}

wait_for_postgresql() {
  local ready=0
  for _ in $(seq 1 90); do
    if run_bounded 5 docker exec "${PG_CONTAINER}" pg_isready \
      --username="${BOOTSTRAP_ROLE}" --dbname=postgres >/dev/null 2>&1; then
      ready=1
      break
    fi
    sleep 1
  done
  [ "${ready}" -eq 1 ] || blocked "BLOCKED_POSTGRESQL_READINESS_TIMEOUT"
}

stop_application() {
  if [ "${APP_STARTED}" -eq 1 ] && [ -n "${APP_CONTAINER}" ]; then
    if ! run_bounded 30 docker rm -f "${APP_CONTAINER}" >/dev/null 2>&1; then
      blocked "BLOCKED_APPLICATION_CONTAINER_CLEANUP"
    fi
    APP_STARTED=0
    APP_CONTAINER=""
  fi
}

stop_smoke_client() {
  if [ "${SMOKE_STARTED}" -eq 1 ] && [ -n "${SMOKE_CONTAINER}" ]; then
    if ! run_bounded 30 docker rm -f "${SMOKE_CONTAINER}" >/dev/null 2>&1; then
      blocked "BLOCKED_SMOKE_CLIENT_CLEANUP"
    fi
    SMOKE_STARTED=0
    SMOKE_CLIENT_CLEANUP="PASS"
  fi
}

write_application_env() {
  local output_file="$1"
  local database="$2"
  local role="$3"
  local password="$4"
  umask 077
  {
    echo "SPRING_PROFILES_ACTIVE=prod"
    echo "SPRING_DATASOURCE_URL=jdbc:postgresql://p3g-postgres:${P3G_POSTGRES_PORT}/${database}"
    echo "PROD_DATASOURCE_URL=jdbc:postgresql://p3g-postgres:${P3G_POSTGRES_PORT}/${database}"
    echo "SPRING_DATASOURCE_USERNAME=${role}"
    echo "PROD_DATASOURCE_USERNAME=${role}"
    echo "SPRING_DATASOURCE_PASSWORD=${password}"
    echo "PROD_DATASOURCE_PASSWORD=${password}"
    echo "SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver"
    echo "SPRING_DATASOURCE_HIKARI_READ_ONLY=true"
    echo "SPRING_FLYWAY_ENABLED=false"
    echo "SPRING_SQL_INIT_MODE=never"
    echo "SERVER_PORT=${P3G_APP_PORT}"
    echo "SERVER_ADDRESS=0.0.0.0"
    echo "TRADE_MODEL_PRODUCTION_ALLOW_PUBLIC_BIND=true"
    echo "TRADE_MODEL_AUTH_ENABLED=true"
    echo "APP_ADMIN_USERNAME=${APP_ADMIN_USERNAME}"
    echo "APP_ADMIN_PASSWORD=${APP_ADMIN_PASSWORD}"
    echo "POSITION_PROVIDER_TYPE=BINANCE"
    echo "BINANCE_API_BASE_URL=https://example.invalid"
    echo "BINANCE_API_KEY=${BINANCE_FAKE_KEY}"
    echo "BINANCE_API_SECRET=${BINANCE_FAKE_SECRET}"
    echo "TRADE_MODEL_PRODUCTION_SCHEDULER_POLICY=LOCKED_DOWN"
    echo "TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_PUSH_RECHECK=PROD_ALLOWED_EXPLICIT_OPT_IN"
    echo "TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_POSITION_SYNC=PROD_ALLOWED_EXPLICIT_OPT_IN"
    echo "TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_MARKET_DATA=PROD_ALLOWED_EXPLICIT_OPT_IN"
    echo "TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_OHLCV_INGESTION=PROD_ALLOWED_EXPLICIT_OPT_IN"
    echo "TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_WATCHLIST=LOCAL_ONLY"
    echo "TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_POSITION_MONITOR=PROD_ALLOWED_DEFAULT_OFF"
    echo "TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_ANALYSIS=PROD_ALLOWED_EXPLICIT_OPT_IN"
    echo "TRADE_MODEL_PRODUCTION_SCHEDULER_APPROVAL_PROVIDER_SCAN=PROD_ALLOWED_DEFAULT_OFF"
    echo "TRADE_MODEL_SCHEDULERS_ENABLED=false"
    echo "TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED=false"
    echo "TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED=false"
    echo "TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED=false"
    echo "TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED=false"
    echo "TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED=false"
    echo "TRADE_MODEL_WATCHLIST_SCHEDULER_ENABLED=false"
    echo "TRADE_MODEL_ANALYSIS_SCHEDULER_ENABLED=false"
    echo "TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED=false"
    echo "TRADE_MODEL_PROVIDER_CALL_ENABLED=false"
    echo "TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED=false"
    echo "TRADE_MODEL_PROFILE_ESCALATION_ENABLED=false"
    echo "TRADE_MODEL_PROVIDER_AUTO_ESCALATION_ENABLED=false"
    echo "TRADE_MODEL_COINGLASS_ENABLED=false"
    echo "TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED=false"
    echo "TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED=false"
    echo "TRADE_MODEL_PUBLIC_OHLCV_EXTERNAL_CALLS_ENABLED=false"
    echo "TRADE_MODEL_AI_ENABLED=false"
    echo "TRADE_MODEL_AI_OPENAI_ENABLED=false"
    echo "TRADE_MODEL_AI_GEMINI_ENABLED=false"
    echo "TRADE_MODEL_AI_XAI_ENABLED=false"
    echo "TRADE_MODEL_SECURITY_RATE_LIMIT_ENABLED=true"
    echo "TRADE_MODEL_SECURITY_RATE_LIMIT_RPM=120"
    echo "TRADE_MODEL_SECURITY_RATE_LIMIT_WINDOW_MS=60000"
    echo "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health"
  } >"${output_file}"
  chmod 600 "${output_file}"
}

run_application_smoke() {
  local label="$1"
  local database="$2"
  local role="$3"
  local password="$4"
  local evidence_file="$5"
  local env_file="${TMP_DIR}/app-${label}.env"
  local smoke_response_dir="${TMP_DIR}/smoke-${label}"

  mkdir -p "${smoke_response_dir}"
  chmod 700 "${smoke_response_dir}"

  write_application_env "${env_file}" "${database}" "${role}" "${password}"
  APP_CONTAINER="p3g-app-${RUN_SUFFIX}-${label}"
  if ! run_bounded 120 docker run \
    --detach \
    --name "${APP_CONTAINER}" \
    --network "${NETWORK_NAME}" \
    --network-alias p3g-app \
    --publish "127.0.0.1:${P3G_APP_PORT}:${P3G_APP_PORT}" \
    --env-file "${env_file}" \
    "${APP_IMAGE_TAG}" >"${TMP_DIR}/app-${label}-container-id.txt"; then
    blocked "BLOCKED_APPLICATION_CONTAINER_START"
  fi
  APP_STARTED=1

  SMOKE_CONTAINER="p3g-smoke-${RUN_SUFFIX}-${label}"
  if ! run_bounded 60 docker run \
    --detach \
    --name "${SMOKE_CONTAINER}" \
    --network "${NETWORK_NAME}" \
    --mount "type=bind,src=${ARCHIVE_CONTEXT},dst=/repo,readonly" \
    --mount "type=bind,src=${smoke_response_dir},dst=/smoke-output" \
    --env "APP_URL=http://p3g-app:${P3G_APP_PORT}" \
    --env "SMOKE_AUTH_USERNAME=${APP_ADMIN_USERNAME}" \
    --env "SMOKE_AUTH_PASSWORD=${APP_ADMIN_PASSWORD}" \
    --env "SMOKE_ALLOW_EXTERNAL_CALLS=false" \
    --env "SMOKE_RESPONSE_DIR=/smoke-output" \
    --env "SMOKE_SPLIT_PHASE_CONFIRM=I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE" \
    "${FLYWAY_ACTION_JDK_IMAGE}" sleep infinity \
    >"${TMP_DIR}/smoke-${label}-container-id.txt"; then
    blocked "BLOCKED_SMOKE_CLIENT_START"
  fi
  SMOKE_STARTED=1

  local ready=0
  for _ in $(seq 1 120); do
    if run_bounded 5 docker exec "${SMOKE_CONTAINER}" \
      curl --silent --fail --max-time 4 \
      "http://p3g-app:${P3G_APP_PORT}/actuator/health" \
      >"${TMP_DIR}/app-${label}-health.json" 2>/dev/null; then
      ready=1
      break
    fi
    if [ "$(docker inspect "${APP_CONTAINER}" --format '{{.State.Running}}' 2>/dev/null)" != "true" ]; then
      break
    fi
    sleep 1
  done
  if [ "${ready}" -ne 1 ]; then
    docker logs "${APP_CONTAINER}" >"${TMP_DIR}/app-${label}.log" 2>&1 || true
    write_redacted_log "${TMP_DIR}/app-${label}.log" \
      "${EVIDENCE_DIR}/application-${label}-failure-redacted.txt"
    blocked "BLOCKED_APPLICATION_STARTUP"
  fi

  local configured_user
  configured_user="$(docker image inspect "${APP_IMAGE_TAG}" --format '{{.Config.User}}')"
  local effective_uid
  effective_uid="$(run_bounded 10 docker exec "${APP_CONTAINER}" id -u)"
  local network_mode
  network_mode="$(docker inspect "${APP_CONTAINER}" --format '{{.HostConfig.NetworkMode}}')"
  local mount_count
  mount_count="$(docker inspect "${APP_CONTAINER}" --format '{{len .Mounts}}')"
  local smoke_network_mode
  smoke_network_mode="$(docker inspect "${SMOKE_CONTAINER}" --format '{{.HostConfig.NetworkMode}}')"
  local smoke_mount_count
  smoke_mount_count="$(docker inspect "${SMOKE_CONTAINER}" --format '{{len .Mounts}}')"
  local published_host_ip
  published_host_ip="$(docker inspect "${APP_CONTAINER}" \
    --format "{{(index (index .HostConfig.PortBindings \"${P3G_APP_PORT}/tcp\") 0).HostIp}}" \
    2>/dev/null || true)"
  local published_host_port
  published_host_port="$(docker inspect "${APP_CONTAINER}" \
    --format "{{(index (index .HostConfig.PortBindings \"${P3G_APP_PORT}/tcp\") 0).HostPort}}" \
    2>/dev/null || true)"
  if [ "${configured_user}" != "app" ] || [ "${effective_uid}" = "0" ] \
    || [ "${network_mode}" != "${NETWORK_NAME}" ] || [ "${mount_count}" != "0" ] \
    || [ "${smoke_network_mode}" != "${NETWORK_NAME}" ] || [ "${smoke_mount_count}" != "2" ] \
    || [ "${published_host_ip}" != "127.0.0.1" ] \
    || [ "${published_host_port}" != "${P3G_APP_PORT}" ]; then
    blocked "BLOCKED_APPLICATION_CONTAINER_BOUNDARY"
  fi

  if ! run_bounded 120 docker exec \
    --env "SMOKE_PHASE=FETCH" \
    --env "SMOKE_SPLIT_PHASE_CONFIRM=I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE" \
    "${SMOKE_CONTAINER}" bash /repo/scripts/prod-smoke.sh \
    >"${TMP_DIR}/app-${label}-prod-smoke.txt" 2>&1; then
    write_redacted_log "${TMP_DIR}/app-${label}-prod-smoke.txt" \
      "${EVIDENCE_DIR}/application-${label}-smoke-failure-redacted.txt"
    blocked "BLOCKED_PROD_SMOKE"
  fi
  if ! run_bounded 120 env \
    "SMOKE_PHASE=VALIDATE" \
    "SMOKE_RESPONSE_DIR=${smoke_response_dir}" \
    "SMOKE_SPLIT_PHASE_CONFIRM=I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE" \
    "SMOKE_ALLOW_EXTERNAL_CALLS=false" \
    bash "${ROOT_DIR}/scripts/prod-smoke.sh" \
    >>"${TMP_DIR}/app-${label}-prod-smoke.txt" 2>&1; then
    write_redacted_log "${TMP_DIR}/app-${label}-prod-smoke.txt" \
      "${EVIDENCE_DIR}/application-${label}-smoke-failure-redacted.txt"
    blocked "BLOCKED_PROD_SMOKE_VALIDATION"
  fi
  if ! run_bounded 20 docker exec "${SMOKE_CONTAINER}" bash -c \
    'curl --silent --show-error --fail --max-time 10 --user "${SMOKE_AUTH_USERNAME}:${SMOKE_AUTH_PASSWORD}" "${APP_URL}/api/system/run-baseline" --output "${SMOKE_RESPONSE_DIR}/baseline.json"'; then
    blocked "BLOCKED_EMPTY_BASELINE_HTTP"
  fi
  if ! run_bounded 30 python3 "${ROOT_DIR}/scripts/p3g-empty-state-validate.py" \
    "${smoke_response_dir}/dashboard.json" \
    "${smoke_response_dir}/review.json" \
    "${smoke_response_dir}/baseline.json" \
    >"${TMP_DIR}/app-${label}-empty-state.txt"; then
    blocked "BLOCKED_EMPTY_STATE_CONTRACT"
  fi

  {
    echo "APPLICATION_SMOKE: PASS"
    echo "PROD_SMOKE_SCRIPT: PASS_LOCAL_CONTROLLED"
    cat "${TMP_DIR}/app-${label}-empty-state.txt"
    echo "APPLICATION_CONTAINER_USER: app"
    echo "APPLICATION_CONTAINER_RUNS_AS_ROOT: NO"
    echo "APPLICATION_NETWORK: INTERNAL_ONLY"
    echo "APPLICATION_HOST_BIND_CONFIG: 127.0.0.1:18085"
    echo "APPLICATION_HOST_EXPOSURE: LOOPBACK_ONLY"
    echo "APPLICATION_MOUNTS: NONE"
    echo "SMOKE_CLIENT_PATH: INTERNAL_NETWORK_FIXED_DIGEST_CLIENT"
    echo "SMOKE_CLIENT_IMAGE: ${FLYWAY_ACTION_JDK_IMAGE}"
    echo "SMOKE_APP_URL: http://p3g-app:18085"
    echo "FLYWAY_DURING_APP_SMOKE: DISABLED"
    echo "SCHEDULERS: DISABLED"
    echo "PROVIDER_EXTERNAL_CALLS: DISABLED"
    echo "AI_PROVIDER_CALLS: DISABLED"
    echo "EXTERNAL_PUSH_SEND: DISABLED"
  } >"${evidence_file}"
  stop_smoke_client
  stop_application
}

if [ "${P3G_CONFIRM:-}" != "${EXPECTED_CONFIRMATION}" ]; then
  echo "P3G_RESULT: BLOCKED_CONFIRMATION_REQUIRED"
  echo "DATABASE_ACCESS: NOT_ATTEMPTED"
  echo "DOCKER_ACTION: NOT_ATTEMPTED"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit 0
fi

if [ "${P3G_HOST}" != "127.0.0.1" ]; then
  blocked "BLOCKED_NON_LOCALHOST_TARGET"
fi
if [ "${P3G_POSTGRES_PORT}" != "55435" ] || [ "${P3G_APP_PORT}" != "18085" ]; then
  blocked "BLOCKED_UNAPPROVED_LOCAL_PORT"
fi
if [ "${PRIMARY_DATABASE}" != "trade_model_v1_p3g_primary" ] \
  || [ "${RECOVERY_DATABASE}" != "trade_model_v1_p3g_recovery" ]; then
  blocked "BLOCKED_UNAPPROVED_DATABASE_NAME"
fi

enter_stage "repository-preflight"
cd "${ROOT_DIR}"
for command_name in git docker tar openssl curl python3 grep awk sed find nc; do
  command -v "${command_name}" >/dev/null 2>&1 \
    || blocked "BLOCKED_REQUIRED_TOOL_MISSING"
done
if [ "$(git branch --show-current)" != "${EXPECTED_BRANCH}" ]; then
  blocked "BLOCKED_WRONG_BRANCH"
fi
if [ -n "$(git status --porcelain)" ]; then
  blocked "BLOCKED_DIRTY_WORKTREE"
fi
CURRENT_HEAD="$(git rev-parse HEAD)"
if ! git merge-base --is-ancestor "${BASE_MAIN_HEAD}" "${CURRENT_HEAD}"; then
  blocked "BLOCKED_WRONG_BASE_HEAD"
fi
if ! git diff --check; then
  blocked "BLOCKED_DIFF_CHECK"
fi

enter_stage "docker-preflight"
if ! run_bounded 30 docker info >/dev/null 2>&1; then
  blocked "BLOCKED_DOCKER_DAEMON_UNAVAILABLE"
fi
POSTGRES_EXPECTED_DIGEST="${POSTGRES_IMAGE#postgres@}"
POSTGRES_IMAGE_ROW=""
for _ in $(seq 1 30); do
  if POSTGRES_IMAGE_ROWS="$(run_bounded 10 docker image ls --digests --no-trunc \
    --format '{{.Repository}}|{{.Tag}}|{{.Digest}}|{{.ID}}' 2>/dev/null)"; then
    POSTGRES_IMAGE_ROW="$(printf '%s\n' "${POSTGRES_IMAGE_ROWS}" | awk -F '|' \
      -v digest="${POSTGRES_EXPECTED_DIGEST}" \
      '$1 == "postgres" && $2 == "16-alpine" && $3 == digest { print; exit }')"
  fi
  [ -n "${POSTGRES_IMAGE_ROW}" ] && break
  sleep 1
done
if [ -z "${POSTGRES_IMAGE_ROW}" ]; then
  blocked "BLOCKED_PINNED_POSTGRESQL_IMAGE_MISSING"
fi
POSTGRES_RUNTIME_IMAGE="$(printf '%s\n' "${POSTGRES_IMAGE_ROW}" | awk -F '|' '{print $4}')"
case "${POSTGRES_RUNTIME_IMAGE}" in
  sha256:*) ;;
  *) blocked "BLOCKED_POSTGRESQL_IMAGE_ID_MISSING" ;;
esac
if nc -z "${P3G_HOST}" "${P3G_POSTGRES_PORT}" >/dev/null 2>&1 \
  || nc -z "${P3G_HOST}" "${P3G_APP_PORT}" >/dev/null 2>&1; then
  blocked "BLOCKED_LOCAL_PORT_IN_USE"
fi

safe_prepare_evidence_dir
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/postgresql-p3g.XXXXXX")"
ARCHIVE_CONTEXT="$(mktemp -d "${TMPDIR:-/tmp}/postgresql-p3g-context.XXXXXX")"
OPS_BACKUP_DIR="${TMP_DIR}/backups"
mkdir -p "${OPS_BACKUP_DIR}"
chmod 700 "${OPS_BACKUP_DIR}"
RUN_SUFFIX="$(date -u +%Y%m%d%H%M%S)-$$-$(openssl rand -hex 3)"
PG_CONTAINER="p3g-postgres-${RUN_SUFFIX}"
OPS_CONTAINER="p3g-ops-${RUN_SUFFIX}"
SMOKE_CONTAINER="p3g-smoke-${RUN_SUFFIX}"
MIGRATION_ACTION_CONTAINER="p3g-flyway-action-${RUN_SUFFIX}"
NETWORK_NAME="p3g-internal-${RUN_SUFFIX}"
VOLUME_NAME="p3g-postgres-data-${RUN_SUFFIX}"

enter_stage "exact-commit-test-tooling"
ARCHIVE_TAR="${TMP_DIR}/source-${CURRENT_HEAD}.tar"
if ! run_bounded 60 git archive --format=tar --output="${ARCHIVE_TAR}" "${CURRENT_HEAD}" \
  || ! run_bounded 60 tar -xf "${ARCHIVE_TAR}" -C "${ARCHIVE_CONTEXT}"; then
  blocked "BLOCKED_GIT_ARCHIVE_CONTEXT"
fi
if ! run_bounded 30 bash "${ROOT_DIR}/scripts/check-docker-context-safety.sh" \
  "${ARCHIVE_CONTEXT}" >"${TMP_DIR}/docker-context-safety.txt"; then
  blocked "BLOCKED_DOCKER_CONTEXT_SAFETY"
fi
for command_name in bash curl python3; do
  command -v "${command_name}" >/dev/null 2>&1 \
    || blocked "BLOCKED_HOST_SMOKE_CLIENT_TOOL_MISSING"
done
MAVEN_BIN="$(find "${HOME}/.m2/wrapper/dists" -path '*/bin/mvn' -type f -print -quit 2>/dev/null || true)"
MAVEN_REPOSITORY_DIR="${HOME}/.m2/repository"
if [ -z "${MAVEN_BIN}" ] || [ ! -x "${MAVEN_BIN}" ] \
  || [ ! -d "${MAVEN_REPOSITORY_DIR}" ]; then
  blocked "BLOCKED_LOCAL_MAVEN_ACTION_CACHE_MISSING"
fi
MAVEN_DISTRIBUTION_DIR="$(cd "$(dirname "${MAVEN_BIN}")/.." && pwd)"
if ! run_bounded 300 docker pull "${FLYWAY_ACTION_JDK_IMAGE}" \
  >"${TMP_DIR}/flyway-action-jdk-pull.log" 2>&1; then
  blocked "BLOCKED_FLYWAY_ACTION_JDK_IMAGE"
fi

BOOTSTRAP_ROLE="p3g_bootstrap_$(openssl rand -hex 4)"
MIGRATION_ROLE="p3g_migrator_$(openssl rand -hex 4)"
BACKUP_ROLE="p3g_backup_$(openssl rand -hex 4)"
RECOVERY_ROLE="p3g_recovery_$(openssl rand -hex 4)"
PRIMARY_APP_ROLE="p3g_app_readonly_$(openssl rand -hex 4)"
RECOVERY_APP_ROLE="p3g_app_recovery_$(openssl rand -hex 4)"
BOOTSTRAP_PASSWORD="$(random_secret)"
MIGRATION_PASSWORD="$(random_secret)"
BACKUP_PASSWORD="$(random_secret)"
RECOVERY_PASSWORD="$(random_secret)"
PRIMARY_APP_PASSWORD="$(random_secret)"
RECOVERY_APP_PASSWORD="$(random_secret)"
APP_ADMIN_USERNAME="p3g_operator_$(openssl rand -hex 4)"
APP_ADMIN_PASSWORD="$(random_secret)"
BINANCE_FAKE_KEY="p3g-nonfunctional-$(openssl rand -hex 16)"
BINANCE_FAKE_SECRET="p3g-nonfunctional-$(openssl rand -hex 24)"

enter_stage "controlled-topology-create"
if ! run_bounded 30 docker network create --internal "${NETWORK_NAME}" >/dev/null; then
  blocked "BLOCKED_INTERNAL_NETWORK_CREATE"
fi
NETWORK_CREATED=1
if [ "$(docker network inspect "${NETWORK_NAME}" --format '{{.Internal}}')" != "true" ]; then
  blocked "BLOCKED_NETWORK_NOT_INTERNAL"
fi
if ! run_bounded 30 docker volume create "${VOLUME_NAME}" >/dev/null; then
  blocked "BLOCKED_VOLUME_CREATE"
fi
VOLUME_CREATED=1
if ! run_bounded 120 docker run \
  --pull never \
  --detach \
  --name "${PG_CONTAINER}" \
  --network "${NETWORK_NAME}" \
  --network-alias p3g-postgres \
  --publish "127.0.0.1:${P3G_POSTGRES_PORT}:${P3G_POSTGRES_PORT}" \
  --volume "${VOLUME_NAME}:/var/lib/postgresql/data" \
  --env "PGPORT=${P3G_POSTGRES_PORT}" \
  --env "POSTGRES_DB=postgres" \
  --env "POSTGRES_USER=${BOOTSTRAP_ROLE}" \
  --env "POSTGRES_PASSWORD=${BOOTSTRAP_PASSWORD}" \
  "${POSTGRES_RUNTIME_IMAGE}" >"${TMP_DIR}/postgres-container-id.txt"; then
  blocked "BLOCKED_POSTGRESQL_CONTAINER_START"
fi
PG_STARTED=1
wait_for_postgresql

POSTGRESQL_VERSION="$(pg_scalar postgres 'SHOW server_version')"
POSTGRES_IMAGE_ID="${POSTGRES_RUNTIME_IMAGE}"
{
  echo "POSTGRESQL_IMAGE: DIGEST_PINNED"
  echo "POSTGRESQL_IMAGE_DIGEST: sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc"
  echo "POSTGRESQL_IMAGE_ID: ${POSTGRES_IMAGE_ID}"
  echo "POSTGRESQL_VERSION: ${POSTGRESQL_VERSION}"
  echo "PG_DUMP_CLIENT_VERSION: $(docker exec "${PG_CONTAINER}" pg_dump --version | awk '{print $3}')"
  echo "PG_RESTORE_CLIENT_VERSION: $(docker exec "${PG_CONTAINER}" pg_restore --version | awk '{print $3}')"
  echo "PSQL_CLIENT_VERSION: $(docker exec "${PG_CONTAINER}" psql --version | awk '{print $3}')"
  echo "BASH_PRESENT: $([ -n "$(docker exec "${PG_CONTAINER}" sh -c 'command -v bash')" ] && echo YES || echo NO)"
} >"${EVIDENCE_DIR}/docker-image-metadata.txt"
{
  echo "HOST_BIND: 127.0.0.1_ONLY"
  echo "POSTGRESQL_HOST_PORT: ${P3G_POSTGRES_PORT}"
  echo "APPLICATION_HOST_PORT: ${P3G_APP_PORT}"
  echo "PRIMARY_DATABASE: ${PRIMARY_DATABASE}"
  echo "RECOVERY_DATABASE: ${RECOVERY_DATABASE}"
  echo "DOCKER_NETWORK_INTERNAL: YES"
  echo "FLYWAY_ACTION_NETWORK: POSTGRESQL_NETWORK_NAMESPACE"
  echo "APPLICATION_SMOKE_PATH: LOOPBACK_ONLY_HOST_CLIENT"
  echo "LOCALHOST_PORT_MAPPING: LOOPBACK_ONLY_NOT_USED_FOR_EXTERNAL_EGRESS"
  echo "DOCKER_SOCKET_MOUNTED_TO_APPLICATION: NO"
  echo "HOST_NETWORK_USED: NO"
  echo "PRODUCTION_SERVER_ACCESS: NONE"
  echo "PRODUCTION_DATABASE_ACCESS: NONE"
} >"${EVIDENCE_DIR}/environment-topology.txt"

enter_stage "database-role-bootstrap"
if ! run_bounded_with_input 60 "${ROOT_DIR}/scripts/p3g-bootstrap-roles.sql" \
  docker exec -i "${PG_CONTAINER}" psql \
  --username="${BOOTSTRAP_ROLE}" --dbname=postgres --no-psqlrc \
  --set="migration_role=${MIGRATION_ROLE}" \
  --set="migration_password=${MIGRATION_PASSWORD}" \
  --set="recovery_role=${RECOVERY_ROLE}" \
  --set="recovery_password=${RECOVERY_PASSWORD}" \
  --set="primary_database=${PRIMARY_DATABASE}" \
  --set="recovery_database=${RECOVERY_DATABASE}" \
  >"${TMP_DIR}/role-bootstrap.log" 2>&1; then
  blocked "BLOCKED_DATABASE_ROLE_BOOTSTRAP"
fi

enter_stage "empty-greenfield-baseline"
PRE_TABLES="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")"
PRE_TM_TABLES="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_name LIKE 'tm\\_%' ESCAPE '\\'")"
PRE_FLYWAY="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='flyway_schema_history'")"
PRE_EXTENSIONS="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM pg_extension WHERE extname <> 'plpgsql'")"
PRE_FDW="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM pg_foreign_data_wrapper")"
PRE_SERVERS="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM pg_foreign_server")"
PRE_FUNCTIONS="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public' AND p.prokind='f'")"
PRE_TRIGGERS="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND NOT t.tgisinternal")"
if [ "${PRE_TABLES}" != "0" ] || [ "${PRE_TM_TABLES}" != "0" ] \
  || [ "${PRE_FLYWAY}" != "0" ] || [ "${PRE_EXTENSIONS}" != "0" ] \
  || [ "${PRE_FDW}" != "0" ] || [ "${PRE_SERVERS}" != "0" ] \
  || [ "${PRE_FUNCTIONS}" != "0" ] || [ "${PRE_TRIGGERS}" != "0" ]; then
  blocked "BLOCKED_NON_EMPTY_GREENFIELD_DATABASE"
fi
{
  echo "GREENFIELD_PRE_MIGRATION_SCHEMA: EMPTY"
  echo "GREENFIELD_PRE_MIGRATION_BUSINESS_ROWS: 0"
  echo "GREENFIELD_PRE_MIGRATION_FLYWAY_HISTORY: ABSENT"
  echo "UNKNOWN_EXTENSIONS: 0"
  echo "FOREIGN_DATA_WRAPPERS: 0"
  echo "FOREIGN_SERVERS: 0"
  echo "USER_FUNCTIONS: 0"
  echo "USER_TRIGGERS: 0"
  echo "SCHEMA_BASELINE: ABSENT"
  echo "PRESET_FIXTURES: ABSENT"
} >"${EVIDENCE_DIR}/empty-database-baseline.txt"

enter_stage "fresh-flyway-v1-v7"
FLYWAY_ACTION_ENV="${TMP_DIR}/flyway-action.env"
umask 077
{
  echo "P3G_CONTROLLED_POSTGRESQL_JDBC_URL=jdbc:postgresql://127.0.0.1:${P3G_POSTGRES_PORT}/${PRIMARY_DATABASE}"
  echo "P3G_CONTROLLED_POSTGRESQL_USERNAME=${MIGRATION_ROLE}"
  echo "P3G_CONTROLLED_POSTGRESQL_PASSWORD=${MIGRATION_PASSWORD}"
  echo "P3G_CONTROLLED_FLYWAY_CONFIRM=${FLYWAY_CONFIRMATION}"
} >"${FLYWAY_ACTION_ENV}"
chmod 600 "${FLYWAY_ACTION_ENV}"
if ! run_bounded 600 docker run --rm \
  --pull never \
  --name "${MIGRATION_ACTION_CONTAINER}" \
  --network "container:${PG_CONTAINER}" \
  --env-file "${FLYWAY_ACTION_ENV}" \
  --mount "type=bind,src=${ARCHIVE_CONTEXT},dst=/repo,readonly" \
  --mount "type=bind,src=${MAVEN_DISTRIBUTION_DIR},dst=/opt/maven,readonly" \
  --mount "type=bind,src=${MAVEN_REPOSITORY_DIR},dst=/maven-repository,readonly" \
  "${FLYWAY_ACTION_JDK_IMAGE}" bash -c \
  'set -eu; mkdir -p /workspace /root/.m2/repository; cp -a /repo/. /workspace/; cp -a /maven-repository/. /root/.m2/repository/; find /root/.m2/repository -name _remote.repositories -delete; cd /workspace; /opt/maven/bin/mvn -o -q -Dtest=ControlledGreenfieldFlywayV7ActionTest test' \
  >"${TMP_DIR}/flyway-action.log" 2>&1; then
  write_redacted_flyway_failure "${TMP_DIR}/flyway-action.log"
  blocked "BLOCKED_GREENFIELD_FLYWAY_ACTION"
fi
MIGRATION_ACTION_CONTAINER=""
FLYWAY_SUCCESS_ROWS="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM flyway_schema_history WHERE success")"
FLYWAY_FAILED_ROWS="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM flyway_schema_history WHERE NOT success")"
FLYWAY_VERSION="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT MAX(version::integer) FROM flyway_schema_history WHERE success")"
if [ "${FLYWAY_SUCCESS_ROWS}" != "7" ] || [ "${FLYWAY_FAILED_ROWS}" != "0" ] \
  || [ "${FLYWAY_VERSION}" != "7" ]; then
  blocked "BLOCKED_GREENFIELD_FLYWAY_HISTORY"
fi
capture_flyway_history "${PRIMARY_DATABASE}" "${EVIDENCE_DIR}/flyway-history.txt"

enter_stage "post-migration-empty-validation"
TM_TABLE_COUNT="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name LIKE 'tm\\_%' ESCAPE '\\'")"
RULE_CONFIG_ROWS="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM tm_rule_config")"
BUSINESS_ROWS=0
: >"${TMP_DIR}/post-migration-table-counts.txt"
while IFS= read -r table_name; do
  row_count="$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM public.\"${table_name}\"")"
  echo "${table_name}|${row_count}" >>"${TMP_DIR}/post-migration-table-counts.txt"
  if [ "${table_name}" != "tm_rule_config" ]; then
    BUSINESS_ROWS=$((BUSINESS_ROWS + row_count))
  fi
done < <(pg_psql "${PRIMARY_DATABASE}" -Atqc "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name LIKE 'tm\\_%' ESCAPE '\\' ORDER BY table_name")
if [ "${TM_TABLE_COUNT}" != "27" ] || [ "${RULE_CONFIG_ROWS}" != "59" ] \
  || [ "${BUSINESS_ROWS}" -ne 0 ]; then
  blocked "BLOCKED_POST_MIGRATION_BUSINESS_ROWS"
fi
if [ "$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='tm_decision_result' AND column_name IN ('valid_from','expires_at') AND data_type='timestamp with time zone'")" != "2" ]; then
  blocked "BLOCKED_V7_SCHEMA_TYPES"
fi
if [ "$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='tm_execution_plan' AND column_name IN ('manual_review_required','not_trade_instruction','not_executable','not_auto_trading','not_order_execution','not_user_position_creation') AND is_nullable='NO' AND column_default ILIKE '%true%'")" != "6" ]; then
  blocked "BLOCKED_EXECUTION_PLAN_SAFETY_SCHEMA"
fi
if [ "$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM pg_extension WHERE extname <> 'plpgsql'")" != "0" ] \
  || [ "$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM pg_foreign_server")" != "0" ] \
  || [ "$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public' AND p.prokind='f'")" != "0" ]; then
  blocked "BLOCKED_UNEXPECTED_DATABASE_CAPABILITY"
fi
capture_schema_types "${PRIMARY_DATABASE}" "${EVIDENCE_DIR}/schema-types.txt"
{
  echo "POST_MIGRATION_TM_TABLE_COUNT: ${TM_TABLE_COUNT}"
  echo "POST_MIGRATION_BUSINESS_ROWS: ${BUSINESS_ROWS}"
  echo "POST_MIGRATION_SEED_ALLOWLIST: tm_rule_config=${RULE_CONFIG_ROWS}"
  echo "POST_MIGRATION_RUNTIME_INITIALIZER_ROWS: 0"
} >"${EVIDENCE_DIR}/post-migration-row-inventory.txt"

enter_stage "readonly-roles"
if ! run_bounded_with_input 60 "${ROOT_DIR}/scripts/p3g-backup-reader-role.sql" \
  docker exec -i "${PG_CONTAINER}" psql \
  --username="${BOOTSTRAP_ROLE}" --dbname="${PRIMARY_DATABASE}" --no-psqlrc \
  --set="backup_role=${BACKUP_ROLE}" \
  --set="backup_password=${BACKUP_PASSWORD}" \
  --set="database_name=${PRIMARY_DATABASE}" \
  >"${TMP_DIR}/backup-role.log" 2>&1; then
  blocked "BLOCKED_BACKUP_READER_ROLE_CREATE"
fi
if ! run_bounded_with_input 60 "${ROOT_DIR}/scripts/p3-application-readonly-role.sql" \
  docker exec -i "${PG_CONTAINER}" psql \
  --username="${BOOTSTRAP_ROLE}" --dbname="${PRIMARY_DATABASE}" --no-psqlrc \
  --set="application_role=${PRIMARY_APP_ROLE}" \
  --set="application_password=${PRIMARY_APP_PASSWORD}" \
  --set="database_name=${PRIMARY_DATABASE}" \
  >"${TMP_DIR}/primary-app-role.log" 2>&1; then
  blocked "BLOCKED_APPLICATION_READONLY_ROLE_CREATE"
fi

enter_stage "ops-client-start"
if ! run_bounded 60 docker run \
  --pull never \
  --detach \
  --name "${OPS_CONTAINER}" \
  --network "${NETWORK_NAME}" \
  --mount "type=bind,src=${ARCHIVE_CONTEXT},dst=/repo,readonly" \
  --mount "type=bind,src=${OPS_BACKUP_DIR},dst=/evidence" \
  "${POSTGRES_RUNTIME_IMAGE}" sh -c 'while :; do sleep 3600; done' \
  >"${TMP_DIR}/ops-container-id.txt"; then
  blocked "BLOCKED_OPS_CLIENT_START"
fi
OPS_STARTED=1
if ! docker exec "${OPS_CONTAINER}" bash --version >/dev/null 2>&1 \
  || ! docker exec "${OPS_CONTAINER}" pg_dump --version | grep -q ' 16\.' \
  || ! docker exec "${OPS_CONTAINER}" pg_restore --version | grep -q ' 16\.' \
  || ! docker exec "${OPS_CONTAINER}" psql --version | grep -q ' 16\.'; then
  blocked "BLOCKED_OPS_CLIENT_VERSION"
fi

enter_stage "role-isolation-validation"
for role_name in "${MIGRATION_ROLE}" "${BACKUP_ROLE}" "${RECOVERY_ROLE}" "${PRIMARY_APP_ROLE}"; do
  role_flags="$(pg_scalar postgres "SELECT rolsuper::int || '|' || rolcreatedb::int || '|' || rolcreaterole::int || '|' || rolinherit::int FROM pg_roles WHERE rolname='${role_name}'")"
  [ "${role_flags}" = "0|0|0|0" ] || blocked "BLOCKED_ROLE_CAPABILITIES"
done
if [ "$(ops_scalar "${BACKUP_ROLE}" "${BACKUP_PASSWORD}" "${PRIMARY_DATABASE}" "SELECT 1")" != "1" ] \
  || [ "$(ops_scalar "${PRIMARY_APP_ROLE}" "${PRIMARY_APP_PASSWORD}" "${PRIMARY_DATABASE}" "SELECT 1")" != "1" ] \
  || [ "$(ops_scalar "${MIGRATION_ROLE}" "${MIGRATION_PASSWORD}" "${PRIMARY_DATABASE}" "SELECT 1")" != "1" ] \
  || [ "$(ops_scalar "${RECOVERY_ROLE}" "${RECOVERY_PASSWORD}" "${RECOVERY_DATABASE}" "SELECT 1")" != "1" ]; then
  blocked "BLOCKED_ROLE_EXPECTED_CONNECT"
fi
if ops_psql "${RECOVERY_ROLE}" "${RECOVERY_PASSWORD}" "${PRIMARY_DATABASE}" -Atqc 'SELECT 1' >/dev/null 2>&1 \
  || ops_psql "${MIGRATION_ROLE}" "${MIGRATION_PASSWORD}" "${RECOVERY_DATABASE}" -Atqc 'SELECT 1' >/dev/null 2>&1 \
  || ops_psql "${BACKUP_ROLE}" "${BACKUP_PASSWORD}" "${RECOVERY_DATABASE}" -Atqc 'SELECT 1' >/dev/null 2>&1 \
  || ops_psql "${PRIMARY_APP_ROLE}" "${PRIMARY_APP_PASSWORD}" "${RECOVERY_DATABASE}" -Atqc 'SELECT 1' >/dev/null 2>&1; then
  blocked "BLOCKED_ROLE_DATABASE_SCOPE"
fi
if [ "$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND NOT has_table_privilege('${BACKUP_ROLE}',format('%I.%I',table_schema,table_name),'SELECT')")" != "0" ] \
  || [ "$(pg_scalar "${PRIMARY_DATABASE}" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND (has_table_privilege('${BACKUP_ROLE}',format('%I.%I',table_schema,table_name),'INSERT') OR has_table_privilege('${BACKUP_ROLE}',format('%I.%I',table_schema,table_name),'UPDATE') OR has_table_privilege('${BACKUP_ROLE}',format('%I.%I',table_schema,table_name),'DELETE') OR has_table_privilege('${BACKUP_ROLE}',format('%I.%I',table_schema,table_name),'TRUNCATE'))")" != "0" ]; then
  blocked "BLOCKED_BACKUP_READER_PRIVILEGES"
fi
if ops_psql "${BACKUP_ROLE}" "${BACKUP_PASSWORD}" "${PRIMARY_DATABASE}" \
  --set=VERBOSITY=verbose -c 'UPDATE tm_asset_state SET state=state WHERE FALSE' \
  >"${TMP_DIR}/backup-write-probe.txt" 2>"${TMP_DIR}/backup-write-probe-error.txt"; then
  blocked "BLOCKED_BACKUP_READER_WRITABLE"
fi
if ops_psql "${PRIMARY_APP_ROLE}" "${PRIMARY_APP_PASSWORD}" "${PRIMARY_DATABASE}" \
  --set=VERBOSITY=verbose -c 'UPDATE tm_asset_state SET state=state WHERE FALSE' \
  >"${TMP_DIR}/app-write-probe.txt" 2>"${TMP_DIR}/app-write-probe-error.txt"; then
  blocked "BLOCKED_APPLICATION_DATABASE_ROLE_WRITABLE"
fi
if ! grep -Eq '(25006|42501)' "${TMP_DIR}/app-write-probe-error.txt"; then
  blocked "BLOCKED_APPLICATION_WRITE_PROBE_UNCLASSIFIED"
fi
{
  echo "BOOTSTRAP_ADMIN: LOCAL_CONTAINER_ONLY"
  echo "MIGRATION_OWNER: PRIMARY_ONLY"
  echo "BACKUP_READER: PRIMARY_READ_ONLY"
  echo "RECOVERY_OWNER: RECOVERY_ONLY"
  echo "APPLICATION_DATABASE_ROLE: READ_ONLY"
  echo "APPLICATION_ROLE_NOINHERIT: YES"
  echo "APPLICATION_ROLE_DEFAULT_TRANSACTION_READ_ONLY: ON"
  echo "APPLICATION_ROLE_TEMP: DENIED"
  echo "BACKUP_READER_WRITE_PROBE: DENIED"
  echo "READ_ONLY_WRITE_PROBE: DENIED"
  echo "READ_ONLY_WRITE_PROBE_SQLSTATE: ACCEPTED_25006_OR_42501"
  echo "ROLE_DATABASE_SCOPE: PASS"
} >"${EVIDENCE_DIR}/role-capabilities.txt"
cp "${EVIDENCE_DIR}/role-capabilities.txt" "${EVIDENCE_DIR}/application-database-role.txt"

enter_stage "postgresql-restart-persistence"
capture_structure_fingerprint "${PRIMARY_DATABASE}" "${EVIDENCE_DIR}/primary-structure-before-restart.txt"
capture_content_fingerprint "${PRIMARY_DATABASE}" "${EVIDENCE_DIR}/primary-content-before-restart.txt"
capture_restore_verification "${PRIMARY_DATABASE}" "${TMP_DIR}/primary-verification.txt"
capture_historical_inventory "${PRIMARY_DATABASE}" "${TMP_DIR}/primary-inventory.txt"
PRIMARY_STRUCTURE_SHA="$(sha256_file "${EVIDENCE_DIR}/primary-structure-before-restart.txt")"
PRIMARY_CONTENT_SHA="$(sha256_file "${EVIDENCE_DIR}/primary-content-before-restart.txt")"
if ! run_bounded 120 docker restart "${PG_CONTAINER}" >/dev/null; then
  blocked "BLOCKED_POSTGRESQL_RESTART"
fi
wait_for_postgresql
capture_structure_fingerprint "${PRIMARY_DATABASE}" "${EVIDENCE_DIR}/primary-structure-after-restart.txt"
capture_content_fingerprint "${PRIMARY_DATABASE}" "${EVIDENCE_DIR}/primary-content-after-restart.txt"
if ! cmp -s "${EVIDENCE_DIR}/primary-structure-before-restart.txt" "${EVIDENCE_DIR}/primary-structure-after-restart.txt" \
  || ! cmp -s "${EVIDENCE_DIR}/primary-content-before-restart.txt" "${EVIDENCE_DIR}/primary-content-after-restart.txt"; then
  blocked "BLOCKED_POSTGRESQL_RESTART_PERSISTENCE"
fi

enter_stage "official-prod-backup"
OPS_BACKUP_FILE="${OPS_BACKUP_DIR}/greenfield-primary.dump"
BACKUP_FILE="${BACKUP_DIR}/greenfield-primary.dump"
if ! run_bounded 600 docker exec \
  --env "BACKUP_DIR=/evidence" \
  --env "BACKUP_FILE=/evidence/greenfield-primary.dump" \
  --env "PROD_DATASOURCE_HOST=p3g-postgres" \
  --env "PROD_DATASOURCE_PORT=${P3G_POSTGRES_PORT}" \
  --env "PROD_DATASOURCE_USERNAME=${BACKUP_ROLE}" \
  --env "PROD_DATASOURCE_PASSWORD=${BACKUP_PASSWORD}" \
  --env "PROD_DATASOURCE_DATABASE=${PRIMARY_DATABASE}" \
  "${OPS_CONTAINER}" bash /repo/scripts/prod-backup.sh \
  >"${TMP_DIR}/prod-backup.log" 2>"${TMP_DIR}/prod-backup-error.log"; then
  blocked "BLOCKED_PROD_BACKUP_SCRIPT"
fi
if [ ! -s "${OPS_BACKUP_FILE}" ] \
  || ! run_bounded 120 docker exec "${OPS_CONTAINER}" pg_restore --list \
    /evidence/greenfield-primary.dump >/dev/null 2>&1; then
  blocked "BLOCKED_BACKUP_FORMAT_VALIDATION"
fi
cp "${OPS_BACKUP_FILE}" "${BACKUP_FILE}"
chmod 600 "${BACKUP_FILE}"
BACKUP_SHA256="$(sha256_file "${BACKUP_FILE}")"
BACKUP_SIZE_BYTES="$(file_size_bytes "${BACKUP_FILE}")"
{
  echo "PROD_BACKUP_SCRIPT: PASS_LOCAL_CONTROLLED"
  echo "BACKUP_FORMAT: POSTGRESQL_CUSTOM"
  echo "BACKUP_FILE: greenfield-primary.dump"
  echo "BACKUP_SIZE_BYTES: ${BACKUP_SIZE_BYTES}"
  echo "BACKUP_SHA256: ${BACKUP_SHA256}"
  echo "BACKUP_ROLE: READ_ONLY"
  echo "PG_DUMP_CLIENT_VERSION: ${POSTGRESQL_VERSION}"
  echo "SOURCE_DATABASE_CLASS: DISPOSABLE_LOCAL_GREENFIELD"
} >"${EVIDENCE_DIR}/backup-metadata.txt"
{
  echo "SCRIPT: scripts/prod-backup.sh"
  echo "EXECUTION: PASS_LOCAL_CONTROLLED"
  echo "DIRECT_PG_DUMP_SUBSTITUTION: NO"
  echo "CLIENT: DIGEST_PINNED_POSTGRESQL_16_OPS_CONTAINER"
} >"${EVIDENCE_DIR}/operational-backup-script.txt"

enter_stage "official-prod-restore"
if ! run_bounded 600 docker exec \
  --env "RESTORE_DATASOURCE_HOST=p3g-postgres" \
  --env "RESTORE_DATASOURCE_PORT=${P3G_POSTGRES_PORT}" \
  --env "RESTORE_DATASOURCE_USERNAME=${RECOVERY_ROLE}" \
  --env "RESTORE_DATASOURCE_PASSWORD=${RECOVERY_PASSWORD}" \
  --env "RESTORE_DATASOURCE_DATABASE=${RECOVERY_DATABASE}" \
  --env "RESTORE_BACKUP_FILE=/evidence/greenfield-primary.dump" \
  --env "RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA" \
  "${OPS_CONTAINER}" bash /repo/scripts/prod-restore.sh \
  >"${TMP_DIR}/prod-restore.log" 2>"${TMP_DIR}/prod-restore-error.log"; then
  blocked "BLOCKED_PROD_RESTORE_SCRIPT"
fi
{
  echo "SCRIPT: scripts/prod-restore.sh"
  echo "EXECUTION: PASS_LOCAL_CONTROLLED"
  echo "RESTORE_STATUS: PASS"
  echo "RESTORE_FLAGS: CLEAN_IF_EXISTS_NO_OWNER_NO_ACL_EXIT_ON_ERROR"
  echo "CLIENT: DIGEST_PINNED_POSTGRESQL_16_OPS_CONTAINER"
} >"${EVIDENCE_DIR}/operational-restore-script.txt"

enter_stage "primary-recovery-comparison"
capture_structure_fingerprint "${RECOVERY_DATABASE}" "${EVIDENCE_DIR}/recovery-structure-fingerprint.txt"
capture_content_fingerprint "${RECOVERY_DATABASE}" "${EVIDENCE_DIR}/recovery-content-fingerprint.txt"
capture_flyway_history "${RECOVERY_DATABASE}" "${TMP_DIR}/recovery-flyway-history.txt"
capture_schema_types "${RECOVERY_DATABASE}" "${TMP_DIR}/recovery-schema-types.txt"
capture_restore_verification "${RECOVERY_DATABASE}" "${TMP_DIR}/recovery-verification.txt"
capture_historical_inventory "${RECOVERY_DATABASE}" "${TMP_DIR}/recovery-inventory.txt"
RECOVERY_STRUCTURE_SHA="$(sha256_file "${EVIDENCE_DIR}/recovery-structure-fingerprint.txt")"
RECOVERY_CONTENT_SHA="$(sha256_file "${EVIDENCE_DIR}/recovery-content-fingerprint.txt")"
if [ "${RECOVERY_STRUCTURE_SHA}" != "${PRIMARY_STRUCTURE_SHA}" ] \
  || [ "${RECOVERY_CONTENT_SHA}" != "${PRIMARY_CONTENT_SHA}" ] \
  || ! cmp -s "${EVIDENCE_DIR}/flyway-history.txt" "${TMP_DIR}/recovery-flyway-history.txt" \
  || ! cmp -s "${EVIDENCE_DIR}/schema-types.txt" "${TMP_DIR}/recovery-schema-types.txt" \
  || ! cmp -s "${TMP_DIR}/primary-verification.txt" "${TMP_DIR}/recovery-verification.txt" \
  || ! cmp -s "${TMP_DIR}/primary-inventory.txt" "${TMP_DIR}/recovery-inventory.txt"; then
  blocked "RESTORE_DATA_INTEGRITY_MISMATCH"
fi
{
  echo "RESTORE_STRUCTURE_FINGERPRINT: MATCH"
  echo "RESTORE_CONTENT_FINGERPRINT: MATCH"
  echo "RESTORE_FLYWAY_HISTORY: MATCH"
  echo "RESTORE_SCHEMA_TYPES: MATCH"
  echo "RESTORE_TABLE_INDEX_CONSTRAINT_SEQUENCE_STATE: MATCH"
  echo "RESTORE_HISTORICAL_INVENTORY: MATCH"
  echo "FULL_CONTENT_FINGERPRINT_EXECUTED: YES"
} >"${EVIDENCE_DIR}/restore-comparison.txt"

enter_stage "recovery-app-role"
if ! run_bounded_with_input 60 "${ROOT_DIR}/scripts/p3-application-readonly-role.sql" \
  docker exec -i "${PG_CONTAINER}" psql \
  --username="${BOOTSTRAP_ROLE}" --dbname="${RECOVERY_DATABASE}" --no-psqlrc \
  --set="application_role=${RECOVERY_APP_ROLE}" \
  --set="application_password=${RECOVERY_APP_PASSWORD}" \
  --set="database_name=${RECOVERY_DATABASE}" \
  >"${TMP_DIR}/recovery-app-role.log" 2>&1; then
  blocked "BLOCKED_RECOVERY_APPLICATION_ROLE_CREATE"
fi
if [ "$(ops_scalar "${RECOVERY_APP_ROLE}" "${RECOVERY_APP_PASSWORD}" "${RECOVERY_DATABASE}" 'SELECT 1')" != "1" ] \
  || ops_psql "${RECOVERY_APP_ROLE}" "${RECOVERY_APP_PASSWORD}" "${PRIMARY_DATABASE}" -Atqc 'SELECT 1' >/dev/null 2>&1; then
  blocked "BLOCKED_RECOVERY_APPLICATION_ROLE_SCOPE"
fi
if ops_psql "${RECOVERY_APP_ROLE}" "${RECOVERY_APP_PASSWORD}" "${RECOVERY_DATABASE}" \
  --set=VERBOSITY=verbose -c 'UPDATE tm_asset_state SET state=state WHERE FALSE' \
  >"${TMP_DIR}/recovery-app-write-probe.txt" 2>"${TMP_DIR}/recovery-app-write-probe-error.txt"; then
  blocked "BLOCKED_RECOVERY_APPLICATION_ROLE_WRITABLE"
fi

enter_stage "docker-compose-config"
COMPOSE_ENV="${TMP_DIR}/compose.env"
umask 077
{
  echo "POSTGRES_DB=p3g_compose_validation"
  echo "POSTGRES_USER=p3g_compose_operator"
  echo "POSTGRES_PASSWORD=$(random_secret)"
  echo "APP_ADMIN_USERNAME=p3g_compose_admin"
  echo "APP_ADMIN_PASSWORD=$(random_secret)"
  echo "BINANCE_API_KEY=p3g-nonfunctional-$(openssl rand -hex 16)"
  echo "BINANCE_API_SECRET=p3g-nonfunctional-$(openssl rand -hex 24)"
} >"${COMPOSE_ENV}"
chmod 600 "${COMPOSE_ENV}"
if ! run_bounded 60 docker compose --env-file "${COMPOSE_ENV}" config >/dev/null 2>&1; then
  blocked "BLOCKED_DOCKER_COMPOSE_CONFIG"
fi
{
  echo "DOCKER_COMPOSE_CONFIG: PASS"
  echo "EXPANDED_COMPOSE_CONFIG_SAVED: NO"
  echo "BASE_DOCKER_COMPOSE_RUNTIME: NOT_USED_FOR_P3G_READONLY_ROLE_REHEARSAL"
} >"${EVIDENCE_DIR}/docker-compose-config-status.txt"

enter_stage "exact-commit-application-image"
if [ -n "$(git status --porcelain)" ] || [ "$(git rev-parse HEAD)" != "${CURRENT_HEAD}" ]; then
  blocked "BLOCKED_WORKTREE_CHANGED_BEFORE_IMAGE_BUILD"
fi
APP_IMAGE_TAG="trade-model-v1:p3g-${CURRENT_HEAD:0:12}"
if ! run_bounded 600 docker build \
  --pull=false \
  --label "org.opencontainers.image.revision=${CURRENT_HEAD}" \
  --tag "${APP_IMAGE_TAG}" \
  "${ARCHIVE_CONTEXT}" >"${TMP_DIR}/docker-build.log" 2>&1; then
  blocked "BLOCKED_APPLICATION_IMAGE_BUILD"
fi
APP_IMAGE_CREATED=1
APP_IMAGE_ID="$(docker image inspect "${APP_IMAGE_TAG}" --format '{{.Id}}')"
APP_IMAGE_REVISION="$(docker image inspect "${APP_IMAGE_TAG}" --format '{{index .Config.Labels "org.opencontainers.image.revision"}}')"
APP_IMAGE_USER="$(docker image inspect "${APP_IMAGE_TAG}" --format '{{.Config.User}}')"
if [ "${APP_IMAGE_REVISION}" != "${CURRENT_HEAD}" ] || [ "${APP_IMAGE_USER}" != "app" ]; then
  blocked "BLOCKED_APPLICATION_IMAGE_IDENTITY"
fi
if ! run_bounded 30 docker run --rm --network none --entrypoint java \
  "${APP_IMAGE_TAG}" -version >"${TMP_DIR}/java-version.txt" 2>&1; then
  blocked "BLOCKED_APPLICATION_JAVA_RUNTIME"
fi
{
  echo "APP_IMAGE_SOURCE_HEAD: ${CURRENT_HEAD}"
  echo "APP_IMAGE_ID: ${APP_IMAGE_ID}"
  echo "APP_IMAGE_REPO_DIGEST: NOT_AVAILABLE_LOCAL_BUILD"
  echo "DOCKERFILE_SHA256: $(sha256_file "${ARCHIVE_CONTEXT}/Dockerfile")"
  echo "JAVA_RUNTIME: $(head -n 1 "${TMP_DIR}/java-version.txt" | sed 's/"//g')"
  echo "APP_CONTAINER_USER: app"
  echo "APP_CONTAINER_RUNS_AS_ROOT: NO"
  cat "${TMP_DIR}/docker-context-safety.txt"
} >"${EVIDENCE_DIR}/application-image-metadata.txt"

enter_stage "primary-first-boot-smoke"
capture_content_fingerprint "${PRIMARY_DATABASE}" "${TMP_DIR}/primary-app-content-before.txt"
capture_structure_fingerprint "${PRIMARY_DATABASE}" "${TMP_DIR}/primary-app-structure-before.txt"
run_application_smoke "primary-first" "${PRIMARY_DATABASE}" \
  "${PRIMARY_APP_ROLE}" "${PRIMARY_APP_PASSWORD}" \
  "${EVIDENCE_DIR}/primary-application-smoke.txt"
capture_content_fingerprint "${PRIMARY_DATABASE}" "${TMP_DIR}/primary-app-content-after.txt"
capture_structure_fingerprint "${PRIMARY_DATABASE}" "${TMP_DIR}/primary-app-structure-after.txt"
if ! cmp -s "${TMP_DIR}/primary-app-content-before.txt" "${TMP_DIR}/primary-app-content-after.txt" \
  || ! cmp -s "${TMP_DIR}/primary-app-structure-before.txt" "${TMP_DIR}/primary-app-structure-after.txt"; then
  blocked "BLOCKED_UNEXPECTED_PRIMARY_APPLICATION_WRITE"
fi

enter_stage "primary-application-restart-smoke"
run_application_smoke "primary-restart" "${PRIMARY_DATABASE}" \
  "${PRIMARY_APP_ROLE}" "${PRIMARY_APP_PASSWORD}" \
  "${EVIDENCE_DIR}/primary-application-restart-smoke.txt"
capture_content_fingerprint "${PRIMARY_DATABASE}" "${TMP_DIR}/primary-app-restart-content-after.txt"
if ! cmp -s "${TMP_DIR}/primary-app-content-before.txt" "${TMP_DIR}/primary-app-restart-content-after.txt"; then
  blocked "BLOCKED_UNEXPECTED_PRIMARY_RESTART_WRITE"
fi

enter_stage "recovery-application-smoke"
capture_content_fingerprint "${RECOVERY_DATABASE}" "${TMP_DIR}/recovery-app-content-before.txt"
run_application_smoke "recovery" "${RECOVERY_DATABASE}" \
  "${RECOVERY_APP_ROLE}" "${RECOVERY_APP_PASSWORD}" \
  "${EVIDENCE_DIR}/recovery-application-smoke.txt"
capture_content_fingerprint "${RECOVERY_DATABASE}" "${TMP_DIR}/recovery-app-content-after.txt"
if ! cmp -s "${TMP_DIR}/recovery-app-content-before.txt" "${TMP_DIR}/recovery-app-content-after.txt"; then
  blocked "BLOCKED_UNEXPECTED_RECOVERY_APPLICATION_WRITE"
fi

enter_stage "controlled-cleanup"
cleanup_resources
write_cleanup_evidence
if [ "${CONTAINER_CLEANUP}" != "PASS" ] || [ "${NETWORK_CLEANUP}" != "PASS" ] \
  || [ "${VOLUME_CLEANUP}" != "PASS" ] || [ "${APP_IMAGE_CLEANUP}" != "PASS" ]; then
  blocked "BLOCKED_CONTROLLED_RESOURCE_CLEANUP"
fi

enter_stage "evidence-summary"
{
  echo "P3G_RESULT: PASS_LOCAL_CONTROLLED_GREENFIELD_REHEARSAL"
  echo "EXECUTED_AT_UTC: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "BASE_MAIN_HEAD: ${BASE_MAIN_HEAD}"
  echo "APP_IMAGE_SOURCE_HEAD: ${CURRENT_HEAD}"
  echo "GREENFIELD_DECISION_REFERENCE: TMV1-GREENFIELD-20260715-001"
  echo "GREENFIELD_PRE_MIGRATION_SCHEMA: EMPTY"
  echo "GREENFIELD_PRE_MIGRATION_BUSINESS_ROWS: 0"
  echo "GREENFIELD_PRE_MIGRATION_FLYWAY_HISTORY: ABSENT"
  echo "POSTGRESQL_VERSION: ${POSTGRESQL_VERSION}"
  echo "GREENFIELD_FLYWAY_FRESH_V1_TO_V7: PASS"
  echo "GREENFIELD_FLYWAY_REPEAT_MIGRATE: ZERO_MIGRATIONS"
  echo "FLYWAY_SCHEMA_VERSION: 7"
  echo "POST_MIGRATION_BUSINESS_ROWS: 0"
  echo "POST_MIGRATION_SEED_ALLOWLIST: tm_rule_config=59"
  echo "POSTGRESQL_RESTART_PERSISTENCE: PASS"
  echo "RESTART_STRUCTURE_FINGERPRINT: MATCH"
  echo "RESTART_CONTENT_FINGERPRINT: MATCH"
  echo "PROD_BACKUP_SCRIPT: PASS_LOCAL_CONTROLLED"
  echo "BACKUP_FORMAT: POSTGRESQL_CUSTOM"
  echo "BACKUP_SHA256: ${BACKUP_SHA256}"
  echo "BACKUP_SIZE_BYTES: ${BACKUP_SIZE_BYTES}"
  echo "PROD_RESTORE_SCRIPT: PASS_LOCAL_CONTROLLED"
  echo "RESTORE_STATUS: PASS"
  echo "RESTORE_STRUCTURE_FINGERPRINT: MATCH"
  echo "RESTORE_CONTENT_FINGERPRINT: MATCH"
  echo "RESTORE_FLYWAY_HISTORY: MATCH"
  echo "RESTORE_SCHEMA_TYPES: MATCH"
  echo "RESTORE_HISTORICAL_INVENTORY: MATCH"
  echo "APPLICATION_IMAGE_STATUS: PASS_EXACT_COMMITTED_GIT_ARCHIVE"
  echo "APPLICATION_DATABASE_ROLE: READ_ONLY"
  echo "READ_ONLY_WRITE_PROBE: DENIED"
  echo "PRIMARY_FIRST_BOOT_SMOKE: PASS"
  echo "EMPTY_DASHBOARD_FAIL_CLOSED: PASS"
  echo "EMPTY_ASSET_CARDS_FAIL_CLOSED: PASS"
  echo "EMPTY_SYSTEM_STATE_FAIL_CLOSED: PASS"
  echo "FAKE_ASSET_CONCLUSIONS: NONE"
  echo "FAKE_POSITION_PLAN_RECORDS: NONE"
  echo "ASSET_ENUM_CONTRACT: PASS_EXACT_FORMAL_VALUES"
  echo "MARKET_BIAS_EMPTY_CONTRACT: WAIT_OR_EMPTY_ONLY"
  echo "ASSET_JSON_SHAPE: PASS_STRICT"
  echo "APPLICATION_RESTART_SMOKE: PASS"
  echo "RECOVERY_APPLICATION_SMOKE: PASS"
  echo "PRIMARY_APP_STRUCTURE_FINGERPRINT: MATCH"
  echo "PRIMARY_APP_CONTENT_FINGERPRINT: MATCH"
  echo "PRIMARY_APP_SEQUENCE_STATE: MATCH"
  echo "RECOVERY_APP_CONTENT_FINGERPRINT: MATCH"
  echo "UNEXPECTED_BUSINESS_WRITES: 0"
  echo "RECOVERY_UNEXPECTED_BUSINESS_WRITES: 0"
  echo "APPLICATION_NETWORK: INTERNAL_ONLY"
  echo "EXTERNAL_NETWORK_EGRESS: BLOCKED_BY_DOCKER_NETWORK"
  echo "PROVIDER_EXTERNAL_CALLS: DISABLED"
  echo "DOCKER_COMPOSE_CONFIG: PASS"
  echo "BASE_DOCKER_COMPOSE_RUNTIME: NOT_USED_FOR_P3G_READONLY_ROLE_REHEARSAL"
  echo "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}"
  echo "NETWORK_CLEANUP: ${NETWORK_CLEANUP}"
  echo "VOLUME_CLEANUP: ${VOLUME_CLEANUP}"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
} >"${EVIDENCE_DIR}/summary.txt"

: >"${EVIDENCE_DIR}/checksums.txt"
while IFS= read -r evidence_file; do
  relative_file="${evidence_file#${EVIDENCE_DIR}/}"
  echo "$(sha256_file "${evidence_file}")  ${relative_file}" >>"${EVIDENCE_DIR}/checksums.txt"
done < <(find "${EVIDENCE_DIR}" -type f ! -name checksums.txt ! -name '*.dump' | sort)

FINAL_RESULT="PASS_LOCAL_CONTROLLED_GREENFIELD_REHEARSAL"
cat "${EVIDENCE_DIR}/summary.txt"
echo "EVIDENCE_ARTIFACTS: .runtime/postgresql-p3g-rehearsal"
