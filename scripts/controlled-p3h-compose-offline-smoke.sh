#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONFIRMATION="I_CONFIRM_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE"
EXPECTED_BRANCH="codex/p3-u1-personal-login-session-auth"
GREENFIELD_CONFIRMATION="I_CONFIRM_EMPTY_GREENFIELD_INITIALIZATION"
GREENFIELD_RECOVERY_CONFIRMATION="I_CONFIRM_RECOVER_CONTROLLED_GREENFIELD_INITIALIZATION"
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

print_sanitized_p3h_status_lines() {
  printf '%s\n' "$1" \
    | LC_ALL=C grep -E \
      '^(P3H_[A-Z0-9_]+|FAILED_START_[A-Z0-9_]+|PROJECT_CONTAINER_COUNT|MATERIALIZED_SECRET_VOLUME|PRIMARY_DATABASE_VOLUME|BACKUP_VOLUME): [A-Z0-9_,.-]+$' \
    || true
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
P3H_GREENFIELD_RECOVERY_CONFIRM=""
export P3H_COMPOSE_PROJECT_NAME P3H_APPLICATION_IMAGE_TAG P3H_STAGING_HOSTNAME
export P3H_SECRET_MOUNT_DIR P3H_HTTP_HOST_PORT P3H_HTTPS_HOST_PORT
export P3H_ACTIVE_APP_DATABASE_SECRET_VERSION P3H_ACTIVE_APP_ADMIN_SECRET_VERSION
export P3H_START_MODE P3H_GREENFIELD_INITIALIZE_CONFIRM P3H_GREENFIELD_RECOVERY_CONFIRM

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

expect_input_contract_rejection() {
  local contract_type="$1"
  local fixture_name="$2"
  local fixture_value="$3"
  if "${ROOT_DIR}/scripts/p3h-controlled-input-contract.sh" \
      "${contract_type}" "${fixture_value}" >/dev/null 2>&1; then
    blocked "BLOCKED_INPUT_CONTRACT_ACCEPTED_${fixture_name}"
  fi
}

CURRENT_STAGE="controlled-input-injection-contract"
"${ROOT_DIR}/scripts/p3h-controlled-input-contract.sh" \
  STAGING_HOSTNAME stage.example.invalid >/dev/null \
  || blocked "BLOCKED_CANONICAL_STAGING_HOSTNAME"
"${ROOT_DIR}/scripts/p3h-controlled-input-contract.sh" \
  SSH_HOST stage.example.invalid >/dev/null \
  || blocked "BLOCKED_CANONICAL_SSH_HOST"
"${ROOT_DIR}/scripts/p3h-controlled-input-contract.sh" \
  SSH_USER p3h-deploy >/dev/null \
  || blocked "BLOCKED_CANONICAL_SSH_USER"
expect_input_contract_rejection STAGING_HOSTNAME HOSTNAME_SEMICOLON \
  'stage.example.invalid;load_module'
expect_input_contract_rejection STAGING_HOSTNAME HOSTNAME_NEWLINE \
  $'stage.example.invalid\nserver_name injected.invalid'
expect_input_contract_rejection STAGING_HOSTNAME HOSTNAME_NGINX_DIRECTIVE \
  'stage.example.invalid{include=/tmp/x;}'
expect_input_contract_rejection STAGING_HOSTNAME HOSTNAME_LEADING_DASH \
  '-stage.example.invalid'
expect_input_contract_rejection STAGING_HOSTNAME HOSTNAME_INVALID_LABEL \
  'stage.-invalid.example'
expect_input_contract_rejection SSH_HOST SSH_HOST_OPTION \
  '-oProxyCommand=invalid'
expect_input_contract_rejection SSH_HOST SSH_HOST_USERINFO \
  'operator@stage.example.invalid'
expect_input_contract_rejection SSH_USER SSH_USER_AT_SIGN 'p3h@deploy'
expect_input_contract_rejection SSH_USER SSH_USER_WHITESPACE 'p3h deploy'
expect_input_contract_rejection SSH_USER SSH_USER_OPTION '-oProxyCommand'

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

flyway_success_count() {
  compose exec -T postgres psql --username=p3h_bootstrap \
    --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
    --command="SELECT count(*) FROM flyway_schema_history WHERE success=true"
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

start_postgres_for_recovery_fixture() {
  run_bounded 120 compose up --detach --wait --wait-timeout 90 postgres >/dev/null \
    || blocked "BLOCKED_RECOVERY_FIXTURE_POSTGRES_START"
}

expect_recovery_rejection() {
  local fixture_name="$1"
  set +e
  local recovery_output
  recovery_output="$(run_bounded 360 "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" 2>&1)"
  local recovery_status=$?
  set -e
  [ "${recovery_status}" -ne 0 ] \
    || blocked "BLOCKED_RECOVERY_FIXTURE_ACCEPTED_${fixture_name}"
}

expect_recovery_contract_rejection() {
  local fixture_name="$1"
  set +e
  compose run --rm --no-deps greenfield-recovery-verify >/dev/null 2>&1
  local verify_status=$?
  set -e
  [ "${verify_status}" -ne 0 ] \
    || blocked "BLOCKED_RECOVERY_CONTRACT_DRIFT_ACCEPTED_${fixture_name}"
}

reset_to_v3_recovery_fixture() {
  run_bounded 180 compose --profile validation down --volumes --remove-orphans >/dev/null \
    || blocked "BLOCKED_RECOVERY_V3_RESET"
  start_postgres_for_recovery_fixture
  compose run --rm --no-deps role-bootstrap >/dev/null \
    || blocked "BLOCKED_RECOVERY_V3_ROLE_BOOTSTRAP"
  compose run --rm --no-deps -e FLYWAY_TARGET=3 migrate >/dev/null \
    || blocked "BLOCKED_RECOVERY_V3_MIGRATE"
  [ "$(flyway_success_count)" = "3" ] \
    || blocked "BLOCKED_RECOVERY_V3_VERSION"
}

restore_flyway_history_fixture() {
  start_postgres_for_recovery_fixture
  psql_admin trade_model_v1_p3h_primary "TRUNCATE TABLE flyway_schema_history" \
    || blocked "BLOCKED_RECOVERY_HISTORY_TRUNCATE"
  compose exec -T postgres psql --username=p3h_bootstrap \
    --dbname=trade_model_v1_p3h_primary --no-psqlrc --set=ON_ERROR_STOP=1 \
    <"${TEMP_ROOT}/flyway-v3-history.sql" >/dev/null \
    || blocked "BLOCKED_RECOVERY_HISTORY_RESTORE"
}

CURRENT_STAGE="partial-initialization-recovery"
compose run --rm --no-deps role-bootstrap >/dev/null \
  || blocked "BLOCKED_PARTIAL_RECOVERY_ROLE_BOOTSTRAP"
compose run --rm --no-deps -e FLYWAY_TARGET=3 migrate >/dev/null \
  || blocked "BLOCKED_PARTIAL_RECOVERY_V3_SETUP"
[ "$(flyway_success_count)" = "3" ] \
  || blocked "BLOCKED_PARTIAL_RECOVERY_V3_NOT_REACHED"

P3H_START_MODE=RECOVER_GREENFIELD_INITIALIZATION
P3H_GREENFIELD_INITIALIZE_CONFIRM=""
P3H_GREENFIELD_RECOVERY_CONFIRM=""
export P3H_START_MODE P3H_GREENFIELD_INITIALIZE_CONFIRM P3H_GREENFIELD_RECOVERY_CONFIRM
set +e
recovery_confirmation_output="$(run_bounded 30 \
  "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" 2>&1)"
recovery_confirmation_status=$?
set -e
[ "${recovery_confirmation_status}" -ne 0 ] \
  && printf '%s' "${recovery_confirmation_output}" \
    | grep -q 'BLOCKED_GREENFIELD_RECOVERY_CONFIRMATION' \
  || blocked "BLOCKED_RECOVERY_CONFIRMATION_NOT_REQUIRED"

P3H_GREENFIELD_RECOVERY_CONFIRM="${GREENFIELD_RECOVERY_CONFIRMATION}"
export P3H_GREENFIELD_RECOVERY_CONFIRM
compose exec -T postgres pg_dump --username=p3h_bootstrap \
  --dbname=trade_model_v1_p3h_primary --table=public.flyway_schema_history \
  --data-only --column-inserts --no-owner --no-privileges \
  >"${TEMP_ROOT}/flyway-v3-history.sql" \
  || blocked "BLOCKED_RECOVERY_HISTORY_BACKUP"

psql_admin trade_model_v1_p3h_primary \
  "DELETE FROM flyway_schema_history WHERE version='2'" \
  || blocked "BLOCKED_NONCONTIGUOUS_FIXTURE_SETUP"
expect_recovery_rejection NONCONTIGUOUS_FLYWAY_HISTORY
restore_flyway_history_fixture

psql_admin trade_model_v1_p3h_primary \
  "UPDATE flyway_schema_history SET checksum=checksum+1 WHERE version='3'" \
  || blocked "BLOCKED_CHECKSUM_FIXTURE_SETUP"
expect_recovery_rejection CHECKSUM_MISMATCH
restore_flyway_history_fixture

psql_admin trade_model_v1_p3h_primary "
  INSERT INTO flyway_schema_history(
    installed_rank, version, description, type, script, checksum,
    installed_by, execution_time, success
  )
  SELECT max(installed_rank)+1, '3.1', 'controlled failed fixture', 'SQL',
         'V3_1__controlled_failed_fixture.sql', NULL, 'p3h_migration_owner', 0, false
  FROM flyway_schema_history" \
  || blocked "BLOCKED_FAILED_MIGRATION_FIXTURE_SETUP"
expect_recovery_rejection FAILED_MIGRATION
restore_flyway_history_fixture

psql_admin trade_model_v1_p3h_primary \
  "CREATE TABLE public.p3h_unknown_business_object(id integer)" \
  || blocked "BLOCKED_UNKNOWN_OBJECT_FIXTURE_SETUP"
expect_recovery_rejection UNKNOWN_BUSINESS_OBJECT
start_postgres_for_recovery_fixture
psql_admin trade_model_v1_p3h_primary \
  "DROP TABLE public.p3h_unknown_business_object" \
  || blocked "BLOCKED_UNKNOWN_OBJECT_FIXTURE_CLEANUP"

CURRENT_STAGE="versioned-rule-default-content-drift"
psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_value='71' WHERE rule_id='cfg-ai-conflict-level3-max'" \
  || blocked "BLOCKED_RULE_VALUE_FIXTURE_SETUP"
expect_recovery_contract_rejection RULE_VALUE_MUTATION
psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_value='70' WHERE rule_id='cfg-ai-conflict-level3-max'" \
  || blocked "BLOCKED_RULE_VALUE_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_key='hot_reset_config.mutated_price_move' WHERE rule_id='cfg-hot-reset-price-move'" \
  || blocked "BLOCKED_RULE_KEY_FIXTURE_SETUP"
expect_recovery_contract_rejection RULE_KEY_MUTATION
psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_key='hot_reset_config.extreme_price_move_ratio_threshold' WHERE rule_id='cfg-hot-reset-price-move'" \
  || blocked "BLOCKED_RULE_KEY_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_type='mutated_type' WHERE rule_id='cfg-hot-reset-oi-collapse'" \
  || blocked "BLOCKED_RULE_TYPE_FIXTURE_SETUP"
expect_recovery_contract_rejection RULE_TYPE_MUTATION
psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_type='hot_reset_config' WHERE rule_id='cfg-hot-reset-oi-collapse'" \
  || blocked "BLOCKED_RULE_TYPE_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET version='v9.9' WHERE rule_id='cfg-confused-enter-threshold'" \
  || blocked "BLOCKED_RULE_VERSION_FIXTURE_SETUP"
expect_recovery_contract_rejection RULE_VERSION_MUTATION
psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET version='v1.0' WHERE rule_id='cfg-confused-enter-threshold'" \
  || blocked "BLOCKED_RULE_VERSION_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET enabled=false WHERE rule_id='cfg-hot-reset-systemic-severity'" \
  || blocked "BLOCKED_RULE_ENABLED_FIXTURE_SETUP"
expect_recovery_contract_rejection RULE_DISABLED_MUTATION
psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET enabled=true WHERE rule_id='cfg-hot-reset-systemic-severity'" \
  || blocked "BLOCKED_RULE_ENABLED_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary "
  INSERT INTO tm_rule_config(rule_id, rule_type, rule_key, rule_value, description, version, enabled)
  VALUES ('cfg-p3h-unexpected', 'p3h_fixture', 'p3h.fixture.unexpected', '1',
          'P3-H controlled unexpected rule fixture', 'v1.0', true)" \
  || blocked "BLOCKED_UNEXPECTED_RULE_FIXTURE_SETUP"
expect_recovery_contract_rejection UNEXPECTED_RULE_ROW
psql_admin trade_model_v1_p3h_primary \
  "DELETE FROM tm_rule_config WHERE rule_id='cfg-p3h-unexpected'" \
  || blocked "BLOCKED_UNEXPECTED_RULE_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "DELETE FROM tm_rule_config WHERE rule_id='cfg-push-recheck-drift-ratio'" \
  || blocked "BLOCKED_MISSING_RULE_FIXTURE_SETUP"
expect_recovery_contract_rejection MISSING_RULE_ROW
psql_admin trade_model_v1_p3h_primary "
  INSERT INTO tm_rule_config(rule_id, rule_type, rule_key, rule_value, description, version, enabled)
  VALUES ('cfg-push-recheck-drift-ratio', 'push_recheck_config',
          'push_recheck_config.drift_ratio_threshold', '0.02',
          'Push recheck drift ratio threshold', 'v1.0', true)" \
  || blocked "BLOCKED_MISSING_RULE_FIXTURE_CLEANUP"

CURRENT_STAGE="recovery-schema-contract-drift"
psql_admin trade_model_v1_p3h_primary \
  "DROP INDEX uk_tm_analysis_run_idempotency_key" \
  || blocked "BLOCKED_DROP_UNIQUE_INDEX_FIXTURE_SETUP"
expect_recovery_contract_rejection DROP_UNIQUE_INDEX
psql_admin trade_model_v1_p3h_primary \
  "CREATE UNIQUE INDEX uk_tm_analysis_run_idempotency_key ON tm_analysis_run(idempotency_key)" \
  || blocked "BLOCKED_DROP_UNIQUE_INDEX_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_execution_plan DROP CONSTRAINT ck_tm_execution_plan_safety_flags" \
  || blocked "BLOCKED_DROP_CHECK_FIXTURE_SETUP"
expect_recovery_contract_rejection DROP_SAFETY_CHECK
psql_admin trade_model_v1_p3h_primary "
  ALTER TABLE tm_execution_plan ADD CONSTRAINT ck_tm_execution_plan_safety_flags CHECK (
    manual_review_required = TRUE
    AND not_trade_instruction = TRUE
    AND not_executable = TRUE
    AND not_auto_trading = TRUE
    AND not_order_execution = TRUE
    AND not_user_position_creation = TRUE
  )" || blocked "BLOCKED_DROP_CHECK_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run ALTER COLUMN rule_version TYPE varchar(64)" \
  || blocked "BLOCKED_COLUMN_TYPE_FIXTURE_SETUP"
expect_recovery_contract_rejection ALTER_COLUMN_TYPE
psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run ALTER COLUMN rule_version TYPE varchar(32)" \
  || blocked "BLOCKED_COLUMN_TYPE_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run ALTER COLUMN symbol DROP NOT NULL" \
  || blocked "BLOCKED_COLUMN_NULLABILITY_FIXTURE_SETUP"
expect_recovery_contract_rejection ALTER_COLUMN_NULLABILITY
psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run ALTER COLUMN symbol SET NOT NULL" \
  || blocked "BLOCKED_COLUMN_NULLABILITY_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run ALTER COLUMN attempt_count SET DEFAULT 2" \
  || blocked "BLOCKED_COLUMN_DEFAULT_FIXTURE_SETUP"
expect_recovery_contract_rejection ALTER_COLUMN_DEFAULT
psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run ALTER COLUMN attempt_count SET DEFAULT 1" \
  || blocked "BLOCKED_COLUMN_DEFAULT_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run ADD COLUMN p3h_unexpected_column integer" \
  || blocked "BLOCKED_EXTRA_COLUMN_FIXTURE_SETUP"
expect_recovery_contract_rejection EXTRA_COLUMN
psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run DROP COLUMN p3h_unexpected_column" \
  || blocked "BLOCKED_EXTRA_COLUMN_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run ENABLE ROW LEVEL SECURITY" \
  || blocked "BLOCKED_RLS_FIXTURE_SETUP"
expect_recovery_contract_rejection ROW_LEVEL_SECURITY
psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run DISABLE ROW LEVEL SECURITY" \
  || blocked "BLOCKED_RLS_FIXTURE_CLEANUP"

# A dropped migrated column cannot be recreated with the original catalog position.
# Run it last, prove rejection, then rebuild the disposable V3 prefix exactly.
psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_analysis_run DROP COLUMN error_message" \
  || blocked "BLOCKED_MISSING_COLUMN_FIXTURE_SETUP"
expect_recovery_contract_rejection MISSING_COLUMN
reset_to_v3_recovery_fixture

set +e
recovery_output="$(run_bounded 360 \
  "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" 2>&1)"
recovery_status=$?
set -e
if [ "${recovery_status}" -ne 0 ]; then
  print_sanitized_p3h_status_lines "${recovery_output}"
  blocked "BLOCKED_PARTIAL_INITIALIZATION_RECOVERY"
fi
printf '%s' "${recovery_output}" | grep -q 'PARTIAL_INITIALIZATION_RECOVERY: PASS' \
  || blocked "BLOCKED_PARTIAL_INITIALIZATION_RECOVERY_EVIDENCE"
printf '%s' "${recovery_output}" | grep -q 'RECOVERED_READONLY_CONTRACT: PASS' \
  || blocked "BLOCKED_PARTIAL_INITIALIZATION_READONLY_RECOVERY"
[ "$(flyway_success_count)" = "8" ] \
  || blocked "BLOCKED_PARTIAL_INITIALIZATION_FINAL_VERSION"

run_bounded 180 compose --profile validation down --volumes --remove-orphans >/dev/null \
  || blocked "BLOCKED_PARTIAL_RECOVERY_FIXTURE_RESET"
P3H_START_MODE=INITIALIZE_GREENFIELD
P3H_GREENFIELD_INITIALIZE_CONFIRM="${GREENFIELD_CONFIRMATION}"
P3H_GREENFIELD_RECOVERY_CONFIRM=""
export P3H_START_MODE P3H_GREENFIELD_INITIALIZE_CONFIRM P3H_GREENFIELD_RECOVERY_CONFIRM

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
      if [ "${table_name}" = "tm_user" ]; then
        compose exec -T postgres psql --username=p3h_bootstrap \
          --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
          --command="SELECT 'tm_user', count(*), coalesce(md5(string_agg(md5(row_to_json(t)::text), '' ORDER BY md5(row_to_json(t)::text))), md5('')) FROM (SELECT id, username, password_hash, created_at FROM public.tm_user) t"
      else
        compose exec -T postgres psql --username=p3h_bootstrap \
          --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
          --command="SELECT '${quoted_table}', count(*), coalesce(md5(string_agg(md5(row_to_json(t)::text), '' ORDER BY md5(row_to_json(t)::text))), md5('')) FROM public.\"${quoted_table}\" t"
      fi
    done < <(compose exec -T postgres psql --username=p3h_bootstrap \
      --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
      --command="SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY table_name")
  } | sha256_stream
}

