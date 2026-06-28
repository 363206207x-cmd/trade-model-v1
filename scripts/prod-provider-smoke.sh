#!/usr/bin/env bash
set -euo pipefail

PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS="${PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS:-false}"
PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED="${PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED:-false}"
PROVIDER_SMOKE_OPENAI_ENABLED="${PROVIDER_SMOKE_OPENAI_ENABLED:-false}"
PROVIDER_SMOKE_GEMINI_ENABLED="${PROVIDER_SMOKE_GEMINI_ENABLED:-false}"
PROVIDER_SMOKE_XAI_ENABLED="${PROVIDER_SMOKE_XAI_ENABLED:-false}"

BINANCE_API_BASE_URL="${BINANCE_API_BASE_URL:-https://fapi.binance.com}"
OPENAI_BASE_URL="${TRADE_MODEL_AI_OPENAI_BASE_URL:-https://api.openai.com}"
GEMINI_BASE_URL="${TRADE_MODEL_AI_GEMINI_BASE_URL:-https://generativelanguage.googleapis.com}"
XAI_BASE_URL="${TRADE_MODEL_AI_XAI_BASE_URL:-https://api.x.ai}"

overall_status="SKIPPED"

normalize_base_url() {
  local raw="$1"
  printf '%s' "${raw%/}"
}

record_result() {
  local name="$1"
  local status="$2"
  local detail="$3"
  echo "${name}: ${status} - ${detail}"
  case "$status" in
    FAIL)
      overall_status="FAIL"
      ;;
    PASS)
      if [ "$overall_status" = "SKIPPED" ]; then
        overall_status="PASS"
      fi
      ;;
    SKIPPED|NOT_CONFIGURED)
      if [ "$overall_status" = "SKIPPED" ]; then
        overall_status="INCOMPLETE"
      fi
      ;;
  esac
}

http_check() {
  python3 - "$@" <<'PY'
import sys
import urllib.error
import urllib.request

url = sys.argv[1]
headers = {}
for item in sys.argv[2:]:
    key, value = item.split("=", 1)
    if value:
        headers[key] = value

request = urllib.request.Request(url, headers=headers, method="GET")
try:
    with urllib.request.urlopen(request, timeout=10) as response:
        code = response.getcode()
except urllib.error.HTTPError as exc:
    code = exc.code
except Exception:
    code = 0

print(code)
PY
}

if [ "$PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS" != "true" ]; then
  echo "PROVIDER_LIVE_SMOKE: SKIPPED - set PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true and per-provider flags to run live checks"
  echo "BINANCE_PUBLIC_SMOKE: SKIPPED"
  echo "OPENAI_SMOKE: SKIPPED"
  echo "GEMINI_SMOKE: SKIPPED"
  echo "XAI_SMOKE: SKIPPED"
  exit 0
fi

echo "Provider live smoke is explicitly enabled. Secrets are intentionally not printed."

if [ "$PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED" = "true" ]; then
  binance_url="$(normalize_base_url "$BINANCE_API_BASE_URL")/fapi/v1/time"
  code="$(http_check "$binance_url")"
  if [ "$code" = "200" ]; then
    record_result "BINANCE_PUBLIC_SMOKE" "PASS" "public futures time endpoint reachable"
  else
    record_result "BINANCE_PUBLIC_SMOKE" "FAIL" "public futures time endpoint returned HTTP ${code}"
  fi
else
  record_result "BINANCE_PUBLIC_SMOKE" "SKIPPED" "set PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true to check Binance public market data"
fi

if [ "$PROVIDER_SMOKE_OPENAI_ENABLED" = "true" ]; then
  if [ -z "${OPENAI_API_KEY:-}" ]; then
    record_result "OPENAI_SMOKE" "NOT_CONFIGURED" "OPENAI_API_KEY missing"
  else
    openai_url="$(normalize_base_url "$OPENAI_BASE_URL")/v1/models"
    code="$(http_check "$openai_url" "Authorization=Bearer ${OPENAI_API_KEY}")"
    if [ "$code" = "200" ]; then
      record_result "OPENAI_SMOKE" "PASS" "models endpoint reachable"
    else
      record_result "OPENAI_SMOKE" "FAIL" "models endpoint returned HTTP ${code}"
    fi
  fi
else
  record_result "OPENAI_SMOKE" "SKIPPED" "set PROVIDER_SMOKE_OPENAI_ENABLED=true to check OpenAI"
fi

if [ "$PROVIDER_SMOKE_GEMINI_ENABLED" = "true" ]; then
  if [ -z "${GEMINI_API_KEY:-}" ]; then
    record_result "GEMINI_SMOKE" "NOT_CONFIGURED" "GEMINI_API_KEY missing"
  else
    gemini_url="$(normalize_base_url "$GEMINI_BASE_URL")/v1beta/models"
    code="$(http_check "$gemini_url" "x-goog-api-key=${GEMINI_API_KEY}")"
    if [ "$code" = "200" ]; then
      record_result "GEMINI_SMOKE" "PASS" "models endpoint reachable"
    else
      record_result "GEMINI_SMOKE" "FAIL" "models endpoint returned HTTP ${code}"
    fi
  fi
else
  record_result "GEMINI_SMOKE" "SKIPPED" "set PROVIDER_SMOKE_GEMINI_ENABLED=true to check Gemini"
fi

if [ "$PROVIDER_SMOKE_XAI_ENABLED" = "true" ]; then
  if [ -z "${XAI_API_KEY:-}" ]; then
    record_result "XAI_SMOKE" "NOT_CONFIGURED" "XAI_API_KEY missing"
  else
    xai_url="$(normalize_base_url "$XAI_BASE_URL")/v1/models"
    code="$(http_check "$xai_url" "Authorization=Bearer ${XAI_API_KEY}")"
    if [ "$code" = "200" ]; then
      record_result "XAI_SMOKE" "PASS" "models endpoint reachable"
    else
      record_result "XAI_SMOKE" "FAIL" "models endpoint returned HTTP ${code}"
    fi
  fi
else
  record_result "XAI_SMOKE" "SKIPPED" "set PROVIDER_SMOKE_XAI_ENABLED=true to check XAI"
fi

if [ -n "${NEWS_API_KEY:-}" ] || [ -n "${MACRO_CALENDAR_API_KEY:-}" ] || [ -n "${ETF_FLOW_API_KEY:-}" ]; then
  echo "EXTERNAL_CONTEXT_SMOKE: CONFIGURED - external context keys are present; no live external-context call is implemented by this harness"
else
  echo "EXTERNAL_CONTEXT_SMOKE: SKIPPED - no external context keys configured"
fi

echo "PROVIDER_LIVE_SMOKE: ${overall_status}"

case "$overall_status" in
  FAIL) exit 1 ;;
  *) exit 0 ;;
esac
