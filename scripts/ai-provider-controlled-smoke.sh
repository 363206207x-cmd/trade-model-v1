#!/usr/bin/env bash
set -euo pipefail

emit_result() {
  local provider="${1:---}"
  local model="${2:---}"
  local auth="${3:-NOT_CHECKED}"
  local status="${4:-FAIL_UNEXPECTED}"
  local calls="${5:-0}"
  local key_reads="${6:-0}"
  local timeout_limit_ms=15000
  if [[ "${provider}" == "GEMINI" ]]; then
    timeout_limit_ms=30000
  elif [[ "${provider}" == "--" || -z "${provider}" ]]; then
    timeout_limit_ms=0
  fi
  printf '%s\n' \
    "AI_PROVIDER: ${provider}" \
    "AI_MODEL: ${model}" \
    "AI_AUTH_STATUS: ${auth}" \
    "AI_HTTP_STATUS_CLASS: NOT_RUN" \
    "AI_ERROR_CATEGORY: --" \
    "AI_PROVIDER_ERROR_REASON: --" \
    "AI_RESPONSE_PARSE_STATUS: NOT_RUN" \
    "AI_TOKEN_USAGE_PRESENT: NO" \
    "AI_REQUEST_ID_PRESENT: NO" \
    "AI_TIMEOUT_LIMIT_MS: ${timeout_limit_ms}" \
    "AI_LATENCY_MS: 0" \
    "AI_PROVIDER_LIVE_SMOKE: ${status}" \
    "LIVE_PROVIDER_CALLS: ${calls}" \
    "REAL_KEYS_READ: ${key_reads}" \
    "PRODUCTION_READINESS: BLOCKED"
}

emit_diagnostic_result() {
  local mode="${1:---}"
  local category="${2:---}"
  local parse_status="${3:-NOT_RUN}"
  local calls="${4:-0}"
  printf '%s\n' \
    "AI_PROVIDER: GEMINI" \
    "AI_DIAGNOSTIC_MODE: ${mode}" \
    "AI_HTTP_STATUS_CLASS: NOT_RUN" \
    "AI_ERROR_CATEGORY: ${category}" \
    "AI_RESPONSE_PARSE_STATUS: ${parse_status}" \
    "AI_LATENCY_MS: 0" \
    "LIVE_PROVIDER_CALLS: ${calls}" \
    "PRODUCTION_READINESS: BLOCKED"
}

target="${AI_PROVIDER_SMOKE_TARGET:-}"
diagnostic_enabled="${AI_PROVIDER_SMOKE_DIAGNOSTIC:-false}"
diagnostic_mode="${GEMINI_DIAGNOSTIC_MODE:-}"
if [[ "${diagnostic_enabled}" == "true" ]]; then
  case "${diagnostic_mode}" in
    A|B|C) ;;
    *)
      emit_diagnostic_result "--" "UNKNOWN_PROVIDER_ERROR" "NOT_RUN" "0"
      exit 2
      ;;
  esac
  if [[ -n "${target}" && "${target}" != "GEMINI" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "UNKNOWN_PROVIDER_ERROR" "NOT_RUN" "0"
    exit 2
  fi
  target="GEMINI"
  export AI_PROVIDER_SMOKE_TARGET=GEMINI
fi
if [[ "${AI_PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS:-false}" != "true" ]]; then
  if [[ "${diagnostic_enabled}" == "true" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "--" "NOT_RUN" "0"
  else
    emit_result "${target:---}" "--" "NOT_CHECKED" "SKIPPED_EXTERNAL_CALLS_DISABLED" "0"
  fi
  exit 0
fi

case "${target}" in
  OPENAI)
    model="${TRADE_MODEL_AI_OPENAI_GPT_FINAL_FAST_MODEL:-gpt-5.6-luna}"
    provider_enabled="${TRADE_MODEL_AI_OPENAI_ENABLED:-false}"
    ;;
  GEMINI)
    model="${TRADE_MODEL_AI_GEMINI_MODEL:-gemini-3.5-flash}"
    provider_enabled="${TRADE_MODEL_AI_GEMINI_ENABLED:-false}"
    ;;
  XAI)
    model="${TRADE_MODEL_AI_XAI_MODEL:-grok-4.5}"
    provider_enabled="${TRADE_MODEL_AI_XAI_ENABLED:-false}"
    ;;
  "")
    emit_result "--" "--" "NOT_CHECKED" "FAIL_INVALID_TARGET" "0"
    exit 2
    ;;
  *)
    emit_result "${target}" "--" "NOT_CHECKED" "FAIL_INVALID_TARGET" "0"
    exit 2
    ;;
esac

if [[ "${TRADE_MODEL_AI_ENABLED:-false}" != "true" || "${provider_enabled}" != "true" ]]; then
  if [[ "${diagnostic_enabled}" == "true" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "UNKNOWN_PROVIDER_ERROR" "NOT_RUN" "0"
  else
    emit_result "${target}" "${model}" "PROVIDER_DISABLED" "SKIPPED_PROVIDER_DISABLED" "0"
  fi
  exit 0
fi

case "${target}" in
  OPENAI) key_present="${OPENAI_API_KEY:+yes}" ;;
  GEMINI) key_present="${GEMINI_API_KEY:+yes}" ;;
  XAI) key_present="${XAI_API_KEY:+yes}" ;;
