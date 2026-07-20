#!/usr/bin/env bash
set -euo pipefail

APP_URL="${APP_URL:-http://localhost:8081}"
APP_URL="${APP_URL%/}"
AUTH_USERNAME="${TRADE_MODEL_SMOKE_USERNAME:-}"
AUTH_PASSWORD="${TRADE_MODEL_SMOKE_PASSWORD:-}"
SMOKE_ALLOW_EXTERNAL_CALLS="${SMOKE_ALLOW_EXTERNAL_CALLS:-false}"
SMOKE_PHASE="${SMOKE_PHASE:-FETCH_AND_VALIDATE}"
SMOKE_RESPONSE_DIR="${SMOKE_RESPONSE_DIR:-}"
SMOKE_SPLIT_PHASE_CONFIRM="${SMOKE_SPLIT_PHASE_CONFIRM:-}"
SMOKE_CA_CERT="${TRADE_MODEL_SMOKE_CA_CERT:-}"
SMOKE_CONNECT_TIMEOUT_SECONDS="${SMOKE_CONNECT_TIMEOUT_SECONDS:-5}"
SMOKE_MAX_TIME_SECONDS="${SMOKE_MAX_TIME_SECONDS:-20}"
EXPECTED_SPLIT_PHASE_CONFIRM="I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE"
umask 077

case "$SMOKE_PHASE" in
  FETCH|VALIDATE|FETCH_AND_VALIDATE) ;;
  *)
    echo "FAIL unsupported SMOKE_PHASE" >&2
    exit 1
    ;;
esac

if [ "$SMOKE_PHASE" = "FETCH" ] || [ "$SMOKE_PHASE" = "VALIDATE" ]; then
  if [ "$SMOKE_SPLIT_PHASE_CONFIRM" != "$EXPECTED_SPLIT_PHASE_CONFIRM" ]; then
    echo "FAIL split smoke phase requires explicit local-controlled confirmation" >&2
    exit 1
  fi
  if [ -z "$SMOKE_RESPONSE_DIR" ]; then
    echo "FAIL split smoke phase requires SMOKE_RESPONSE_DIR" >&2
    exit 1
  fi
fi

runtime_dir="$(mktemp -d)"
owned_response_dir=""
cleanup() {
  rm -rf "$runtime_dir"
  if [ -n "$owned_response_dir" ]; then
    rm -rf "$owned_response_dir"
  fi
}
trap cleanup EXIT HUP INT TERM
chmod 700 "$runtime_dir"

if [ -n "$SMOKE_CA_CERT" ]; then
  if [ ! -f "$SMOKE_CA_CERT" ] || [ -L "$SMOKE_CA_CERT" ]; then
    echo "FAIL smoke CA certificate must be a regular non-symlink file" >&2
    exit 1
  fi
fi

if [ -n "$SMOKE_RESPONSE_DIR" ]; then
  if [ ! -d "$SMOKE_RESPONSE_DIR" ] || [ -L "$SMOKE_RESPONSE_DIR" ]; then
    echo "FAIL smoke response directory must be an existing non-symlink directory" >&2
    exit 1
  fi
  health_body="$SMOKE_RESPONSE_DIR/health.json"
  liveness_body="$SMOKE_RESPONSE_DIR/liveness.json"
  readiness_body="$SMOKE_RESPONSE_DIR/readiness.json"
  dashboard_body="$SMOKE_RESPONSE_DIR/dashboard.json"
  review_body="$SMOKE_RESPONSE_DIR/review.json"
elif [ "$SMOKE_PHASE" = "FETCH_AND_VALIDATE" ]; then
  owned_response_dir="$(mktemp -d)"
  chmod 700 "$owned_response_dir"
  health_body="$owned_response_dir/health.json"
  liveness_body="$owned_response_dir/liveness.json"
  readiness_body="$owned_response_dir/readiness.json"
  dashboard_body="$owned_response_dir/dashboard.json"
  review_body="$owned_response_dir/review.json"
else
  echo "FAIL split smoke phase requires SMOKE_RESPONSE_DIR" >&2
  exit 1
fi

for response_file in "$health_body" "$liveness_body" "$readiness_body" "$dashboard_body" "$review_body"; do
  if [ -L "$response_file" ]; then
    echo "FAIL smoke response artifact must not be a symlink" >&2
    exit 1
  fi
done

login_page="$runtime_dir/login.html"
dashboard_page="$runtime_dir/dashboard.html"
login_headers="$runtime_dir/login.headers"
login_response="$runtime_dir/login-response.html"
login_form="$runtime_dir/login-form.txt"
logout_headers="$runtime_dir/logout.headers"
logout_response="$runtime_dir/logout-response.html"
logout_form="$runtime_dir/logout-form.txt"
cookie_jar="$runtime_dir/session.cookies"
pre_logout_cookie_jar="$runtime_dir/pre-logout-session.cookies"
anonymous_body="$runtime_dir/anonymous.json"
post_logout_body="$runtime_dir/post-logout.json"

