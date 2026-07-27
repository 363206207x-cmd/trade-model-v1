#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/p3h/docker-compose.p3h.yml"
GREENFIELD_CONFIRMATION=I_CONFIRM_EMPTY_GREENFIELD_INITIALIZATION
GREENFIELD_RECOVERY_CONFIRMATION=I_CONFIRM_RECOVER_CONTROLLED_GREENFIELD_INITIALIZATION
FAILURE_INJECTION_CONFIRMATION=I_CONFIRM_LOCAL_P3H_FAILURE_INJECTION
P3H_CURRENT_STEP=PRECHECK

required_nonsecret=(
  P3H_APPLICATION_IMAGE_TAG
  P3H_STAGING_HOSTNAME
  P3H_SECRET_MOUNT_DIR
  P3H_START_MODE
  P3H_ACTIVE_APP_DATABASE_SECRET_VERSION
  P3H_ACTIVE_APP_ADMIN_SECRET_VERSION
)
for input_name in "${required_nonsecret[@]}"; do
  if [ -z "${!input_name:-}" ]; then
    echo "P3H_COMPOSE_START: BLOCKED_MISSING_INPUT"
    exit 2
  fi
done

case "${P3H_ACTIVE_APP_DATABASE_SECRET_VERSION}" in V1|V2) ;; *)
  echo "P3H_COMPOSE_START: BLOCKED_DATABASE_SECRET_VERSION"; exit 2 ;;
esac
case "${P3H_ACTIVE_APP_ADMIN_SECRET_VERSION}" in V1|V2) ;; *)
  echo "P3H_COMPOSE_START: BLOCKED_ADMIN_SECRET_VERSION"; exit 2 ;;
esac
case "${P3H_START_MODE}" in
  INITIALIZE_GREENFIELD)
    if [ "${P3H_GREENFIELD_INITIALIZE_CONFIRM:-}" != "${GREENFIELD_CONFIRMATION}" ]; then
      echo "P3H_COMPOSE_START: BLOCKED_GREENFIELD_CONFIRMATION"
      exit 2
    fi
    ;;
  STEADY_STATE_START) ;;
  RECOVER_GREENFIELD_INITIALIZATION)
    if [ "${P3H_GREENFIELD_RECOVERY_CONFIRM:-}" != "${GREENFIELD_RECOVERY_CONFIRMATION}" ]; then
      echo "P3H_COMPOSE_START: BLOCKED_GREENFIELD_RECOVERY_CONFIRMATION"
      exit 2
    fi
    ;;
  *) echo "P3H_COMPOSE_START: BLOCKED_START_MODE"; exit 2 ;;
esac

case "${P3H_FAILURE_INJECTION_STAGE:-}" in
  ""|AFTER_MIGRATION_BEFORE_READONLY_GRANTS|AFTER_SECRET_MATERIALIZATION|AFTER_APP_START|DURING_PROXY_HEALTH) ;;
  *) echo "P3H_COMPOSE_START: BLOCKED_FAILURE_INJECTION_STAGE"; exit 2 ;;
esac
if [ -n "${P3H_FAILURE_INJECTION_STAGE:-}" ] \
    && [ "${P3H_FAILURE_INJECTION_CONFIRM:-}" != "${FAILURE_INJECTION_CONFIRMATION}" ]; then
  echo "P3H_COMPOSE_START: BLOCKED_FAILURE_INJECTION_CONFIRMATION"
  exit 2
fi

compose=(docker compose -f "${COMPOSE_FILE}")
if ! "${compose[@]}" config --quiet; then
  echo "P3H_COMPOSE_START: BLOCKED_COMPOSE_CONFIG"
  exit 2
fi

remove_materialized_secret_volume() {
  local project_name="${P3H_COMPOSE_PROJECT_NAME:-trade-model-p3h}"
  local secret_volume
  while IFS= read -r secret_volume; do
    [ -n "${secret_volume}" ] || continue
    docker volume rm "${secret_volume}" >/dev/null 2>&1 || return 1
  done < <(docker volume ls --quiet \
    --filter "label=com.docker.compose.project=${project_name}" \
    --filter "label=com.docker.compose.volume=p3h_materialized_secrets")
}

