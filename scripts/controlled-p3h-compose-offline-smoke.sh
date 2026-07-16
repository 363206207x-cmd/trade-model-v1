#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONFIRMATION="I_CONFIRM_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE"
EXPECTED_BRANCH="codex/staging-readonly-tls-secrets-p3h"
GREENFIELD_CONFIRMATION="I_CONFIRM_EMPTY_GREENFIELD_INITIALIZATION"
FAILURE_INJECTION_CONFIRMATION="I_CONFIRM_LOCAL_P3H_FAILURE_INJECTION"
ROTATION_CONFIRMATION="I_CONFIRM_CONTROLLED_APP_DATABASE_SECRET_ROTATION"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/p3h/docker-compose.p3h.yml"
CURRENT_STAGE="confirmation"
TEMP_ROOT=""
ARCHIVE_CONTEXT=""
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

sha256_stream() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 | awk '{print $1}'
  else
    sha256sum | awk '{print $1}'
  fi
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
  if [ "${exit_status}" -eq 0 ] && [ -n "${PROJECT_NAME}" ]; then
    echo "LOCAL_DISPOSABLE_RESOURCE_CLEANUP: ${CLEANUP_STATUS}"
    [ "${CLEANUP_STATUS}" = "PASS" ] || exit 2
  elif [ "${exit_status}" -ne 0 ] && [ "${CLEANUP_STATUS}" = "PASS" ]; then
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

CURRENT_STAGE="exact-source-preflight"
for required_tool in docker git openssl curl realpath tar ssh-keygen; do
  command -v "${required_tool}" >/dev/null 2>&1 \
    || blocked "BLOCKED_REQUIRED_TOOL_MISSING"
done
current_branch="$(git -C "${ROOT_DIR}" branch --show-current)"
current_head="$(git -C "${ROOT_DIR}" rev-parse HEAD)"
if [ "${current_branch}" != "${EXPECTED_BRANCH}" ]; then
  blocked "BLOCKED_WRONG_BRANCH"
fi
if [ -n "$(git -C "${ROOT_DIR}" status --porcelain)" ]; then
  blocked "BLOCKED_DIRTY_WORKTREE"
fi
if [ -n "${P3H_EXPECTED_HEAD:-}" ] && [ "${P3H_EXPECTED_HEAD}" != "${current_head}" ]; then
  blocked "BLOCKED_WRONG_HEAD"
fi
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
ARCHIVE_CONTEXT="${TEMP_ROOT}/exact-source"
SECRET_DIR="${TEMP_ROOT}/credentials"
mkdir -p "${ARCHIVE_CONTEXT}" "${SECRET_DIR}"
chmod 700 "${TEMP_ROOT}" "${ARCHIVE_CONTEXT}" "${SECRET_DIR}"

archive_file="${TEMP_ROOT}/trade-model-p3h-${current_head}.tar"
run_bounded 60 git -C "${ROOT_DIR}" archive --format=tar \
  --output="${archive_file}" "${current_head}" \
  || blocked "BLOCKED_EXACT_GIT_ARCHIVE"
run_bounded 60 tar -xf "${archive_file}" -C "${ARCHIVE_CONTEXT}" \
  || blocked "BLOCKED_EXACT_GIT_ARCHIVE"
run_bounded 30 bash "${ROOT_DIR}/scripts/check-docker-context-safety.sh" \
  "${ARCHIVE_CONTEXT}" >/dev/null \
  || blocked "BLOCKED_DOCKER_CONTEXT_SAFETY"
if [ "$(git -C "${ROOT_DIR}" rev-parse HEAD)" != "${current_head}" ] \
    || [ -n "$(git -C "${ROOT_DIR}" status --porcelain)" ]; then
  blocked "BLOCKED_SOURCE_CHANGED_DURING_ARCHIVE"
fi

