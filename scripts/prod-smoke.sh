#!/usr/bin/env bash
set -euo pipefail

APP_URL="${APP_URL:-http://localhost:8081}"
AUTH_USERNAME="${SMOKE_AUTH_USERNAME:-${APP_ADMIN_USERNAME:-}}"
AUTH_PASSWORD="${SMOKE_AUTH_PASSWORD:-${APP_ADMIN_PASSWORD:-}}"
SMOKE_ALLOW_EXTERNAL_CALLS="${SMOKE_ALLOW_EXTERNAL_CALLS:-false}"

if [ -z "$AUTH_USERNAME" ] || [ -z "$AUTH_PASSWORD" ]; then
  echo "FAIL smoke auth credentials missing; set APP_ADMIN_USERNAME/APP_ADMIN_PASSWORD or SMOKE_AUTH_USERNAME/SMOKE_AUTH_PASSWORD" >&2
  exit 1
fi

health_body="$(mktemp)"
liveness_body="$(mktemp)"
readiness_body="$(mktemp)"
dashboard_body="$(mktemp)"
review_body="$(mktemp)"
trap 'rm -f "$health_body" "$liveness_body" "$readiness_body" "$dashboard_body" "$review_body"' EXIT

request_public_json() {
  local path="$1"
  local out="$2"
  local code
  code="$(curl -sS -o "$out" -w '%{http_code}' "${APP_URL}${path}" || true)"
  if [ "$code" != "200" ]; then
    echo "FAIL ${path} returned HTTP ${code}" >&2
    return 1
  fi
}

request_authenticated_json() {
  local path="$1"
  local out="$2"
  local code
  code="$(curl -sS -u "${AUTH_USERNAME}:${AUTH_PASSWORD}" -o "$out" -w '%{http_code}' "${APP_URL}${path}" || true)"
  if [ "$code" != "200" ]; then
    echo "FAIL ${path} returned HTTP ${code}" >&2
    return 1
  fi
}

request_public_json "/actuator/health" "$health_body"
request_public_json "/actuator/health/liveness" "$liveness_body"
request_public_json "/actuator/health/readiness" "$readiness_body"
request_authenticated_json "/api/dashboard/home" "$dashboard_body"
request_authenticated_json "/api/review/center" "$review_body"

python3 - "$health_body" "$liveness_body" "$readiness_body" "$dashboard_body" "$review_body" "$SMOKE_ALLOW_EXTERNAL_CALLS" <<'PY'
import json
import sys

health_path, liveness_path, readiness_path, dashboard_path, review_path, allow_external_calls = sys.argv[1:]
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

allowed_statuses = {"CONNECTED", "CONFIGURED", "NOT_CONFIGURED", "WAITING_SYNC", "FAIL_CLOSED", "UNKNOWN"}
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

print("PASS production smoke checks")
PY