curl_args=(
  --silent
  --show-error
  --connect-timeout "$SMOKE_CONNECT_TIMEOUT_SECONDS"
  --max-time "$SMOKE_MAX_TIME_SECONDS"
)
if [ -n "$SMOKE_CA_CERT" ]; then
  curl_args+=(--cacert "$SMOKE_CA_CERT")
fi

request_public_json() {
  local path="$1"
  local out="$2"
  local code
  code="$(curl "${curl_args[@]}" --output "$out" --write-out '%{http_code}' \
    "${APP_URL}${path}" || true)"
  if [ "$code" != "200" ]; then
    echo "FAIL ${path} returned HTTP ${code}" >&2
    return 1
  fi
}

request_unauthenticated_api() {
  local path="$1"
  local out="$2"
  local code
  code="$(curl "${curl_args[@]}" --output "$out" --write-out '%{http_code}' \
    "${APP_URL}${path}" || true)"
  if [ "$code" != "401" ]; then
    echo "FAIL unauthenticated ${path} returned HTTP ${code}" >&2
    return 1
  fi
}

fetch_login_page() {
  local code
  code="$(curl "${curl_args[@]}" --cookie-jar "$cookie_jar" \
    --output "$login_page" --write-out '%{http_code}' "${APP_URL}/login" || true)"
  if [ "$code" != "200" ]; then
    echo "FAIL /login returned HTTP ${code}" >&2
    return 1
  fi
}

extract_form_csrf() {
  local page="$1"
  local form_path="$2"
  python3 - "$page" "$form_path" <<'PY'
import sys
from html.parser import HTMLParser

class CsrfFormParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.in_target_form = False
        self.target_form_seen = False
        self.csrf = None

    def handle_starttag(self, tag, attrs):
        attributes = dict(attrs)
        if tag == "form":
            action = attributes.get("action", "")
            method = attributes.get("method", "").lower()
            self.in_target_form = action.endswith(sys.argv[2]) and method == "post"
            self.target_form_seen = self.target_form_seen or self.in_target_form
        elif tag == "input" and self.in_target_form:
            if attributes.get("name") == "_csrf" and attributes.get("value"):
                self.csrf = attributes["value"]

    def handle_endtag(self, tag):
        if tag == "form":
            self.in_target_form = False

parser = CsrfFormParser()
with open(sys.argv[1], "r", encoding="utf-8") as handle:
    parser.feed(handle.read())
if not parser.target_form_seen or not parser.csrf:
    raise SystemExit(1)
sys.stdout.write(parser.csrf)
PY
}

write_form_body() {
  local target="$1"
  local include_credentials="$2"
  local form_csrf_token="$3"
  SMOKE_FORM_USERNAME="$AUTH_USERNAME" \
  SMOKE_FORM_PASSWORD="$AUTH_PASSWORD" \
  SMOKE_FORM_CSRF="$form_csrf_token" \
  SMOKE_FORM_INCLUDE_CREDENTIALS="$include_credentials" \
    python3 - "$target" <<'PY'
import os
import sys
from urllib.parse import urlencode

fields = {"_csrf": os.environ["SMOKE_FORM_CSRF"]}
if os.environ["SMOKE_FORM_INCLUDE_CREDENTIALS"] == "true":
    fields["username"] = os.environ["SMOKE_FORM_USERNAME"]
    fields["password"] = os.environ["SMOKE_FORM_PASSWORD"]
with open(sys.argv[1], "w", encoding="utf-8") as handle:
    handle.write(urlencode(fields))
PY
  chmod 600 "$target"
}

assert_redirect_location() {
  local headers="$1"
  local expected="$2"
  python3 - "$headers" "$expected" <<'PY'
import sys
from urllib.parse import urlsplit

locations = []
with open(sys.argv[1], "r", encoding="iso-8859-1") as handle:
    for line in handle:
        name, separator, value = line.partition(":")
        if separator and name.strip().lower() == "location":
            locations.append(value.strip())
if not locations:
    raise SystemExit(1)
path = urlsplit(locations[-1]).path
query = urlsplit(locations[-1]).query
if sys.argv[2] == "LOGIN_SUCCESS":
    if path.endswith("/login") or "error" in query:
        raise SystemExit(1)
elif sys.argv[2] == "LOGOUT_SUCCESS":
    if not path.endswith("/login") or "logout" not in query:
        raise SystemExit(1)
else:
    raise SystemExit(1)
PY
}

