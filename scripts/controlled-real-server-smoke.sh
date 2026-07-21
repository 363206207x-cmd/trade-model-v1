#!/usr/bin/env bash
set -euo pipefail

# Controlled real/staging server smoke gate.
# Default behavior is no-op/skip unless CONTROLLED_SERVER_BASE_URL is present.
# This script never prints URL, username, password, cookies, tokens, or response bodies.

BASE_URL="${CONTROLLED_SERVER_BASE_URL:-}"
ADMIN_USERNAME="${CONTROLLED_SERVER_ADMIN_USERNAME:-}"
ADMIN_PASSWORD="${CONTROLLED_SERVER_ADMIN_PASSWORD:-}"
SMOKE_TIMEOUT_SECONDS="${CONTROLLED_SERVER_SMOKE_TIMEOUT_SECONDS:-300}"

if [ -z "$BASE_URL" ]; then
  echo "CONTROLLED_SERVER_ENV: CONTROLLED_SERVER_BASE_URL_MISSING"
  echo "REAL_SERVER_SMOKE_RESULT: SKIPPED_MISSING_CONTROLLED_SERVER"
  exit 0
fi

echo "CONTROLLED_SERVER_ENV: CONTROLLED_SERVER_BASE_URL_PRESENT"

case "$BASE_URL" in
  https://*)
    echo "CONTROLLED_SERVER_HTTPS_STATUS: PASS"
    ;;
  http://localhost*|http://127.0.0.1*|http://[[]::1[]]*)
    echo "CONTROLLED_SERVER_HTTPS_STATUS: LOCAL_ONLY_HTTP_NOT_REAL_SERVER"
    echo "REAL_SERVER_SMOKE_RESULT: SKIPPED_MISSING_CONTROLLED_SERVER"
    exit 0
    ;;
  http://*)
    echo "CONTROLLED_SERVER_HTTPS_STATUS: BLOCKED_NON_HTTPS_ENDPOINT"
    echo "REAL_SERVER_SMOKE_RESULT: BLOCKED_NON_HTTPS_ENDPOINT"
    exit 2
    ;;
  *)
    echo "CONTROLLED_SERVER_HTTPS_STATUS: BLOCKED_UNKNOWN_SCHEME"
    echo "REAL_SERVER_SMOKE_RESULT: BLOCKED_SERVER_UNAVAILABLE"
    exit 2
    ;;
esac

request_public_head() {
  local path="$1"
  local code
  code="$(curl -fsS --max-time 20 -o /dev/null -w '%{http_code}' "${BASE_URL}${path}" || true)"
  if [ "$code" = "200" ]; then
    echo "PUBLIC_SMOKE ${path}: PASS"
    return 0
  fi
  echo "PUBLIC_SMOKE ${path}: FAIL_HTTP_${code:-000}"
  return 1
}

health_status="PASS"
request_public_head "/actuator/health" || health_status="FAIL"
request_public_head "/actuator/health/liveness" || health_status="FAIL"
request_public_head "/actuator/health/readiness" || health_status="FAIL"

if [ -z "$ADMIN_USERNAME" ] || [ -z "$ADMIN_PASSWORD" ]; then
  echo "CONTROLLED_SERVER_AUTH_ENV: SKIPPED_MISSING_SECRET"
  echo "AUTHENTICATED_SERVER_SMOKE: SKIPPED_MISSING_SECRET"
  if [ "$health_status" = "PASS" ]; then
    echo "REAL_SERVER_SMOKE_RESULT: PARTIAL_HEALTH_ONLY"
    exit 0
  fi
  echo "REAL_SERVER_SMOKE_RESULT: BLOCKED_SERVER_UNAVAILABLE"
  exit 2
fi

echo "CONTROLLED_SERVER_AUTH_ENV: PRESENT_REDACTED"

python3 - <<'PYRUN'
import os
import subprocess
import sys

timeout = int(os.environ.get("CONTROLLED_SERVER_SMOKE_TIMEOUT_SECONDS", "300"))
env = os.environ.copy()
env["APP_URL"] = env["CONTROLLED_SERVER_BASE_URL"]
env["TRADE_MODEL_SMOKE_USERNAME"] = env["CONTROLLED_SERVER_ADMIN_USERNAME"]
env["TRADE_MODEL_SMOKE_PASSWORD"] = env["CONTROLLED_SERVER_ADMIN_PASSWORD"]
env.setdefault("SMOKE_ALLOW_EXTERNAL_CALLS", "false")
try:
    completed = subprocess.run(
        ["bash", "scripts/prod-smoke.sh"],
        env=env,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        timeout=timeout,
        check=False,
    )
except subprocess.TimeoutExpired:
    print("AUTHENTICATED_SERVER_SMOKE: SKIPPED_TIMEOUT")
    print("REAL_SERVER_SMOKE_RESULT: SKIPPED_TIMEOUT")
    sys.exit(2)

for line in completed.stdout.splitlines():
    if "password" in line.lower() or "secret" in line.lower() or "authorization" in line.lower() or "cookie" in line.lower():
        print("[redacted smoke output line]")
    else:
        print(line)

if completed.returncode == 0:
    print("AUTHENTICATED_SERVER_SMOKE: PASS")
    print("REAL_SERVER_SMOKE_RESULT: PASS")
    sys.exit(0)
print("AUTHENTICATED_SERVER_SMOKE: FAIL")
print("REAL_SERVER_SMOKE_RESULT: FAIL")
sys.exit(completed.returncode)
PYRUN