CURRENT_STAGE="first-boot"
run_bounded 360 "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" \
  || blocked "BLOCKED_FIRST_BOOT"
first_flyway_count="$(flyway_success_count)"
[ "${first_flyway_count}" = "8" ] || blocked "BLOCKED_FIRST_BOOT_FLYWAY"
first_fingerprint="$(database_fingerprint)"

CURRENT_STAGE="post-migration-readonly-grant-recovery"
run_bounded 120 "${ROOT_DIR}/deploy/p3h/p3h-compose-stop.sh" >/dev/null \
  || blocked "BLOCKED_POST_MIGRATION_GRANT_FIXTURE_STOP"
start_postgres_for_recovery_fixture
psql_admin trade_model_v1_p3h_primary "
  REVOKE ALL ON ALL TABLES IN SCHEMA public FROM p3h_app_readonly;
  REVOKE ALL ON ALL TABLES IN SCHEMA public FROM p3h_backup_reader;
  ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    REVOKE ALL ON TABLES FROM p3h_app_readonly;
  ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    REVOKE ALL ON TABLES FROM p3h_backup_reader" \
  || blocked "BLOCKED_POST_MIGRATION_GRANT_FIXTURE_SETUP"
P3H_START_MODE=STEADY_STATE_START
P3H_GREENFIELD_INITIALIZE_CONFIRM=""
P3H_GREENFIELD_RECOVERY_CONFIRM=""
export P3H_START_MODE P3H_GREENFIELD_INITIALIZE_CONFIRM P3H_GREENFIELD_RECOVERY_CONFIRM
set +e
grant_recovery_output="$(run_bounded 360 \
  "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" 2>&1)"
