#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  printf '%s\n' "OWNER_PASSWORD_RESET=FAILED" "REASON_CODE=ROOT_REQUIRED" >&2
  exit 1
fi
if [ "$#" -ne 0 ]; then
  printf '%s\n' "OWNER_PASSWORD_RESET=FAILED" "REASON_CODE=PASSWORD_ARGUMENTS_FORBIDDEN" >&2
  exit 1
fi
if [ ! -t 0 ]; then
  printf '%s\n' "OWNER_PASSWORD_RESET=FAILED" "REASON_CODE=INTERACTIVE_CONSOLE_REQUIRED" >&2
  exit 1
fi

umask 077
app_jar="${RINE_LOGIC_APP_JAR:-/opt/rine-logic/current/app.jar}"
active_env="${RINE_LOGIC_ACTIVE_ENV_FILE:-/etc/rine-logic/active.env}"
ai_env="${RINE_LOGIC_AI_ENV_FILE:-/etc/rine-logic/ai.env}"
for required in "${app_jar}" "${active_env}"; do
  if [ ! -r "${required}" ]; then
    printf '%s\n' "OWNER_PASSWORD_RESET=FAILED" "REASON_CODE=REQUIRED_RUNTIME_FILE_MISSING" >&2
    exit 1
  fi
done

set -a
# shellcheck disable=SC1090
. "${active_env}"
if [ -r "${ai_env}" ]; then
  # shellcheck disable=SC1090
  . "${ai_env}"
fi
set +a
export RINE_LOGIC_ACTIVE_ENV_FILE="${active_env}"

exec /usr/bin/java \
  -Dloader.main=org.example.trademodel.security.PersonalOwnerPasswordResetTool \
  -cp "${app_jar}" \
  org.springframework.boot.loader.launch.PropertiesLauncher
