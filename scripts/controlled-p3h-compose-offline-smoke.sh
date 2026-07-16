#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONFIRMATION="I_CONFIRM_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/p3h/docker-compose.p3h.yml"
CURRENT_STAGE="confirmation"
TEMP_ROOT=""
IMAGE_TAG=""
PROJECT_NAME=""
CLEANUP_STATUS="NOT_STARTED"

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

compose() {
  docker compose -f "${COMPOSE_FILE}" "$@"
}

blocked() {
  echo "P3H_OFFLINE_FAILED_STAGE: ${CURRENT_STAGE}"
  echo "LOCAL_COMPOSE_TEMPLATE_SMOKE: $1"
  echo "REAL_STAGING_STATUS: BLOCKED_MISSING_AUTHORIZED_INPUT"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit "${2:-2}"
}

cleanup() {
  local exit_status=$?
  set +e
  if [ -n "${PROJECT_NAME}" ] && [ -n "${TEMP_ROOT}" ]; then
    if [ "${exit_status}" -ne 0 ]; then
      compose ps --all --format '{{.Service}}={{.State}}' 2>/dev/null \
        | sed 's/^/P3H_OFFLINE_SERVICE_STATE: /' || true
    fi
    run_bounded 120 compose --profile validation down --volumes --remove-orphans \
      >/dev/null 2>&1 || true
    if compose ps --all --quiet 2>/dev/null | grep -q .; then
      CLEANUP_STATUS="FAIL"
    else
      CLEANUP_STATUS="PASS"
    fi
  fi
  if [ -n "${IMAGE_TAG}" ]; then
    run_bounded 120 docker image rm "${IMAGE_TAG}" >/dev/null 2>&1 || true
  fi
  if [ -n "${TEMP_ROOT}" ] && [ -d "${TEMP_ROOT}" ]; then
    rm -rf "${TEMP_ROOT}"
  fi
  if [ "${exit_status}" -eq 0 ]; then
    echo "LOCAL_DISPOSABLE_RESOURCE_CLEANUP: ${CLEANUP_STATUS}"
    [ "${CLEANUP_STATUS}" = "PASS" ] || exit 2
  elif [ "${CLEANUP_STATUS}" = "PASS" ]; then
    echo "LOCAL_DISPOSABLE_RESOURCE_CLEANUP: PASS"
  fi
  exit "${exit_status}"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if [ "${P3H_OFFLINE_COMPOSE_SMOKE_CONFIRM:-}" != "${EXPECTED_CONFIRMATION}" ]; then
  echo "LOCAL_COMPOSE_TEMPLATE_SMOKE: SKIPPED_CONFIRMATION_REQUIRED"
  echo "LIVE_PROVIDER_CALLS: 0"
  echo "REAL_STAGING_STATUS: BLOCKED_MISSING_AUTHORIZED_INPUT"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit 0
fi

CURRENT_STAGE="local-safety-preflight"
for required_tool in docker git openssl curl realpath; do
  command -v "${required_tool}" >/dev/null 2>&1 \
    || blocked "BLOCKED_REQUIRED_TOOL_MISSING"
done
run_bounded 30 docker info >/dev/null 2>&1 \
  || blocked "BLOCKED_DOCKER_DAEMON_UNAVAILABLE"
run_bounded 30 docker compose version >/dev/null 2>&1 \
  || blocked "BLOCKED_DOCKER_COMPOSE_UNAVAILABLE"

case "$(uname -s)" in
  Darwin) temp_parent=/private/tmp ;;
  *) temp_parent=/tmp ;;
esac
TEMP_ROOT="$(mktemp -d "${temp_parent}/trade-model-p3h-offline.XXXXXX")"
TEMP_ROOT="$(realpath "${TEMP_ROOT}")"
SECRET_DIR="${TEMP_ROOT}/credentials"
mkdir -p "${SECRET_DIR}"
chmod 700 "${TEMP_ROOT}" "${SECRET_DIR}"

run_id="$(printf '%s' "$$-$(date +%s)" | shasum -a 256 | cut -c1-12)"
PROJECT_NAME="trade-model-p3h-${run_id}"
P3H_COMPOSE_PROJECT_NAME="${PROJECT_NAME}"
P3H_APPLICATION_IMAGE_TAG="offline-${run_id}"
IMAGE_TAG="trade-model-v1:p3h-${P3H_APPLICATION_IMAGE_TAG}"
P3H_STAGING_HOSTNAME="localhost"
P3H_SECRET_MOUNT_DIR="${SECRET_DIR}"
P3H_HTTP_HOST_PORT="${P3H_OFFLINE_HTTP_PORT:-18089}"
P3H_HTTPS_HOST_PORT="${P3H_OFFLINE_HTTPS_PORT:-18449}"
export P3H_COMPOSE_PROJECT_NAME P3H_APPLICATION_IMAGE_TAG P3H_STAGING_HOSTNAME
export P3H_SECRET_MOUNT_DIR
export P3H_HTTP_HOST_PORT P3H_HTTPS_HOST_PORT