run_id="$(printf '%s' "$$-$(date +%s)" | sha256_stream | cut -c1-12)"
PROJECT_NAME="trade-model-p3h-${run_id}"
P3H_COMPOSE_PROJECT_NAME="${PROJECT_NAME}"
P3H_APPLICATION_IMAGE_TAG="offline-${run_id}"
IMAGE_TAG="trade-model-v1:p3h-${P3H_APPLICATION_IMAGE_TAG}"
P3H_STAGING_HOSTNAME="localhost"
P3H_SECRET_MOUNT_DIR="${SECRET_DIR}"
P3H_HTTP_HOST_PORT="${P3H_OFFLINE_HTTP_PORT:-18089}"
P3H_HTTPS_HOST_PORT="${P3H_OFFLINE_HTTPS_PORT:-18449}"
P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V1
P3H_ACTIVE_APP_ADMIN_SECRET_VERSION=V1
P3H_START_MODE=INITIALIZE_GREENFIELD
P3H_GREENFIELD_INITIALIZE_CONFIRM="${GREENFIELD_CONFIRMATION}"
export P3H_COMPOSE_PROJECT_NAME P3H_APPLICATION_IMAGE_TAG P3H_STAGING_HOSTNAME
export P3H_SECRET_MOUNT_DIR P3H_HTTP_HOST_PORT P3H_HTTPS_HOST_PORT
export P3H_ACTIVE_APP_DATABASE_SECRET_VERSION P3H_ACTIVE_APP_ADMIN_SECRET_VERSION
export P3H_START_MODE P3H_GREENFIELD_INITIALIZE_CONFIRM

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

CURRENT_STAGE="ssh-known-hosts-filter-contract"
ssh-keygen -q -t ed25519 -N '' -f "${TEMP_ROOT}/approved-key" >/dev/null
ssh-keygen -q -t ed25519 -N '' -f "${TEMP_ROOT}/unapproved-key" >/dev/null
approved_line="$(awk '{print "localhost " $1 " " $2}' "${TEMP_ROOT}/approved-key.pub")"
unapproved_line="$(awk '{print "localhost " $1 " " $2}' "${TEMP_ROOT}/unapproved-key.pub")"
approved_fingerprint="$(ssh-keygen -lf "${TEMP_ROOT}/approved-key.pub" -E sha256 | awk '{print $2}')"
printf '%s\n%s\n' "${approved_line}" "${unapproved_line}" >"${TEMP_ROOT}/known-hosts.candidates"
"${ROOT_DIR}/scripts/p3h-filter-known-hosts.sh" \
  "${TEMP_ROOT}/known-hosts.candidates" "${approved_fingerprint}" \
  "${TEMP_ROOT}/known-hosts.approved" >/dev/null \
  || blocked "BLOCKED_SSH_EXACT_PIN_FILTER"
[ "$(wc -l <"${TEMP_ROOT}/known-hosts.approved" | tr -d ' ')" = "1" ] \
  && grep -Fqx "${approved_line}" "${TEMP_ROOT}/known-hosts.approved" \
  && ! grep -Fq "${unapproved_line}" "${TEMP_ROOT}/known-hosts.approved" \
  || blocked "BLOCKED_SSH_EXACT_PIN_FILTER"
printf '%s\n%s\n' "${approved_line}" "${approved_line}" >"${TEMP_ROOT}/known-hosts.duplicates"
if "${ROOT_DIR}/scripts/p3h-filter-known-hosts.sh" \
    "${TEMP_ROOT}/known-hosts.duplicates" "${approved_fingerprint}" \
    "${TEMP_ROOT}/known-hosts.duplicate-output" >/dev/null 2>&1; then
  blocked "BLOCKED_SSH_DUPLICATE_PIN_ACCEPTED"
fi
printf '%s\n' "${unapproved_line}" >"${TEMP_ROOT}/known-hosts.zero"
if "${ROOT_DIR}/scripts/p3h-filter-known-hosts.sh" \
    "${TEMP_ROOT}/known-hosts.zero" "${approved_fingerprint}" \
    "${TEMP_ROOT}/known-hosts.zero-output" >/dev/null 2>&1; then
  blocked "BLOCKED_SSH_ZERO_MATCH_ACCEPTED"
fi

CURRENT_STAGE="compose-config"
run_bounded 60 compose config --quiet \
  || blocked "BLOCKED_COMPOSE_CONFIG"

