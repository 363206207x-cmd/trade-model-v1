#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXPECTED_ROOT="/Users/xuchao/Documents/trade-model-v1"
RUNTIME_DIR="${ROOT_DIR}/.runtime/local"
CREDENTIALS_FILE="${RUNTIME_DIR}/local.env"
TARGET_RUNTIME_ENV="${FUNDAMENTAL_AI_TARGET_RUNTIME_ENV:-${HOME}/.config/fundamental-ai/v4.1-target-runtime.env}"
COOKIE_JAR="${RUNTIME_DIR}/startup-cookie.txt"
LOGIN_PAGE="${RUNTIME_DIR}/startup-login.html"
DASHBOARD_PAGE="${RUNTIME_DIR}/startup-dashboard.html"
APP_PID=""
REAL_DATA_MODE=false

if [[ "${1:-}" == "--real-data" ]]; then
  REAL_DATA_MODE=true
  shift
fi

fail() {
  printf '%s\n' "LOCAL_STARTUP=FAIL" "BLOCKER=$1" >&2
  exit 1
}

[[ $# -eq 0 ]] || fail "UNSUPPORTED_ARGUMENT"

stop_app() {
  if [[ -n "${APP_PID}" ]] && kill -0 "${APP_PID}" 2>/dev/null; then
    kill "${APP_PID}" 2>/dev/null || true
    wait "${APP_PID}" 2>/dev/null || true
  fi
}

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM
  stop_app
  rm -f -- "${COOKIE_JAR}" "${LOGIN_PAGE}" "${DASHBOARD_PAGE}"
  exit "${exit_code}"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

port_is_available() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    ! lsof -nP -iTCP:"${port}" -sTCP:LISTEN -t >/dev/null 2>&1
    return
  fi
  if command -v nc >/dev/null 2>&1; then
    ! nc -z 127.0.0.1 "${port}" >/dev/null 2>&1
    return
  fi
  fail "PORT_CHECK_TOOL_MISSING"
}

find_available_port() {
  local port=8081
  while (( port <= 65535 )); do
    if port_is_available "${port}"; then
      printf '%s' "${port}"
      return
    fi
    port=$((port + 1))
  done
  fail "NO_LOCAL_PORT_AVAILABLE"
}

load_java_17() {
  local java_home_17=""
  if [[ -x /usr/libexec/java_home ]] && java_home_17="$(/usr/libexec/java_home -v 17 2>/dev/null)"; then
    export JAVA_HOME="${java_home_17}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
  command -v java >/dev/null 2>&1 || fail "JAVA_17_MISSING"
  local java_version
  java_version="$(java -version 2>&1 | sed -n '1s/.*version "\([^"]*\)".*/\1/p')"
  [[ "${java_version}" == 17.* ]] || fail "JAVA_17_REQUIRED_FOUND_${java_version:-UNKNOWN}"
}

extract_csrf_token() {
  local token
  token="$(sed -n 's/.*name="_csrf"[^>]*value="\([^"]*\)".*/\1/p' "${LOGIN_PAGE}" | head -n 1)"
  if [[ -z "${token}" ]]; then
    token="$(sed -n 's/.*value="\([^"]*\)"[^>]*name="_csrf".*/\1/p' "${LOGIN_PAGE}" | head -n 1)"
  fi
  printf '%s' "${token}"
}

[[ "${ROOT_DIR}" == "${EXPECTED_ROOT}" ]] || fail "WRONG_REPOSITORY_${ROOT_DIR}"
cd "${ROOT_DIR}"

[[ -x ./mvnw ]] || fail "MAVEN_WRAPPER_MISSING"
command -v curl >/dev/null 2>&1 || fail "CURL_MISSING"
load_java_17

mkdir -p "${RUNTIME_DIR}"
chmod 700 "${ROOT_DIR}/.runtime" "${RUNTIME_DIR}"

if [[ ! -f "${CREDENTIALS_FILE}" ]]; then
  ./scripts/generate-runtime-password.sh --env-file "${CREDENTIALS_FILE}"
  printf '%s\n' "TRADE_MODEL_INITIAL_USERNAME='admin'" >>"${CREDENTIALS_FILE}"
fi
chmod 600 "${CREDENTIALS_FILE}"

set -a
# shellcheck disable=SC1090
source "${CREDENTIALS_FILE}"
set +a
LOCAL_LOGIN_USERNAME="${TRADE_MODEL_INITIAL_USERNAME:-}"
LOCAL_LOGIN_PASSWORD="${TRADE_MODEL_INITIAL_PASSWORD:-}"

if [[ "${REAL_DATA_MODE}" == "true" ]]; then
  [[ -r "${TARGET_RUNTIME_ENV}" ]] || fail "TARGET_RUNTIME_ENV_MISSING"
  set -a
  # Private runtime values stay in the process environment and are never printed.
  # shellcheck disable=SC1090
  source "${TARGET_RUNTIME_ENV}"
  set +a
  export TRADE_MODEL_INITIAL_USERNAME="${LOCAL_LOGIN_USERNAME}"
  export TRADE_MODEL_INITIAL_PASSWORD="${LOCAL_LOGIN_PASSWORD}"
fi

[[ -n "${TRADE_MODEL_INITIAL_USERNAME:-}" ]] || fail "LOCAL_USERNAME_MISSING"
[[ -n "${TRADE_MODEL_INITIAL_PASSWORD:-}" ]] || fail "LOCAL_PASSWORD_MISSING"

export TRADE_MODEL_AUTH_ENABLED=true
export TRADE_MODEL_SCHEDULERS_ENABLED=false
export TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED=false
export TRADE_MODEL_PROFILE_ESCALATION_ENABLED=false
export TRADE_MODEL_DISCOVERY_ENABLED=false
export TRADE_MODEL_WATCHLIST_SCHEDULER_ENABLED=false
export TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED=false
export TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED=false
export TRADE_MODEL_ANALYSIS_SCHEDULER_ENABLED=false
export TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED=false
if [[ "${REAL_DATA_MODE}" == "true" ]]; then
  export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local-real}"
  export TRADE_MODEL_PROVIDER_CALL_ENABLED=true
  export TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED=true
  export TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED=true
  export TRADE_MODEL_PUBLIC_OHLCV_EXTERNAL_CALLS_ENABLED=true
  export TRADE_MODEL_KRAKEN_OHLCV_ENABLED=true
  export TRADE_MODEL_KRAKEN_OHLCV_EXTERNAL_CALLS_ENABLED=true
  export TRADE_MODEL_BINANCE_OHLCV_ENABLED=true
  export TRADE_MODEL_BINANCE_OHLCV_EXTERNAL_CALLS_ENABLED=true
  export TRADE_MODEL_AI_ENABLED=false
  export TRADE_MODEL_AI_OPENAI_ENABLED=false
  export TRADE_MODEL_AI_GEMINI_ENABLED=false
  export TRADE_MODEL_AI_XAI_ENABLED=false
else
  export TRADE_MODEL_PROVIDER_CALL_ENABLED=false
  export TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED=false
  export TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED=false
  export TRADE_MODEL_PUBLIC_OHLCV_EXTERNAL_CALLS_ENABLED=false
  export TRADE_MODEL_KRAKEN_OHLCV_ENABLED=false
  export TRADE_MODEL_KRAKEN_OHLCV_EXTERNAL_CALLS_ENABLED=false
  export TRADE_MODEL_BINANCE_OHLCV_ENABLED=false
  export TRADE_MODEL_BINANCE_OHLCV_EXTERNAL_CALLS_ENABLED=false
  export TRADE_MODEL_COINGLASS_ENABLED=false
  export TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED=false
  export TRADE_MODEL_AI_ENABLED=false
  export TRADE_MODEL_AI_OPENAI_ENABLED=false
  export TRADE_MODEL_AI_GEMINI_ENABLED=false
  export TRADE_MODEL_AI_XAI_ENABLED=false
fi
export TRADE_MODEL_TELEGRAM_ENABLED=false
export TRADE_MODEL_TELEGRAM_EXTERNAL_CALLS_ENABLED=false
export TRADE_MODEL_TELEGRAM_DISPATCH_ENABLED=false
export AUTO_TRADING_ENABLED=false

PORT="$(find_available_port)"
LOCAL_URL="http://localhost:${PORT}"
LOGIN_URL="${LOCAL_URL}/login"
HOME_URL="${LOCAL_URL}/dashboard"
JAR_PATH="${ROOT_DIR}/target/trade-model-v1-0.0.1-SNAPSHOT.jar"

printf '%s\n' \
  "LOCAL_URL=${LOCAL_URL}" \
  "LOGIN_URL=${LOGIN_URL}" \
  "HOME_URL=${HOME_URL}" \
  "REAL_DATA_MODE=${REAL_DATA_MODE}" \
  "USERNAME=${TRADE_MODEL_INITIAL_USERNAME}" \
  "LOCAL_CREDENTIALS_FILE=${CREDENTIALS_FILE}" \
  "JAVA_HOME=${JAVA_HOME}"

printf '%s\n' "COMPILE=RUNNING"
./mvnw -q -DskipTests compile
printf '%s\n' "COMPILE=PASS"

printf '%s\n' "PACKAGE=RUNNING"
./mvnw -q -DskipTests package
[[ -f "${JAR_PATH}" ]] || fail "STANDARD_RELEASE_JAR_MISSING"
printf '%s\n' "PACKAGE=PASS" "APPLICATION_START=RUNNING"

java -jar "${JAR_PATH}" \
  --server.address=127.0.0.1 \
  --server.port="${PORT}" &
APP_PID=$!

deadline=$((SECONDS + 180))
login_http="000"
while (( SECONDS < deadline )); do
  if ! kill -0 "${APP_PID}" 2>/dev/null; then
    wait "${APP_PID}" || true
    APP_PID=""
    fail "APPLICATION_EXITED_DURING_STARTUP"
  fi
  if login_http="$(curl --silent --show-error --max-time 3 \
      --cookie-jar "${COOKIE_JAR}" --output "${LOGIN_PAGE}" \
      --write-out '%{http_code}' "${LOGIN_URL}" 2>/dev/null)" && [[ "${login_http}" == "200" ]]; then
    break
  fi
  sleep 1
done

[[ "${login_http}" == "200" ]] || fail "LOGIN_HTTP_${login_http}_AFTER_TIMEOUT"
printf '%s\n' "APPLICATION_START=PASS" "LOGIN_HTTP=200"

csrf_token="$(extract_csrf_token)"
[[ -n "${csrf_token}" ]] || fail "LOGIN_CSRF_TOKEN_MISSING"

login_post_http="$(curl --silent --show-error --max-time 10 \
  --cookie "${COOKIE_JAR}" --cookie-jar "${COOKIE_JAR}" \
  --output /dev/null --write-out '%{http_code}' \
  --data-urlencode "username=${TRADE_MODEL_INITIAL_USERNAME}" \
  --data-urlencode "password=${TRADE_MODEL_INITIAL_PASSWORD}" \
  --data-urlencode "_csrf=${csrf_token}" \
  "${LOGIN_URL}")"

[[ "${login_post_http}" == "302" ]] || fail "LOGIN_POST_HTTP_${login_post_http}"

dashboard_http="$(curl --silent --show-error --max-time 20 \
  --cookie "${COOKIE_JAR}" --output "${DASHBOARD_PAGE}" \
  --write-out '%{http_code}' "${HOME_URL}")"

[[ "${dashboard_http}" == "200" ]] || fail "AUTHENTICATED_DASHBOARD_HTTP_${dashboard_http}"
printf '%s\n' \
  "LOGIN=PASS" \
  "DASHBOARD_HTTP=200" \
  "LOCAL_STARTUP=READY" \
  "LOCAL_URL=${LOCAL_URL}" \
  "LOGIN_URL=${LOGIN_URL}" \
  "HOME_URL=${HOME_URL}"

if [[ "${TRADE_MODEL_LOCAL_OPEN_BROWSER:-true}" == "true" ]] && command -v open >/dev/null 2>&1; then
  open "${HOME_URL}" >/dev/null 2>&1 || open "${LOGIN_URL}" >/dev/null 2>&1 || true
fi

printf '%s\n' "The application is running in the foreground. Press Ctrl+C to stop."
wait "${APP_PID}"
APP_PID=""