grant_recovery_status=$?
set -e
if [ "${grant_recovery_status}" -ne 0 ]; then
  print_sanitized_p3h_status_lines "${grant_recovery_output}"
  blocked "BLOCKED_POST_MIGRATION_GRANT_RECOVERY"
fi
printf '%s' "${grant_recovery_output}" | grep -q 'STEADY_STATE_RESTART: PASS' \
  || blocked "BLOCKED_POST_MIGRATION_GRANT_RECOVERY_EVIDENCE"
[ "$(flyway_success_count)" = "${first_flyway_count}" ] \
  || blocked "BLOCKED_POST_MIGRATION_GRANT_RECOVERY_RERAN_MIGRATIONS"
[ "$(database_fingerprint)" = "${first_fingerprint}" ] \
  || blocked "BLOCKED_POST_MIGRATION_GRANT_RECOVERY_FINGERPRINT"

CURRENT_STAGE="v2-secret-activation"
P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V2
P3H_ACTIVE_APP_ADMIN_SECRET_VERSION=V1
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
P3H_GREENFIELD_RECOVERY_CONFIRM=""
export P3H_START_MODE P3H_GREENFIELD_INITIALIZE_CONFIRM P3H_GREENFIELD_RECOVERY_CONFIRM
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

session_smoke() {
  local secret_file="$1"
  local label="$2"
  SESSION_SMOKE_LOG="${TEMP_ROOT}/session-smoke-${label}.log"
  (
    TRADE_MODEL_SMOKE_USERNAME=p3h_operator
    TRADE_MODEL_SMOKE_PASSWORD="$(tr -d '\r\n' <"${secret_file}")"
    TRADE_MODEL_SMOKE_CA_CERT="${SECRET_DIR}/tls_certificate"
    APP_URL="https://localhost:${P3H_HTTPS_HOST_PORT}"
    SMOKE_PHASE=FETCH_AND_VALIDATE
    SMOKE_RESPONSE_DIR=""
    SMOKE_SPLIT_PHASE_CONFIRM=""
    export TRADE_MODEL_SMOKE_USERNAME TRADE_MODEL_SMOKE_PASSWORD
    export TRADE_MODEL_SMOKE_CA_CERT APP_URL SMOKE_PHASE SMOKE_RESPONSE_DIR
    export SMOKE_SPLIT_PHASE_CONFIRM
    run_bounded 180 bash "${ROOT_DIR}/scripts/prod-smoke.sh"
  ) >"${SESSION_SMOKE_LOG}" 2>&1
}