perform_login() {
  local code
  write_form_body "$login_form" true "$login_csrf_token"
  code="$(curl "${curl_args[@]}" --request POST \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --cookie "$cookie_jar" --cookie-jar "$cookie_jar" \
    --data-binary "@${login_form}" --dump-header "$login_headers" \
    --output "$login_response" --write-out '%{http_code}' "${APP_URL}/login" || true)"
  case "$code" in
    302|303) ;;
    *) echo "FAIL login returned HTTP ${code}" >&2; return 1 ;;
  esac
  if ! assert_redirect_location "$login_headers" LOGIN_SUCCESS; then
    echo "FAIL login was not accepted" >&2
    return 1
  fi
  if [ ! -s "$cookie_jar" ]; then
    echo "FAIL login did not establish a Session cookie" >&2
    return 1
  fi
}

fetch_authenticated_dashboard_page() {
  local code
  code="$(curl "${curl_args[@]}" --cookie "$cookie_jar" --cookie-jar "$cookie_jar" \
    --output "$dashboard_page" --write-out '%{http_code}' "${APP_URL}/dashboard" || true)"
  if [ "$code" != "200" ]; then
    echo "FAIL authenticated /dashboard returned HTTP ${code}" >&2
    return 1
  fi
}

request_authenticated_json() {
  local path="$1"
  local out="$2"
  local code
  code="$(curl "${curl_args[@]}" --cookie "$cookie_jar" --cookie-jar "$cookie_jar" \
    --output "$out" --write-out '%{http_code}' "${APP_URL}${path}" || true)"
  if [ "$code" != "200" ]; then
    echo "FAIL ${path} returned HTTP ${code}" >&2
    return 1
  fi
  if ! python3 - "$out" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as handle:
    payload = json.load(handle)
if not isinstance(payload, dict):
    raise SystemExit(1)
PY
  then
    echo "FAIL ${path} did not return authenticated JSON" >&2
    return 1
  fi
}

perform_logout() {
  local code
  cp "$cookie_jar" "$pre_logout_cookie_jar"
  chmod 600 "$pre_logout_cookie_jar"
  write_form_body "$logout_form" false "$logout_csrf_token"
  code="$(curl "${curl_args[@]}" --request POST \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --cookie "$cookie_jar" --cookie-jar "$cookie_jar" \
    --data-binary "@${logout_form}" --dump-header "$logout_headers" \
    --output "$logout_response" --write-out '%{http_code}' "${APP_URL}/logout" || true)"
  case "$code" in
    302|303) ;;
    *) echo "FAIL logout returned HTTP ${code}" >&2; return 1 ;;
  esac
  if ! assert_redirect_location "$logout_headers" LOGOUT_SUCCESS; then
    echo "FAIL logout did not complete" >&2
    return 1
  fi
}

assert_pre_logout_session_invalidated() {
  local code
  code="$(curl "${curl_args[@]}" --cookie "$pre_logout_cookie_jar" \
    --output "$post_logout_body" --write-out '%{http_code}' \
    "${APP_URL}/api/dashboard/home" || true)"
  if [ "$code" != "401" ]; then
    echo "FAIL pre-logout Session remained valid with HTTP ${code}" >&2
    return 1
  fi
}

if [ "$SMOKE_PHASE" != "VALIDATE" ]; then
  if ! command -v python3 >/dev/null 2>&1; then
    echo "FAIL python3 is required for Session smoke" >&2
    exit 1
  fi
  if [ -z "$AUTH_USERNAME" ]; then
    echo "FAIL smoke username missing; set TRADE_MODEL_SMOKE_USERNAME" >&2
    exit 1
  fi
  if [ -z "$AUTH_PASSWORD" ]; then
    echo "FAIL smoke password missing; set TRADE_MODEL_SMOKE_PASSWORD" >&2
    exit 1
  fi
  request_public_json "/actuator/health" "$health_body"
  request_public_json "/actuator/health/liveness" "$liveness_body"
  request_public_json "/actuator/health/readiness" "$readiness_body"
  request_unauthenticated_api "/api/dashboard/home" "$anonymous_body"
  fetch_login_page
  if ! login_csrf_token="$(extract_form_csrf "$login_page" "/login")"; then
    echo "FAIL login page did not provide the CSRF contract" >&2
    exit 1
  fi
  perform_login
  fetch_authenticated_dashboard_page
  if ! logout_csrf_token="$(extract_form_csrf "$dashboard_page" "/logout")"; then
    echo "FAIL authenticated dashboard did not provide the logout CSRF contract" >&2
    exit 1
  fi
  request_authenticated_json "/api/dashboard/home" "$dashboard_body"
  request_authenticated_json "/api/review/center" "$review_body"
  perform_logout
  assert_pre_logout_session_invalidated
  unset login_csrf_token logout_csrf_token AUTH_PASSWORD
  echo "SESSION_AUTH_SMOKE: PASS_FORM_LOGIN_SESSION_CSRF"
  echo "POST_LOGOUT_SESSION_INVALIDATION: PASS"