CURRENT_STAGE="exact-archive-application-image-build"
run_bounded 600 docker build --pull=false \
  --file "${ARCHIVE_CONTEXT}/deploy/p3h/Dockerfile.p3h" \
  --build-arg "VCS_REF=${current_head}" --tag "${IMAGE_TAG}" "${ARCHIVE_CONTEXT}" \
  >/dev/null \
  || blocked "BLOCKED_APPLICATION_IMAGE_BUILD"
[ "$(docker image inspect "${IMAGE_TAG}" --format '{{.Config.User}}')" = "app" ] \
  || blocked "BLOCKED_APPLICATION_NON_ROOT_IDENTITY"
image_revision="$(docker image inspect "${IMAGE_TAG}" \
  --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}')"
[ "${image_revision}" = "${current_head}" ] \
  || blocked "BLOCKED_IMAGE_REVISION_MISMATCH"

psql_admin() {
  local database="$1"
  local sql="$2"
  compose exec -T postgres psql --username=p3h_bootstrap --dbname="${database}" \
    --no-psqlrc --set=ON_ERROR_STOP=1 --command="${sql}" >/dev/null
}

expect_greenfield_rejection() {
  local fixture_name="$1"
  local create_sql="$2"
  local cleanup_sql="$3"
  psql_admin trade_model_v1_p3h_primary "${create_sql}" \
    || blocked "BLOCKED_GREENFIELD_FIXTURE_SETUP_${fixture_name}"
  set +e
  compose run --rm --no-deps greenfield-preflight >/dev/null 2>&1
  local preflight_status=$?
  set -e
  psql_admin trade_model_v1_p3h_primary "${cleanup_sql}" \
    || blocked "BLOCKED_GREENFIELD_FIXTURE_CLEANUP_${fixture_name}"
  [ "${preflight_status}" -ne 0 ] \
    || blocked "BLOCKED_GREENFIELD_FIXTURE_ACCEPTED_${fixture_name}"
}

CURRENT_STAGE="strict-greenfield-object-inventory"
run_bounded 120 compose up --detach --wait --wait-timeout 90 postgres \
  || blocked "BLOCKED_POSTGRES_START"
compose run --rm --no-deps greenfield-preflight >/dev/null \
  || blocked "BLOCKED_CLEAN_GREENFIELD_PREFLIGHT"
expect_greenfield_rejection PUBLIC_FUNCTION \
  "CREATE FUNCTION public.p3h_probe() RETURNS integer LANGUAGE sql AS 'SELECT 1'" \
  "DROP FUNCTION public.p3h_probe()"
expect_greenfield_rejection NON_PUBLIC_TABLE \
  "CREATE SCHEMA p3h_probe_schema; CREATE TABLE p3h_probe_schema.probe(id integer)" \
  "DROP SCHEMA p3h_probe_schema CASCADE"
expect_greenfield_rejection FOREIGN_SERVER \
  "CREATE FOREIGN DATA WRAPPER p3h_probe_fdw NO HANDLER; CREATE SERVER p3h_probe_server FOREIGN DATA WRAPPER p3h_probe_fdw" \
  "DROP SERVER p3h_probe_server; DROP FOREIGN DATA WRAPPER p3h_probe_fdw"
expect_greenfield_rejection UNAPPROVED_EXTENSION \
  "CREATE EXTENSION hstore" "DROP EXTENSION hstore"
expect_greenfield_rejection USER_SEQUENCE \
  "CREATE SEQUENCE public.p3h_probe_sequence" "DROP SEQUENCE public.p3h_probe_sequence"
compose run --rm --no-deps greenfield-preflight >/dev/null \
  || blocked "BLOCKED_GREENFIELD_CLEAN_RECHECK"