print_sanitized_session_smoke_failure() {
  [ -f "${SESSION_SMOKE_LOG:-}" ] || return 0
  LC_ALL=C grep -E '^FAIL ' \
    "${SESSION_SMOKE_LOG}" || true
}

if ! session_smoke "${SECRET_DIR}/app_admin_password_v1" active-admin-v1; then
  print_sanitized_session_smoke_failure
  blocked "BLOCKED_ACTIVE_ADMIN_SESSION_SMOKE"
fi
if session_smoke "${SECRET_DIR}/app_admin_password_v2" inactive-admin-v2; then
  blocked "BLOCKED_UNAPPLIED_ADMIN_SECRET_ACCEPTED"
fi

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
compose --profile validation run --rm --no-deps \
  -e P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V2 app-role-probe >/dev/null \
  || blocked "BLOCKED_REBOOT_V2_DATABASE_SECRET_REJECTED"
if compose --profile validation run --rm --no-deps \
    -e P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V1 app-role-probe >/dev/null 2>&1; then
  blocked "BLOCKED_REBOOT_REACTIVATED_V1_DATABASE_SECRET"
fi
if ! session_smoke "${SECRET_DIR}/app_admin_password_v1" active-admin-v1-after-reboot; then
  print_sanitized_session_smoke_failure
  blocked "BLOCKED_REBOOT_ACTIVE_ADMIN_SESSION_SMOKE"
