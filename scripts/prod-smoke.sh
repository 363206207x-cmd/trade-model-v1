#!/usr/bin/env bash
set -euo pipefail

APP_URL="${APP_URL:-http://localhost:8081}"

dashboard_body="$(mktemp)"
review_body="$(mktemp)"
trap 'rm -f "$dashboard_body" "$review_body"' EXIT

request_json() {
  local path="$1"
  local out="$2"
  local code
  code="$(curl -sS -o "$out" -w '%{http_code}' "${APP_URL}${path}" || true)"
  if [ "$code" != "200" ]; then
    echo "FAIL ${path} returned HTTP ${code}" >&2
    return 1
  fi
}

request_json "/api/dashboard/home" "$dashboard_body"
request_json "/api/review/center" "$review_body"

python3 - "$dashboard_body" "$review_body" <<'PY'
import json
import sys

dashboard_path, review_path = sys.argv[1], sys.argv[2]

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

telegram_status = (dashboard.get("pushInbox") or {}).get("telegramStatus")
if telegram_status == "CONNECTED":
    raise SystemExit("FAIL pushInbox.telegramStatus must not be CONNECTED without a verified source")

print("PASS production smoke checks")
PY