database_fingerprint() {
  {
    compose exec -T postgres psql --username=p3h_bootstrap \
      --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
      --command="SELECT table_schema, table_name, ordinal_position, column_name, data_type, is_nullable FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name, ordinal_position"
    compose exec -T postgres psql --username=p3h_bootstrap \
      --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
      --command="SELECT indexname, indexdef FROM pg_indexes WHERE schemaname='public' ORDER BY indexname"
    compose exec -T postgres psql --username=p3h_bootstrap \
      --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
      --command="SELECT version, description, type, script, checksum, success FROM flyway_schema_history ORDER BY installed_rank"
    while IFS= read -r table_name; do
      [ -n "${table_name}" ] || continue
      quoted_table="${table_name//\"/\"\"}"
      compose exec -T postgres psql --username=p3h_bootstrap \
        --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
        --command="SELECT '${quoted_table}', count(*), coalesce(md5(string_agg(md5(row_to_json(t)::text), '' ORDER BY md5(row_to_json(t)::text))), md5('')) FROM public.\"${quoted_table}\" t"
    done < <(compose exec -T postgres psql --username=p3h_bootstrap \
      --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
      --command="SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY table_name")
  } | sha256_stream
}

flyway_success_count() {
  compose exec -T postgres psql --username=p3h_bootstrap \
    --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
    --command="SELECT count(*) FROM flyway_schema_history WHERE success=true"
}

CURRENT_STAGE="first-boot"
run_bounded 360 "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" \
  || blocked "BLOCKED_FIRST_BOOT"
first_flyway_count="$(flyway_success_count)"
[ "${first_flyway_count}" = "7" ] || blocked "BLOCKED_FIRST_BOOT_FLYWAY"
first_fingerprint="$(database_fingerprint)"

CURRENT_STAGE="v2-secret-activation"
P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V2
P3H_ACTIVE_APP_ADMIN_SECRET_VERSION=V2
P3H_SECRET_VERSION_ACTIVATION_CONFIRM="${ROTATION_CONFIRMATION}"
export P3H_ACTIVE_APP_DATABASE_SECRET_VERSION P3H_ACTIVE_APP_ADMIN_SECRET_VERSION
export P3H_SECRET_VERSION_ACTIVATION_CONFIRM
compose run --rm --no-deps app-database-secret-activate >/dev/null \
  || blocked "BLOCKED_V2_DATABASE_SECRET_ACTIVATION"

CURRENT_STAGE="steady-state-restart"
run_bounded 120 "${ROOT_DIR}/deploy/p3h/p3h-compose-stop.sh" >/dev/null \
  || blocked "BLOCKED_FIRST_STOP"
primary_volume="$(docker volume ls --quiet \
  --filter "label=com.docker.compose.project=${PROJECT_NAME}" \
  --filter "label=com.docker.compose.volume=p3h_postgresql")"
[ -n "${primary_volume}" ] || blocked "BLOCKED_PRIMARY_VOLUME_NOT_PRESERVED"
P3H_START_MODE=STEADY_STATE_START
P3H_GREENFIELD_INITIALIZE_CONFIRM=""
export P3H_START_MODE P3H_GREENFIELD_INITIALIZE_CONFIRM
run_bounded 360 "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" \
  || blocked "BLOCKED_STEADY_STATE_RESTART"
[ "$(flyway_success_count)" = "${first_flyway_count}" ] \
  || blocked "BLOCKED_STEADY_STATE_RERAN_MIGRATIONS"
steady_fingerprint="$(database_fingerprint)"
[ "${steady_fingerprint}" = "${first_fingerprint}" ] \
  || blocked "BLOCKED_STEADY_STATE_FINGERPRINT_MISMATCH"
compose --profile validation run --rm --no-deps \
  -e P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V2 app-role-probe >/dev/null \
  || blocked "BLOCKED_V2_DATABASE_SECRET_REJECTED"
if compose --profile validation run --rm --no-deps \
    -e P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V1 app-role-probe >/dev/null 2>&1; then
  blocked "BLOCKED_V1_DATABASE_SECRET_STILL_ACCEPTED"
fi