fi
if session_smoke "${SECRET_DIR}/app_admin_password_v2" inactive-admin-v2-after-reboot; then
  blocked "BLOCKED_REBOOT_UNAPPLIED_ADMIN_SECRET_ACCEPTED"
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
  printf '%s' "${failure_output}" | grep -q 'FAILED_START_DATABASE_PROCESS: STOPPED' \
    || blocked "BLOCKED_FAILED_START_DATABASE_PROCESS"
  printf '%s' "${failure_output}" | grep -q 'PROJECT_CONTAINER_COUNT: 0' \
    || blocked "BLOCKED_FAILED_START_CONTAINER_COUNT_EVIDENCE"
  printf '%s' "${failure_output}" | grep -q 'MATERIALIZED_SECRET_VOLUME: ABSENT' \
    || blocked "BLOCKED_FAILED_START_SECRET_VOLUME_EVIDENCE"
  printf '%s' "${failure_output}" | grep -q 'PRIMARY_DATABASE_VOLUME: PRESENT' \
    || blocked "BLOCKED_FAILED_START_PRIMARY_VOLUME_EVIDENCE"
  if compose ps --all --quiet | grep -q .; then
    blocked "BLOCKED_FAILED_START_PROJECT_CONTAINER_RETAINED"
  fi
  if materialized_volume_exists; then
    blocked "BLOCKED_FAILED_START_SECRET_VOLUME_RETAINED"
  fi
  primary_volume="$(docker volume ls --quiet \
    --filter "label=com.docker.compose.project=${PROJECT_NAME}" \
    --filter "label=com.docker.compose.volume=p3h_postgresql")"
  [ -n "${primary_volume}" ] \
    || blocked "BLOCKED_FAILED_START_PRIMARY_VOLUME_DELETED"
  backup_volume="$(docker volume ls --quiet \
    --filter "label=com.docker.compose.project=${PROJECT_NAME}" \
    --filter "label=com.docker.compose.volume=p3h_backups")"
  [ -n "${backup_volume}" ] \
    || blocked "BLOCKED_FAILED_START_BACKUP_VOLUME_DELETED"
  for source_secret in postgres_admin_password flyway_password \
      app_database_password_v1 app_database_password_v2 \
      app_admin_password_v1 app_admin_password_v2 backup_reader_password \
      recovery_owner_password binance_nonfunctional_key \
      binance_nonfunctional_secret tls_certificate tls_private_key; do
    [ -s "${SECRET_DIR}/${source_secret}" ] \
      || blocked "BLOCKED_FAILED_START_SOURCE_SECRET_DELETED"
  done
}