cleanup_failed_start() {
  local exit_status=$?
  local cleanup_status=0
  trap - ERR
  echo "P3H_COMPOSE_FAILED_STEP: ${P3H_CURRENT_STEP}"
  set +e
  "${compose[@]}" --profile validation down --remove-orphans >/dev/null 2>&1
  local down_status=$?
  remove_materialized_secret_volume
  local secret_cleanup_status=$?
  local project_name="${P3H_COMPOSE_PROJECT_NAME:-trade-model-p3h}"
  local project_container_count
  local materialized_secret_volume_count
  local primary_database_volume_count
  local backup_volume_count
  project_container_count="$(docker ps --all --quiet \
    --filter "label=com.docker.compose.project=${project_name}" 2>/dev/null | awk 'NF {count++} END {print count+0}')"
  local container_query_status=$?
  materialized_secret_volume_count="$(docker volume ls --quiet \
    --filter "label=com.docker.compose.project=${project_name}" \
    --filter "label=com.docker.compose.volume=p3h_materialized_secrets" 2>/dev/null \
    | awk 'NF {count++} END {print count+0}')"
  local materialized_query_status=$?
  primary_database_volume_count="$(docker volume ls --quiet \
    --filter "label=com.docker.compose.project=${project_name}" \
    --filter "label=com.docker.compose.volume=p3h_postgresql" 2>/dev/null \
    | awk 'NF {count++} END {print count+0}')"
  local primary_query_status=$?
  backup_volume_count="$(docker volume ls --quiet \
    --filter "label=com.docker.compose.project=${project_name}" \
    --filter "label=com.docker.compose.volume=p3h_backups" 2>/dev/null \
    | awk 'NF {count++} END {print count+0}')"
  local backup_query_status=$?
  set -e

  echo "PROJECT_CONTAINER_COUNT: ${project_container_count}"
  if [ "${materialized_secret_volume_count}" = "0" ]; then
    echo "MATERIALIZED_SECRET_VOLUME: ABSENT"
  else
    echo "MATERIALIZED_SECRET_VOLUME: PRESENT"
  fi
  if [ "${primary_database_volume_count}" = "1" ]; then
    echo "PRIMARY_DATABASE_VOLUME: PRESENT"
  else
    echo "PRIMARY_DATABASE_VOLUME: ABSENT"
  fi
  if [ "${backup_volume_count}" = "1" ]; then
    echo "BACKUP_VOLUME: PRESENT"
  else
    echo "BACKUP_VOLUME: ABSENT"
  fi

  if [ "${down_status}" -eq 0 ] && [ "${secret_cleanup_status}" -eq 0 ] \
      && [ "${container_query_status}" -eq 0 ] && [ "${materialized_query_status}" -eq 0 ] \
      && [ "${primary_query_status}" -eq 0 ] && [ "${backup_query_status}" -eq 0 ] \
      && [ "${project_container_count}" = "0" ] \
      && [ "${materialized_secret_volume_count}" = "0" ] \
      && [ "${primary_database_volume_count}" = "1" ] \
      && [ "${backup_volume_count}" = "1" ]; then
    echo "FAILED_START_SECRET_CLEANUP: PASS"
    echo "FAILED_START_PARTIAL_STACK_CLEANUP: PASS"
    echo "FAILED_START_DATABASE_PROCESS: STOPPED"
  else
    echo "FAILED_START_SECRET_CLEANUP: FAIL"
    echo "FAILED_START_PARTIAL_STACK_CLEANUP: FAIL"
    echo "FAILED_START_DATABASE_PROCESS: UNVERIFIED"
    cleanup_status=98
  fi
  echo "FAILED_START_PRIMARY_VOLUME_POLICY: PRESERVED_UNLESS_EXPLICIT_TEARDOWN"
  if [ "${cleanup_status}" -ne 0 ]; then
    echo "P3H_COMPOSE_START: FAIL_CLEANUP_INCOMPLETE"
    exit "${cleanup_status}"
  fi
  echo "P3H_COMPOSE_START: FAIL_CLEANED"
  exit "${exit_status}"
}
trap cleanup_failed_start ERR

inject_failure_if_requested() {
  local stage="$1"
  if [ "${P3H_FAILURE_INJECTION_STAGE:-}" = "${stage}" ]; then
    echo "P3H_FAILURE_INJECTION: ${stage}"
    return 97
  fi
}

run_core_state_verify() {
  P3H_STEADY_VERIFY_SCOPE=CORE_STATE_VERIFY
  export P3H_STEADY_VERIFY_SCOPE
  "${compose[@]}" run --rm --no-deps steady-state-verify
}

run_full_readonly_state_verify() {
  P3H_STEADY_VERIFY_SCOPE=FULL_READONLY_STATE_VERIFY
  export P3H_STEADY_VERIFY_SCOPE
  "${compose[@]}" run --rm --no-deps steady-state-verify
}

P3H_CURRENT_STEP=POSTGRES_START
"${compose[@]}" up --detach --wait --wait-timeout 300 postgres

