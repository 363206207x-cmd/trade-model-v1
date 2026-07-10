#!/usr/bin/env bash
set -euo pipefail

if [ "${COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS:-false}" != "true" ]; then
  echo "COINGLASS_LIVE_SMOKE: SKIPPED_EXTERNAL_CALLS_DISABLED"
  exit 0
fi

if [ "${TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED:-false}" != "true" ] \
  || [ "${TRADE_MODEL_COINGLASS_ENABLED:-false}" != "true" ]; then
  echo "COINGLASS_LIVE_SMOKE: SKIPPED_EXTERNAL_CALLS_DISABLED"
  exit 0
fi

if [ -z "${COINGLASS_API_KEY:-}" ]; then
  echo "COINGLASS_LIVE_SMOKE: SKIPPED_MISSING_API_KEY"
  exit 0
fi

echo "CoinGlass controlled smoke enabled; credentials and response bodies are redacted."

TRADE_MODEL_PROVIDER_CALL_ENABLED=true \
TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED=false \
TRADE_MODEL_SCHEDULERS_ENABLED=false \
COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS=true \
python3 - <<'PY'
import subprocess
import sys

try:
    result = subprocess.run(
        ["./mvnw", "-q", "-Dtest=CoinGlassControlledSmokeTest", "test"],
        timeout=300,
        check=False,
    )
except subprocess.TimeoutExpired:
    print("COINGLASS_LIVE_SMOKE: FAIL_TIMEOUT")
    sys.exit(1)

sys.exit(result.returncode)
PY