expect_full_readonly_verify_rejection() {
  local fixture_name="$1"
  set +e
  P3H_STEADY_VERIFY_SCOPE=FULL_READONLY_STATE_VERIFY \
    compose run --rm --no-deps steady-state-verify >/dev/null 2>&1
  local verify_status=$?
  set -e
  [ "${verify_status}" -ne 0 ] \
    || blocked "BLOCKED_READONLY_DRIFT_ACCEPTED_${fixture_name}"
}

CURRENT_STAGE="failed-start-cleanup"
exercise_failed_start_cleanup AFTER_SECRET_MATERIALIZATION
exercise_failed_start_cleanup AFTER_APP_START
exercise_failed_start_cleanup DURING_PROXY_HEALTH
run_bounded 360 "${ROOT_DIR}/deploy/p3h/p3h-compose-start.sh" \
  || blocked "BLOCKED_FINAL_STEADY_STATE_RECOVERY"

CURRENT_STAGE="v8-rule-and-schema-drift"
psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_value='61' WHERE rule_id='cfg-provider-scan-data-quality'" \
  || blocked "BLOCKED_V8_PROVIDER_RULE_FIXTURE_SETUP"
expect_full_readonly_verify_rejection V8_PROVIDER_RULE_VALUE
psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_value='60' WHERE rule_id='cfg-provider-scan-data-quality'" \
  || blocked "BLOCKED_V8_PROVIDER_RULE_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_value='59' WHERE rule_id='cfg-deriv-min-data-quality'" \
  || blocked "BLOCKED_V8_DERIV_RULE_FIXTURE_SETUP"
expect_full_readonly_verify_rejection V8_DERIV_RULE_VALUE
psql_admin trade_model_v1_p3h_primary \
  "UPDATE tm_rule_config SET rule_value='60' WHERE rule_id='cfg-deriv-min-data-quality'" \
  || blocked "BLOCKED_V8_DERIV_RULE_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "DROP INDEX idx_tm_ai_call_log_status_time" \
  || blocked "BLOCKED_V8_MISSING_INDEX_FIXTURE_SETUP"
expect_full_readonly_verify_rejection V8_MISSING_INDEX
psql_admin trade_model_v1_p3h_primary \
  "CREATE INDEX idx_tm_ai_call_log_status_time ON tm_ai_call_log(call_status, started_at)" \
  || blocked "BLOCKED_V8_MISSING_INDEX_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_decision_result ALTER COLUMN valid_from TYPE varchar(64)" \
  || blocked "BLOCKED_V8_OFFSET_COLUMN_FIXTURE_SETUP"
expect_full_readonly_verify_rejection V8_OFFSET_COLUMN_TYPE
psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_decision_result ALTER COLUMN valid_from TYPE timestamp with time zone USING valid_from::timestamptz" \
  || blocked "BLOCKED_V8_OFFSET_COLUMN_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_user_position ENABLE ROW LEVEL SECURITY" \
  || blocked "BLOCKED_V8_RLS_FIXTURE_SETUP"
expect_full_readonly_verify_rejection V8_ROW_LEVEL_SECURITY
psql_admin trade_model_v1_p3h_primary \
  "ALTER TABLE tm_user_position DISABLE ROW LEVEL SECURITY" \
  || blocked "BLOCKED_V8_RLS_FIXTURE_CLEANUP"

CURRENT_STAGE="readonly-role-membership-drift"
psql_admin postgres "GRANT p3h_migration_owner TO p3h_app_readonly" \
  || blocked "BLOCKED_APP_MEMBERSHIP_FIXTURE_SETUP"
expect_full_readonly_verify_rejection APP_ROLE_MEMBERSHIP
set +e
P3H_READONLY_GRANTS_MODE=STEADY_STATE \
  compose run --rm --no-deps readonly-grants >/dev/null 2>&1
membership_grant_refresh_status=$?
set -e
[ "${membership_grant_refresh_status}" -ne 0 ] \
  || blocked "BLOCKED_APP_MEMBERSHIP_AUTO_ACCEPTED"
psql_admin postgres "REVOKE p3h_migration_owner FROM p3h_app_readonly" \
  || blocked "BLOCKED_APP_MEMBERSHIP_FIXTURE_CLEANUP"

psql_admin postgres "GRANT p3h_recovery_owner TO p3h_backup_reader" \
  || blocked "BLOCKED_BACKUP_MEMBERSHIP_FIXTURE_SETUP"
expect_full_readonly_verify_rejection BACKUP_ROLE_MEMBERSHIP
psql_admin postgres "REVOKE p3h_recovery_owner FROM p3h_backup_reader" \
  || blocked "BLOCKED_BACKUP_MEMBERSHIP_FIXTURE_CLEANUP"