fi

if [ "$SMOKE_PHASE" = "FETCH" ]; then
  echo "PASS controlled local split smoke response fetch"
  echo "SMOKE_EVIDENCE_SCOPE: LOCAL_CONTROLLED_SPLIT_ONLY"
  exit 0
fi

for response_file in "$health_body" "$liveness_body" "$readiness_body" "$dashboard_body" "$review_body"; do
  if [ ! -f "$response_file" ] || [ -L "$response_file" ]; then
    echo "FAIL smoke response artifact missing" >&2
    exit 1
  fi
done

if ! command -v python3 >/dev/null 2>&1; then
  echo "FAIL python3 is required for smoke response validation" >&2
  exit 1
fi

python3 - "$health_body" "$liveness_body" "$readiness_body" "$dashboard_body" "$review_body" "$SMOKE_ALLOW_EXTERNAL_CALLS" "$SMOKE_PHASE" <<'PY'
import json
import sys

health_path, liveness_path, readiness_path, dashboard_path, review_path, allow_external_calls, smoke_phase = sys.argv[1:]
allow_external_calls = allow_external_calls.lower() == "true"

def load_payload(path):
    with open(path, "r", encoding="utf-8") as handle:
        payload = json.load(handle)
    if isinstance(payload, dict) and isinstance(payload.get("data"), dict):
        return payload["data"]
    return payload

def require_keys(name, payload, keys):
    missing = [key for key in keys if key not in payload]
    if missing:
        raise SystemExit(f"FAIL {name} missing keys: {', '.join(missing)}")

dashboard = load_payload(dashboard_path)
review = load_payload(review_path)

for name, path in (
    ("health", health_path),
    ("liveness", liveness_path),
    ("readiness", readiness_path),
):
    payload = load_payload(path)
    if payload.get("status") != "UP":
        raise SystemExit(f"FAIL {name} status is not UP")
    if "components" in payload or "details" in payload:
        raise SystemExit(f"FAIL {name} exposes health details")

require_keys("dashboard", dashboard, [
    "header",
    "systemState",
    "assets",
    "positions",
    "executionSuggestion",
    "aiDecision",
    "pushInbox",
    "diagnostics",
    "safety",
])
require_keys("review center", review, [
    "summary",
    "positionReviews",
    "opportunityReviews",
    "pushReviews",
    "ruleFeedback",
])

safety = dashboard.get("safety") or {}
if safety.get("notAutoTrading") is not True:
    raise SystemExit("FAIL safety.notAutoTrading is not true")
if safety.get("notOrderExecution") is not True:
    raise SystemExit("FAIL safety.notOrderExecution is not true")

header = dashboard.get("header") or {}
if not header.get("dataSourceText"):
    raise SystemExit("FAIL dashboard header.dataSourceText missing")

diagnostics = dashboard.get("diagnostics") or {}
for key in ("marketDataProvider", "aiProvider", "externalContextProvider", "providerReadiness"):
    if key not in diagnostics:
        raise SystemExit(f"FAIL dashboard diagnostics.{key} missing")

allowed_statuses = {
    "CONNECTED", "CONFIGURED", "NOT_CONFIGURED", "WAITING_SYNC", "FAIL_CLOSED", "UNKNOWN",
    "NOT_CALLED", "DISABLED",
}
provider_readiness = diagnostics.get("providerReadiness") or {}
provider_statuses = [
    diagnostics.get("marketDataProvider"),
    diagnostics.get("aiProvider"),
    diagnostics.get("externalContextProvider"),
    (header.get("aiStatus") or ""),
]
for provider in provider_readiness.get("providers") or []:
    provider_statuses.append((provider or {}).get("status"))

for status in [value for value in provider_statuses if value]:
    if status not in allowed_statuses:
        raise SystemExit(f"FAIL unknown provider readiness status: {status}")
    if status == "CONNECTED" and not allow_external_calls:
        raise SystemExit("FAIL provider status CONNECTED requires SMOKE_ALLOW_EXTERNAL_CALLS=true and a verified source")

telegram_status = (dashboard.get("pushInbox") or {}).get("telegramStatus")
if telegram_status == "CONNECTED":
    raise SystemExit("FAIL pushInbox.telegramStatus must not be CONNECTED without a verified source")

if smoke_phase == "FETCH_AND_VALIDATE":
    print("PASS production smoke checks")
else:
    print("PASS controlled local split smoke validation")
    print("SMOKE_EVIDENCE_SCOPE: LOCAL_CONTROLLED_SPLIT_ONLY")
PY
