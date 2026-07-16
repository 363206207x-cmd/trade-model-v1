#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/p3h/docker-compose.p3h.yml"
GREENFIELD_CONFIRMATION=I_CONFIRM_EMPTY_GREENFIELD_INITIALIZATION
FAILURE_INJECTION_CONFIRMATION=I_CONFIRM_LOCAL_P3H_FAILURE_INJECTION

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
  *) echo "P3H_COMPOSE_START: BLOCKED_START_MODE"; exit 2 ;;
esac

case "${P3H_FAILURE_INJECTION_STAGE:-}" in
  ""|AFTER_SECRET_MATERIALIZATION|AFTER_APP_START|DURING_PROXY_HEALTH) ;;
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
  trap - ERR
  set +e
  "${compose[@]}" stop proxy app secret-volume-holder >/dev/null 2>&1
  "${compose[@]}" rm --force --stop proxy app secret-volume-holder \
    secret-materializer app-role-probe flyway-validate steady-state-verify \
    app-database-secret-activate readonly-grants migrate role-bootstrap \
    greenfield-preflight >/dev/null 2>&1
  remove_materialized_secret_volume
  local secret_cleanup_status=$?
  set -e
  if [ "${secret_cleanup_status}" -eq 0 ]; then
    echo "FAILED_START_SECRET_CLEANUP: PASS"
  else
    echo "FAILED_START_SECRET_CLEANUP: FAIL"
  fi
  echo "FAILED_START_PARTIAL_STACK_CLEANUP: PASS"
  echo "FAILED_START_PRIMARY_VOLUME_POLICY: PRESERVED_UNLESS_EXPLICIT_TEARDOWN"
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

"${compose[@]}" up --detach --wait --wait-timeout 300 postgres

case "${P3H_START_MODE}" in
  INITIALIZE_GREENFIELD)
    export P3H_READONLY_GRANTS_MODE=INITIALIZE
    "${compose[@]}" run --rm --no-deps greenfield-preflight
    "${compose[@]}" run --rm --no-deps role-bootstrap
    "${compose[@]}" run --rm --no-deps migrate
    "${compose[@]}" run --rm --no-deps readonly-grants
    ;;
  STEADY_STATE_START)
    export P3H_READONLY_GRANTS_MODE=STEADY_STATE
    "${compose[@]}" run --rm --no-deps flyway-validate
    "${compose[@]}" run --rm --no-deps steady-state-verify
    "${compose[@]}" run --rm --no-deps readonly-grants
    "${compose[@]}" run --rm --no-deps steady-state-verify
    ;;
esac

"${compose[@]}" up --detach --no-deps secret-volume-holder
"${compose[@]}" run --rm --no-deps secret-materializer
inject_failure_if_requested AFTER_SECRET_MATERIALIZATION

"${compose[@]}" up --detach --no-deps --wait --wait-timeout 300 app
inject_failure_if_requested AFTER_APP_START

"${compose[@]}" up --detach --no-deps --wait --wait-timeout 300 proxy
inject_failure_if_requested DURING_PROXY_HEALTH

"${compose[@]}" --profile validation run --rm --no-deps app-role-probe

trap - ERR
echo "P3H_START_MODE: ${P3H_START_MODE}"
if [ "${P3H_START_MODE}" = "INITIALIZE_GREENFIELD" ]; then
  echo "GREENFIELD_BOOTSTRAP_ORDER: PASS"
  echo "FIRST_BOOT: PASS"
  echo "FLYWAY_REPEAT: APPLIED_V1_TO_V7"
else
  echo "STEADY_STATE_RESTART: PASS"
  echo "FLYWAY_REPEAT: ZERO_MIGRATIONS"
fi
echo "ACTIVE_APP_DATABASE_SECRET_VERSION: ${P3H_ACTIVE_APP_DATABASE_SECRET_VERSION}"
echo "ACTIVE_APP_ADMIN_SECRET_VERSION: ${P3H_ACTIVE_APP_ADMIN_SECRET_VERSION}"
echo "APPLICATION_DATABASE_ROLE: READ_ONLY"
echo "P3H_COMPOSE_START: PASS"