CURRENT_STAGE="readonly-default-acl-and-sequence-drift"
psql_admin trade_model_v1_p3h_primary "
  ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    GRANT INSERT, UPDATE ON TABLES TO p3h_app_readonly;
  ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    GRANT INSERT ON TABLES TO p3h_backup_reader;
  ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    GRANT USAGE, UPDATE ON SEQUENCES TO p3h_app_readonly;
  GRANT USAGE, UPDATE ON ALL SEQUENCES IN SCHEMA public TO p3h_app_readonly;
  GRANT TEMPORARY ON DATABASE trade_model_v1_p3h_primary TO p3h_backup_reader" \
  || blocked "BLOCKED_READONLY_ACL_DRIFT_FIXTURE_SETUP"
expect_full_readonly_verify_rejection DEFAULT_ACL_SEQUENCE_AND_DATABASE
P3H_READONLY_GRANTS_MODE=STEADY_STATE
export P3H_READONLY_GRANTS_MODE
compose run --rm --no-deps readonly-grants >/dev/null \
  || blocked "BLOCKED_READONLY_ACL_DRIFT_REPAIR"
P3H_STEADY_VERIFY_SCOPE=FULL_READONLY_STATE_VERIFY
export P3H_STEADY_VERIFY_SCOPE
compose run --rm --no-deps steady-state-verify >/dev/null \
  || blocked "BLOCKED_READONLY_ACL_EXACT_CONTRACT"
compose --profile validation run --rm --no-deps \
  -e P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V2 app-role-probe >/dev/null \
  || blocked "BLOCKED_SET_ROLE_DENIAL_PROBE"

CURRENT_STAGE="readonly-public-and-column-drift"
psql_admin trade_model_v1_p3h_primary \
  "GRANT UPDATE ON tm_rule_config TO PUBLIC" \
  || blocked "BLOCKED_PUBLIC_UPDATE_FIXTURE_SETUP"
expect_full_readonly_verify_rejection PUBLIC_EFFECTIVE_UPDATE
psql_admin trade_model_v1_p3h_primary \
  "REVOKE UPDATE ON tm_rule_config FROM PUBLIC" \
  || blocked "BLOCKED_PUBLIC_UPDATE_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "GRANT INSERT ON tm_rule_config TO PUBLIC" \
  || blocked "BLOCKED_PUBLIC_INSERT_FIXTURE_SETUP"
expect_full_readonly_verify_rejection PUBLIC_INSERT
psql_admin trade_model_v1_p3h_primary \
  "REVOKE INSERT ON tm_rule_config FROM PUBLIC" \
  || blocked "BLOCKED_PUBLIC_INSERT_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "GRANT UPDATE(rule_value) ON tm_rule_config TO p3h_app_readonly" \
  || blocked "BLOCKED_APP_COLUMN_UPDATE_FIXTURE_SETUP"
expect_full_readonly_verify_rejection APP_COLUMN_UPDATE
psql_admin trade_model_v1_p3h_primary \
  "REVOKE UPDATE(rule_value) ON tm_rule_config FROM p3h_app_readonly" \
  || blocked "BLOCKED_APP_COLUMN_UPDATE_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "GRANT UPDATE(rule_value) ON tm_rule_config TO p3h_backup_reader" \
  || blocked "BLOCKED_BACKUP_COLUMN_UPDATE_FIXTURE_SETUP"
expect_full_readonly_verify_rejection BACKUP_COLUMN_UPDATE
psql_admin trade_model_v1_p3h_primary \
  "REVOKE UPDATE(rule_value) ON tm_rule_config FROM p3h_backup_reader" \
  || blocked "BLOCKED_BACKUP_COLUMN_UPDATE_FIXTURE_CLEANUP"

psql_admin trade_model_v1_p3h_primary \
  "GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO PUBLIC" \
  || blocked "BLOCKED_PUBLIC_SEQUENCE_FIXTURE_SETUP"
expect_full_readonly_verify_rejection PUBLIC_SEQUENCE_USAGE
psql_admin trade_model_v1_p3h_primary \
  "REVOKE USAGE ON ALL SEQUENCES IN SCHEMA public FROM PUBLIC" \
  || blocked "BLOCKED_PUBLIC_SEQUENCE_FIXTURE_CLEANUP"

# Prove the grant refresh normalizes all three indirect write paths together.
psql_admin trade_model_v1_p3h_primary "
  GRANT UPDATE ON tm_rule_config TO PUBLIC;
  GRANT UPDATE(rule_value) ON tm_rule_config TO p3h_app_readonly;
  GRANT UPDATE(rule_value) ON tm_rule_config TO p3h_backup_reader;
  GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO PUBLIC" \
  || blocked "BLOCKED_COMBINED_WRITE_DRIFT_FIXTURE_SETUP"
P3H_READONLY_GRANTS_MODE=STEADY_STATE \
  compose run --rm --no-deps readonly-grants >/dev/null \
  || blocked "BLOCKED_PUBLIC_COLUMN_DRIFT_REPAIR"
P3H_STEADY_VERIFY_SCOPE=FULL_READONLY_STATE_VERIFY \
  compose run --rm --no-deps steady-state-verify >/dev/null \
  || blocked "BLOCKED_PUBLIC_COLUMN_EXACT_CONTRACT"
compose --profile validation run --rm --no-deps \
  -e P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V2 app-role-probe >/dev/null \
  || blocked "BLOCKED_EFFECTIVE_PERMISSION_WRITE_PROBE"

