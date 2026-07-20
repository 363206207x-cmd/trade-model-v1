#!/usr/bin/env bash
set -euo pipefail

APP_URL="${APP_URL:-http://localhost:8081}"
AUTH_USERNAME="${TRADE_MODEL_SMOKE_USERNAME:-}"
AUTH_PASSWORD="${TRADE_MODEL_SMOKE_PASSWORD:-}"
RELEASE_GATE_REQUIRE_DOCKER="${RELEASE_GATE_REQUIRE_DOCKER:-true}"
RELEASE_GATE_REQUIRE_BACKUP="${RELEASE_GATE_REQUIRE_BACKUP:-false}"
RELEASE_GATE_ALLOW_EXTERNAL_CALLS="${RELEASE_GATE_ALLOW_EXTERNAL_CALLS:-false}"
RELEASE_GATE_REQUIRE_PROVIDER_SMOKE="${RELEASE_GATE_REQUIRE_PROVIDER_SMOKE:-false}"

status="PASS"

mark_incomplete() {
  local message="$1"
  echo "INCOMPLETE ${message}" >&2
  if [ "$status" = "PASS" ]; then
    status="INCOMPLETE"
  fi
}

mark_fail() {
  local message="$1"
  echo "FAIL ${message}" >&2
  status="FAIL"
}

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    return 1
  fi
}

run_docker_config_check() {
  if ! require_command docker; then
    if [ "$RELEASE_GATE_REQUIRE_DOCKER" = "true" ]; then
      mark_incomplete "docker is unavailable; real server compose config cannot be verified"
    else
      echo "SKIP docker compose config; docker unavailable and RELEASE_GATE_REQUIRE_DOCKER=false"
    fi
    return
  fi

  echo "CHECK docker compose config"
  if docker compose config >/dev/null; then
    echo "PASS docker compose config"
  else
    mark_fail "docker compose config failed"
  fi
}

run_smoke_check() {
  if [ -z "$AUTH_USERNAME" ] || [ -z "$AUTH_PASSWORD" ]; then
    mark_fail "Session smoke credentials missing; set TRADE_MODEL_SMOKE_USERNAME and TRADE_MODEL_SMOKE_PASSWORD"
    return
  fi

  echo "CHECK production smoke at ${APP_URL}"
  if TRADE_MODEL_SMOKE_USERNAME="$AUTH_USERNAME" \
    TRADE_MODEL_SMOKE_PASSWORD="$AUTH_PASSWORD" \
    SMOKE_ALLOW_EXTERNAL_CALLS="$RELEASE_GATE_ALLOW_EXTERNAL_CALLS" \
    SMOKE_PHASE="FETCH_AND_VALIDATE" \
    SMOKE_RESPONSE_DIR="" \
    SMOKE_SPLIT_PHASE_CONFIRM="" \
    bash scripts/prod-smoke.sh; then
    echo "PASS production smoke"
  else
    mark_fail "production smoke failed"
  fi
}

run_backup_check() {
  if [ "$RELEASE_GATE_REQUIRE_BACKUP" != "true" ]; then
    mark_incomplete "backup drill not required by this script run; set RELEASE_GATE_REQUIRE_BACKUP=true after server DB env is ready"
    return
  fi

  echo "CHECK production backup drill"
  if bash scripts/prod-backup.sh; then
    echo "PASS production backup drill"
  else
    mark_fail "production backup drill failed"
  fi
}

run_provider_smoke_check() {
  if [ "$RELEASE_GATE_REQUIRE_PROVIDER_SMOKE" != "true" ]; then
    mark_incomplete "provider live smoke not required by this script run; set RELEASE_GATE_REQUIRE_PROVIDER_SMOKE=true after provider env is ready"
    return
  fi

  echo "CHECK provider live smoke"
  local provider_output
  if provider_output="$(PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS="true" bash scripts/prod-provider-smoke.sh)"; then
    echo "$provider_output"
    if echo "$provider_output" | grep -q "PROVIDER_LIVE_SMOKE: PASS"; then
      echo "PASS provider live smoke"
    elif echo "$provider_output" | grep -q "PROVIDER_LIVE_SMOKE: FAIL"; then
      mark_fail "provider live smoke failed"
    else
      mark_incomplete "provider live smoke did not produce PASS"
    fi
  else
    echo "$provider_output"
    mark_fail "provider live smoke failed"
  fi
}

echo "Production release gate evidence runner"
echo "APP_URL=${APP_URL}"
echo "RELEASE_GATE_REQUIRE_DOCKER=${RELEASE_GATE_REQUIRE_DOCKER}"
echo "RELEASE_GATE_REQUIRE_BACKUP=${RELEASE_GATE_REQUIRE_BACKUP}"
echo "RELEASE_GATE_ALLOW_EXTERNAL_CALLS=${RELEASE_GATE_ALLOW_EXTERNAL_CALLS}"
echo "RELEASE_GATE_REQUIRE_PROVIDER_SMOKE=${RELEASE_GATE_REQUIRE_PROVIDER_SMOKE}"
echo "Passwords and secrets are intentionally not printed."

run_docker_config_check
run_smoke_check
run_backup_check
run_provider_smoke_check

if [ "$status" = "PASS" ]; then
  echo "INCOMPLETE restore drill, HTTPS/reverse-proxy smoke, and human review evidence must still be recorded before readiness can move beyond BLOCKED"
  status="INCOMPLETE"
fi

echo "PRODUCTION_RELEASE_GATE: ${status}"

case "$status" in
  PASS) exit 0 ;;
  INCOMPLETE) exit 2 ;;
  *) exit 1 ;;
esac