curl_auth_status() {
  local secret_file="$1"
  local config_file="$2"
  {
    echo 'silent'
    echo 'show-error'
    echo 'max-time = 20'
    echo "cacert = \"${SECRET_DIR}/tls_certificate\""
    printf 'user = "p3h_operator:%s"\n' "$(tr -d '\r\n' <"${secret_file}")"
  } >"${config_file}"
  chmod 600 "${config_file}"
  curl --config "${config_file}" --output /dev/null --write-out '%{http_code}' \
    "https://localhost:${P3H_HTTPS_HOST_PORT}/api/dashboard/home"
}

new_admin_status="$(curl_auth_status "${SECRET_DIR}/app_admin_password_v2" \
  "${TEMP_ROOT}/curl-admin-v2.conf")"
old_admin_status="$(curl_auth_status "${SECRET_DIR}/app_admin_password_v1" \
  "${TEMP_ROOT}/curl-admin-v1.conf")"
[ "${new_admin_status}" = "200" ] \
  || blocked "BLOCKED_V2_ADMIN_SECRET_REJECTED"
case "${old_admin_status}" in 401|403) ;; *)
  blocked "BLOCKED_V1_ADMIN_SECRET_STILL_ACCEPTED" ;;
esac

CURRENT_STAGE="reboot-like-restart"
run_bounded 120 compose stop >/dev/null \
  || blocked "BLOCKED_REBOOT_LIKE_STOP"
if compose ps --status running --quiet | grep -q .; then
  blocked "BLOCKED_REBOOT_LIKE_CONTAINERS_STILL_RUNNING"
fi
run_bounded 360 "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" \
  || blocked "BLOCKED_REBOOT_LIKE_RESTART"
[ "$(flyway_success_count)" = "${first_flyway_count}" ] \
  || blocked "BLOCKED_REBOOT_LIKE_RERAN_MIGRATIONS"
reboot_fingerprint="$(database_fingerprint)"
[ "${reboot_fingerprint}" = "${first_fingerprint}" ] \
  || blocked "BLOCKED_REBOOT_LIKE_FINGERPRINT_MISMATCH"
if compose --profile validation run --rm --no-deps \
    -e P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V1 app-role-probe >/dev/null 2>&1; then
  blocked "BLOCKED_REBOOT_REACTIVATED_V1_DATABASE_SECRET"
fi

materialized_volume_exists() {
  docker volume ls --quiet \
    --filter "label=com.docker.compose.project=${PROJECT_NAME}" \
    --filter "label=com.docker.compose.volume=p3h_materialized_secrets" | grep -q .
}

exercise_failed_start_cleanup() {
  local injection_stage="$1"
  run_bounded 120 "${ROOT_DIR}/deploy/p3h/p3h-compose-stop.sh" >/dev/null \
    || blocked "BLOCKED_FAILURE_FIXTURE_STOP"
  set +e
  failure_output="$(P3H_FAILURE_INJECTION_STAGE="${injection_stage}" \
    P3H_FAILURE_INJECTION_CONFIRM="${FAILURE_INJECTION_CONFIRMATION}" \
    "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" 2>&1)"
  failure_status=$?
  set -e
  [ "${failure_status}" -ne 0 ] \
    || blocked "BLOCKED_FAILURE_INJECTION_NOT_TRIGGERED"
  printf '%s' "${failure_output}" | grep -q 'FAILED_START_SECRET_CLEANUP: PASS' \
    || blocked "BLOCKED_FAILED_START_SECRET_CLEANUP"
  printf '%s' "${failure_output}" | grep -q 'FAILED_START_PARTIAL_STACK_CLEANUP: PASS' \
    || blocked "BLOCKED_FAILED_START_PARTIAL_CLEANUP"
  if compose ps --status running --services \
      | grep -Eq '^(app|proxy|secret-volume-holder)$'; then
    blocked "BLOCKED_FAILED_START_PARTIAL_STACK_RUNNING"
  fi
  if materialized_volume_exists; then
    blocked "BLOCKED_FAILED_START_SECRET_VOLUME_RETAINED"
  fi
  primary_volume="$(docker volume ls --quiet \
    --filter "label=com.docker.compose.project=${PROJECT_NAME}" \
    --filter "label=com.docker.compose.volume=p3h_postgresql")"
  [ -n "${primary_volume}" ] \
    || blocked "BLOCKED_FAILED_START_PRIMARY_VOLUME_DELETED"
  for source_secret in app_database_password_v1 app_database_password_v2 \
      app_admin_password_v1 app_admin_password_v2; do
    [ -s "${SECRET_DIR}/${source_secret}" ] \
      || blocked "BLOCKED_FAILED_START_SOURCE_SECRET_DELETED"
  done
}