CURRENT_STAGE="final-runtime-verification"
if ! flyway_state="$(compose exec -T postgres psql --username=p3h_bootstrap \
  --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
  --command="SELECT count(*) || '|' || max(version) FROM flyway_schema_history WHERE success=true")"; then
  blocked "BLOCKED_FLYWAY_V8_QUERY"
fi
[ "${flyway_state}" = "8|8" ] || blocked "BLOCKED_FLYWAY_V8_VERIFICATION"

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

run_bounded 60 compose run --rm --no-deps \
  -e P3H_STAGING_HOSTNAME=stage.example.invalid proxy nginx -t >/dev/null 2>&1 \
  || blocked "BLOCKED_CANONICAL_STAGING_NGINX_RENDER"

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
echo "PARTIAL_INITIALIZATION_RECOVERY: PASS"
echo "RECOVERY_CONFIRMATION_STATUS: REQUIRED_AND_PROVEN"
echo "RECOVERED_FLYWAY_VERSION: 8"
echo "RECOVERED_READONLY_CONTRACT: PASS"
echo "STEADY_STATE_RESTART: PASS"
echo "REBOOT_LIKE_RESTART: PASS"
echo "DATABASE_VOLUME_PRESERVED: PASS"
echo "PRIMARY_DATABASE_VOLUME: PRESENT"
echo "FLYWAY_REPEAT: ZERO_MIGRATIONS"
echo "CONTENT_FINGERPRINT: MATCH"
echo "ACTIVE_DATABASE_SECRET_VERSION_PRESERVED: PASS"
echo "ACTIVE_APP_DATABASE_SECRET_VERSION: V2"
echo "ACTIVE_APP_ADMIN_SECRET_VERSION: V1"
echo "ADMIN_SECRET_ROTATION_STATUS: NOT_RUN_REQUIRES_CONTROLLED_TM_USER_PASSWORD_ROTATION"
echo "SESSION_AUTH_SMOKE: PASS_FORM_LOGIN_SESSION_CSRF"
echo "POST_LOGOUT_SESSION_INVALIDATION: PASS"
echo "DATABASE_SECRET_VERSION_AFTER_REBOOT: V2_ACTIVE_V1_DENIED"
echo "FAILED_START_DATABASE_PROCESS: STOPPED"
echo "FAILED_START_CLEANUP: PASS"
echo "READONLY_ROLE_MEMBERSHIP_CONTRACT: PASS"
echo "READONLY_DEFAULT_ACL_CONTRACT: PASS"
echo "READONLY_SEQUENCE_PRIVILEGE_CONTRACT: PASS"
echo "RULE_DEFAULT_CONTENT_CONTRACT: MATCH_EXACT_VERSIONED_ROWS"
echo "RECOVERY_SCHEMA_CONTRACT: MATCH_EXACT_PREFIX"
echo "STEADY_STATE_SCHEMA_CONTRACT: MATCH_EXACT_V8"
echo "READONLY_EFFECTIVE_TABLE_PRIVILEGES: PASS"
echo "READONLY_COLUMN_PRIVILEGES: PASS"
echo "PUBLIC_WRITE_PRIVILEGES: NONE"
echo "STAGING_HOSTNAME_CONTRACT: PASS_STRICT_DNS"
echo "SSH_HOST_CONTRACT: PASS_STRICT"
echo "SSH_USER_CONTRACT: PASS_STRICT"
echo "RULE_MUTATION_FIXTURES: PASS"
echo "SCHEMA_DRIFT_FIXTURES: PASS"
echo "PRIVILEGE_DRIFT_FIXTURES: PASS"
echo "INPUT_INJECTION_FIXTURES: PASS_BEFORE_NETWORK"
echo "GREENFIELD_OBJECT_INVENTORY: PASS_STRICT"
echo "SSH_KNOWN_HOSTS_FILTER: PASS_EXACT_PIN"
echo "APP_IMAGE_SOURCE: PASS_EXACT_COMMITTED_GIT_ARCHIVE"
echo "APP_IMAGE_REVISION: ${current_head}"
echo "ROLE_PROVISIONING_STATUS: PASS"
echo "DATABASE_PROVISIONING_STATUS: PASS_PRIMARY_AND_RECOVERY"
echo "FLYWAY_V1_TO_V8_STATUS: PASS"
echo "APP_SECRET_READABILITY_STATUS: PASS_ACTUAL_CONTAINER"
echo "UNRELATED_UID_SECRET_READABILITY: DENIED"
echo "SECRET_VALUES_IN_DOCKER_INSPECT: ABSENT"
echo "SECRET_VALUES_IN_PROCESS_ARGUMENTS: ABSENT"
echo "APP_RUNTIME_USER: NON_ROOT_UID_10001"
echo "HOST_HEADER_CONTRACT: PASS"
echo "UNKNOWN_HTTPS_HOST_REJECTED: PASS"
echo "TLS_LOCAL_HEALTH: PASS"
echo "TLS_1_3_LOCAL: ${local_tls_1_3}"
echo "BUSINESS_WRITE_PROBE: DENIED"
echo "AUTH_SESSION_WRITE_CONTRACT: ALLOWED_BOUNDED"
echo "LIVE_PROVIDER_CALLS: 0"
echo "LOCAL_COMPOSE_TEMPLATE_SMOKE: PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE"
echo "REAL_STAGING_STATUS: BLOCKED_MISSING_AUTHORIZED_INPUT"
echo "P4_ALLOWED: NO"
echo "PRODUCTION_READINESS: BLOCKED"