esac
if [[ "${key_present:-}" != "yes" ]]; then
  if [[ "${diagnostic_enabled}" == "true" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "UNKNOWN_PROVIDER_ERROR" "NOT_RUN" "0"
  else
    emit_result "${target}" "${model}" "MISSING" "SKIPPED_MISSING_API_KEY" "0"
  fi
  exit 0
fi

export TRADE_MODEL_SCHEDULERS_ENABLED=false
export TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED=false
export TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED=false
export TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED=false
export TRADE_MODEL_WATCHLIST_SCHEDULER_ENABLED=false
export TRADE_MODEL_ANALYSIS_SCHEDULER_ENABLED=false
export TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED=false
export TRADE_MODEL_AI_MAX_OUTPUT_TOKENS=128
export TRADE_MODEL_AI_REQUEST_TIMEOUT_MS=15000
export TRADE_MODEL_AI_OVERALL_TIMEOUT_MS=15000
export AI_PROVIDER_SMOKE_HARNESS_ENTRY=I_CONFIRM_SINGLE_PROVIDER_SMOKE

output_file="$(mktemp)"
trap 'rm -f "${output_file}"' EXIT

./mvnw -q \
  -Dtest=AiProviderControlledSmokeTest#controlledLiveSmokeEntryPoint \
  test >"${output_file}" 2>&1 &
smoke_pid=$!
(
  sleep 60
  kill -TERM "${smoke_pid}" 2>/dev/null || true
) &
watchdog_pid=$!

if wait "${smoke_pid}"; then
  smoke_exit=0
else
  smoke_exit=$?
fi
kill "${watchdog_pid}" 2>/dev/null || true
wait "${watchdog_pid}" 2>/dev/null || true

if [[ "${diagnostic_enabled}" == "true" ]]; then
  allowed_output="$(awk '/^(AI_PROVIDER|AI_DIAGNOSTIC_MODE|AI_HTTP_STATUS_CLASS|AI_ERROR_CATEGORY|AI_RESPONSE_PARSE_STATUS|AI_LATENCY_MS|LIVE_PROVIDER_CALLS|PRODUCTION_READINESS|GEMINI_REQUEST_DIAGNOSTIC|MODEL|RESPONSE_MIME_TYPE|RESPONSE_SCHEMA_PRESENT|MAX_OUTPUT_TOKENS|TEMPERATURE|SYSTEM_INSTRUCTION_LENGTH|USER_INPUT_LENGTH|STOP_SEQUENCES_PRESENT|TOOLS_PRESENT|GEMINI_EXTRACTION_DIAGNOSTIC_STATUS|CANDIDATES_PRESENT|CANDIDATE_COUNT|CONTENT_PRESENT|PARTS_PRESENT|TEXT_NODE_PRESENT|TEXT_LENGTH|EMPTY_TEXT|EXTRACTED_JSON_PARSE_STATUS|GEMINI_SCHEMA_DIAGNOSTIC_STATUS|GEMINI_EXPECTED_FIELDS|GEMINI_ACTUAL_FIELDS|GEMINI_MISSING_FIELDS|GEMINI_UNEXPECTED_FIELDS|GEMINI_TYPE_MISMATCH): / { print }' "${output_file}")"
else
  allowed_output="$(awk '/^(AI_PROVIDER|AI_MODEL|AI_AUTH_STATUS|AI_HTTP_STATUS_CLASS|AI_ERROR_CATEGORY|AI_PROVIDER_ERROR_REASON|AI_RESPONSE_PARSE_STATUS|AI_TOKEN_USAGE_PRESENT|AI_REQUEST_ID_PRESENT|AI_TIMEOUT_LIMIT_MS|AI_LATENCY_MS|AI_PROVIDER_LIVE_SMOKE|LIVE_PROVIDER_CALLS|REAL_KEYS_READ|PRODUCTION_READINESS|GEMINI_REQUEST_DIAGNOSTIC|MODEL|RESPONSE_MIME_TYPE|RESPONSE_SCHEMA_PRESENT|MAX_OUTPUT_TOKENS|TEMPERATURE|SYSTEM_INSTRUCTION_LENGTH|USER_INPUT_LENGTH|STOP_SEQUENCES_PRESENT|TOOLS_PRESENT|GEMINI_EXTRACTION_DIAGNOSTIC_STATUS|CANDIDATES_PRESENT|CANDIDATE_COUNT|CONTENT_PRESENT|PARTS_PRESENT|TEXT_NODE_PRESENT|TEXT_LENGTH|EMPTY_TEXT|EXTRACTED_JSON_PARSE_STATUS|GEMINI_SCHEMA_DIAGNOSTIC_STATUS|GEMINI_EXPECTED_FIELDS|GEMINI_ACTUAL_FIELDS|GEMINI_MISSING_FIELDS|GEMINI_UNEXPECTED_FIELDS|GEMINI_TYPE_MISMATCH|GEMINI_SCHEMA_DIAGNOSTIC|EXPECTED_FIELDS|ACTUAL_FIELDS|MISSING_FIELDS|UNEXPECTED_FIELDS|TYPE_MISMATCH_FIELDS): / { print }' "${output_file}")"
fi
if [[ "${smoke_exit}" -ne 0 || -z "${allowed_output}" ]]; then
  if [[ "${diagnostic_enabled}" == "true" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "UNKNOWN_PROVIDER_ERROR" "FAIL" "0"
  else
    emit_result "${target}" "${model}" "KEY_PRESENT_NOT_EXPOSED" "FAIL_UNEXPECTED" "0" "1"
  fi
  exit 1
fi

printf '%s\n' "${allowed_output}"