case "${P3H_START_MODE}" in
  INITIALIZE_GREENFIELD)
    export P3H_READONLY_GRANTS_MODE=INITIALIZE
    P3H_CURRENT_STEP=GREENFIELD_PREFLIGHT
    "${compose[@]}" run --rm --no-deps greenfield-preflight
    P3H_CURRENT_STEP=ROLE_BOOTSTRAP
    "${compose[@]}" run --rm --no-deps role-bootstrap
    P3H_CURRENT_STEP=FLYWAY_MIGRATE
    "${compose[@]}" run --rm --no-deps migrate
    P3H_CURRENT_STEP=FAILURE_INJECTION_AFTER_MIGRATION
    inject_failure_if_requested AFTER_MIGRATION_BEFORE_READONLY_GRANTS
    P3H_CURRENT_STEP=FLYWAY_VALIDATE
    "${compose[@]}" run --rm --no-deps flyway-validate
    P3H_CURRENT_STEP=CORE_STATE_VERIFY
    run_core_state_verify
    P3H_CURRENT_STEP=READONLY_GRANTS
    "${compose[@]}" run --rm --no-deps readonly-grants
    P3H_CURRENT_STEP=FULL_READONLY_STATE_VERIFY
    run_full_readonly_state_verify
    ;;
  STEADY_STATE_START)
    export P3H_READONLY_GRANTS_MODE=STEADY_STATE
    P3H_CURRENT_STEP=FLYWAY_VALIDATE
    "${compose[@]}" run --rm --no-deps flyway-validate
    P3H_CURRENT_STEP=CORE_STATE_VERIFY
    run_core_state_verify
    P3H_CURRENT_STEP=READONLY_GRANTS
    "${compose[@]}" run --rm --no-deps readonly-grants
    P3H_CURRENT_STEP=FULL_READONLY_STATE_VERIFY
    run_full_readonly_state_verify
    ;;
  RECOVER_GREENFIELD_INITIALIZATION)
    export P3H_READONLY_GRANTS_MODE=RECOVERY
    P3H_CURRENT_STEP=RECOVERY_FLYWAY_VALIDATE_BEFORE_MIGRATE
    "${compose[@]}" run --rm --no-deps \
      -e 'FLYWAY_IGNORE_MIGRATION_PATTERNS=*:pending' flyway-validate
    P3H_CURRENT_STEP=GREENFIELD_RECOVERY_VERIFY
    "${compose[@]}" run --rm --no-deps greenfield-recovery-verify
    P3H_CURRENT_STEP=RECOVERY_FLYWAY_MIGRATE
    "${compose[@]}" run --rm --no-deps migrate
    P3H_CURRENT_STEP=RECOVERY_FLYWAY_VALIDATE_AFTER_MIGRATE
    "${compose[@]}" run --rm --no-deps flyway-validate
    P3H_CURRENT_STEP=RECOVERY_CORE_STATE_VERIFY
    run_core_state_verify
    P3H_CURRENT_STEP=RECOVERY_READONLY_GRANTS
    "${compose[@]}" run --rm --no-deps readonly-grants
    P3H_CURRENT_STEP=RECOVERY_FULL_READONLY_STATE_VERIFY
    run_full_readonly_state_verify
    ;;
esac

P3H_CURRENT_STEP=SECRET_VOLUME_HOLDER_START
"${compose[@]}" up --detach --no-deps secret-volume-holder
P3H_CURRENT_STEP=SECRET_MATERIALIZATION
"${compose[@]}" run --rm --no-deps secret-materializer
P3H_CURRENT_STEP=FAILURE_INJECTION_AFTER_SECRET_MATERIALIZATION
inject_failure_if_requested AFTER_SECRET_MATERIALIZATION

P3H_CURRENT_STEP=APP_START
"${compose[@]}" up --detach --no-deps --wait --wait-timeout 300 app
P3H_CURRENT_STEP=FAILURE_INJECTION_AFTER_APP_START
inject_failure_if_requested AFTER_APP_START

P3H_CURRENT_STEP=PROXY_START
"${compose[@]}" up --detach --no-deps --wait --wait-timeout 300 proxy
P3H_CURRENT_STEP=FAILURE_INJECTION_DURING_PROXY_HEALTH
inject_failure_if_requested DURING_PROXY_HEALTH

P3H_CURRENT_STEP=APP_ROLE_PROBE
"${compose[@]}" --profile validation run --rm --no-deps app-role-probe

trap - ERR
echo "P3H_START_MODE: ${P3H_START_MODE}"
case "${P3H_START_MODE}" in
  INITIALIZE_GREENFIELD)
    echo "GREENFIELD_BOOTSTRAP_ORDER: PASS"
    echo "FIRST_BOOT: PASS"
    echo "FLYWAY_REPEAT: APPLIED_V1_TO_V9"
    ;;
  STEADY_STATE_START)
    echo "STEADY_STATE_RESTART: PASS"
    echo "FLYWAY_REPEAT: ZERO_MIGRATIONS"
    ;;
  RECOVER_GREENFIELD_INITIALIZATION)
    echo "PARTIAL_INITIALIZATION_RECOVERY: PASS"
    echo "RECOVERED_FLYWAY_VERSION: 9"
    echo "RECOVERED_READONLY_CONTRACT: PASS"
    ;;
esac
echo "ACTIVE_APP_DATABASE_SECRET_VERSION: ${P3H_ACTIVE_APP_DATABASE_SECRET_VERSION}"
echo "ACTIVE_APP_ADMIN_SECRET_VERSION: ${P3H_ACTIVE_APP_ADMIN_SECRET_VERSION}"
echo "APPLICATION_DATABASE_ROLE: BUSINESS_READ_ONLY_AUTH_SESSION_WRITE"
echo "P3H_COMPOSE_START: PASS"