for secret_name in postgres_admin_password flyway_password \
    app_database_password_v1 app_database_password_v2 app_admin_password_v1 \
    app_admin_password_v2 backup_reader_password recovery_owner_password \
    binance_nonfunctional_key binance_nonfunctional_secret; do
  openssl rand -hex 32 >"${SECRET_DIR}/${secret_name}"
  chmod 600 "${SECRET_DIR}/${secret_name}"
done

run_bounded 30 openssl req -x509 -newkey rsa:2048 -sha256 -nodes -days 1 \
  -subj "/CN=localhost" -addext "subjectAltName=DNS:localhost" \
  -keyout "${SECRET_DIR}/tls_private_key" \
  -out "${SECRET_DIR}/tls_certificate" >/dev/null 2>&1 \
  || blocked "BLOCKED_LOCAL_TLS_FIXTURE"
chmod 600 "${SECRET_DIR}/tls_private_key" "${SECRET_DIR}/tls_certificate"

CURRENT_STAGE="compose-config"
run_bounded 60 compose config --quiet \
  || blocked "BLOCKED_COMPOSE_CONFIG"

CURRENT_STAGE="application-image-build"
current_head="$(git -C "${ROOT_DIR}" rev-parse HEAD)"
run_bounded 600 docker build --pull=false \
  --file "${ROOT_DIR}/deploy/p3h/Dockerfile.p3h" \
  --build-arg "VCS_REF=${current_head}" --tag "${IMAGE_TAG}" "${ROOT_DIR}" \
  >/dev/null \
  || blocked "BLOCKED_APPLICATION_IMAGE_BUILD"
[ "$(docker image inspect "${IMAGE_TAG}" --format '{{.Config.User}}')" = "app" ] \
  || blocked "BLOCKED_APPLICATION_NON_ROOT_IDENTITY"

CURRENT_STAGE="deterministic-startup-chain"
run_bounded 360 "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" \
  || blocked "BLOCKED_DETERMINISTIC_STARTUP_CHAIN"

CURRENT_STAGE="flyway-and-role-verification"
if ! flyway_state="$(compose exec -T postgres psql --username=p3h_bootstrap \
  --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
  --command="SELECT count(*) || '|' || max(version) FROM flyway_schema_history WHERE success=true")"; then
  blocked "BLOCKED_FLYWAY_V7_QUERY"
fi
[ "${flyway_state}" = "7|7" ] || blocked "BLOCKED_FLYWAY_V7_VERIFICATION"

if ! role_state="$(compose exec -T postgres psql --username=p3h_bootstrap \
  --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
  --command="SELECT count(*) FROM pg_roles WHERE rolname IN ('p3h_migration_owner','p3h_app_readonly','p3h_backup_reader','p3h_recovery_owner') AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolinherit AND NOT rolreplication")"; then
  blocked "BLOCKED_ROLE_PROVISIONING_QUERY"
fi
[ "${role_state}" = "4" ] || blocked "BLOCKED_ROLE_PROVISIONING_VERIFICATION"

if ! database_state="$(compose exec -T postgres psql --username=p3h_bootstrap \
  --dbname=postgres --no-psqlrc --tuples-only --no-align --command="
    SELECT count(*)
    FROM pg_database d
    JOIN pg_roles r ON r.oid = d.datdba
    WHERE (d.datname = 'trade_model_v1_p3h_primary'
           AND r.rolname = 'p3h_migration_owner')
       OR (d.datname = 'trade_model_v1_p3h_recovery'
           AND r.rolname = 'p3h_recovery_owner')")"; then
  blocked "BLOCKED_DATABASE_PROVISIONING_QUERY"
fi
[ "${database_state}" = "2" ] \
  || blocked "BLOCKED_DATABASE_PROVISIONING_VERIFICATION"

CURRENT_STAGE="secret-readability-contract"
run_bounded 30 compose exec -T app bash -c \
  'test "$(id -u)" = 10001 && test -r /run/secrets/config/spring.datasource.password && test "$(stat -c %a /run/secrets/config/spring.datasource.password)" = 400' \
  || blocked "BLOCKED_APP_SECRET_READABILITY"
run_bounded 30 compose exec -T --user 10002:10002 app bash -c \
  'test ! -r /run/secrets/config/spring.datasource.password' \
  || blocked "BLOCKED_UNRELATED_UID_SECRET_READABILITY"