CURRENT_STAGE="failed-start-cleanup"
exercise_failed_start_cleanup AFTER_SECRET_MATERIALIZATION
exercise_failed_start_cleanup AFTER_APP_START
exercise_failed_start_cleanup DURING_PROXY_HEALTH
run_bounded 360 "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" \
  || blocked "BLOCKED_FINAL_STEADY_STATE_RECOVERY"

CURRENT_STAGE="final-runtime-verification"
if ! flyway_state="$(compose exec -T postgres psql --username=p3h_bootstrap \
  --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
  --command="SELECT count(*) || '|' || max(version) FROM flyway_schema_history WHERE success=true")"; then
  blocked "BLOCKED_FLYWAY_V7_QUERY"
fi
[ "${flyway_state}" = "7|7" ] || blocked "BLOCKED_FLYWAY_V7_VERIFICATION"

role_state="$(compose exec -T postgres psql --username=p3h_bootstrap \
  --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
  --command="SELECT count(*) FROM pg_roles WHERE rolname IN ('p3h_migration_owner','p3h_app_readonly','p3h_backup_reader','p3h_recovery_owner') AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolinherit AND NOT rolreplication")"
[ "${role_state}" = "4" ] || blocked "BLOCKED_ROLE_PROVISIONING_VERIFICATION"

database_state="$(compose exec -T postgres psql --username=p3h_bootstrap \
  --dbname=postgres --no-psqlrc --tuples-only --no-align --command="
    SELECT count(*) FROM pg_database d JOIN pg_roles r ON r.oid=d.datdba
    WHERE (d.datname='trade_model_v1_p3h_primary' AND r.rolname='p3h_migration_owner')
       OR (d.datname='trade_model_v1_p3h_recovery' AND r.rolname='p3h_recovery_owner')")"
[ "${database_state}" = "2" ] \
  || blocked "BLOCKED_DATABASE_PROVISIONING_VERIFICATION"

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
  docker top "${running_container_id}" >>"${process_file}" \
    || blocked "BLOCKED_PROCESS_ARGUMENT_EVIDENCE"
done <"${running_container_ids}"
for secret_name in postgres_admin_password flyway_password app_database_password_v1 \
    app_database_password_v2 app_admin_password_v1 app_admin_password_v2 \
    binance_nonfunctional_key binance_nonfunctional_secret; do
  if grep -Fq "$(tr -d '\r\n' <"${SECRET_DIR}/${secret_name}")" \
      "${inspect_file}" "${process_file}"; then
    blocked "BLOCKED_SECRET_VALUE_EXPOSURE"
  fi
done

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

echo "FIRST_BOOT: PASS"
echo "STEADY_STATE_RESTART: PASS"
echo "REBOOT_LIKE_RESTART: PASS"
echo "DATABASE_VOLUME_PRESERVED: PASS"
echo "FLYWAY_REPEAT: ZERO_MIGRATIONS"
echo "CONTENT_FINGERPRINT: MATCH"
echo "ACTIVE_SECRET_VERSION_PRESERVED: PASS"
echo "ACTIVE_APP_DATABASE_SECRET_VERSION: V2"
echo "ACTIVE_APP_ADMIN_SECRET_VERSION: V2"
echo "OLD_SECRET_V1_POST_ROTATION: DENIED"
echo "FAILED_START_CLEANUP: PASS"
echo "GREENFIELD_OBJECT_INVENTORY: PASS_STRICT"
echo "SSH_KNOWN_HOSTS_FILTER: PASS_EXACT_PIN"
echo "APP_IMAGE_SOURCE: PASS_EXACT_COMMITTED_GIT_ARCHIVE"
echo "APP_IMAGE_REVISION: ${current_head}"
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