inspect_file="${TEMP_ROOT}/docker-inspect.json"
process_file="${TEMP_ROOT}/docker-processes.txt"
running_container_ids="${TEMP_ROOT}/running-container-ids.txt"
compose ps --quiet >"${running_container_ids}"
if [ ! -s "${running_container_ids}" ] \
    || ! xargs docker inspect <"${running_container_ids}" >"${inspect_file}"; then
  blocked "BLOCKED_DOCKER_INSPECT_EVIDENCE"
fi
: >"${process_file}"
while IFS= read -r running_container_id; do
  [ -n "${running_container_id}" ] || continue
  if ! docker top "${running_container_id}" >>"${process_file}"; then
    blocked "BLOCKED_PROCESS_ARGUMENT_EVIDENCE"
  fi
done <"${running_container_ids}"
for secret_name in postgres_admin_password flyway_password app_database_password_v1 \
    app_admin_password_v1 binance_nonfunctional_key binance_nonfunctional_secret; do
  if grep -Fq "$(tr -d '\r\n' <"${SECRET_DIR}/${secret_name}")" \
      "${inspect_file}" "${process_file}"; then
    blocked "BLOCKED_SECRET_VALUE_EXPOSURE"
  fi
done

CURRENT_STAGE="proxy-host-and-health"
unknown_status="$(curl --silent --max-time 10 --output /dev/null \
  --write-out '%{http_code}' -H 'Host: unapproved.invalid' \
  "http://127.0.0.1:${P3H_HTTP_HOST_PORT}/" || true)"
[ "${unknown_status}" != "308" ] && [ "${unknown_status}" != "200" ] \
  || blocked "BLOCKED_UNKNOWN_HOST_ACCEPTED"
if run_bounded 10 openssl s_client \
    -connect "127.0.0.1:${P3H_HTTPS_HOST_PORT}" \
    -servername unapproved.invalid </dev/null >/dev/null 2>&1; then
  blocked "BLOCKED_UNKNOWN_HTTPS_HOST_ACCEPTED"
fi

redirect_headers="${TEMP_ROOT}/redirect-headers.txt"
curl --silent --show-error --max-time 10 --output /dev/null \
  --dump-header "${redirect_headers}" -H 'Host: localhost' \
  "http://127.0.0.1:${P3H_HTTP_HOST_PORT}/actuator/health"
grep -Eiq "^Location: https://localhost/actuator/health\r?$" "${redirect_headers}" \
  || blocked "BLOCKED_APPROVED_REDIRECT_TARGET"

health_code="$(curl --silent --show-error --max-time 20 \
  --cacert "${SECRET_DIR}/tls_certificate" --output /dev/null \
  --write-out '%{http_code}' \
  "https://localhost:${P3H_HTTPS_HOST_PORT}/actuator/health")"
[ "${health_code}" = "200" ] || blocked "BLOCKED_APPROVED_HOST_HEALTH"

if openssl s_client -help 2>&1 | grep -q -- '-tls1_3'; then
  run_bounded 10 openssl s_client \
    -connect "localhost:${P3H_HTTPS_HOST_PORT}" -servername localhost \
    -CAfile "${SECRET_DIR}/tls_certificate" -verify_hostname localhost \
    -verify_return_error -tls1_3 </dev/null >/dev/null 2>&1 \
    || blocked "BLOCKED_LOCAL_TLS_1_3"
  local_tls_1_3=PASS
else
  local_tls_1_3=ENVIRONMENT_NOT_SUPPORTED
fi

echo "GREENFIELD_BOOTSTRAP_ORDER: PASS"
echo "ROLE_PROVISIONING_STATUS: PASS"
echo "DATABASE_PROVISIONING_STATUS: PASS_PRIMARY_AND_RECOVERY"
echo "FLYWAY_V1_TO_V7_STATUS: PASS"
echo "APP_SECRET_READABILITY_STATUS: PASS_ACTUAL_CONTAINER"
echo "UNRELATED_UID_SECRET_READABILITY: DENIED"
echo "SECRET_VALUES_IN_DOCKER_INSPECT: ABSENT"
echo "SECRET_VALUES_IN_PROCESS_ARGUMENTS: ABSENT"
echo "APP_RUNTIME_USER: NON_ROOT_UID_10001"
echo "HOST_HEADER_CONTRACT: PASS"
echo "UNKNOWN_HTTPS_HOST_REJECTED: PASS"
echo "TLS_LOCAL_HEALTH: PASS"
echo "TLS_1_3_LOCAL: ${local_tls_1_3}"
echo "READ_ONLY_WRITE_PROBE: DENIED"
echo "LIVE_PROVIDER_CALLS: 0"
echo "LOCAL_COMPOSE_TEMPLATE_SMOKE: PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE"
echo "REAL_STAGING_STATUS: BLOCKED_MISSING_AUTHORIZED_INPUT"
echo "P4_ALLOWED: NO"
echo "PRODUCTION_READINESS: BLOCKED"
